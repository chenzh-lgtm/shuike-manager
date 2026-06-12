package com.shuike.manager.modules.dashboard.service;

import com.shuike.manager.common.security.SecurityUtils;
import com.shuike.manager.modules.teachingplan.entity.TeachingPlan;
import com.shuike.manager.modules.teachingplan.mapper.TeachingPlanMapper;
import com.shuike.manager.modules.aievaluation.entity.AiEvaluation;
import com.shuike.manager.modules.aievaluation.mapper.AiEvaluationMapper;
import com.shuike.manager.modules.talentplan.entity.TalentCultivationPlan;
import com.shuike.manager.modules.talentplan.mapper.TalentCultivationPlanMapper;
import com.shuike.manager.modules.coursestandard.entity.CourseStandard;
import com.shuike.manager.modules.coursestandard.mapper.CourseStandardMapper;
import com.shuike.manager.modules.phasematerial.entity.PhaseMaterial;
import com.shuike.manager.modules.phasematerial.mapper.PhaseMaterialMapper;
import com.shuike.manager.modules.college.entity.College;
import com.shuike.manager.modules.college.mapper.CollegeMapper;
import com.shuike.manager.modules.user.entity.User;
import com.shuike.manager.modules.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final TeachingPlanMapper planMapper;
    private final AiEvaluationMapper evalMapper;
    private final PhaseMaterialMapper materialMapper;
    private final CollegeMapper collegeMapper;
    private final TalentCultivationPlanMapper talentPlanMapper;
    private final CourseStandardMapper courseStandardMapper;
    private final UserMapper userMapper;

    public Map<String, Object> getTeacherStats() {
        Long userId = SecurityUtils.getCurrentUserId();
        Map<String, Object> stats = new HashMap<>();
        stats.put("pendingModify", planMapper.selectCount(new LambdaQueryWrapper<TeachingPlan>().eq(TeachingPlan::getTeacherId, userId).eq(TeachingPlan::getStatus, "REJECTED")));
        stats.put("pendingCollegeReview", planMapper.selectCount(new LambdaQueryWrapper<TeachingPlan>().eq(TeachingPlan::getTeacherId, userId).eq(TeachingPlan::getStatus, "SUBMITTED")));
        stats.put("pendingOfficeReview", planMapper.selectCount(new LambdaQueryWrapper<TeachingPlan>().eq(TeachingPlan::getTeacherId, userId).eq(TeachingPlan::getStatus, "COLLEGE_PASSED")));
        stats.put("approved", planMapper.selectCount(new LambdaQueryWrapper<TeachingPlan>().eq(TeachingPlan::getTeacherId, userId).eq(TeachingPlan::getStatus, "APPROVED")));
        return stats;
    }

    public Map<String, Object> getCollegeStats() {
        Long userId = SecurityUtils.getCurrentUserId();
        User currentUser = userMapper.selectById(userId);
        Long collegeId = currentUser != null ? currentUser.getCollegeId() : null;

        Map<String, Object> stats = new LinkedHashMap<>();

        // 获取本院所有教师的ID列表
        List<User> collegeTeachers = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(collegeId != null, User::getCollegeId, collegeId));
        Set<Long> teacherIds = new HashSet<>();
        Map<Long, String> teacherNames = new HashMap<>();
        for (User u : collegeTeachers) {
            teacherIds.add(u.getId());
            teacherNames.put(u.getId(), u.getRealName());
        }

        // 获取本院所有材料
        List<PhaseMaterial> allMaterials = materialMapper.selectList(null);
        List<PhaseMaterial> collegeMaterials = new ArrayList<>();
        for (PhaseMaterial m : allMaterials) {
            if (teacherIds.contains(m.getTeacherId())) {
                collegeMaterials.add(m);
            }
        }

        // ===== 1. KPI汇总 =====
        long pendingReview = 0, reviewed = 0, rejected = 0;
        for (PhaseMaterial m : collegeMaterials) {
            if ("AI_COMPLETED".equals(m.getStatus())) pendingReview++;
            else if ("OFFICE_APPROVED".equals(m.getStatus()) || "COLLEGE_APPROVED".equals(m.getStatus())) reviewed++;
            else if ("AI_REJECTED".equals(m.getStatus())) rejected++;
        }
        stats.put("totalMaterials", (long) collegeMaterials.size());
        stats.put("pendingReview", pendingReview);
        stats.put("reviewed", reviewed);
        stats.put("rejected", rejected);

        // ===== 2. 每种材料类型审核状态 =====
        String[] types = {"TEACHING_PLAN","LESSON_PLAN","COURSEWARE","EXAM_PLAN"};
        String[] typeLabels = {"授课计划","教案","课件","考核方案"};
        String[] statusKeys = {"AI_COMPLETED","COLLEGE_APPROVED","OFFICE_APPROVED","AI_REJECTED"};
        String[] statusLabels = {"待主任审核","待教务处审核","已通过","已驳回"};
        List<Map<String,Object>> typeStats = new ArrayList<>();
        for (int i = 0; i < types.length; i++) {
            Map<String,Object> entry = new LinkedHashMap<>();
            entry.put("type", types[i]); entry.put("name", typeLabels[i]);
            List<Integer> vals = new ArrayList<>();
            for (String sk : statusKeys) {
                int cnt = 0;
                for (PhaseMaterial m : collegeMaterials) {
                    if (types[i].equals(m.getMaterialType()) && sk.equals(m.getStatus())) cnt++;
                }
                vals.add(cnt);
            }
            entry.put("values", vals);
            typeStats.add(entry);
        }
        stats.put("typeStats", typeStats);
        stats.put("statusLabels", statusLabels);

        // ===== 3. 各位教师提交统计 =====
        Map<Long, Integer> teacherSubmitCount = new LinkedHashMap<>();
        for (PhaseMaterial m : collegeMaterials) {
            teacherSubmitCount.merge(m.getTeacherId(), 1, Integer::sum);
        }
        List<Map<String,Object>> teacherStats = new ArrayList<>();
        teacherSubmitCount.entrySet().stream()
            .sorted((a,b) -> b.getValue().compareTo(a.getValue()))
            .forEach(e -> {
                Map<String,Object> entry = new LinkedHashMap<>();
                entry.put("name", teacherNames.getOrDefault(e.getKey(), "教师"+e.getKey()));
                entry.put("count", e.getValue());
                Long tid = e.getKey();
                // 该教师各状态材料数
                Map<String,Integer> st = new LinkedHashMap<>();
                for (PhaseMaterial m : collegeMaterials) {
                    if (tid.equals(m.getTeacherId())) {
                        st.merge(m.getMaterialType(), 1, Integer::sum);
                    }
                }
                entry.put("typeDetails", st);
                teacherStats.add(entry);
            });
        stats.put("teacherStats", teacherStats);

        // ===== 4. AI评分相关统计 =====
        List<AiEvaluation> allEvals = evalMapper.selectList(null);
        Map<Long, Integer> materialEvalMap = new HashMap<>();
        for (AiEvaluation e : allEvals) {
            if (e.getScore() != null) materialEvalMap.put(e.getMaterialId(), e.getScore());
        }

        // 4a. 每位教师的平均分和最高分
        Map<Long, List<Integer>> teacherScores = new LinkedHashMap<>();
        for (PhaseMaterial m : collegeMaterials) {
            Integer score = materialEvalMap.get(m.getId());
            if (score != null) {
                teacherScores.computeIfAbsent(m.getTeacherId(), k -> new ArrayList<>()).add(score);
            }
        }
        List<Map<String,Object>> teacherScoresList = new ArrayList<>();
        for (Map.Entry<Long, List<Integer>> e : teacherScores.entrySet()) {
            Map<String,Object> entry = new LinkedHashMap<>();
            entry.put("name", teacherNames.getOrDefault(e.getKey(), "教师"+e.getKey()));
            List<Integer> scores = e.getValue();
            double avg = scores.stream().mapToInt(Integer::intValue).average().orElse(0);
            int max = scores.stream().mapToInt(Integer::intValue).max().orElse(0);
            int min = scores.stream().mapToInt(Integer::intValue).min().orElse(0);
            entry.put("avg", Math.round(avg * 10.0) / 10.0);
            entry.put("max", max);
            entry.put("min", min);
            entry.put("count", scores.size());
            teacherScoresList.add(entry);
        }
        stats.put("teacherScores", teacherScoresList);

        // 4b. 各材料类型平均分
        List<Map<String,Object>> typeAvgScores = new ArrayList<>();
        for (int i = 0; i < types.length; i++) {
            double sum = 0; int cnt = 0;
            for (PhaseMaterial m : collegeMaterials) {
                if (types[i].equals(m.getMaterialType())) {
                    Integer score = materialEvalMap.get(m.getId());
                    if (score != null) { sum += score; cnt++; }
                }
            }
            Map<String,Object> entry = new LinkedHashMap<>();
            entry.put("type", types[i]); entry.put("name", typeLabels[i]);
            entry.put("avg", cnt > 0 ? Math.round(sum / cnt * 10.0) / 10.0 : 0);
            entry.put("count", cnt);
            typeAvgScores.add(entry);
        }
        stats.put("typeAvgScores", typeAvgScores);

        // 4c. 得分最高材料排行
        List<Map<String,Object>> topMaterials = new ArrayList<>();
        for (PhaseMaterial m : collegeMaterials) {
            Integer score = materialEvalMap.get(m.getId());
            if (score == null) continue;
            Map<String,Object> entry = new LinkedHashMap<>();
            entry.put("id", m.getId());
            entry.put("teacherName", teacherNames.getOrDefault(m.getTeacherId(), "教师"+m.getTeacherId()));
            entry.put("materialType", m.getMaterialType());
            entry.put("score", score);
            topMaterials.add(entry);
        }
        topMaterials.sort((a,b) -> ((Integer)b.get("score")).compareTo((Integer)a.get("score")));
        if (topMaterials.size() > 10) topMaterials = topMaterials.subList(0, 10);
        stats.put("topMaterials", topMaterials);

        // 5. AI评分分布
        int[] scoreBuckets = new int[5];
        for (PhaseMaterial m : collegeMaterials) {
            Integer score = materialEvalMap.get(m.getId());
            if (score == null) continue;
            if (score < 60) scoreBuckets[0]++;
            else if (score < 75) scoreBuckets[1]++;
            else if (score < 85) scoreBuckets[2]++;
            else if (score < 95) scoreBuckets[3]++;
            else scoreBuckets[4]++;
        }
        stats.put("scoreDistribution", scoreBuckets);

        return stats;
    }

    public Map<String, Object> getOfficeStats() {
        Map<String, Object> stats = new HashMap<>();

        // ===== 1. 材料状态总览 =====
        List<PhaseMaterial> allMaterials = materialMapper.selectList(null);
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (PhaseMaterial m : allMaterials) {
            statusCounts.merge(m.getStatus(), 1L, Long::sum);
        }
        stats.put("statusCounts", statusCounts);

        // ===== 2. 每种材料类型 x 状态二维统计 =====
        // TEACHING_PLAN, LESSON_PLAN, COURSEWARE, EXAM_PLAN
        String[] types = {"TEACHING_PLAN", "LESSON_PLAN", "COURSEWARE", "EXAM_PLAN"};
        String[] typeLabels = {"授课计划", "教案", "课件", "考核方案"};
        String[] statusKeys = {"AI_EVALUATING","AI_COMPLETED","COLLEGE_APPROVED","OFFICE_APPROVED","AI_REJECTED"};
        String[] statusLabels = {"AI评审中","待主任审核","待教务处审核","已通过","已驳回"};
        List<Map<String,Object>> typeStats = new ArrayList<>();
        for (int i = 0; i < types.length; i++) {
            Map<String,Object> entry = new LinkedHashMap<>();
            entry.put("type", types[i]); entry.put("name", typeLabels[i]);
            List<Integer> vals = new ArrayList<>();
            for (String sk : statusKeys) {
                long cnt = 0;
                for (PhaseMaterial m : allMaterials) {
                    if (types[i].equals(m.getMaterialType()) && sk.equals(m.getStatus())) cnt++;
                }
                vals.add((int)cnt);
            }
            entry.put("values", vals);
            typeStats.add(entry);
        }
        stats.put("typeStats", typeStats);
        stats.put("statusLabels", statusLabels);

        // ===== 3. AI评分分布 =====
        List<AiEvaluation> allEvals = evalMapper.selectList(null);
        int[] scoreBuckets = new int[5]; // <60, 60-74, 75-84, 85-94, 95-100
        for (AiEvaluation e : allEvals) {
            if (e.getScore() == null) continue;
            int s = e.getScore();
            if (s < 60) scoreBuckets[0]++;
            else if (s < 75) scoreBuckets[1]++;
            else if (s < 85) scoreBuckets[2]++;
            else if (s < 95) scoreBuckets[3]++;
            else scoreBuckets[4]++;
        }
        stats.put("scoreDistribution", scoreBuckets);

        // ===== 4. 学院材料统计 =====
        List<College> colleges = collegeMapper.selectList(null);
        Map<Long, String> collegeNames = new HashMap<>();
        for (College c : colleges) collegeNames.put(c.getId(), c.getName());
        // 按user的collegeId关联
        Map<String, Integer> collegeCounts = new LinkedHashMap<>();
        for (PhaseMaterial m : allMaterials) {
            User u = userMapper.selectById(m.getTeacherId());
            String cn = u != null && u.getCollegeId() != null ? collegeNames.getOrDefault(u.getCollegeId(), "未知") : "未知";
            collegeCounts.merge(cn, 1, Integer::sum);
        }
        stats.put("collegeCounts", collegeCounts);

        // ===== 5. 教师提交统计 Top10 =====
        Map<Long, Integer> teacherCounts = new LinkedHashMap<>();
        Map<Long, String> teacherNames = new HashMap<>();
        for (PhaseMaterial m : allMaterials) {
            User u = userMapper.selectById(m.getTeacherId());
            if (u != null) {
                teacherCounts.merge(m.getTeacherId(), 1, Integer::sum);
                teacherNames.put(m.getTeacherId(), u.getRealName());
            }
        }
        List<Map<String,Object>> topTeachers = new ArrayList<>();
        teacherCounts.entrySet().stream()
            .sorted((a,b) -> b.getValue().compareTo(a.getValue()))
            .limit(10)
            .forEach(e -> {
                Map<String,Object> entry = new HashMap<>();
                entry.put("name", teacherNames.getOrDefault(e.getKey(), "教师"+e.getKey()));
                entry.put("count", e.getValue());
                topTeachers.add(entry);
            });
        stats.put("topTeachers", topTeachers);

        // ===== 6. 按材料类型平均分 =====
        List<Map<String,Object>> typeAvgScores = new ArrayList<>();
        for (int i = 0; i < types.length; i++) {
            double sum = 0; int cnt = 0;
            for (AiEvaluation e : allEvals) {
                if (e.getScore() == null) continue;
                PhaseMaterial pm = materialMapper.selectById(e.getMaterialId());
                if (pm == null) continue;
                if (types[i].equals(pm.getMaterialType())) { sum += e.getScore(); cnt++; }
            }
            Map<String,Object> entry = new LinkedHashMap<>();
            entry.put("type", types[i]); entry.put("name", typeLabels[i]);
            entry.put("avg", cnt > 0 ? Math.round(sum / cnt * 10.0) / 10.0 : 0);
            entry.put("count", cnt);
            typeAvgScores.add(entry);
        }
        stats.put("typeAvgScores", typeAvgScores);

        // ===== 7. 审核流转效率 =====
        long totalReviewed = allMaterials.stream().filter(m -> "OFFICE_APPROVED".equals(m.getStatus())).count();
        long awaitingReview = allMaterials.stream().filter(m -> "COLLEGE_APPROVED".equals(m.getStatus())).count();
        stats.put("totalReviewed", totalReviewed);
        stats.put("awaitingReview", awaitingReview);
        stats.put("totalMaterials", (long) allMaterials.size());

        return stats;
    }

    public Map<String, Object> getDeanStats() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);
        Long collegeId = user != null ? user.getCollegeId() : null;

        Map<String, Object> stats = new HashMap<>();
        // 只统计院长所属学院的人培方案和课程标准
        LambdaQueryWrapper<TalentCultivationPlan> tcpQw = new LambdaQueryWrapper<>();
        if (collegeId != null) tcpQw.eq(TalentCultivationPlan::getCollegeId, collegeId);
        stats.put("talentPlanCount", talentPlanMapper.selectCount(tcpQw));

        LambdaQueryWrapper<CourseStandard> csQw = new LambdaQueryWrapper<>();
        if (collegeId != null) csQw.eq(CourseStandard::getDeanId, userId);
        stats.put("courseStandardCount", courseStandardMapper.selectCount(csQw));

        stats.put("collegeId", collegeId);
        return stats;
    }
}
