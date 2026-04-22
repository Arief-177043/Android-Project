# 📈 GrowwX — AI-Powered Investment Tracker & Simulator

> A production-quality Android app built with **Kotlin + Jetpack Compose**, following **Clean Architecture** and **MVVM** patterns. Inspired by Groww's fintech design language.

---

## 📱 Screenshots

| Onboarding | Dashboard | Simulator | Watchlist | Alerts | Portfolio |
|---|---|---|---|---|---|
| 4-slide animated intro | Portfolio value + AI insights | Buy/sell with live prices | Real-time watchlist | Price alerts | Pie chart + holdings |

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 1.9 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt (Dagger) |
| Database | Room (SQLite) |
| Preferences | Jetpack DataStore |
| Networking | Retrofit 2 + OkHttp |
| Async | Kotlin Coroutines + Flow |
| Navigation | Navigation Compose |
| Charts | Canvas (custom) + Vico |
| Image loading | Coil |
| Biometrics | AndroidX Biometric |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |

---

## 🏗 Architecture

```
app/
└── src/main/java/com/growwx/
    ├── data/
    │   ├── api/           # Retrofit service + DTOs (Alpha Vantage)
    │   ├── local/         # Room DB, DAOs, DataStore preferences
    │   ├── model/         # Domain + Room entities
    │   └── repository/    # StockRepository, PortfolioRepository
    ├── di/                # Hilt module (AppModule)
    ├── ui/
    │   ├── auth/          # Login/Signup screen + ViewModel
    │   ├── dashboard/     # Home screen + ViewModel
    │   ├── watchlist/     # Watchlist + ViewModel
    │   ├── simulator/     # Buy/sell simulator + ViewModel (CORE)
    │   ├── alerts/        # Price alerts + ViewModel
    │   ├── portfolio/     # Portfolio + pie chart + ViewModel
    │   ├── profile/       # Profile + dark mode + ViewModel
    │   ├── onboarding/    # 4-slide intro + ViewModel
    │   ├── components/    # Reusable Composables
    │   ├── theme/         # Color, Typography, Theme
    │   └── navigation/    # NavGraph + Screen sealed class
    ├── util/              # Extensions, Analytics, BiometricHelper
    └── data/service/      # PriceAlertService (background notifications)
```

### Data Flow
```
UI (Composable)
    ↕ StateFlow / collectAsState()
ViewModel
    ↕ suspend fun / Flow
Repository
    ↕ DAO (Room) / API (Retrofit) / DataStore
Local DB / Network / Preferences
```

---

## ✨ Features

### 1. Authentication
- Email/password login & signup
- Persistent session via DataStore
- Form validation with inline error messages
- Demo account (no signup required)
- Biometric login support (`BiometricHelper.kt`)

### 2. Onboarding
- 4-slide animated intro with `AnimatedContent`
- Per-slide gradient theme
- Skip / Back / Next / Get Started flow
- One-time display, stored in DataStore

### 3. Portfolio Dashboard
- Total portfolio value (cash + holdings)
- Overall P&L (₹ and %)
- 30-day / 7-day chart (custom Canvas SVG)
- Quick action buttons (Simulate, Watchlist, Alerts)
- AI-generated insight cards (rule-based)
- Horizontal scroll of top gainers with sparklines

### 4. Investment Simulator (Core Feature)
- ₹1,00,000 virtual balance
- Buy & Sell stocks/crypto
- Weighted average buy price calculation
- Live price updates every 5 seconds (polling)
- Debounced search across all assets
- Max quantity auto-fill
- Transaction history (Room persisted)
- Holdings tracker with real-time P&L

### 5. Watchlist
- Add/remove stocks and crypto
- Real-time price + % change
- Sparkline mini charts
- Debounced search (300ms)
- Persisted in Room DB

### 6. Price Alerts
- Set above/below price targets
- Background service checks every 10s
- Push notification when triggered
- "Near target" warning badge
- Persisted in Room DB

### 7. Portfolio
- Pie chart allocation (custom Canvas)
- Per-holding P&L breakdown
- Overall summary card

### 8. Profile
- Dark mode toggle (DataStore persisted)
- Biometric toggle
- Logout

---

## 🚀 Setup Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34

### Step 1: Clone the repo
```bash
git clone https://github.com/yourusername/GrowwX.git
cd GrowwX
```

### Step 2: Get a free API key
1. Go to [alphavantage.co/support/#api-key](https://www.alphavantage.co/support/#api-key)
2. Sign up for a **free key** (5 requests/min, 500/day)

### Step 3: Add API key
Open `app/build.gradle.kts` and replace the key:
```kotlin
buildConfigField("String", "ALPHA_VANTAGE_KEY", "\"YOUR_KEY_HERE\"")
```

> **Note:** The app works fully without a key — it falls back to simulated live prices automatically. Perfect for demo/testing.

### Step 4: Build & Run
```bash
./gradlew assembleDebug
# or just press ▶️ Run in Android Studio
```

### Step 5: Generate release APK
```bash
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

---

## 🧪 Testing the App

| Flow | Steps |
|---|---|
| **Auth** | Tap "Try Demo Account" → instant login |
| **Buy stock** | Simulator tab → search "TCS" → Buy → enter qty → confirm |
| **Sell stock** | Holdings section → Sell → enter qty |
| **Add to watchlist** | Watchlist tab → + Add → search "BTC" → tap bookmark |
| **Set alert** | Alerts tab → New Alert → RELIANCE, above, 3000 |
| **Dark mode** | Profile → Dark Mode toggle |

---

## 🔑 Key Design Decisions

### Cache-First API Strategy
`StockRepository` checks a 60-second Room cache before hitting the network. On cache miss or network error, it falls back to simulated prices — so the app **never shows a blank screen**.

### Weighted Average Buy Price
When you buy the same stock multiple times, `PortfolioRepository.buy()` computes the correct weighted average:
```
newAvg = (prevAvg × prevQty + price × newQty) / (prevQty + newQty)
```

### Debounced Search
Both Watchlist and Simulator search use `Flow.debounce(300)` via `StateFlow` in the ViewModel — no UI-layer debounce hacks.

### StateFlow over LiveData
All ViewModels expose `StateFlow` — they're lifecycle-aware, testable without Android, and work seamlessly with `collectAsState()`.

### Single Activity
One `MainActivity` hosts the entire app. Navigation is handled by Compose Navigation with animated transitions.

---

## 📦 Dependencies Summary

```toml
# Core
androidx-core-ktx = "1.13.1"
compose-bom = "2024.05.00"
navigation-compose = "2.7.7"

# DI
hilt = "2.51.1"

# Network
retrofit = "2.11.0"
okhttp = "4.12.0"

# Storage
room = "2.6.1"
datastore = "1.1.1"

# Charts
vico = "1.15.0"

# Image
coil = "2.6.0"
```

---

## 🎯 Internship Submission Checklist

- [x] Kotlin + Jetpack Compose (not XML)
- [x] MVVM architecture
- [x] Hilt dependency injection
- [x] Room database with multiple entities
- [x] DataStore for preferences
- [x] Retrofit + OkHttp for API calls
- [x] Clean Architecture (data / domain / UI layers)
- [x] StateFlow + coroutines throughout
- [x] Reusable Compose components
- [x] Dark mode support
- [x] Animated navigation
- [x] Custom Canvas charts (sparklines + pie chart)
- [x] Background service (price alerts)
- [x] Biometric authentication helper
- [x] ProGuard rules
- [x] Meaningful commit structure (see git log)
- [x] README with setup instructions

---

## 👤 Author

**Your Name**
- GitHub: [@yourusername](https://github.com/yourusername)
- LinkedIn: [linkedin.com/in/yourprofile](https://linkedin.com/in/yourprofile)
- Email: you@example.com

---

## 📄 License

```
MIT License — free to use, modify, and distribute.
```

---

> Built as part of Groww SDE Android Internship Application — 2025
