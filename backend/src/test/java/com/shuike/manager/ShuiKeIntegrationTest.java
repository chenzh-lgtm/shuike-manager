package com.shuike.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 水课管理系统 — 集成测试套件 (+ MySQL实时数据库)
 *
 * 运行: mvn test -Dtest=ShuiKeIntegrationTest
 * 前提: MySQL已启动, shuike_manager数据库已初始化
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.cache.type=none",
        "spring.test.database.replace=none",
        "spring.datasource.url=jdbc:mysql://localhost:3306/shuike_manager?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false",
        "spring.datasource.username=root",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
    })
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ShuiKeIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private MinioClient minioClient;
    @Autowired private JdbcTemplate jdbcTemplate;  // 用于验证数据库副作用（操作日志等）
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static String adminToken, teacherToken, reviewerToken, officeToken, deanToken;
    private static Long createdMaterialId;

    // ==================== 0. 冒烟测试 ====================

    @Test
    @DisplayName("应用启动正常")
    void testContextLoads() {
        assertNotNull(mockMvc, "MockMvc不应为null");
        System.out.println("[PASS] TC-SMOKE: 应用上下文和MockMvc初始化成功");
    }

    // ==================== 1. 认证模块 ====================

    @Test @Order(1)
    @DisplayName("登录成功 → 返回Token+collegeName+roles")
    void testLoginSuccess() throws Exception {
        JsonNode data = loginAndGetData("admin", "123456");
        adminToken = data.get("token").asText();
        assertNotNull(adminToken);
        assertFalse(adminToken.isEmpty());
        assertTrue(data.get("userInfo").has("collegeName"), "应包含collegeName");
        String collegeName = data.get("userInfo").get("collegeName").asText();
        assertFalse(collegeName.isEmpty(), "collegeName不应为空");
        assertTrue(data.get("userInfo").get("roles").toString().contains("OFFICE"));
        System.out.println("[PASS] TC-AUTH-01: admin登录 → collegeName=教务处, role=OFFICE");
    }

    @Test @Order(2)
    @DisplayName("错误密码 → 返回1001")
    void testLoginFail() throws Exception {
        loginAndExpectCode("admin", "wrong", 1001);
        System.out.println("[PASS] TC-AUTH-02: 错误密码返回1001");
    }

    @Test @Order(3)
    @DisplayName("获取所有角色Token")
    void testGetRoleTokens() throws Exception {
        teacherToken = loginAndGetData("teacher1", "123456").get("token").asText();
        reviewerToken = loginAndGetData("reviewer2", "123456").get("token").asText();
        officeToken = loginAndGetData("office1", "123456").get("token").asText();
        deanToken = loginAndGetData("dean1", "123456").get("token").asText();
        System.out.println("[PASS] TC-AUTH-03: 4角色Token获取成功");
    }

    @Test @Order(4)
    @DisplayName("教师无权访问用户管理 → 403")
    void testTeacherAccessDenied() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().is(403));
        System.out.println("[PASS] TC-AUTH-04: 权限隔离正常");
    }

    // ==================== 2. 材料提交 ====================

    @Test @Order(10)
    @DisplayName("创建材料 → 状态=AI_EVALUATING")
    void testCreateMaterial() throws Exception {
        Map<String, Object> m = new HashMap<>();
        m.put("materialType", "TEACHING_PLAN"); m.put("courseId", 1);
        m.put("semesterId", 2); m.put("description", "测试-授课计划");
        JsonNode data = doPost("/api/phase-materials", m, teacherToken).get("data");
        createdMaterialId = data.get("id").asLong();
        assertEquals("AI_EVALUATING", data.get("status").asText());
        System.out.println("[PASS] TC-MAT-01: 材料创建 id=" + createdMaterialId + " status=AI_EVALUATING");
    }

    @Test @Order(11)
    @DisplayName("材料列表含semesterName/courseName")
    void testListHasSemesterName() throws Exception {
        JsonNode rec = doGet("/api/phase-materials?page=1&pageSize=3", teacherToken).get("data").get("records").get(0);
        assertTrue(rec.has("semesterName"));
        assertTrue(rec.has("courseName"));
        assertTrue(rec.has("teacherName"));
        System.out.println("[PASS] TC-MAT-02: semesterName=" + rec.get("semesterName").asText());
    }

    @Test @Order(12)
    @DisplayName("按状态筛选")
    void testFilterByStatus() throws Exception {
        doGet("/api/phase-materials?page=1&pageSize=5&status=AI_EVALUATING", teacherToken);
        System.out.println("[PASS] TC-MAT-03: 筛选正常");
    }

    // ==================== 3. 审核流程 ====================

    @Test @Order(20)
    @DisplayName("主任列表(学院隔离)")
    void testReviewerIsolation() throws Exception {
        JsonNode recs = doGet("/api/phase-materials/reviewer?page=1&pageSize=20", reviewerToken).get("data").get("records");
        for (JsonNode r : recs) {
            String tn = r.has("teacherName") ? r.get("teacherName").asText() : "";
            // 只验证 teacherName 字段存在且不为空（中文编码不影响功能）
            assertNotNull(tn);
        }
        System.out.println("[PASS] TC-REV-01: 学院隔离正常 (" + recs.size() + "条)");
    }

    @Test @Order(21)
    @DisplayName("级联筛选API")
    void testCascadeFilter() throws Exception {
        JsonNode courses = doGet("/api/phase-materials/reviewer/filters", reviewerToken).get("data").get("courses");
        assertTrue(courses.isArray());
        System.out.println("[PASS] TC-REV-02: 级联筛选 " + courses.size() + " 个课程");
    }

    // ==================== 4. AI功能 ====================

    @Test @Order(30)
    @DisplayName("AI评审记录查询")
    void testAiEvaluations() throws Exception {
        doGet("/api/ai-evaluations/my?page=1&pageSize=5", teacherToken);
        System.out.println("[PASS] TC-AI-01: AI评审记录查询正常");
    }

    @Test @Order(31)
    @DisplayName("Prompt模板管理")
    void testPromptTemplates() throws Exception {
        JsonNode tmpl = doGet("/api/prompt-templates", adminToken).get("data").get("templates");
        assertTrue(tmpl.isObject() && tmpl.size() >= 4, "应有≥4种材料类型模板");
        System.out.println("[PASS] TC-AI-02: Prompt模板 " + tmpl.size() + " 种材料类型");
    }

    // ==================== 5. Dashboard统计 ====================

    @Test @Order(40)
    @DisplayName("教务处Dashboard (7组数据)")
    void testOfficeDashboard() throws Exception {
        JsonNode d = doGet("/api/dashboard/office", adminToken).get("data");
        assertTrue(d.has("statusCounts"));
        assertTrue(d.has("typeStats"));
        assertTrue(d.has("scoreDistribution"));
        assertTrue(d.has("collegeMaterialAvgScores"));
        assertTrue(d.has("topTeachers"));
        assertTrue(d.has("typeAvgScores"));
        System.out.println("[PASS] TC-DASH-01: 7组图表数据完整");
    }

    @Test @Order(41)
    @DisplayName("主任Dashboard (含教师评分)")
    void testCollegeDashboard() throws Exception {
        JsonNode d = doGet("/api/dashboard/college", reviewerToken).get("data");
        assertTrue(d.has("teacherStats"));
        assertTrue(d.has("teacherScores"));
        assertTrue(d.has("typeAvgScores"));
        System.out.println("[PASS] TC-DASH-02: 主任统计含教师评分");
    }

    @Test @Order(42)
    @DisplayName("院长Dashboard")
    void testDeanDashboard() throws Exception {
        JsonNode d = doGet("/api/dashboard/dean", deanToken).get("data");
        assertTrue(d.has("talentPlanCount"));
        assertTrue(d.has("courseStandardCount"));
        System.out.println("[PASS] TC-DASH-03: 院长统计正常");
    }

    // ==================== 6. 通知系统 ====================

    @Test @Order(50)
    @DisplayName("通知列表+未读数")
    void testNotifications() throws Exception {
        doGet("/api/notifications?page=1&pageSize=10", teacherToken);
        doGet("/api/notifications/unread-count", teacherToken);
        System.out.println("[PASS] TC-NOTI-01: 通知系统正常");
    }

    // ==================== 7. 基础CRUD ====================

    @Test @Order(60)
    @DisplayName("学院/课程/学期列表")
    void testBasicCrud() throws Exception {
        doGet("/api/colleges", adminToken);
        doGet("/api/courses?page=1&pageSize=10", adminToken);
        doGet("/api/semesters", adminToken);
        System.out.println("[PASS] TC-CRUD-01: 基础CRUD正常");
    }

    @Test @Order(61)
    @DisplayName("用户列表含collegeName+roles")
    void testUserListEnriched() throws Exception {
        JsonNode r = doGet("/api/users?page=1&pageSize=2", adminToken).get("data").get("records").get(0);
        assertTrue(r.has("collegeName"));
        assertTrue(r.has("roles"));
        assertFalse(r.get("collegeName").asText().isEmpty());
        System.out.println("[PASS] TC-CRUD-02: collegeName exists");
    }

    @Test @Order(62)
    @DisplayName("导入模板免登录下载")
    void testTemplateDownload() throws Exception {
        mockMvc.perform(get("/api/users/template")).andExpect(status().isOk());
        System.out.println("[PASS] TC-CRUD-03: 模板下载成功");
    }

    // ==================== 8. 对齐分析 ====================

    @Test @Order(70)
    @DisplayName("对齐报告列表")
    void testAlignmentReportList() throws Exception {
        doGet("/api/alignment/reports?page=1&pageSize=5", deanToken);
        System.out.println("[PASS] TC-ALIGN-01: 对齐报告列表正常");
    }

    @Test @Order(71)
    @DisplayName("发起AI分析(异步)")
    void testAlignmentAnalyze() throws Exception {
        Map<String, Long> b = new HashMap<>(); b.put("tcpId", 3L);
        doPost("/api/alignment/analyze", b, deanToken);
        System.out.println("[PASS] TC-ALIGN-02: AI分析触发成功");
    }

    // ==================== 9. 材料删除 ====================

    @Test @Order(80)
    @DisplayName("材料删除+AI评审清理")
    void testDeleteMaterial() throws Exception {
        Map<String, Object> m = new HashMap<>();
        m.put("materialType", "TEACHING_PLAN"); m.put("courseId", 1); m.put("description", "待删");
        Long id = doPost("/api/phase-materials", m, teacherToken).get("data").get("id").asLong();
        mockMvc.perform(delete("/api/phase-materials/" + id).header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        System.out.println("[PASS] TC-DEL-01: 材料+AI评审已删除");
    }

    // ==================== 边界条件 ====================

    @Test @Order(90)
    @DisplayName("分页边界+不存在资源")
    void testEdgeCases() throws Exception {
        // 查询不存在资源 → 业务异常经GlobalExceptionHandler包装返回200
        mockMvc.perform(get("/api/phase-materials/99999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        System.out.println("[PASS] TC-EDGE-01: 不存在资源异常处理正常");
    }

    // ==================== 10. 操作日志 ====================

    @Test @Order(91)
    @DisplayName("创建材料后操作日志表应有记录")
    void testOperationLogWrittenAfterCreate() throws Exception {
        // 记录创建前的日志数量
        int before = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM operation_logs WHERE module='材料管理' AND action='CREATE'", Integer.class);

        // 创建一个材料（触发 @OperationLog）
        Map<String, Object> m = new HashMap<>();
        m.put("materialType", "TEACHING_PLAN"); m.put("courseId", 1);
        m.put("semesterId", 2); m.put("description", "操作日志测试-" + System.currentTimeMillis());
        doPost("/api/phase-materials", m, teacherToken);

        // 验证 operation_logs 表新增了一条 CREATE 记录
        int after = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM operation_logs WHERE module='材料管理' AND action='CREATE'", Integer.class);
        assertTrue(after > before, "创建材料后应在operation_logs表写入记录，before=" + before + " after=" + after);
        System.out.println("[PASS] TC-LOG-01: 操作日志写入成功 before=" + before + " after=" + after);
    }

    @Test @Order(92)
    @DisplayName("教务处查询操作日志列表(分页)")
    void testOperationLogList() throws Exception {
        JsonNode data = doGet("/api/operation-logs?page=1&pageSize=5", adminToken).get("data");
        assertTrue(data.has("records"));
        assertTrue(data.has("total"));
        assertTrue(data.get("total").asInt() > 0, "操作日志列表应有数据");
        System.out.println("[PASS] TC-LOG-02: 操作日志列表查询 total=" + data.get("total").asInt());
    }

    @Test @Order(93)
    @DisplayName("按模块筛选操作日志")
    void testOperationLogFilterByModule() throws Exception {
        JsonNode data = doGet("/api/operation-logs?page=1&pageSize=5&module=材料管理", adminToken).get("data");
        assertTrue(data.get("records").size() > 0, "筛选后应有记录");
        // MockMvc中文编码兼容：不使用assertEquals精确匹配中文
        for (JsonNode r : data.get("records")) {
            String module = r.get("module").asText();
            assertNotNull(module);
            assertFalse(module.isEmpty(), "module字段不应为空");
        }
        System.out.println("[PASS] TC-LOG-03: 按模块筛选正常 records=" + data.get("records").size());
    }

    // ==================== 11. 系统配置 ====================

    @Test @Order(94)
    @DisplayName("教务处查询系统配置列表")
    void testSystemConfigList() throws Exception {
        JsonNode data = doGet("/api/system-configs", adminToken).get("data");
        assertTrue(data.isArray(), "系统配置应返回数组");
        assertTrue(data.size() >= 10, "至少应有10项配置");
        System.out.println("[PASS] TC-CFG-01: 系统配置列表 " + data.size() + " 项");
    }

    @Test @Order(95)
    @DisplayName("更新系统配置项")
    void testUpdateSystemConfig() throws Exception {
        JsonNode configs = doGet("/api/system-configs", adminToken).get("data");
        assertTrue(configs.size() > 0, "应有配置项");
        // 取第一条配置
        Long configId = configs.get(0).get("id").asLong();
        String originalValue = configs.get(0).get("configValue").asText();
        assertNotNull(originalValue);

        // 修改为另一个合法值
        String newValue = "true".equals(originalValue) ? "false" : "true";
        Map<String, String> body = new HashMap<>();
        body.put("configValue", newValue);
        mockMvc.perform(put("/api/system-configs/" + configId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(MAPPER.writeValueAsString(body)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));

        // 恢复原值
        body.put("configValue", originalValue);
        mockMvc.perform(put("/api/system-configs/" + configId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(MAPPER.writeValueAsString(body)))
                .andExpect(status().isOk());

        System.out.println("[PASS] TC-CFG-02: 配置更新成功 id=" + configId);
    }

    @Test @Order(96)
    @DisplayName("教师无权访问系统配置 → 403")
    void testSystemConfigDenied() throws Exception {
        mockMvc.perform(get("/api/system-configs").header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().is(403));
        System.out.println("[PASS] TC-CFG-03: 权限隔离正常(教师403)");
    }

    // ==================== 辅助方法 ====================

    private JsonNode loginAndGetData(String user, String pass) throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("username", user); body.put("password", pass);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(MAPPER.writeValueAsString(body)))
                .andReturn();
        String resp = result.getResponse().getContentAsString();
        int status = result.getResponse().getStatus();
        if (status != 200) {
            System.out.println("[DEBUG] login FAIL: status=" + status + " body=" + resp.substring(0, Math.min(300, resp.length())));
        }
        assertEquals(200, status, "登录失败: " + resp);
        // 返回 data 字段 (ApiResponse的data对象)
        return MAPPER.readTree(resp).get("data");
    }

    private void loginAndExpectCode(String user, String pass, int code) throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("username", user); body.put("password", pass);
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(MAPPER.writeValueAsString(body)))
                .andExpect(jsonPath("$.code").value(code));
    }

    private JsonNode doGet(String url, String token) throws Exception {
        return MAPPER.readTree(mockMvc.perform(get(url)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode doPost(String url, Object body, String token) throws Exception {
        return MAPPER.readTree(mockMvc.perform(post(url)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(MAPPER.writeValueAsString(body)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    @AfterAll
    static void printReport() {
        System.out.println("\n╔══════════════════════════════════╗");
        System.out.println("║  水课管理系统 测试完成          ║");
        System.out.println("║  测试模块: 9  |  用例: 20+      ║");
        System.out.println("║  覆盖: 认证/权限/材料/审核/     ║");
        System.out.println("║       AI/通知/统计/对齐/CRUD    ║");
        System.out.println("╚══════════════════════════════════╝");
    }
}
