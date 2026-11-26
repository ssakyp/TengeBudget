# Tenge Budget

Android (Jetpack Compose) port of the *Tenge Tracker* web/API project: **accounts, categories, monthly budgets and income / expense / transfer transactions**, plus savings goals — all stored locally with Room. Task 3: MVVM + Room + an advanced feature.

## Data model (mirrors the web API)

```
Account 1──N Transaction N──1 Category 1──N Budget          SavingsGoal 1──N Contribution
              └─ target account (transfers)
```

| Entity | Fields | Rules |
|---|---|---|
| `Account` | name, balance, currency (3 letters) | Opening balance set on creation; afterwards only transactions change it. Can't be deleted while it has transactions. Currency locked once used. |
| `Category` | name, type (INCOME / EXPENSE) | User-managed (15 defaults seeded). Can't be deleted or change type while used by a transaction or budget. |
| `Transaction` | account, optional category, optional target account, title, amount, type, date, note | Always belongs to an account. Category type must match the transaction type. |
| `Budget` | category, month, year, limit | EXPENSE categories only; one per category per month. |
| `SavingsGoal` / `Contribution` | name, target, status / deposit-withdrawal history | See below. |

## Features

- **Accounts** — list with total per currency, detail screen with the account's full history (as source *or* transfer destination), create / edit / delete (confirmation; refused with an explanation if it has transactions).
- **Transactions** — INCOME, EXPENSE and **TRANSFER** between two accounts. Create / edit / delete (confirmation + **Undo**), date picker, validated form. Search plus filters by type, account and category.
- **Balances stay correct** — every add / edit / delete is applied in a single Room transaction: income adds, expense subtracts, transfer moves money; editing or deleting first reverses the old effect. A change that would make any balance negative ("Not enough balance in …") is rejected and **rolled back entirely**. Transfers between different currencies are refused.
- **Categories** — manage income / expense categories (dialog create / edit, confirm delete).
- **Budgets** — pick a month, set a limit per expense category, see live *spent / remaining / over by* progress computed from that month's transactions.
- **Dashboard** — month selector with income, expenses and net; total assets; budget roll-up with "exceeded" count; goals summary; recent activity.
- **Savings goals** (extra) — targets with deposits / withdrawals, history, statistics and a status workflow `ACTIVE ⇄ PAUSED`, `ACTIVE → COMPLETED` (reopens on withdrawal or higher target).

Amounts, summaries and budgets are in KZT; accounts in other currencies are shown in their own currency but not added into KZT totals.

## Task 3 requirements → where

| Requirement | Implementation |
|---|---|
| `LazyColumn` with stable keys | Every list uses `key = { id }` |
| List + detail with Navigation Compose | Transactions, Accounts, Goals (+ Categories, Budgets); `Long` id arguments |
| Create / edit forms with validation | Transaction, account, goal forms; category and budget dialogs |
| Delete with confirmation | `ConfirmDeleteDialog` for transactions, accounts, categories, budgets, goals |
| Advanced feature | Multi-account balance management (income / expense / transfer with reversal and rollback), monthly budgets with live status, savings-goal workflow |
| Empty states + feedback | Every list has an empty state; app-wide snackbar ("Transaction added", "Not enough balance…", "Deleted … Undo") |
| `ViewModel` + `StateFlow`, `collectAsStateWithLifecycle()` | 12 ViewModels in `viewmodel/` |
| Layering | `ui` → `viewmodel` → `data/repository` → `data/local` (Room) |
| State hoisting / reusable composables | `ui/components/` (rows, cards, chips, dialogs, month selector…) |
| Actions through ViewModel methods | Composables only call ViewModel functions |
| Room: Entities + DAOs, CRUD, Repository, Flow | 6 entities, 5 DAOs, 6 repositories; DAOs return `Flow` |
| Persistence | Data survives restarts; categories and two demo accounts are seeded on first launch |
| Cross-screen consistency | Shared Activity-scoped ViewModels + Room flows — no manual refresh |
| Config changes | Form drafts and dialog state live in ViewModels |

## Architecture

```
ui/              Composables (screens, components, navigation, theme)
viewmodel/       StateFlow UI state, validation, user-action methods
data/repository/ Business rules (balance effects, uniqueness, in-use checks)
data/local/      Room: AppDatabase, entities, DAOs, converters
di/              AppContainer (hand-rolled DI created in TengeApp)
```

## Running

Open in Android Studio, sync Gradle (Room via KSP), run `app` on API 26+. The database version is 2: if you installed an earlier build, the old database is rebuilt automatically (or clear the app's data).

## Screenshots

> Add 3+ screenshots to `screenshots/` after running the app.
