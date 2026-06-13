# Phase 4: Detailed Design -- 图书管理系统详细设计

## 1. 数据库详细设计 (DDL)

### 1.1 建表语句

```sql
-- 图书表
CREATE TABLE book (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(200)    NOT NULL COMMENT '书名',
    author          VARCHAR(100)    NOT NULL COMMENT '作者',
    isbn            VARCHAR(20)     NOT NULL UNIQUE COMMENT 'ISBN号',
    publisher       VARCHAR(100)    COMMENT '出版社',
    publish_date    DATE            COMMENT '出版日期',
    category        VARCHAR(50)     COMMENT '分类',
    total_copies    INT             NOT NULL DEFAULT 1 COMMENT '总册数',
    available_copies INT            NOT NULL DEFAULT 1 COMMENT '可借册数',
    status          VARCHAR(20)     NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态: AVAILABLE/BORROWED/MAINTENANCE',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_book_title (title),
    INDEX idx_book_author (author),
    INDEX idx_book_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书表';

-- 用户表
CREATE TABLE user (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(50)     NOT NULL COMMENT '姓名',
    phone            VARCHAR(20)     NOT NULL UNIQUE COMMENT '手机号',
    email            VARCHAR(100)    UNIQUE COMMENT '邮箱',
    address          VARCHAR(200)    COMMENT '地址',
    member_type      VARCHAR(20)     NOT NULL DEFAULT 'PUBLIC' COMMENT '会员类型: STUDENT/TEACHER/PUBLIC',
    max_borrow_limit INT             NOT NULL DEFAULT 3 COMMENT '最大借阅数',
    create_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_name (name),
    INDEX idx_user_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 借阅记录表
CREATE TABLE borrow_record (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id     BIGINT          NOT NULL COMMENT '图书ID',
    user_id     BIGINT          NOT NULL COMMENT '用户ID',
    borrow_date DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '借阅日期',
    due_date    DATETIME        NOT NULL COMMENT '应还日期',
    return_date DATETIME        COMMENT '实际归还日期',
    status      VARCHAR(20)     NOT NULL DEFAULT 'BORROWED' COMMENT '状态: BORROWED/RETURNED/OVERDUE',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT fk_borrow_book FOREIGN KEY (book_id) REFERENCES book(id),
    CONSTRAINT fk_borrow_user FOREIGN KEY (user_id) REFERENCES user(id),
    INDEX idx_borrow_user_id (user_id),
    INDEX idx_borrow_book_id (book_id),
    INDEX idx_borrow_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借阅记录表';
```

---

## 2. 类详细设计

### 2.1 Entity 类设计

#### Book.java

| 属性              | 类型          | JPA 注解                          |
|------------------|--------------|-----------------------------------|
| id               | Long         | @Id @GeneratedValue(IDENTITY)     |
| title            | String       | @Column(nullable=false, length=200)|
| author           | String       | @Column(nullable=false, length=100)|
| isbn             | String       | @Column(nullable=false, unique=true, length=20)|
| publisher        | String       | @Column(length=100)                |
| publishDate      | LocalDate    | @Column                            |
| category         | String       | @Column(length=50)                 |
| totalCopies      | Integer      | @Column(nullable=false)            |
| availableCopies  | Integer      | @Column(nullable=false)            |
| status           | BookStatus   | @Enumerated(STRING) 枚举            |
| createTime       | LocalDateTime| @CreatedDate                       |
| updateTime       | LocalDateTime| @LastModifiedDate                  |

**BookStatus 枚举值：** `AVAILABLE`, `BORROWED`, `MAINTENANCE`

#### User.java

| 属性             | 类型           | JPA 注解                          |
|-----------------|---------------|-----------------------------------|
| id              | Long          | @Id @GeneratedValue(IDENTITY)     |
| name            | String        | @Column(nullable=false, length=50)|
| phone           | String        | @Column(nullable=false, unique=true, length=20)|
| email           | String        | @Column(unique=true, length=100)  |
| address         | String        | @Column(length=200)               |
| memberType      | MemberType    | @Enumerated(STRING)               |
| maxBorrowLimit  | Integer       | @Column(nullable=false)           |
| currentBorrowCount| Integer     | @Column (计算字段，非持久化)         |
| createTime      | LocalDateTime | @CreatedDate                      |
| updateTime      | LocalDateTime | @LastModifiedDate                 |

**MemberType 枚举值：** `STUDENT(5, 15)`, `TEACHER(10, 30)`, `PUBLIC(3, 14)`

#### BorrowRecord.java

| 属性         | 类型            | JPA 注解                          |
|-------------|----------------|-----------------------------------|
| id          | Long           | @Id @GeneratedValue(IDENTITY)     |
| book        | Book           | @ManyToOne                        |
| user        | User           | @ManyToOne                        |
| borrowDate  | LocalDateTime  | @Column(nullable=false)           |
| dueDate     | LocalDateTime  | @Column(nullable=false)           |
| returnDate  | LocalDateTime  | @Column                           |
| status      | RecordStatus   | @Enumerated(STRING)               |
| createTime  | LocalDateTime  | @CreatedDate                      |

**RecordStatus 枚举值：** `BORROWED`, `RETURNED`, `OVERDUE`

### 2.2 DTO 类设计

#### BookDTO
```java
// 请求字段
String title;          // @NotBlank
String author;         // @NotBlank
String isbn;           // @NotBlank
String publisher;
LocalDate publishDate;
String category;
Integer totalCopies;   // @NotNull @Min(1)
// 响应额外字段
Long id;
Integer availableCopies;
String status;
LocalDateTime createTime;
```

#### UserDTO
```java
// 请求字段
String name;           // @NotBlank
String phone;          // @NotBlank
String email;
String address;
String memberType;     // "STUDENT" | "TEACHER" | "PUBLIC"
// 响应额外字段
Long id;
Integer maxBorrowLimit;
LocalDateTime createTime;
```

#### BorrowRequest
```java
Long bookId;           // @NotNull
Long userId;           // @NotNull
Long recordId;         // 归还时使用
```

#### ApiResponse<T>
```java
int code;              // 200 成功, 4xx/5xx 异常
String message;
T data;
LocalDateTime timestamp;

// 静态工厂方法
static <T> ApiResponse<T> success(T data)
static <T> ApiResponse<T> success(String message, T data)
static <T> ApiResponse<T> error(int code, String message)
```

### 2.3 Service 层详细设计

#### BookService
```java
public class BookService {
    BookDTO createBook(BookDTO dto);
    BookDTO updateBook(Long id, BookDTO dto);
    void deleteBook(Long id);
    BookDTO getBookById(Long id);
    Page<BookDTO> searchBooks(String keyword, Pageable pageable);
    void decreaseAvailableCopies(Long bookId);
    void increaseAvailableCopies(Long bookId);
}
```

**业务规则：**
- `createBook`: 校验 ISBN 唯一性；`totalCopies` = `availableCopies` 初始值
- `deleteBook`: 检查是否存在未归还记录，有则抛异常
- `decreaseAvailableCopies`: 借书时调用，availableCopies-1；若变为0则状态改为BORROWED
- `increaseAvailableCopies`: 还书时调用，availableCopies+1；状态恢复为AVAILABLE

#### UserService
```java
public class UserService {
    UserDTO createUser(UserDTO dto);
    UserDTO updateUser(Long id, UserDTO dto);
    UserDTO getUserById(Long id);
    Page<UserDTO> searchUsers(String keyword, Pageable pageable);
    int getCurrentBorrowCount(Long userId);
    int getMaxBorrowLimit(Long userId);
}
```

**业务规则：**
- `createUser`: 根据 memberType 自动设置 maxBorrowLimit
- `getCurrentBorrowCount`: 查询 user 状态为 BORROWED 的记录数

#### BorrowService
```java
public class BorrowService {
    @Transactional
    BorrowRecord borrowBook(Long userId, Long bookId);

    @Transactional
    BorrowRecord returnBook(Long recordId);

    Page<BorrowRecord> getBorrowRecords(Long userId, Long bookId, Pageable pageable);
}
```

**borrowBook 流程：**
1. 校验 User 存在
2. 校验 Book 存在且 availableCopies > 0
3. 校验 currentBorrowCount < maxBorrowLimit
4. 校验用户未重复借阅同一本书
5. 计算 dueDate = now + memberType.getBorrowDays()
6. 创建 BorrowRecord (status=BORROWED)
7. 调用 BookService.decreaseAvailableCopies(bookId)
8. 返回 BorrowRecord

**returnBook 流程：**
1. 校验 BorrowRecord 存在且 status=BORROWED
2. 设置 returnDate = now
3. 判断是否超期，超期则 status=OVERDUE，否则 status=RETURNED
4. 调用 BookService.increaseAvailableCopies(bookId)
5. 保存 BorrowRecord

### 2.4 Controller 层详细设计

#### BookController
```java
@RestController
@RequestMapping("/api/books")
public class BookController {

    @GetMapping
    ApiResponse<Page<BookDTO>> searchBooks(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    );

    @GetMapping("/{id}")
    ApiResponse<BookDTO> getBook(@PathVariable Long id);

    @PostMapping
    ApiResponse<BookDTO> createBook(@Valid @RequestBody BookDTO dto);

    @PutMapping("/{id}")
    ApiResponse<BookDTO> updateBook(@PathVariable Long id, @Valid @RequestBody BookDTO dto);

    @DeleteMapping("/{id}")
    ApiResponse<Void> deleteBook(@PathVariable Long id);
}
```

#### UserController
```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping
    ApiResponse<Page<UserDTO>> searchUsers(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    );

    @GetMapping("/{id}")
    ApiResponse<UserDTO> getUser(@PathVariable Long id);

    @PostMapping
    ApiResponse<UserDTO> createUser(@Valid @RequestBody UserDTO dto);

    @PutMapping("/{id}")
    ApiResponse<UserDTO> updateUser(@PathVariable Long id, @Valid @RequestBody UserDTO dto);
}
```

#### BorrowController
```java
@RestController
@RequestMapping("/api/borrow")
public class BorrowController {

    @PostMapping
    ApiResponse<BorrowResponse> borrowBook(@Valid @RequestBody BorrowRequest request);

    @PutMapping("/{id}/return")
    ApiResponse<BorrowResponse> returnBook(@PathVariable Long id);

    @GetMapping
    ApiResponse<Page<BorrowResponse>> getBorrowRecords(
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) Long bookId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    );
}
```

### 2.5 异常处理设计

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    ApiResponse<Void> handleBusinessException(BusinessException e);  // 400

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e); // 400

    @ExceptionHandler(Exception.class)
    ApiResponse<Void> handleException(Exception e);  // 500
}
```

#### BusinessException
```java
public class BusinessException extends RuntimeException {
    private int code;        // 业务错误码
    private String message;  // 错误消息

    public BusinessException(String message) { this(400, message); }
    public BusinessException(int code, String message) { ... }
}
```

**业务异常场景：**

| 场景                       | 异常消息                       |
|---------------------------|-------------------------------|
| 图书不存在                  | "图书不存在"                   |
| 图书库存不足                | "该图书已全部借出"              |
| 存在未归还记录无法删除       | "该图书存在未归还的借阅记录，无法删除"|
| 用户不存在                  | "用户不存在"                   |
| 用户借阅数量已达上限         | "借阅数量已达上限"              |
| 重复借阅                    | "您已借阅此书，请勿重复借阅"      |
| 借阅记录不存在              | "借阅记录不存在"                |
| 借阅记录已归还              | "该借阅记录已归还"              |
| ISBN重复                   | "ISBN号已存在"                  |

---

## 3. 序列图

### 3.1 借书流程序列图描述

```
Client          BookController     BorrowService      BookService    BookRepository   BorrowRecordRepo
  |                   |                  |                  |               |               |
  |-- POST /api/borrow->|                  |                  |               |               |
  |                   |--- borrowBook() -->|                  |               |               |
  |                   |                  |-- findById() -----|-------------->|               |
  |                   |                  |<-- User ----------|---------------|               |
  |                   |                  |-- findById() -----|-------------->|               |
  |                   |                  |<-- Book ----------|---------------|               |
  |                   |                  |-- (校验借阅数量) ---|               |               |
  |                   |                  |-- (校验库存) ------|               |               |
  |                   |                  |-- (校验重复借阅) ---|               |               |
  |                   |                  |-- decreaseCopies()->|               |               |
  |                   |                  |                  |-- save() ----->|               |
  |                   |                  |                  |<-- OK ---------|               |
  |                   |                  |<-- OK ------------|               |               |
  |                   |                  |-- save() ------------------------>|               |
  |                   |                  |<-- BorrowRecord ------------------|               |
  |                   |<-- ApiResponse ---|                  |               |               |
  |<-- 200 OK --------|                  |                  |               |               |
```

### 3.2 还书流程序列图描述

```
Client          BorrowController   BorrowService     BookService    BookRepository   BorrowRecordRepo
  |                   |                  |                  |               |               |
  |-- PUT /api/borrow/|                  |                  |               |               |
  |    {id}/return -->|                  |                  |               |               |
  |                   |-- returnBook() -->|                  |               |               |
  |                   |                  |-- findById() --------------------->|               |
  |                   |                  |<-- BorrowRecord -------------------|               |
  |                   |                  |-- (校验状态=BORROWED)               |               |
  |                   |                  |-- (判断超期) ---|                  |               |
  |                   |                  |-- increaseCopies()->|              |               |
  |                   |                  |                  |-- save() ----->|               |
  |                   |                  |                  |<-- OK ---------|               |
  |                   |                  |<-- OK ------------|               |               |
  |                   |                  |-- save() ------------------------>|               |
  |                   |                  |<-- OK ----------------------------|               |
  |                   |<-- ApiResponse ---|                  |               |               |
  |<-- 200 OK --------|                  |                  |               |               |
```

---

## 4. application.properties 配置

```properties
server.port=8080

spring.datasource.url=jdbc:mysql://localhost:3306/library_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

spring.jackson.date-format=yyyy-MM-dd HH:mm:ss
spring.jackson.time-zone=Asia/Shanghai
```