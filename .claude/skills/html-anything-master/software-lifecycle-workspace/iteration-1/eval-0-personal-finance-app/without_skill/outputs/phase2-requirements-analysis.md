# Phase 2: Requirements Analysis

## 1. Functional Requirements

### FR1: Transaction Management
- **FR1.1** Create new transaction with: amount, type (income/expense), category, date, description
- **FR1.2** View list of all transactions with sorting and filtering capabilities
- **FR1.3** Edit an existing transaction
- **FR1.4** Delete a transaction with confirmation dialog
- **FR1.5** Search transactions by description or category

### FR2: Category Management
- **FR2.1** Predefined income categories: 工资 (Salary), 兼职 (Part-time), 投资收益 (Investment), 奖金 (Bonus), 其他收入 (Other Income)
- **FR2.2** Predefined expense categories: 餐饮 (Food), 交通 (Transport), 购物 (Shopping), 住房 (Housing), 娱乐 (Entertainment), 医疗 (Healthcare), 教育 (Education), 其他支出 (Other Expense)
- **FR2.3** Ability to add custom categories
- **FR2.4** Ability to delete custom categories (predefined ones cannot be deleted)

### FR3: Dashboard & Reports
- **FR3.1** Dashboard showing current month summary: total income, total expense, net balance
- **FR3.2** Monthly report with income vs expense breakdown by category
- **FR3.3** Bar chart: monthly income vs expense comparison
- **FR3.4** Pie chart: expense breakdown by category for selected month
- **FR3.5** Pie chart: income breakdown by category for selected month
- **FR3.6** Month selector to view different monthly reports

### FR4: Data Management
- **FR4.1** All data persisted in SQLite database
- **FR4.2** Export transactions to CSV file

## 2. Non-Functional Requirements

### NFR1: Performance
- **NFR1.1** Page load time < 3 seconds on standard broadband
- **NFR1.2** Transaction CRUD operations < 500ms response time
- **NFR1.3** Report generation < 2 seconds for up to 1000 transactions

### NFR2: Usability
- **NFR2.1** Responsive design supporting desktop and tablet
- **NFR2.2** Clean, intuitive UI with Chinese language labels
- **NFR2.3** Form validation with clear error messages
- **NFR2.4** Confirm before destructive actions (delete)

### NFR3: Reliability
- **NFR3.1** Data integrity guaranteed via database constraints
- **NFR3.2** Graceful error handling with user-friendly messages
- **NFR3.3** Backend validation of all inputs

### NFR4: Maintainability
- **NFR4.1** Clean code with consistent formatting
- **NFR4.2** Unit test coverage >= 80% for backend logic
- **NFR4.3** Separation of concerns (components, services, API layers)

## 3. User Stories

| ID | User Story | Priority |
|----|-----------|----------|
| US1 | As a user, I want to add a new income/expense record so I can track my finances | P0 (Must Have) |
| US2 | As a user, I want to see all my transactions in a list so I can review past records | P0 (Must Have) |
| US3 | As a user, I want to categorize each transaction so I know where my money goes | P0 (Must Have) |
| US4 | As a user, I want to see a monthly summary so I understand my financial status | P0 (Must Have) |
| US5 | As a user, I want to see charts of my spending patterns so I can visualize my habits | P1 (Should Have) |
| US6 | As a user, I want to edit or delete a transaction if I made a mistake | P0 (Must Have) |
| US7 | As a user, I want to export my data to CSV so I can use it elsewhere | P2 (Could Have) |
| US8 | As a user, I want to search transactions so I can quickly find specific records | P1 (Should Have) |

## 4. Use Case Diagram (Textual)

```
Actor: User
Use Cases:
  - Add Transaction
  - View Transactions
  - Edit Transaction
  - Delete Transaction
  - Search Transactions
  - View Dashboard (Monthly Summary)
  - View Monthly Report (Charts)
  - Manage Categories
  - Export to CSV
```

## 5. Data Flow Diagram (Textual)

```
User Input --> [React UI] --> HTTP Request --> [Express API] --> SQL Query --> [SQLite DB]
                                                                                   |
User View  <-- [React UI] <-- JSON Response <-- [Express API] <-- Query Result <---+
```

## 6. Acceptance Criteria

| Feature | Acceptance Criteria |
|---------|-------------------|
| Add Transaction | Form validates all required fields; transaction appears in list immediately |
| View Transactions | Transactions displayed in reverse chronological order; filtering works |
| Edit Transaction | Pre-filled form opens; changes persist after save |
| Delete Transaction | Confirmation dialog; transaction removed from list |
| Dashboard | Shows correct totals for selected month; updates on new transaction |
| Monthly Report | Charts reflect correct data; month switching works |
| Export CSV | Downloads CSV file with correct transaction data |
| Form Validation | Amount must be positive number; date must be valid; category required |