# Phase 3: High-Level Design -- 图书管理系统高层设计

## 1. 设计概述

本阶段定义系统的整体架构、模块划分、技术选型以及数据库概念设计。

---

## 2. 系统架构

### 2.1 架构模式：分层架构 (Layered Architecture)

```
+-------------------------------------------------------------+
|                    外部调用 (HTTP Client)                      |
+-------------------------------------------------------------+
                            |
                            v
+-------------------------------------------------------------+
|               Controller Layer (REST API 层)                 |
|    BookController  |  UserController  |  BorrowController    |
+-------------------------------------------------------------+
                            |
                            v
+-------------------------------------------------------------+
|                Service Layer (业务逻辑层)                      |
|    BookService     |  UserService     |  BorrowService       |
+-------------------------------------------------------------+
                            |
                            v
+-------------------------------------------------------------+
|              Repository Layer (数据访问层)                     |
|    BookRepository  |  UserRepository  |  BorrowRecordRepo    |
+-------------------------------------------------------------+
                            |
                            v
+-------------------------------------------------------------+
|                    MySQL Database                            |
|    book  |  user  |  borrow_record                           |
+-------------------------------------------------------------+
```

### 2.2 分层职责

| 层          | 职责                                              | 依赖方向       |
|------------|--------------------------------------------------|--------------|
| Controller | 接收 HTTP 请求，参数校验，调用 Service，返回响应        | 依赖 Service  |
| Service    | 业务逻辑实现，事务管理，校验业务规则                      | 依赖 Repository|
| Repository | 数据库 CRUD 操作，自定义查询                           | 依赖 Entity   |
| Entity     | 数据模型定义，JPA 映射                                | 无           |

---

## 3. 模块划分

```
com.library
├── entity/          -- 数据实体类
│   ├── Book.java            -- 图书实体
│   ├── User.java            -- 用户实体
│   └── BorrowRecord.java    -- 借阅记录实体
├── dto/             -- 数据传输对象
│   ├── BookDTO.java         -- 图书请求/响应 DTO
│   ├── UserDTO.java         -- 用户请求/响应 DTO
│   ├── BorrowRequest.java   -- 借还请求 DTO
│   └── ApiResponse.java     -- 统一响应格式
├── repository/      -- 数据访问接口
│   ├── BookRepository.java
│   ├── UserRepository.java
│   └── BorrowRecordRepository.java
├── service/         -- 业务逻辑层
│   ├── BookService.java
│   ├── UserService.java
│   └── BorrowService.java
├── controller/      -- 控制器层
│   ├── BookController.java
│   ├── UserController.java
│   └── BorrowController.java
├── config/          -- 配置类
│   └── GlobalExceptionHandler.java
├── exception/       -- 自定义异常
│   └── BusinessException.java
└── LibraryApplication.java  -- 应用入口
```

---

## 4. 技术选型总结

| 类别       | 技术/框架               | 版本     | 说明                     |
|-----------|------------------------|---------|--------------------------|
| 编程语言   | Java                   | 17      | LTS 版本                 |
| 开发框架   | Spring Boot            | 3.2.x   | 快速构建独立应用           |
| 数据访问   | Spring Data JPA        | 3.2.x   | ORM 框架，简化数据库操作     |
| 数据库     | MySQL                  | 8.0     | 关系型数据库               |
| 连接池     | HikariCP               | (内置)   | Spring Boot 默认连接池     |
| 校验       | Jakarta Validation     | 3.0     | Bean Validation          |
| JSON      | Jackson                | (内置)   | JSON 序列化/反序列化        |
| 构建工具   | Maven                  | 3.9+    | 依赖管理和构建             |
| 测试       | JUnit 5 + Mockito      | 5.x     | 单元测试和模拟             |

---

## 5. 数据库概念设计

### 5.1 ER 图描述

```
+-------------------+       +-------------------+
|       Book        |       |       User        |
+-------------------+       +-------------------+
| PK: id            |       | PK: id            |
|     title         |       |     name          |
|     author        |       |     phone         |
|     isbn (UQ)     |       |     email (UQ)    |
|     publisher     |       |     address       |
|     publish_date  |       |     member_type   |
|     category      |       |     max_borrow_limit|
|     total_copies  |       |     create_time   |
|     available_copies|     |     update_time   |
|     status        |       +-------------------+
|     create_time   |               |
|     update_time   |               |
+-------------------+               |
        |                           |
        | 1:N                       | 1:N
        |                           |
        v                           v
+-----------------------------------------+
|              BorrowRecord                |
+-----------------------------------------+
| PK: id                                  |
| FK: book_id   --> Book.id               |
| FK: user_id   --> User.id               |
|     borrow_date                         |
|     due_date                            |
|     return_date                         |
|     status     (BORROWED/RETURNED/OVERDUE)|
|     create_time                         |
+-----------------------------------------+
```

### 5.2 关系说明

- Book 与 BorrowRecord：**一对多**，一本书可有多条借阅记录
- User 与 BorrowRecord：**一对多**，一个用户可有多条借阅记录
- 借阅记录是 Book 和 User 之间的**多对多关联实体**

---

## 6. API 设计概要

Base URL: `/api`

### 6.1 图书管理 API

| 方法     | 路径              | 说明             |
|---------|------------------|-----------------|
| GET     | /books           | 分页查询图书（支持关键字搜索）|
| GET     | /books/{id}      | 查询单本图书      |
| POST    | /books           | 新增图书         |
| PUT     | /books/{id}      | 修改图书信息      |
| DELETE  | /books/{id}      | 删除图书         |

### 6.2 用户管理 API

| 方法     | 路径              | 说明             |
|---------|------------------|-----------------|
| GET     | /users           | 分页查询用户（支持关键字搜索）|
| GET     | /users/{id}      | 查询单个用户      |
| POST    | /users           | 注册新用户       |
| PUT     | /users/{id}      | 修改用户信息      |

### 6.3 借阅管理 API

| 方法     | 路径                     | 说明             |
|---------|-------------------------|-----------------|
| POST    | /borrow                 | 借阅图书         |
| PUT     | /borrow/{id}/return     | 归还图书         |
| GET     | /borrow                 | 查询借阅记录（可按userId/bookId过滤）|

---

## 7. 系统约束与质量属性

- **分层架构**：Controller -> Service -> Repository，禁止跨层调用
- **事务管理**：借还操作使用 @Transactional 保证原子性
- **异常处理**：统一异常拦截，返回标准 JSON 错误格式
- **DTO 隔离**：Controller 层使用 DTO，不直接暴露 Entity
- **RESTful 规范**：URL 使用名词复数，HTTP 方法语义正确