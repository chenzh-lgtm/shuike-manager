# Phase 4: Detailed Design

## 1. Detailed Database Design

### 1.1 Default Categories Seed Data

```sql
-- Income categories
INSERT INTO categories (name, type, is_default) VALUES ('工资', 'income', 1);
INSERT INTO categories (name, type, is_default) VALUES ('兼职', 'income', 1);
INSERT INTO categories (name, type, is_default) VALUES ('投资收益', 'income', 1);
INSERT INTO categories (name, type, is_default) VALUES ('奖金', 'income', 1);
INSERT INTO categories (name, type, is_default) VALUES ('其他收入', 'income', 1);

-- Expense categories
INSERT INTO categories (name, type, is_default) VALUES ('餐饮', 'expense', 1);
INSERT INTO categories (name, type, is_default) VALUES ('交通', 'expense', 1);
INSERT INTO categories (name, type, is_default) VALUES ('购物', 'expense', 1);
INSERT INTO categories (name, type, is_default) VALUES ('住房', 'expense', 1);
INSERT INTO categories (name, type, is_default) VALUES ('娱乐', 'expense', 1);
INSERT INTO categories (name, type, is_default) VALUES ('医疗', 'expense', 1);
INSERT INTO categories (name, type, is_default) VALUES ('教育', 'expense', 1);
INSERT INTO categories (name, type, is_default) VALUES ('其他支出', 'expense', 1);
```

### 1.2 Query Specifications

**Get monthly summary:**
```sql
SELECT
    COALESCE(SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END), 0) as total_income,
    COALESCE(SUM(CASE WHEN type = 'expense' THEN amount ELSE 0 END), 0) as total_expense
FROM transactions
WHERE strftime('%Y', date) = ? AND strftime('%m', date) = ?
```

**Get category breakdown for a month:**
```sql
SELECT c.name as category, COALESCE(SUM(t.amount), 0) as total
FROM categories c
LEFT JOIN transactions t ON c.id = t.category_id
    AND strftime('%Y', t.date) = ? AND strftime('%m', t.date) = ?
WHERE c.type = ?
GROUP BY c.id, c.name
ORDER BY total DESC
```

**Get monthly trend (last N months):**
```sql
SELECT
    strftime('%Y-%m', date) as month,
    COALESCE(SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END), 0) as income,
    COALESCE(SUM(CASE WHEN type = 'expense' THEN amount ELSE 0 END), 0) as expense
FROM transactions
WHERE date >= date('now', ?)
GROUP BY strftime('%Y-%m', date)
ORDER BY month ASC
```

## 2. Detailed API Specifications

### 2.1 Transaction Routes (`server/src/routes/transactions.js`)

```
GET    /api/transactions          -> transactionController.list
GET    /api/transactions/:id      -> transactionController.getById
POST   /api/transactions          -> validation.validateTransaction, transactionController.create
PUT    /api/transactions/:id      -> validation.validateTransaction, transactionController.update
DELETE /api/transactions/:id      -> transactionController.remove
```

### 2.2 Category Routes (`server/src/routes/categories.js`)

```
GET    /api/categories            -> categoryController.list
POST   /api/categories            -> validation.validateCategory, categoryController.create
DELETE /api/categories/:id        -> categoryController.remove
```

### 2.3 Report Routes (`server/src/routes/reports.js`)

```
GET    /api/reports/monthly-summary    -> reportController.monthlySummary
GET    /api/reports/category-breakdown -> reportController.categoryBreakdown
GET    /api/reports/monthly-trend      -> reportController.monthlyTrend
```

### 2.4 Export Route (`server/src/routes/export.js`)

```
GET    /api/export/csv            -> exportController.exportCsv
```

### 2.5 Request Validation Rules

**Transaction validation:**
```javascript
{
  amount: { required: true, type: 'number', min: 0.01 },
  type: { required: true, oneOf: ['income', 'expense'] },
  category_id: { required: true, type: 'integer', mustExist: true },
  date: { required: true, pattern: /^\d{4}-\d{2}-\d{2}$/ },
  description: { required: false, type: 'string', maxLength: 200 }
}
```

**Category validation:**
```javascript
{
  name: { required: true, type: 'string', minLength: 1, maxLength: 50 },
  type: { required: true, oneOf: ['income', 'expense'] }
}
```

### 2.6 Error Response Format

```json
{
  "error": "Human-readable error message",
  "details": "Optional field-level validation details"
}
```

HTTP Status Codes:
- 200: Success
- 201: Created
- 400: Bad Request (validation failure)
- 404: Not Found
- 409: Conflict (duplicate category name)
- 500: Internal Server Error

## 3. Detailed Component Specifications

### 3.1 App Component (`App.js`)

**State:**
```javascript
{
  transactions: [],
  categories: [],
  selectedTab: 'add', // 'add' | 'list' | 'report'
  selectedMonth: { year: currentYear, month: currentMonth },
  loading: false,
  error: null,
  editingTransaction: null // Transaction object or null
}
```

**Context provided values:**
```javascript
{
  state,           // Full app state
  dispatch,        // Reducer dispatch
  addTransaction,  // Async function
  updateTransaction, // Async function
  deleteTransaction, // Async function
  loadTransactions,  // Async function
  loadCategories,    // Async function
  addCategory,       // Async function
  deleteCategory     // Async function
}
```

### 3.2 TransactionForm Component

**Props:** `onSuccess: () => void`, `editTransaction?: Transaction`

**Local State:**
```javascript
{
  amount: '',
  type: 'expense',
  category_id: '',
  date: new Date().toISOString().slice(0, 10),
  description: '',
  errors: {}
}
```

**Use Cases:**
1. **Add mode:** All fields empty/default; submit creates new transaction
2. **Edit mode:** Fields pre-populated from `editTransaction`; submit updates

**Validation Rules (client-side):**
- amount: required, must be a positive number, max 99999999.99
- type: required
- category_id: required
- date: required, valid date format
- description: optional, max 200 chars

**On Submit:**
1. Validate all fields
2. Call `addTransaction()` or `updateTransaction()` from context
3. Reset form on success
4. Display error on failure

### 3.3 TransactionList Component

**Features:**
- Search by description (client-side filtering)
- Filter by type (All / Income / Expense)
- Filter by category (dropdown)
- Filter by date range (start date, end date)
- Sort by date (newest first) or amount
- Pagination (20 per page)
- Edit button -> opens TransactionForm in edit mode (modal or inline)
- Delete button -> confirmation dialog -> delete

**State:**
```javascript
{
  searchTerm: '',
  filterType: 'all',
  filterCategory: '',
  filterStartDate: '',
  filterEndDate: '',
  sortBy: 'date',
  sortOrder: 'desc',
  currentPage: 1,
  pageSize: 20,
  showDeleteConfirm: null, // transaction id or null
  editingId: null // transaction id or null
}
```

### 3.4 MonthlyReport Component

**Features:**
- Month selector (prev/next buttons + display)
- Summary cards: Total Income, Total Expense, Net Balance
- Expense Pie Chart (breakdown by category)
- Income Pie Chart (breakdown by category)
- Monthly Trend Bar Chart (income vs expense for last 12 months)

**State:**
```javascript
{
  year: currentYear,
  month: currentMonth,
  summary: null,
  expenseBreakdown: [],
  incomeBreakdown: [],
  trend: []
}
```

**Chart Configurations:**

*Pie Charts:*
- Chart.js `doughnut` type
- Colors: predefined palette
- Labels: category names
- Data: amounts
- Legend: shown

*Bar Chart:*
- Chart.js `bar` type
- Two datasets: income (green) and expense (red)
- X-axis: months (labels)
- Y-axis: amount in Yuan
- Legend: shown

### 3.5 Category Management (embedded in TransactionForm)

**Display:** In the TransactionForm, a dropdown shows categories filtered by selected type. A small "管理分类" link opens a category management panel.

**Category Management Panel State:**
```javascript
{
  newCategoryName: '',
  newCategoryType: 'expense',
  localCategories: []
}
```

Features:
- Add new category (name + type)
- Delete custom categories (is_default=0)
- Default categories cannot be deleted

## 4. Sequence Diagrams (Textual)

### 4.1 Add Transaction Flow

```
User fills form -> clicks "保存"
  -> TransactionForm validates locally
  -> TransactionForm calls addTransaction() from context
  -> api.post('/api/transactions', payload)
  -> Express receives POST /api/transactions
  -> validation middleware checks payload
  -> transactionController.create()
    -> db.prepare("INSERT INTO transactions...").run(params)
    -> db.prepare("SELECT * FROM transactions WHERE id = ?").get(id)
    -> res.status(201).json({ data: transaction, message: "创建成功" })
  -> api receives response
  -> dispatch({ type: 'ADD_TRANSACTION', payload: transaction })
  -> state updates, UI re-renders with new transaction
  -> onSuccess callback triggers (reset form, switch tab)
```

### 4.2 Load Monthly Report Flow

```
User clicks "月度报表" tab
  -> MonthlyReport component mounts
  -> useEffect fires for selectedMonth
  -> Three parallel API calls:
     1. api.get('/api/reports/monthly-summary?year=&month=')
     2. api.get('/api/reports/category-breakdown?year=&month=&type=expense')
     3. api.get('/api/reports/category-breakdown?year=&month=&type=income')
     4. api.get('/api/reports/monthly-trend?months=12')
  -> Express handles each request
  -> reportController.monthlySummary()
    -> db.prepare(summaryQuery).get(year, month)
    -> res.json({ data: { totalIncome, totalExpense, netBalance } })
  -> reportController.categoryBreakdown()
    -> db.prepare(breakdownQuery).all(year, month, type)
    -> res.json({ data: rows })
  -> All responses received
  -> setState with data
  -> Charts render with Chart.js
```

## 5. State Transition Diagram (Textual)

```
[App Launch]
    |
    v
[Loading State] --loadCategories(), loadTransactions()-->
    |
    v
[Idle State] (selectedTab: 'add')
    |
    +-- User clicks "账单列表" --> [List State] (selectedTab: 'list')
    +-- User clicks "月度报表" --> [Report State] (selectedTab: 'report')
    +-- User submits form --> [Loading] --> [Idle State] (show success)
    +-- User clicks edit --> [Editing State] (editingTransaction set)
    +-- User clicks delete --> [Confirm State] (showDeleteConfirm set)
    |                            |
    |                     [Confirm] -> [Loading] -> [Idle State]
    |                     [Cancel]  -> [Idle State]
```

## 6. CSV Export Format

```
日期,类型,分类,金额,描述
2024-01-15,支出,餐饮,35.50,午餐
2024-01-16,收入,工资,15000.00,1月工资
```

Headers match Chinese labels. Type is 收入/支出. Amount formatted to 2 decimal places.

## 7. CSS Design Tokens

```css
:root {
  /* Colors */
  --color-primary: #4A90D9;
  --color-income: #52C41A;
  --color-expense: #FF4D4F;
  --color-bg: #F5F5F5;
  --color-card: #FFFFFF;
  --color-text: #333333;
  --color-text-secondary: #888888;
  --color-border: #E8E8E8;

  /* Spacing */
  --spacing-xs: 4px;
  --spacing-sm: 8px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;

  /* Typography */
  --font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  --font-size-sm: 12px;
  --font-size-md: 14px;
  --font-size-lg: 16px;
  --font-size-xl: 20px;
  --font-size-xxl: 24px;

  /* Border Radius */
  --radius-sm: 4px;
  --radius-md: 8px;
  --radius-lg: 12px;
}
```

## 8. Testing Strategy

### 8.1 Backend Unit Tests

**transactionController.test.js:**
- create(): validates required fields, creates transaction, returns 201
- create(): validates amount > 0, rejects non-positive amounts
- create(): validates type is income/expense
- create(): validates category exists
- list(): returns all transactions
- list(): filters by type
- list(): filters by date range
- list(): searches by description
- getById(): returns transaction when found
- getById(): returns 404 when not found
- update(): updates transaction fields
- update(): returns 404 for non-existent id
- remove(): deletes transaction
- remove(): returns 404 for non-existent id

**categoryController.test.js:**
- list(): returns all categories
- list(): filters by type
- create(): creates new category, returns 201
- create(): rejects duplicate name+type combination (409)
- remove(): deletes custom category
- remove(): rejects deletion of default category (400)
- remove(): returns 404 for non-existent id

**reportController.test.js:**
- monthlySummary(): returns correct totals
- monthlySummary(): returns zeros for month with no transactions
- categoryBreakdown(): returns breakdown by category
- categoryBreakdown(): returns empty array for no data
- monthlyTrend(): returns last N months of data

### 8.2 Frontend Tests (Manual testing checklist provided)
Since we focus on backend unit tests for the coding phase, frontend testing will be manual with a checklist provided.