package com.shuike.manager.modules.phasematerial.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuike.manager.common.aspect.OperationLog;
import com.shuike.manager.common.response.ApiResponse;
import com.shuike.manager.common.response.PageResult;
import com.shuike.manager.common.security.SecurityUtils;
import com.shuike.manager.modules.aievaluation.entity.AiEvaluation;
import com.shuike.manager.modules.aievaluation.mapper.AiEvaluationMapper;
import com.shuike.manager.modules.manualreview.entity.ManualReview;
import com.shuike.manager.modules.manualreview.mapper.ManualReviewMapper;
import com.shuike.manager.modules.phasematerial.entity.PhaseMaterial;
import com.shuike.manager.modules.phasematerial.entity.PhaseMaterialFile;
import com.shuike.manager.modules.phasematerial.mapper.PhaseMaterialMapper;
import com.shuike.manager.modules.phasematerial.service.PhaseMaterialService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.shuike.manager.modules.course.entity.Course;
import com.shuike.manager.modules.course.mapper.CourseMapper;
import com.shuike.manager.modules.semester.entity.Semester;
import com.shuike.manager.modules.semester.mapper.SemesterMapper;
import com.shuike.manager.modules.user.entity.User;
import com.shuike.manager.modules.user.mapper.UserMapper;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/phase-materials")
@RequiredArgsConstructor
public class PhaseMaterialController {
    private final PhaseMaterialService service;
    private final PhaseMaterialMapper materialMapper;
    private final AiEvaluationMapper aiEvaluationMapper;
    private final ManualReviewMapper manualReviewMapper;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;
    private final SemesterMapper semesterMapper;

    @GetMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        if (page < 1) page = 1;  // 防御性校验
        // keyword搜索时取全量做后置过滤
        long querySize = (keyword != null && !keyword.trim().isEmpty()) ? 500 : pageSize;
        IPage<PhaseMaterial> result = service.page(
                new Page<>(page, querySize), SecurityUtils.getCurrentUserId(), status, null);

        Map<Long, String> courseCache = new HashMap<>();
        Map<Long, String> teacherCache = new HashMap<>();
        Map<Long, String> semesterCache = new HashMap<>();

        List<Map<String, Object>> enriched = result.getRecords().stream().map(m -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.getId()); item.put("teacherId", m.getTeacherId());
            item.put("courseId", m.getCourseId()); item.put("semesterId", m.getSemesterId());
            item.put("materialType", m.getMaterialType());
            item.put("description", m.getDescription()); item.put("status", m.getStatus());
            item.put("submitTime", m.getSubmitTime()); item.put("createdAt", m.getCreatedAt());
            // 学期名称
            item.put("semesterName", semesterCache.computeIfAbsent(m.getSemesterId() != null ? m.getSemesterId() : 0L,
                    k -> { Semester s = k > 0 ? semesterMapper.selectById(k) : null; return s != null ? s.getName() : "-"; }));
            // 文件名（第一个附件）
            List<PhaseMaterialFile> mfiles = service.getFiles(m.getId());
            item.put("fileName", mfiles.isEmpty() ? "" : mfiles.get(0).getFileName());
            // 课程名称
            item.put("courseName", courseCache.computeIfAbsent(m.getCourseId() != null ? m.getCourseId() : 0L,
                    k -> { Course c = k > 0 ? courseMapper.selectById(k) : null; return c != null ? c.getName() : "-"; }));
            // 教师姓名
            item.put("teacherName", teacherCache.computeIfAbsent(m.getTeacherId() != null ? m.getTeacherId() : 0L,
                    k -> { User u = k > 0 ? userMapper.selectById(k) : null; return u != null ? u.getRealName() : "-"; }));
            // 复核状态
            AiEvaluation eval = aiEvaluationMapper.selectOne(new LambdaQueryWrapper<AiEvaluation>()
                    .eq(AiEvaluation::getMaterialId, m.getId())
                    .orderByDesc(AiEvaluation::getCreatedAt).last("LIMIT 1"));
            if (eval != null) {
                item.put("evaluationId", eval.getId());
                List<ManualReview> allReviews = manualReviewMapper.selectList(new LambdaQueryWrapper<ManualReview>()
                        .eq(ManualReview::getEvaluationId, eval.getId())
                        .orderByDesc(ManualReview::getReviewTime));
                // 分别获取主任和教务处的审核记录
                ManualReview collegeReview = allReviews.stream()
                        .filter(r -> r.getReviewerId() != null).findFirst().orElse(null);
                item.put("reviewStageLabel", getStageLabel(m.getStatus()));
                item.put("reviewStage", m.getStatus());
                if (collegeReview != null) {
                    item.put("reviewAction", collegeReview.getAction());
                    item.put("reviewComment", collegeReview.getReviewComment());
                    item.put("revisionRequirements", collegeReview.getRevisionRequirements());
                    item.put("deadline", collegeReview.getDeadline() != null ? collegeReview.getDeadline().toString() : null);
                }
            } else {
                item.put("reviewStageLabel", "AI评审中");
                item.put("reviewStage", m.getStatus());
            }
            return item;
        }).collect(Collectors.toList());

        // keyword过滤
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            enriched = enriched.stream().filter(item -> {
                String cn = String.valueOf(item.getOrDefault("courseName", "")).toLowerCase();
                String tn = String.valueOf(item.getOrDefault("teacherName", "")).toLowerCase();
                String desc = String.valueOf(item.getOrDefault("description", "")).toLowerCase();
                return cn.contains(kw) || tn.contains(kw) || desc.contains(kw);
            }).collect(Collectors.toList());
        }

        long total = enriched.size();
        int start = (int) ((page - 1) * pageSize);
        int end = Math.min(start + (int) pageSize, enriched.size());
        List<Map<String, Object>> paged = start < enriched.size() ? enriched.subList(start, end) : new ArrayList<>();
        return ApiResponse.success(PageResult.of(paged, total, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<PhaseMaterial> detail(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }

    @OperationLog(module = "材料管理", action = "CREATE", targetType = "PHASE_MATERIAL")
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<PhaseMaterial> create(@RequestBody PhaseMaterial material) {
        return ApiResponse.success(service.create(material));
    }

    @OperationLog(module = "材料管理", action = "DELETE", targetType = "PHASE_MATERIAL")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success(null);
    }

    /** 重新提交已驳回的材料 */
    @OperationLog(module = "材料管理", action = "SUBMIT", targetType = "PHASE_MATERIAL")
    @PostMapping("/{id}/resubmit")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<PhaseMaterial> resubmit(@PathVariable Long id) {
        return ApiResponse.success(service.resubmit(id));
    }

    @GetMapping("/{id}/files")
    public ApiResponse<List<PhaseMaterialFile>> files(@PathVariable Long id) {
        return ApiResponse.success(service.getFiles(id));
    }

    /** 材料归档：查所有材料含文件+教师+课程信息 */
    @GetMapping("/archive")
    @PreAuthorize("hasRole('OFFICE')")
    public ApiResponse<PageResult<Map<String,Object>>> archive(
            @RequestParam(defaultValue="1") long page,
            @RequestParam(defaultValue="10") long pageSize,
            @RequestParam(required=false) String materialType,
            @RequestParam(required=false) String keyword,
            @RequestParam(defaultValue="approved") String status) {
        if (page < 1) page = 1;
        // 默认只看已通过的材料
        String archiveStatus = "approved".equals(status) ? "OFFICE_APPROVED" : (status.isEmpty()?null:status);
        IPage<PhaseMaterial> result = service.page(new Page<>(page,pageSize), null, archiveStatus, keyword);
        Map<Long,String> cn=new HashMap<>(), tn=new HashMap<>();
        List<Map<String,Object>> enriched=new ArrayList<>();
        for(PhaseMaterial m:result.getRecords()){
            // 双重过滤：materialType + 状态
            if(archiveStatus!=null&&!archiveStatus.equals(m.getStatus()))continue;
            if(materialType!=null&&!materialType.isEmpty()&&!materialType.equals(m.getMaterialType()))continue;
            Map<String,Object> item=new LinkedHashMap<>();
            item.put("id",m.getId());item.put("teacherId",m.getTeacherId());item.put("courseId",m.getCourseId());
            item.put("materialType",m.getMaterialType());item.put("description",m.getDescription());
            item.put("status",m.getStatus());item.put("submitTime",m.getSubmitTime());
            item.put("courseName",cn.computeIfAbsent(m.getCourseId()!=null?m.getCourseId():0L,
                k->{Course c=k>0?courseMapper.selectById(k):null;return c!=null?c.getName():"-";}));
            item.put("teacherName",tn.computeIfAbsent(m.getTeacherId()!=null?m.getTeacherId():0L,
                k->{User u=k>0?userMapper.selectById(k):null;return u!=null?u.getRealName():"-";}));
            List<PhaseMaterialFile> fs = service.getFiles(m.getId());
            item.put("files", fs);
            item.put("fileCount", fs.size());
            enriched.add(item);
        }
        if(materialType!=null&&!materialType.isEmpty()) enriched=enriched.stream().filter(i->materialType.equals(i.get("materialType"))).collect(Collectors.toList());
        long total=enriched.size();int s=(int)((page-1)*pageSize);int e=Math.min((int)(s+pageSize),enriched.size());
        return ApiResponse.success(PageResult.of(s<enriched.size()?enriched.subList(s,e):new ArrayList<>(),total,page,pageSize));
    }

    @PostMapping("/{id}/files")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Void> addFiles(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        service.addFiles(id, body.get("fileIds"));
        return ApiResponse.success(null);
    }

    /**
     * 获取主任所属学院的课程-教师级联选项
     */
    @GetMapping("/reviewer/filters")
    @PreAuthorize("hasAnyRole('COLLEGE_REVIEWER','OFFICE')")
    public ApiResponse<Map<String, Object>> reviewerFilters() {
        Long userId = SecurityUtils.getCurrentUserId();
        User currentUser = userMapper.selectById(userId);
        Long collegeId = currentUser != null ? currentUser.getCollegeId() : null;

        Map<Long, String> teacherNames = new HashMap<>();
        Map<Long, Set<Long>> courseTeacherMap = new LinkedHashMap<>();

        List<PhaseMaterial> allMaterials = materialMapper.selectList(null);
        for (PhaseMaterial m : allMaterials) {
            User t = userMapper.selectById(m.getTeacherId());
            if (t == null) continue;
            if (collegeId != null && !collegeId.equals(t.getCollegeId())) continue;
            teacherNames.put(m.getTeacherId(), t.getRealName());
            Long cid = m.getCourseId() != null ? m.getCourseId() : 0L;
            courseTeacherMap.computeIfAbsent(cid, k -> new LinkedHashSet<>()).add(m.getTeacherId());
        }

        List<Map<String, Object>> courseOpts = new ArrayList<>();
        for (Map.Entry<Long, Set<Long>> e : courseTeacherMap.entrySet()) {
            Map<String, Object> ci = new LinkedHashMap<>();
            ci.put("id", e.getKey());
            ci.put("name", e.getKey() == 0 ? "未关联课程" : (courseMapper.selectById(e.getKey()) != null ? courseMapper.selectById(e.getKey()).getName() : "课程"+e.getKey()));
            List<Map<String, Object>> teachers = new ArrayList<>();
            for (Long tid : e.getValue()) {
                Map<String, Object> ti = new LinkedHashMap<>();
                ti.put("id", tid); ti.put("name", teacherNames.getOrDefault(tid, "教师"+tid));
                teachers.add(ti);
            }
            ci.put("teachers", teachers);
            courseOpts.add(ci);
        }
        courseOpts.sort(Comparator.comparing(o -> (String) o.get("name")));

        Map<String, Object> result = new HashMap<>();
        result.put("courses", courseOpts);
        return ApiResponse.success(result);
    }

    // 学院审核员/OFFICE：查看材料列表（含AI评分），按学院隔离
    @GetMapping("/reviewer")
    @PreAuthorize("hasAnyRole('COLLEGE_REVIEWER','OFFICE')")
    public ApiResponse<PageResult<Map<String, Object>>> reviewerList(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Integer aiScoreMin,
            @RequestParam(required = false) Integer aiScoreMax) {
        // 获取当前用户的学院ID（用于COLLEGE_REVIEWER隔离）
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userMapper.selectById(currentUserId);
        Long userCollegeId = currentUser != null ? currentUser.getCollegeId() : null;
        List<String> currentRoles = SecurityUtils.getCurrentUserRoles();
        boolean isOfficeUser = currentRoles.contains("OFFICE");

        long querySize = (keyword != null && !keyword.trim().isEmpty()) ? 500 : pageSize;
        IPage<PhaseMaterial> result = service.pageForReviewer(new Page<>(page, querySize), status);
        // 预加载缓存
        Map<Long, String> courseCache = new HashMap<>();
        Map<Long, String> teacherCache = new HashMap<>();
        Map<Long, Long> teacherCollegeCache = new HashMap<>();

        List<Map<String, Object>> enriched = result.getRecords().stream().map(m -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.getId()); item.put("teacherId", m.getTeacherId());
            item.put("courseId", m.getCourseId()); item.put("semesterId", m.getSemesterId());
            item.put("materialType", m.getMaterialType());
            item.put("description", m.getDescription()); item.put("status", m.getStatus());
            item.put("submitTime", m.getSubmitTime()); item.put("createdAt", m.getCreatedAt());
            // 课程名称
            item.put("courseName", courseCache.computeIfAbsent(m.getCourseId() != null ? m.getCourseId() : 0L,
                    k -> { Course c = k > 0 ? courseMapper.selectById(k) : null; return c != null ? c.getName() : "-"; }));
            // 教师姓名
            item.put("teacherName", teacherCache.computeIfAbsent(m.getTeacherId() != null ? m.getTeacherId() : 0L,
                    k -> { User u = k > 0 ? userMapper.selectById(k) : null; return u != null ? u.getRealName() : "-"; }));

            // AI评审
            AiEvaluation eval = aiEvaluationMapper.selectOne(new LambdaQueryWrapper<AiEvaluation>()
                    .eq(AiEvaluation::getMaterialId, m.getId())
                    .orderByDesc(AiEvaluation::getCreatedAt).last("LIMIT 1"));
            if (eval != null) {
                item.put("aiScore", eval.getScore());
                item.put("dimensionScores", eval.getDimensionScores());
                item.put("suggestions", eval.getSuggestions());
                item.put("aiStatus", eval.getStatus());
                item.put("evalTime", eval.getEvalTime());
                item.put("evaluationId", eval.getId());

                ManualReview review = manualReviewMapper.selectOne(new LambdaQueryWrapper<ManualReview>()
                        .eq(ManualReview::getEvaluationId, eval.getId())
                        .orderByDesc(ManualReview::getReviewTime).last("LIMIT 1"));
                if (review != null) {
                    item.put("reviewAction", review.getAction());
                    item.put("reviewComment", review.getReviewComment());
                }
            }
            return item;
        }).collect(Collectors.toList());

        // 主任只能看本院教师的材料
        if (!isOfficeUser && userCollegeId != null) {
            enriched = enriched.stream().filter(item -> {
                Long tId = (Long) item.get("teacherId");
                if (tId == null) return false;
                Long tcId = teacherCollegeCache.computeIfAbsent(tId, k -> {
                    User u = userMapper.selectById(k);
                    return u != null ? u.getCollegeId() : null;
                });
                return userCollegeId.equals(tcId);
            }).collect(Collectors.toList());
        }

        // 按课程ID过滤
        if (courseId != null && courseId > 0) {
            enriched = enriched.stream()
                    .filter(item -> courseId.equals(item.get("courseId")))
                    .collect(Collectors.toList());
        }

        // 按教师ID过滤
        if (teacherId != null && teacherId > 0) {
            enriched = enriched.stream()
                    .filter(item -> teacherId.equals(item.get("teacherId")))
                    .collect(Collectors.toList());
        }

        // 按AI评分区间过滤
        if (aiScoreMin != null) {
            enriched = enriched.stream()
                    .filter(item -> { Object sc = item.get("aiScore"); return sc instanceof Number && ((Number)sc).intValue() >= aiScoreMin; })
                    .collect(Collectors.toList());
        }
        if (aiScoreMax != null) {
            enriched = enriched.stream()
                    .filter(item -> { Object sc = item.get("aiScore"); return sc instanceof Number && ((Number)sc).intValue() <= aiScoreMax; })
                    .collect(Collectors.toList());
        }

        // keyword 搜索过滤
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            enriched = enriched.stream().filter(item -> {
                String cn = String.valueOf(item.getOrDefault("courseName", "")).toLowerCase();
                String tn = String.valueOf(item.getOrDefault("teacherName", "")).toLowerCase();
                String desc = String.valueOf(item.getOrDefault("description", "")).toLowerCase();
                return cn.contains(kw) || tn.contains(kw) || desc.contains(kw);
            }).collect(Collectors.toList());
        }

        long total = enriched.size();
        int start = (int) ((page - 1) * pageSize);
        int end = Math.min(start + (int) pageSize, enriched.size());
        List<Map<String, Object>> paged = start < enriched.size() ? enriched.subList(start, end) : new ArrayList<>();
        return ApiResponse.success(PageResult.of(paged, total, page, pageSize));
    }

    /** 批量审核（主任端勾选通过） */
    @OperationLog(module = "审核管理", action = "REVIEW", targetType = "PHASE_MATERIAL")
    @PostMapping("/reviewer/batch-review")
    @PreAuthorize("hasRole('COLLEGE_REVIEWER')")
    public ApiResponse<Void> batchReview(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) body.get("ids");
        if (ids == null || ids.isEmpty()) return ApiResponse.error(400, "请选择材料");
        String action = (String) body.getOrDefault("action", "CONFIRM");
        for (Integer id : ids) {
            AiEvaluation eval = aiEvaluationMapper.selectOne(new LambdaQueryWrapper<AiEvaluation>()
                    .eq(AiEvaluation::getMaterialId, id.longValue()).orderByDesc(AiEvaluation::getCreatedAt).last("LIMIT 1"));
            if (eval != null && "COMPLETED".equals(eval.getStatus())) {
                ManualReview review = new ManualReview();
                review.setEvaluationId(eval.getId()); review.setReviewerId(SecurityUtils.getCurrentUserId());
                review.setAction(action); review.setReviewComment("批量审核");
                review.setReviewTime(java.time.LocalDateTime.now());
                manualReviewMapper.insert(review);
                PhaseMaterial m = materialMapper.selectById(id.longValue());
                if (m != null) { m.setStatus("REJECT".equals(action) ? "AI_REJECTED" : "COLLEGE_APPROVED"); materialMapper.updateById(m); }
            }
        }
        return ApiResponse.success("批量审核完成", null);
    }

    /** 导出已通过材料为Excel（主任只导出本院，教务处导出全部） */
    @GetMapping("/export/approved")
    @PreAuthorize("hasAnyRole('OFFICE','COLLEGE_REVIEWER')")
    public ResponseEntity<byte[]> exportApproved(
            @RequestParam(required = false) String materialType,
            @RequestParam(required = false) Long courseId) {
        // 主任只能导出本院教师的数据
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userMapper.selectById(currentUserId);
        List<String> currentRoles = SecurityUtils.getCurrentUserRoles();
        boolean isReviewer = currentRoles.contains("COLLEGE_REVIEWER") && !currentRoles.contains("OFFICE");

        List<PhaseMaterial> materials = materialMapper.selectList(new LambdaQueryWrapper<PhaseMaterial>()
                .eq(PhaseMaterial::getStatus, "OFFICE_APPROVED")
                .eq(materialType != null, PhaseMaterial::getMaterialType, materialType));
        Map<Long, String> cn = new HashMap<>(), tn = new HashMap<>();
        Map<Long, Long> tc = new HashMap<>();
        StringBuilder sb = new StringBuilder("﻿"); // BOM for Excel UTF8
        sb.append("编号,教师,课程,材料类型,描述,提交时间\n");
        for (PhaseMaterial m : materials) {
            Long teacherCollegeId = tc.computeIfAbsent(m.getTeacherId(), k -> {
                User u = userMapper.selectById(k); return u != null ? u.getCollegeId() : null; });
            // 主任只导出本院
            if (isReviewer && (teacherCollegeId == null || !teacherCollegeId.equals(currentUser.getCollegeId()))) continue;
            String courseName = cn.computeIfAbsent(m.getCourseId(), k -> {
                Course c = courseMapper.selectById(k); return c != null ? c.getName() : "-"; });
            String teacherName = tn.computeIfAbsent(m.getTeacherId(), k -> {
                User u = userMapper.selectById(k); return u != null ? u.getRealName() : "-"; });
            if (courseId != null && !courseId.equals(m.getCourseId())) continue;
            sb.append(m.getId()).append(",").append(teacherName).append(",").append(courseName).append(",")
              .append(m.getMaterialType()).append(",").append(m.getDescription() != null ? m.getDescription().replace(",", "，") : "-")
              .append(",").append(m.getSubmitTime()).append("\n");
        }
        byte[] bytes = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok().header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=approved_materials.csv").header(org.springframework.http.HttpHeaders.CONTENT_TYPE,
                "text/csv; charset=UTF-8").body(bytes);
    }

    private String getStageLabel(String status) {
        switch (status) {
            case "AI_EVALUATING": return "AI评审中";
            case "AI_COMPLETED":  return "待专业主任审核";
            case "COLLEGE_APPROVED": return "待教务处审核";
            case "OFFICE_APPROVED": return "已通过";
            case "AI_REJECTED": return "已驳回";
            case "AI_CONFIRMED": return "待教务处审核";  // 旧状态兼容
            default: return status;
        }
    }
}
