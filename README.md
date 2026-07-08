# PulseSpend

A lightweight, offline-first Android app for tracking monthly spending against a budget. It detects
expenses automatically from bank/UPI **SMS** alerts (you confirm before anything is saved) and also
supports quick manual entry. Everything is stored locally on the device — no accounts, no network.

---

## Features

### Budgeting
- **Monthly budget** with a live "remaining this month" view and total spent vs. limit.
- **Per-category limits** — set a cap for any category; the app warns as you approach/exceed it.
- **Category alerts** — a dialog surfaces when a transaction pushes a category past its threshold
  (≈90% of the limit), shown above every screen so it never gets missed.
- **Month rollover** — budgets and category limits carry forward to a new month; spending resets.

### Transactions
- **Add / edit / delete** transactions, each with amount, description, and category.
- **Description autocomplete** — suggests descriptions you've used before.
- **Filter by category** on the transactions list; clear a whole month at once.
- **Default categories**: Food, Transport, Rent, Shopping, Utilities, Entertainment, Health,
  Eating Out, Other.

### Automatic SMS detection
- A `BroadcastReceiver` reads incoming SMS and a pure, unit-tested parser (`SmsParser`) extracts the
  amount, merchant, and debit/credit type from Indian bank/UPI messages (HDFC, SBI, ICICI, Paytm,
  generic UPI, card swipes).
- **Only expenses (debits) are surfaced**; credits, OTPs, promos, and failed/declined messages are
  ignored. Nothing is ever saved without your confirmation.
- When the app is **in the foreground**, a bottom sheet pre-filled with amount/merchant/category pops
  up for review. When **backgrounded**, a notification appears — tapping it opens the same sheet.
- Merchants are auto-categorized via keyword rules (`CategoryMatcher`), e.g. Swiggy/Zomato →
  Eating Out, Uber/Ola → Transport, Amazon → Shopping.

### Insights & history
- **Charts** — spending breakdown by category for the current month.
- **History** — browse past months and drill into any month's detail.

---

## Tech stack

| Area | Choice |
|------|--------|
| Language | Kotlin 1.9.24 |
| UI | Jetpack Compose (Material 3), Navigation Compose |
| Architecture | MVVM + Repository; manual DI (no Hilt/Koin) |
| Persistence | Room 2.6.1 (SQLite) with schema migrations |
| Async | Coroutines + `Flow` / `StateFlow` |
| Build | AGP 8.5.2, KSP, Gradle version catalog (`gradle/libs.versions.toml`) |
| Min / Target / Compile SDK | 26 / 34 / 34 |
| JDK | 17 (required) |

### Project layout
```
app/src/main/java/com/expensetracker/
├── MainActivity.kt        # Compose entry, navigation graph, drawer
├── ExpenseApp.kt          # Application: builds the repository (manual DI), tracks foreground
├── data/                  # Room: AppDatabase, Entities, Daos, RoomExpenseRepository
├── domain/                # Models, Categories, BudgetCalculator, LimitValidator, ChartData, …
├── sms/                   # SmsReceiver, SmsParser, CategoryMatcher, SmsNotifier, SmsTransactionBus
├── ui/                    # Compose screens + ExpenseViewModel
└── util/                  # MoneyFormat, MonthFormat
```

---

## Prerequisites

- **JDK 17**
- **Android SDK** with platform 34 (`compileSdk = 34`)
- Android Studio (recommended) or just the command line + `adb`
- A device/emulator on **API 26+**

Create `local.properties` at the repo root pointing at your SDK (Android Studio does this
automatically):
```properties
sdk.dir=/Users/<you>/Library/Android/sdk
```

---

## Build

```bash
# Compile + assemble the debug APK (output: app/build/outputs/apk/debug/app-debug.apk)
./gradlew :app:assembleDebug

# Release build
./gradlew :app:assembleRelease
```

## Run

**From Android Studio:** open the project, pick a device, press ▶.

**From the command line** (device/emulator connected, `adb` on PATH):
```bash
# Build + install on the connected device
./gradlew :app:installDebug

# Launch it
adb shell monkey -p com.expensetracker -c android.intent.category.LAUNCHER 1
```

On first launch the app requests SMS + notification permissions. If denied, the app stays fully
usable — only automatic SMS detection goes dormant.

---

## Test

```bash
# JVM unit tests (parser, budget logic, view models — fast, no device)
./gradlew :app:testDebugUnitTest

# Instrumented UI tests (needs a connected device/emulator)
./gradlew :app:connectedDebugAndroidTest
```

Unit tests live in `app/src/test/` (e.g. `SmsParserTest`, `BudgetCalculatorTest`,
`ExpenseViewModelTest`) and use JUnit 4 + Turbine + a `FakeExpenseRepository`. Test reports are
written to `app/build/reports/tests/`.

---

## Debug

### Logs
Key components log under their own tags:
```bash
# SMS detection pipeline
adb logcat -s SmsReceiver

# Everything from the app process
adb logcat --pid=$(adb shell pidof -s com.expensetracker)
```

### Simulating an SMS without sending a real text
`SmsParser` is pure, so the fastest check is a unit test. To exercise the full on-device pipeline,
broadcast a fake SMS to the receiver (emulator, or a debug build):
```bash
adb shell am broadcast -a android.provider.Telephony.SMS_RECEIVED \
  --es body "Sent Rs.500 from HDFC Bank A/C x1234 To ZOMATO on 01/01/26"
```
> Note: delivery of synthetic `SMS_RECEIVED` broadcasts is restricted on many OS versions/OEMs; the
> reliable path is to test the parser via unit tests and the UI via the confirm-sheet flow.

### Inspecting the local database
```bash
adb shell run-as com.expensetracker ls databases/          # debug build only
# Pull and open expense-tracker.db with any SQLite browser
```

### Common issues
- **`adb devices` shows nothing after plugging in:** `adb kill-server && adb start-server`, and
  accept the "Allow USB debugging" prompt on the phone.
- **Build can't find the SDK:** check `local.properties` `sdk.dir`.
- **Wrong JDK:** ensure Gradle runs on JDK 17 (`./gradlew -version`).

---

## Notes

- The app is **fully offline** — no analytics, no backend, no network permission.
- `READ_SMS` / `RECEIVE_SMS` are used only to detect transaction alerts locally; SMS content never
  leaves the device.
- Data persists across reinstalls of the same package; uninstalling clears it.
