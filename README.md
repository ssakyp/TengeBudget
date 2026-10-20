# Tenge Budget

A personal budget tracker for Android, built for the **Midterm Project**. It continues the Tenge Tracker line (HW1/HW2 → Task 2) with a multi-screen Jetpack Compose app: log income and expenses, search and filter them, track a monthly spending goal, and drill into any transaction's full detail.

## Features

1. **Add transactions** — title, amount, type (Income/Expense), category, and an optional note, with inline validation.
2. **Search & filter** — a search field plus type chips (All/Income/Expense) and a scrollable row of category chips on the Transactions screen; all three combine.
3. **Monthly budget goal** — the Dashboard shows a progress bar for this month's spending against a fixed budget, switching to a warning color once you go over — the feature that gives the app a purpose beyond plain CRUD.
4. **Transaction detail + delete** — tapping any row (on the Dashboard's recent list or the full Transactions list) opens a detail screen with every field, reached by passing the transaction's id through Navigation Compose; delete asks for confirmation first.

## Screens & navigation

A bottom navigation bar switches between the three main destinations; the detail screen is reached only by tapping a transaction and is not part of the bottom bar.

```
Dashboard  ──tap "See all"──▶ Transactions
    │                              │
    └──────tap a row───────────────┴──▶ Transaction Detail ──▶ back
Add  (bottom bar) ──submit──▶ Transactions
```

- **Dashboard** (`dashboard`) — totals, budget progress, recent activity.
- **Transactions** (`transactions`) — full list with search/filter.
- **Add** (`add`) — the entry form.
- **Transaction Detail** (`detail/{id}`) — every field of one transaction, with delete.

## Architecture

- `data/Models.kt` — plain Kotlin (`Transaction`, `Category`, `TxType`, `Summary`); no Android dependency.
- `BudgetViewModel.kt` — the single source of truth, created once where `AppNavHost` is hosted and handed down to every screen (state hoisting at the navigation-graph level). All derived data (recent list, filtered list, totals) is built with `combine`/`map`/`stateIn` over the underlying list, not stored separately.
- `ui/navigation/AppNavHost.kt` — the `NavHost` + bottom navigation bar.
- `ui/screens/` — one file per screen (`DashboardScreen`, `TransactionsScreen`, `AddTransactionScreen`, `TransactionDetailScreen`); each one only reads from/calls the shared ViewModel and holds no business logic of its own.
- `ui/components/Components.kt` — 4 reusable, stateless composables: `SummaryCard`, `SelectableChip`, `TransactionRow`, `BudgetProgressBar`.

### `remember` vs `rememberSaveable` vs `ViewModel`, in this app

| Tool | Used for | Why |
|---|---|---|
| `remember` | The Add screen's validation error message; the delete-confirmation dialog's visibility on the Detail screen | Purely transient UI state |
| `rememberSaveable` | The Add screen's draft fields (title, amount, note, type, category) | Should survive rotation before submission, but has no reason to live in the ViewModel once added |
| `ViewModel` (`StateFlow`) | The transaction list, search text, type/category filters, the monthly budget | The actual application data — must survive both rotation **and** navigating between screens, since every screen shares the same instance |

### Configuration changes

`BudgetViewModel` is obtained with the default `viewModel()` factory where `AppNavHost` is hosted, so it's scoped to the Activity and outlives Activity recreation (rotation). The `NavController`'s back stack is similarly preserved by `rememberNavController()` across recomposition. In practice: rotate the device on any screen — the current route, the transaction list, filters, and any in-progress Add-form draft are all exactly as you left them.

## Running the project

1. Open the project folder in Android Studio (Koala or newer).
2. Let Gradle sync (needs network access to `google()` and `mavenCentral()`).
3. Run the `app` configuration on an emulator or device with API 26+.

No backend, no network permissions, no API keys needed — everything is in-memory for this assignment ("may be extended with databases, networking... during the second half of the semester").

## Screenshots

> 3 screenshots added after running the app.
