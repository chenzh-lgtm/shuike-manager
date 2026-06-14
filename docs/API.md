# 水课管理系统 — API 接口文档

> **版本**: v1.0 | **基础URL**: `<ECS_IP>/api` | **协议**: REST JSON | **认证**: JWT Bearer Token

---

## 通用规范

### 响应格式

所有接口统一封装在 `ApiResponse<T>` 中：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1781445888859
}
```

### 错误码

| Code | 含义 |
|:--|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未认证 / Token 过期 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 1001 | 密码错误 |
| 4001 | AI 服务不可用 |
| 4003 | AI 结果解析失败 |

### 分页格式

```json
{
  "records": [...],
  "total": 100,
  "page": 1,
  "pageSize": 10
}
```

### 认证方式

请求头携带 Token：
```
Authorization: Bearer <token>
```

通过 `/api/auth/login` 获取 Token，有效期 7200 秒（2小时），过期后调用 `/api/auth/refresh` 刷新。

---

## 1. 认证模块 `/api/auth`

### 1.1 登录

```
POST /api/auth/login
```

**请求体**：
```json
{
  "username": "office1",
  "password": "123456"
}
```

**响应**：
```json
{
  "data": {
    "token": "eyJhbG...",
    "refreshToken": "eyJhbG...",
    "expiresIn": 7200,
    "userInfo": {
      "id": 7,
      "username": "office1",
      "realName": "孙教务",
      "collegeName": "教务处",
      "collegeId": 4,
      "roles": ["OFFICE"]
    }
  }
}
```

| 字段 | 说明 |
|------|------|
| `token` | JWT 访问令牌，2 小时有效 |
| `refreshToken` | 刷新令牌，7 天有效 |
| `expiresIn` | Token 有效期（秒） |
| `roles` | 可选值：`TEACHER`（教师）、`COLLEGE_REVIEWER`（专业主任）、`OFFICE`（教务处）、`DEAN`（院长） |

### 1.2 刷新 Token

```
POST /api/auth/refresh
```

**请求体**：
```json
{
  "refreshToken": "eyJhbG..."
}
```

### 1.3 修改密码

```
PUT /api/auth/password
```

**请求体**：
```json
{
  "oldPassword": "123456",
  "newPassword": "654321"
}
```

---

## 2. 学院管理 `/api/colleges`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| `GET` | `/api/colleges` | 无 | 获取所有学院列表 |
| `GET` | `/api/colleges/{id}` | 无 | 获取学院详情 |
| `POST` | `/api/colleges` | OFFICE | 新增学院 |
| `PUT` | `/api/colleges/{id}` | OFFICE | 更新学院 |
| `DELETE` | `/api/colleges/{id}` | OFFICE | 删除学院 |

### 2.1 新增/更新学院

```json
{
  "name": "计算机学院",
  "code": "CS001"
}
```

---

## 3. 用户管理 `/api/users`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| `GET` | `/api/users` | OFFICE | 分页查询用户列表 |
| `GET` | `/api/users/{id}` | OFFICE | 获取用户详情 |
| `POST` | `/api/users` | OFFICE | 新增用户 |
| `PUT` | `/api/users/{id}` | OFFICE | 编辑用户 |
| `PUT` | `/api/users/{id}/status` | OFFICE | 启用/禁用用户 |
| `DELETE` | `/api/users/{id}` | OFFICE | 删除用户 |
| `POST` | `/api/users/import` | OFFICE | 批量导入 CSV |
| `GET` | `/api/users/template` | — | 下载导入模板 |

### 3.1 查询参数

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `page` | long | 1 | 页码 |
| `pageSize` | long | 10 | 每页条数 |
| `collegeId` | Long | — | 按学院筛选 |
| `keyword` | String | — | 按用户名/姓名搜索 |

### 3.2 新增/编辑用户

```json
{
  "username": "T001",
  "password": "fzrjxyT001",
  "realName": "张三",
  "email": "zhangsan@school.edu.cn",
  "phone": "13800138000",
  "collegeId": 2,
  "roles": ["TEACHER"]
}
```

密码留空则默认为 `fzrjxy` + 工号。

### 3.3 批量导入格式

CSV/TXT，每行格式：`工号,姓名,密码(可选),学院名称或ID,角色(用-连接)`

```
T001,张三,,数学学院,教师
T002,李四,,计算机学院,教师-主任
T003,王五,mypass,教务处,教务处
```

---

## 4. 学期管理 `/api/semesters`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| `GET` | `/api/semesters` | 无 | 获取学期列表 |
| `GET` | `/api/semesters/{id}` | 无 | 获取学期详情 |
| `POST` | `/api/semesters` | OFFICE | 新增学期 |
| `PUT` | `/api/semesters/{id}` | OFFICE | 更新学期 |
| `DELETE` | `/api/semesters/{id}` | OFFICE | 删除学期 |
| `PUT` | `/api/semesters/{id}/activate` | OFFICE | 激活学期（设为当前学期） |

---

## 5. 课程管理 `/api/courses`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| `GET` | `/api/courses` | 无 | 分页查询课程列表 |
| `GET` | `/api/courses/{id}` | 无 | 获取课程详情 |
| `POST` | `/api/courses` | OFFICE | 新增课程 |
| `PUT` | `/api/courses/{id}` | OFFICE | 更新课程 |
| `DELETE` | `/api/courses/{id}` | OFFICE | 删除课程 |

### 5.1 查询参数

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `page` | long | 1 | 页码 |
| `pageSize` | long | 10 | 每页条数 |
| `collegeId` | Long | — | 按学院筛选 |

---

## 6. 阶段性材料（核心） `/api/phase-materials`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| `GET` | `/api/phase-materials` | TEACHER | 教师查看自己的材料 |
| `GET` | `/api/phase-materials/{id}` | 登录用户 | 获取材料详情 |
| `POST` | `/api/phase-materials` | TEACHER | 创建材料 |
| `DELETE` | `/api/phase-materials/{id}` | TEACHER | 删除材料 |
| `GET` | `/api/phase-materials/{id}/files` | 登录用户 | 获取材料附件 |
| `POST` | `/api/phase-materials/{id}/files` | TEACHER | 添加附件 |
| `GET` | `/api/phase-materials/reviewer` | COLLEGE_REVIEWER / OFFICE | 主任/教务处审核列表 |
| `GET` | `/api/phase-materials/reviewer/filters` | COLLEGE_REVIEWER / OFFICE | 审核筛选条件 |
| `GET` | `/api/phase-materials/archive` | OFFICE | 材料归档列表 |

### 6.1 教师列表查询参数

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `page` | long | 1 | 页码 |
| `pageSize` | long | 10 | 每页条数 |
| `status` | String | — | 按状态筛选 |
| `keyword` | String | — | 按课程名/教师名/描述搜索 |

### 6.2 审核列表查询参数

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `page` | long | 1 | 页码 |
| `pageSize` | long | 10 | 每页条数 |
| `status` | String | — | 按状态筛选 |
| `keyword` | String | — | 搜索 |
| `courseId` | Long | — | 按课程筛选 |
| `teacherId` | Long | — | 按教师筛选 |

### 6.3 归档列表查询参数

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `page` | long | 1 | 页码 |
| `pageSize` | long | 10 | 每页条数 |
| `materialType` | String | — | 材料类型筛选 |
| `keyword` | String | — | 搜索 |
| `status` | String | `approved` | 状态（默认已通过） |

### 6.4 材料状态流转

```
AI_EVALUATING → AI_COMPLETED → COLLEGE_APPROVED → OFFICE_APPROVED
                                                    (终审通过)
                    ↘ AI_REJECTED (驳回)
```

### 6.5 材料类型枚举

| 值 | 中文 |
|------|------|
| `TEACHING_PLAN` | 授课计划 |
| `LESSON_PLAN` | 教案 |
| `COURSEWARE` | 课件 |
| `EXAM_PLAN` | 考核方案 |

---

## 7. AI 评审 `/api/ai-evaluations`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| `POST` | `/api/ai-evaluations` | TEACHER | 提交材料进行 AI 评审 |
| `GET` | `/api/ai-evaluations/{id}` | 登录用户 | 查看评审详情 |
| `GET` | `/api/ai-evaluations/my` | TEACHER | 我的评审历史 |
| `GET` | `/api/ai-evaluations/all` | OFFICE | 全部评审记录 |

### 7.1 提交评审

```json
{
  "materialId": 1
}
```

### 7.2 查询参数

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `page` | long | 1 | 页码 |
| `pageSize` | long | 10 | 每页条数 |
| `status` | String | — | 按状态筛选（`/all`） |

### 7.3 评审结果字段

| 字段 | 说明 |
|------|------|
| `score` | 四维加权总分（0-100） |
| `dimensionScores` | 各维度评分 JSON |
| `suggestions` | AI 生成的优化建议 |

---

## 8. Prompt 模板管理 `/api/prompt-templates`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| `GET` | `/api/prompt-templates` | OFFICE | 查询模板（按场景/材料类型分组） |
| `GET` | `/api/prompt-templates/{id}` | OFFICE | 获取模板详情 |
| `PUT` | `/api/prompt-templates/{id}` | OFFICE | 更新模板（自动创建新版本） |
| `PUT` | `/api/prompt-templates/{id}/toggle-active` | OFFICE | 切换激活状态 |

### 8.1 查询参数

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `scene` | String | `MATERIAL_EVALUATION` | 场景 |

### 8.2 更新模板

```json
{
  "templateText": "新模板内容...",
  "description": "修改说明"
}
```

系统自动记录版本历史，旧版本设为不激活。

---

## 9. 手动审核（主任/教务处） `/api/manual-reviews`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| `GET` | `/api/manual-reviews/pending` | COLLEGE_REVIEWER / OFFICE | 待审核列表 |
| `GET` | `/api/manual-reviews/history` | COLLEGE_REVIEWER / OFFICE | 审核历史 |
| `GET` | `/api/manual-reviews/evaluation/{evaluationId}` | 登录用户 | 按评审ID查看 |
| `POST` | `/api/manual-reviews/{evaluationId}` | COLLEGE_REVIEWER / OFFICE | 提交审核 |

### 9.1 提交审核请求体

```json
{
  "action": "APPROVE",
  "modifiedScore": 85,
  "modifyReason": "内容完整度评分为85分",
  "reviewComment": "材料质量良好，予以通过",
  "revisionRequirements": "建议补充实践案例",
  "deadline": "2026-06-20",
  "reviewLevel": "COLLEGE"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `action` | String | `APPROVE`（通过）、`REJECT`（驳回） |
| `modifiedScore` | Integer | 调整后的分数（可选） |
| `modifyReason` | String | 修改分数原因（修改分数时必填） |
| `reviewComment` | String | 审核意见 |
| `revisionRequirements` | String | 修改要求（驳回时填写） |
| `deadline` | String | 修改截止日期（驳回时填写，格式 `yyyy-MM-dd`） |
| `reviewLevel` | String | `COLLEGE`（主任审核）、`OFFICE`（教务处终审） |

---

## 10. 对齐分析 `/api/alignment`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| `POST` | `/api/alignment/analyze` | DEAN | 发起 AI 对齐分析（异步） |
| `GET` | `/api/alignment/reports` | 登录用户 | 分析报告列表 |
| `GET` | `/api/alignment/reports/{id}` | 登录用户 | 报告详情 |
| `DELETE` | `/api/alignment/reports/{id}` | DEAN | 删除报告 |

### 10.1 发起分析

```json
{
  "tcpId": 1
}
```

调用后立即返回，AI 异步生成报告（约 1-2 分钟），生成后刷新报告列表查看。

### 10.2 报告列表查询参数

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `page` | long | 1 | 页码 |
| `pageSize` | long | 10 | 每页条数 |
| `collegeId` | Long | — | 按学院筛选（非OFFICE用户自动过滤本院） |

### 10.3 报告内容

报告详情中的 `reportJson` 字段包含 8 章节完整分析：

```
一、专业概况与产业背景
二、产业人才需求分析
三、课程体系匹配分析
四、学习成果与能力达成
五、就业前景分析
六、差距分析与短板识别
七、改进建议与改革方向
八、综合评分与结论
```

---

## 11. 人才培养方案 `/api/talent-plans`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| `GET` | `/api/talent-plans` | 登录用户 | 分页查询（非OFFICE自动过滤本院） |
| `GET` | `/api/talent-plans/{id}` | 登录用户 | 获取详情 |
| `POST` | `/api/talent-plans` | DEAN | 新增人培方案 |
| `PUT` | `/api/talent-plans/{id}` | DEAN | 更新人培方案 |
| `DELETE` | `/api/talent-plans/{id}` | DEAN | 删除人培方案 |
| `PUT` | `/api/talent-plans/{id}/mapping` | DEAN | 更新培养目标/课程体系/矩阵映射 |

### 11.1 查询参数

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `page` | long | 1 | 页码 |
| `pageSize` | long | 10 | 每页条数 |
| `collegeId` | Long | — | 按学院筛选 |
| `status` | String | — | 按状态筛选 |

### 11.2 更新映射矩阵

```json
{
  "targets": "培养目标内容...",
  "requirements": "毕业要求...",
  "courseSystem": "课程体系...",
  "mappingMatrix": "映射矩阵..."
}
```

---

## 12. 课程标准 `/api/course-standards`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| `GET` | `/api/course-standards` | 登录用户 | 分页查询（非OFFICE仅见自己创建的） |
| `GET` | `/api/course-standards/{id}` | 登录用户 | 获取详情 |
| `POST` | `/api/course-standards` | DEAN | 新增课程标准 |
| `PUT` | `/api/course-standards/{id}` | DEAN | 更新课程标准 |
| `DELETE` | `/api/course-standards/{id}` | DEAN | 删除课程标准 |

### 12.1 查询参数

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `page` | long | 1 | 页码 |
| `pageSize` | long | 10 | 每页条数 |
| `courseId` | Long | — | 按课程筛选 |

---

## 13. 教学计划（旧版） `/api/teaching-plans`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| `GET` | `/api/teaching-plans` | 登录用户 | 分页查询自己的教学计划 |
| `GET` | `/api/teaching-plans/{id}` | 登录用户 | 获取详情 |
| `POST` | `/api/teaching-plans` | TEACHER | 新增 |
| `PUT` | `/api/teaching-plans/{id}` | TEACHER | 更新 |
| `DELETE` | `/api/teaching-plans/{id}` | TEACHER | 删除 |
| `POST` | `/api/teaching-plans/{id}/submit` | TEACHER | 提交审核 |
| `POST` | `/api/teaching-plans/{id}/withdraw` | TEACHER | 撤回 |
| `GET` | `/api/teaching-plans/{id}/files` | 登录用户 | 获取附件 |
| `POST` | `/api/teaching-plans/{id}/files` | TEACHER | 添加附件 |

### 13.1 查询参数

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `page` | long | 1 | 页码 |
| `pageSize` | long | 10 | 每页条数 |
| `courseId` | Long | — | 按课程筛选 |
| `semesterId` | Long | — | 按学期筛选 |
| `status` | String | — | 按状态筛选 |

---

## 14. 审核（旧版） `/api/reviews`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| `GET` | `/api/reviews/college/pending` | COLLEGE_REVIEWER | 学院待审核列表 |
| `GET` | `/api/reviews/office/pending` | OFFICE | 教务处待审核列表 |
| `POST` | `/api/reviews/college/{planId}` | COLLEGE_REVIEWER | 学院审核 |
| `POST` | `/api/reviews/office/{planId}` | OFFICE | 教务处终审 |
| `POST` | `/api/reviews/college/batch` | COLLEGE_REVIEWER | 批量审核 |
| `GET` | `/api/reviews/history/{planId}` | 登录用户 | 审核历史 |

### 14.1 审核请求体

```json
{
  "action": "APPROVE",
  "comment": "审核意见",
  "rejectTarget": "TEACHING_PLAN"
}
```

---

## 15. 文件管理 `/api/files`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| `POST` | `/api/files/upload` | 登录用户 | 上传文件到 MinIO |
| `POST` | `/api/files/upload-parse` | 登录用户 | 上传并解析文档文本 |
| `GET` | `/api/files/{fileId}/download` | 登录用户 | 下载/预览文件 |

### 15.1 上传文件

**请求**: `multipart/form-data`

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `file` | File | 必填 | 上传文件 |
| `type` | String | `PLAN` | 业务类型：`PLAN`（教学计划）、`MATERIAL`（阶段材料） |
| `materialType` | String | — | 材料类型，影响存储子目录 |

**响应**：
```json
{
  "data": {
    "fileId": 1,
    "fileName": "授课计划.docx",
    "fileUrl": "/shuike-manager/phase-materials/teaching-plan/2026/06/授课计划_122720.docx",
    "fileSize": 245760,
    "storage": "MinIO"
  }
}
```

### 15.2 上传并解析文档

**请求**: `multipart/form-data`

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `file` | File | 必填 | 上传文件 |
| `type` | String | `TALENT_PLAN` | `TALENT_PLAN` / `COURSE_STANDARD` |

**响应**包含 `parsedText` 字段返回解析后的文本内容，支持 Word（.doc/.docx）和 PDF。

### 15.3 下载/预览

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `mode` | String | `download` | `download`（强制下载）/ `preview`（浏览器预览+解析文本） |

预览模式下同时返回解析后的文本内容（`textContent` 字段）。

---

## 16. 通知中心 `/api/notifications`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| `GET` | `/api/notifications` | 登录用户 | 分页获取通知列表 |
| `GET` | `/api/notifications/unread-count` | 登录用户 | 获取未读数量 |
| `PUT` | `/api/notifications/{id}/read` | 登录用户 | 标记单条已读 |
| `PUT` | `/api/notifications/read-all` | 登录用户 | 全部标记已读 |

### 16.1 查询参数

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `page` | long | 1 | 页码 |
| `pageSize` | long | 10 | 每页条数 |

自动按当前登录用户过滤，仅返回自己的通知。

---

## 17. Dashboard 统计 `/api/dashboard`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| `GET` | `/api/dashboard/teacher` | 登录用户 | 教师工作台 |
| `GET` | `/api/dashboard/college` | 登录用户 | 学院主任工作台 |
| `GET` | `/api/dashboard/office` | 登录用户 | 教务处工作台 |
| `GET` | `/api/dashboard/dean` | 登录用户 | 院长工作台 |

### 17.1 教务处 `/office` 返回字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `totalMaterials` | long | 材料总数 |
| `statusCounts` | Map | 各状态材料数量 |
| `typeStats` | List | 材料类型 × 状态二维统计 |
| `scoreDistribution` | int[5] | AI评分分布（<60 / 60-74 / 75-84 / 85-94 / 95-100） |
| `collegeMaterialAvgScores` | Map | **各学院 × 各材料类型 AI 平均分** |
| `materialTypeLabels` | List | 材料类型中文标签 |
| `materialTypeKeys` | List | 材料类型枚举值 |
| `topTeachers` | List | 教师提交量 Top10 |
| `typeAvgScores` | List | 各材料类型平均分和数量 |
| `totalReviewed` | long | 已通过材料数 |
| `awaitingReview` | long | 待教务处终审数 |

### 17.2 学院主任 `/college` 返回字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `totalMaterials` | long | 本院材料总数 |
| `pendingReview` | long | 待审核数 |
| `reviewed` | long | 已审核数 |
| `rejected` | long | 已驳回数 |
| `typeStats` | List | 材料类型 × 状态统计 |
| `teacherStats` | List | 各教师提交统计（含各材料类型明细） |
| `teacherScores` | List | 各教师平均分/最高分/最低分 |
| `typeAvgScores` | List | 各材料类型平均分 |
| `topMaterials` | List | 得分最高材料 Top10 |
| `scoreDistribution` | int[5] | AI 评分分布 |

### 17.3 教师 `/teacher` 返回字段

| 字段 | 说明 |
|------|------|
| `pendingModify` | 待修改数 |
| `pendingCollegeReview` | 待主任审核数 |
| `pendingOfficeReview` | 待教务处审核数 |
| `approved` | 已通过数 |

### 17.4 院长 `/dean` 返回字段

| 字段 | 说明 |
|------|------|
| `talentPlanCount` | 人培方案数量（仅本院） |
| `courseStandardCount` | 课程标准数量 |
| `collegeId` | 所属学院 ID |

---

## 角色权限矩阵

| API 模块 | 教师 | 学院主任 | 教务处 | 院长 | 未登录 |
|----------|:--:|:--:|:--:|:--:|:--:|
| 认证 | ✅ | ✅ | ✅ | ✅ | ✅ |
| 学院（查询） | ✅ | ✅ | ✅ | ✅ | ✅ |
| 学院（写入） | — | — | ✅ | — | — |
| 用户管理 | — | — | ✅ | — | — |
| 学期管理（写入） | — | — | ✅ | — | — |
| 课程管理（写入） | — | — | ✅ | — | — |
| 阶段材料（创建/删除） | ✅ | — | — | — | — |
| 阶段材料（审核列表） | — | ✅ | ✅ | — | — |
| 阶段材料（归档） | — | — | ✅ | — | — |
| AI 评审（提交） | ✅ | — | — | — | — |
| AI 评审（查看全部） | — | — | ✅ | — | — |
| Prompt 模板 | — | — | ✅ | — | — |
| 手动审核 | — | ✅ | ✅ | — | — |
| 对齐分析 | — | — | — | ✅ | — |
| 人培方案 | — | — | — | ✅ | — |
| 课程标准 | — | — | — | ✅ | — |
| Dashboard | ✅ | ✅ | ✅ | ✅ | — |
| 文件上传 | ✅ | ✅ | ✅ | ✅ | — |
| 通知 | ✅ | ✅ | ✅ | ✅ | — |

---

## 默认账号

| 用户名 | 密码 | 角色 | 所属学院 |
|--------|------|------|---------|
| `office1` | `123456` | 教务处 | 教务处 |
| `teacher1` | `123456` | 教师 | 计算机学院 |
| `teacher2` | `123456` | 教师 | 数学学院 |
| `reviewer2` | `123456` | 专业主任 | 计算机学院 |
| `reviewer3` | `123456` | 专业主任 | 数学学院 |
| `dean1` | `123456` | 院长 | 计算机学院 |
| `dean2` | `123456` | 院长 | 数学学院 |
| `admin` | `123456` | 教务处 | — |

---

> **最后更新**: 2026-06-14 | **接口总数**: 50+ 个 REST API | **测试用例**: 42 个（全部通过）
