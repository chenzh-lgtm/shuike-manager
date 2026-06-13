# Phase 3: High-Level Design

## 1. System Architecture

```
+----------------------------------------------------+
|                    CLIENT (Browser)                 |
|  +----------------------------------------------+  |
|  |              React SPA (Port 3000)            |  |
|  |  +--------+  +--------+  +---------------+   |  |
|  |  | Header |  |  Tabs  |  | Page Content  |   |  |
|  |  | (Nav)  |  |(记账/  |  | (Conditional  |   |  |
|  |  |        |  | 报表/   |  |  Rendering)   |   |  |
|  |  |        |  | 列表)  |  |               |   |  |
|  |  +--------+  +--------+  +---------------+   |  |
|  |       API Service Layer (fetch wrapper)       |  |
|  +----------------------------------------------+  |
+----------------------------------------------------+
                        |  HTTP REST (JSON)
                        v
+----------------------------------------------------+
|                  SERVER (Node.js)                   |
|  +----------------------------------------------+  |
|  |           Express.js API (Port 3001)          |  |
|  |  +------------+  +-------------------------+  |  |
|  |  |  Routes    |  |   Controllers           |  |  |
|  |  |  /api/     |  |   - transactionCtrl     |  |  |
|  |  |  transactions| |   - categoryCtrl       |  |  |
|  |  |  /api/     |  |   - reportCtrl          |  |  |
|  |  |  categories|  |   - exportCtrl          |  |  |
|  |  |  /api/     |  |                         |  |  |
|  |  |  reports   |  |                         |  |  |
|  |  |  /api/     |  |                         |  |  |
|  |  |  export    |  |                         |  |  |
|  |  +------------+  +-------------------------+  |  |
|  |  +-----------------------------------------+  |  |
|  |  |            Database Layer                |  |  |
|  |  |  better-sqlite3 + db initialization      |  |  |
|  |  +-----------------------------------------+  |  |
|  +----------------------------------------------+  |
+----------------------------------------------------+
                        |
                        v
+----------------------------------------------------+
|              SQLite Database (finance.db)            |
|  +------------------+  +-------------------------+  |
|  |  transactions    |  |  categories             |  |
|  |  - id (PK)       |  |  - id (PK)              |  |
|  |  - amount        |  |  - name                 |  |
|  |  - type          |  |  - type (income/expense)|  |
|  |  - category_id   |  |  - is_default           |  |
|  |  - date          |  |                         |  |
|  |  - description   |  +-------------------------+  |
|  |  - created_at    |                              |
|  +------------------+                              |
+----------------------------------------------------+
```

## 2. Component Tree (Frontend)

```
App
├── Header (应用标题: 个人记账)
├── TabNavigation (记账 | 账单列表 | 月度报表)
├── TransactionForm (Tab: 记账)
│   ├── AmountInput
│   ├── TypeSelector (收入/支出 toggle)
│   ├── CategoryDropdown (dynamic based on type)
│   ├── DatePicker
│   ├── DescriptionInput
│   └── SubmitButton
├── TransactionList (Tab: 账单列表)
│   ├── SearchBar
│   ├── FilterControls (type, category, date range)
│   ├── TransactionTable
│   │   └── TransactionRow (with Edit/Delete buttons)
│   ├── EditModal (TransactionForm in edit mode)
│   └── DeleteConfirmDialog
├── MonthlyReport (Tab: 月度报表)
│   ├── MonthSelector
│   ├── SummaryCards (收入总计, 支出总计, 结余)
│   ├── IncomeByCategoryChart (Pie)
│   ├── ExpenseByCategoryChart (Pie)
│   └── MonthlyTrendChart (Bar: income vs expense)
└── ExportButton (导出CSV)
```

## 3. API Design

### Base URL: `http://localhost:3001/api`

### Transactions API

| Method | Endpoint | Description | Request Body | Response |
|--------|----------|-------------|-------------|----------|
| GET | /transactions | List all (with filters) | Query params: `?type=&category_id=&start_date=&end_date=&search=` | `{ data: Transaction[] }` |
| GET | /transactions/:id | Get single transaction | - | `{ data: Transaction }` |
| POST | /transactions | Create transaction | `{ amount, type, category_id, date, description }` | `{ data: Transaction, message: "创建成功" }` |
| PUT | /transactions/:id | Update transaction | `{ amount, type, category_id, date, description }` | `{ data: Transaction, message: "更新成功" }` |
| DELETE | /transactions/:id | Delete transaction | - | `{ message: "删除成功" }` |

### Categories API

| Method | Endpoint | Description | Request Body | Response |
|--------|----------|-------------|-------------|----------|
| GET | /categories | List all categories | Query params: `?type=` | `{ data: Category[] }` |
| POST | /categories | Create custom category | `{ name, type }` | `{ data: Category }` |
| DELETE | /categories/:id | Delete custom category | - | `{ message: "删除成功" }` |

### Reports API

| Method | Endpoint | Description | Request Body | Response |
|--------|----------|-------------|-------------|----------|
| GET | /reports/monthly-summary | Monthly summary | Query: `?year=&month=` | `{ data: { totalIncome, totalExpense, netBalance } }` |
| GET | /reports/category-breakdown | Category breakdown | Query: `?year=&month=&type=` | `{ data: [{ category, total }] }` |
| GET | /reports/monthly-trend | Month-to-month trend | Query: `?months=12` | `{ data: [{ month, income, expense }] }` |

### Export API

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| GET | /export/csv | Export transactions as CSV | CSV file download |

## 4. Database Schema

### Table: categories

```sql
CREATE TABLE categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('income', 'expense')),
    is_default INTEGER NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(name, type)
);
```

### Table: transactions

```sql
CREATE TABLE transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    amount REAL NOT NULL CHECK (amount > 0),
    type TEXT NOT NULL CHECK (type IN ('income', 'expense')),
    category_id INTEGER NOT NULL,
    date TEXT NOT NULL,
    description TEXT DEFAULT '',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT
);
```

### Indexes

```sql
CREATE INDEX idx_transactions_date ON transactions(date);
CREATE INDEX idx_transactions_type ON transactions(type);
CREATE INDEX idx_transactions_category ON transactions(category_id);
CREATE INDEX idx_categories_type ON categories(type);
```

## 5. State Management Strategy

```javascript
// App-level state via Context + useReducer
AppState {
  transactions: Transaction[],
  categories: Category[],
  selectedTab: 'add' | 'list' | 'report',
  selectedMonth: { year: number, month: number },
  loading: boolean,
  error: string | null
}

// Actions
- SET_TRANSACTIONS
- ADD_TRANSACTION
- UPDATE_TRANSACTION
- DELETE_TRANSACTION
- SET_CATEGORIES
- ADD_CATEGORY
- DELETE_CATEGORY
- SET_SELECTED_TAB
- SET_SELECTED_MONTH
- SET_LOADING
- SET_ERROR
```

## 6. File Structure

```
personal-finance-app/
├── client/                         # React Frontend
│   ├── public/
│   │   └── index.html
│   ├── src/
│   │   ├── index.js
│   │   ├── App.js
│   │   ├── App.css
│   │   ├── context/
│   │   │   └── AppContext.js        # State management
│   │   ├── services/
│   │   │   └── api.js               # API client
│   │   ├── components/
│   │   │   ├── Header/
│   │   │   │   ├── Header.js
│   │   │   │   └── Header.css
│   │   │   ├── TabNavigation/
│   │   │   │   ├── TabNavigation.js
│   │   │   │   └── TabNavigation.css
│   │   │   ├── TransactionForm/
│   │   │   │   ├── TransactionForm.js
│   │   │   │   └── TransactionForm.css
│   │   │   ├── TransactionList/
│   │   │   │   ├── TransactionList.js
│   │   │   │   └── TransactionList.css
│   │   │   └── MonthlyReport/
│   │   │       ├── MonthlyReport.js
│   │   │       └── MonthlyReport.css
│   │   └── utils/
│   │       └── helpers.js
│   └── package.json
├── server/                         # Node.js Backend
│   ├── src/
│   │   ├── index.js                # Entry point
│   │   ├── db/
│   │   │   ├── database.js         # DB connection & initialization
│   │   │   └── seed.js             # Default categories seeding
│   │   ├── routes/
│   │   │   ├── transactions.js
│   │   │   ├── categories.js
│   │   │   ├── reports.js
│   │   │   └── export.js
│   │   ├── controllers/
│   │   │   ├── transactionController.js
│   │   │   ├── categoryController.js
│   │   │   ├── reportController.js
│   │   │   └── exportController.js
│   │   └── middleware/
│   │       └── validation.js       # Input validation middleware
│   ├── tests/
│   │   ├── transactionController.test.js
│   │   ├── categoryController.test.js
│   │   └── reportController.test.js
│   └── package.json
└── README.md
```

## 7. Cross-Cutting Concerns

### Error Handling Strategy
- Backend: try-catch in all controllers, standardized error response format `{ error: string, details?: any }`
- Frontend: try-catch in API calls, error state in context, toast/snackbar for user notifications

### Validation Strategy
- Frontend: HTML5 form validation + custom validation before API calls
- Backend: Middleware validation (amount > 0, type must be income/expense, date format YYYY-MM-DD, category must exist)

### CORS Configuration
- Allow origin `http://localhost:3000`
- Allow methods: GET, POST, PUT, DELETE
- Allow headers: Content-Type

## 8. Technology Versions

| Package | Version | Purpose |
|---------|---------|---------|
| react | ^18.2.0 | UI Framework |
| react-dom | ^18.2.0 | React DOM rendering |
| react-scripts | 5.0.1 | CRA build tooling |
| chart.js | ^4.4.0 | Charting library |
| react-chartjs-2 | ^5.2.0 | React wrapper for Chart.js |
| express | ^4.18.0 | HTTP server framework |
| better-sqlite3 | ^9.4.0 | SQLite driver |
| cors | ^2.8.5 | CORS middleware |
| jest | ^29.7.0 | Testing framework |
| supertest | ^6.3.0 | HTTP testing utility |