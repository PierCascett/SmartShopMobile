<h1 align="center">SmartShop Mobile</h1>

<p align="center">
  <em>Mobile Programming Project 2024-25</em>
  <br><br>
  <a href="https://github.com/PierCascett/SmartShopMobile/commits"><img alt="Last commit" src="https://img.shields.io/github/last-commit/PierCascett/SmartShopMobile?logo=github&label=last%20commit&display_date=relative"></a>
  <a href="#features"><img alt="Android app" src="https://img.shields.io/badge/app-Android-3DDC84?logo=android&logoColor=fff"></a>
  <a href="#backend-setup-express--postgresql"><img alt="Backend API" src="https://img.shields.io/badge/backend-Node.js%20%7C%20Express-000?logo=node.js&logoColor=fff"></a>
  <br><br>
  Built with the tools and technologies:
  <br><br>
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=fff">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=fff">
  <img alt="Android" src="https://img.shields.io/badge/Android-3DDC84?logo=android&logoColor=fff">
  <img alt="Material3" src="https://img.shields.io/badge/Material%203-6200EE?logo=materialdesign&logoColor=fff">
  <img alt="Coroutines" src="https://img.shields.io/badge/Coroutines-0095D5?logo=kotlin&logoColor=fff">
  <img alt="Retrofit" src="https://img.shields.io/badge/Retrofit-3F7EAB?logo=square&logoColor=fff">
  <img alt="OkHttp" src="https://img.shields.io/badge/OkHttp-2F7EAB?logo=square&logoColor=fff">
  <img alt="Room" src="https://img.shields.io/badge/Room-20232A?logo=sqlite&logoColor=61DAFB">
  <img alt="DataStore" src="https://img.shields.io/badge/DataStore-20232A?logo=google&logoColor=4285F4">
  <img alt="Coil" src="https://img.shields.io/badge/Coil-FF6F00?logo=android&logoColor=fff">
  <img alt="Node.js" src="https://img.shields.io/badge/Node.js-339933?logo=node.js&logoColor=fff">
  <img alt="Express" src="https://img.shields.io/badge/Express-000?logo=express&logoColor=fff">
  <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=fff">
  <img alt="Swagger" src="https://img.shields.io/badge/Swagger-85EA2D?logo=swagger&logoColor=000">
  <img alt="npm" src="https://img.shields.io/badge/npm-CB0000?logo=npm&logoColor=fff">
  <img alt=".env" src="https://img.shields.io/badge/.env-404040?logo=dotenv&logoColor=fff">
  <img alt="Gradle" src="https://img.shields.io/badge/Gradle-02303A?logo=gradle&logoColor=fff">
  <img alt="JaCoCo" src="https://img.shields.io/badge/JaCoCo-9C27B0?logo=checkmarx&logoColor=fff">
  <img alt="Robolectric" src="https://img.shields.io/badge/Robolectric-3B3B3B?logo=android&logoColor=fff">
  <img alt="MockWebServer" src="https://img.shields.io/badge/MockWebServer-4A90E2?logo=square&logoColor=fff">
  <img alt="MockK" src="https://img.shields.io/badge/MockK-512BD4?logo=kotlin&logoColor=fff">
</p>

---

## Authors
<p align="center">
  <a href="https://github.com/cascett2002">Michele Cascione</a> &nbsp; • &nbsp;
  <a href="https://github.com/Pier2690">Pierluigi Boscaglia</a>
</p>

---

## Table of Contents
- [Authors](#authors)
- [Overview](#features)
- [Getting Started](#backend-setup-express--postgresql)
  - [Prerequisites](#prerequisites)
  - [Installation](#backend-setup-express--postgresql)
  - [Usage](#android-app-configuration)
  - [Testing](#build-and-test)
- [Notes](#useful-notes)

---

Android application built with Jetpack Compose for a smart supermarket, featuring dedicated roles (customer, employee, manager) and a Node.js/Express backend with PostgreSQL.  

## Features
- **Customer**: sign-up/login, catalog browsing, search and category filters, cart and favorites, product detail, order creation with locker or home delivery, order history.
- **Employee**: order claiming/assignment, store map for aisle picking, line-by-line pick check, order history, profile.
- **Manager**: inventory and shelf control, restock to suppliers, stock transfers, product and supplier management, profile.
- **Account and session**: user preferences via DataStore, local cache of orders/products with Room for offline-first.
- **UI**: Material Design 3, overlays for menu/cart/favorites, avatars via Coil.
- **Backend**: REST API documented with Swagger, static images served from `/images`, HTTP logging via morgan.

## Tech stack
- **Mobile**: Kotlin, Jetpack Compose, Material3, MVVM (ViewModel + StateFlow), Room, DataStore, Retrofit + OkHttp logging, Coil, Coroutines, ZXing (QR).
- **Backend**: Node.js, Express, PostgreSQL (pg), bcrypt, Swagger UI, CORS, dotenv.
- **Tooling**: Gradle KTS, KSP for Room, JaCoCo for coverage, Robolectric/MockWebServer for tests.

## Project structure
- `app/`: Android app (`MainActivity`, ViewModels, Compose UI, Room DB, Retrofit client).
- `backend/`: Express API (`server.js`, routes for auth, categories, products, orders, restocks, shelves, inventory, suppliers) and static `images/`.
- `app/src/main/java/.../data/SmartshopDumpV2.sql`: PostgreSQL dump with sample data.
- `backend/swagger/SwaggerDocumentation.json`: OpenAPI definition shown in Swagger UI.

## Prerequisites
- Android Studio (JDK 17), SDK 26+.
- Node.js 18+ and npm.
- PostgreSQL 14+ reachable from the backend.
- Android emulator or physical device on the same network as the backend.

## Backend setup (Express + PostgreSQL)
1) `cd backend` and install dependencies:
   ```bash
   npm install
   ```
2) Configure DB/host (defaults live in `backend/db.js`; edit there or override via env vars):
   - Defaults: `DB_HOST=localhost`, `DB_PORT=5432`, `DB_NAME=SmartShopMobileV2`, `DB_USER=postgres`, `DB_PASSWORD=12345`.
   - Optional `.env` (only if you want to override without editing the file):
     ```env
     PORT=3000
     DB_HOST=localhost # PostgreSQL host (the `host` field in db.js)
     DB_PORT=5432
     DB_NAME=SmartShopMobileV2
     DB_USER=postgres
     DB_PASSWORD=<YOUR_DB_PASSWORD>
     ```
3) Import the sample dump into PostgreSQL:
   ```bash
   psql -U postgres -f ../app/src/main/java/it/unito/smartshopmobile/data/SmartshopDumpV2.sql
   ```
4) Start the server:
   ```bash
   npm run dev       # or npm start
   ```
   - Swagger UI: `http://<HOST>:<PORT>/api/docs`
   - Health check: `http://<HOST>:<PORT>/health`
   - Static images: `http://<HOST>:<PORT>/images/...`
   - Windows helper script: `start-server.bat` (also manages firewall rule).

## Android app configuration
1) Open the project in Android Studio.
2) Backend host/port come from `BuildConfig.BACKEND_HOST` / `BuildConfig.BACKEND_PORT`, injected in `app/build.gradle.kts`. The build script auto-detects your machine IPv4 (fallback `10.0.2.2` for emulator):
   ```kotlin
   buildConfigField("String", "BACKEND_HOST", "\"$detectedBackendHost\"") // reachable by emulator/phone
   buildConfigField("String", "BACKEND_PORT", "\"3000\"")
   ```
   `RetrofitInstance` already reads these values; no manual changes needed there.
3) Sync Gradle and run the app on emulator or device (same network as backend).

### Build and test
- Unit & integration tests:  
  ```bash
  ./gradlew testDebugUnitTest
  ```
- Coverage (no emulator needed):  
  ```bash
  ./gradlew generateBasicCoverageReports
  ```
  - Unit/ViewModel: `app/build/reports/jacoco/unitViewModelTest/html/index.html`  
  - Integration: `app/build/reports/jacoco/integrationTest/html/index.html`
- UI tests + coverage (emulator/device required):  
  ```bash
  ./gradlew createDebugCoverageReport jacocoUITestReport
  ```
  - UI: `app/build/reports/jacoco/uiTest/html/index.html`
- All combined (requires UI execution data):  
  ```bash
  ./gradlew jacocoAllTestsReport
  ```
  - Combined: `app/build/reports/jacoco/allTests/html/index.html`
 
## Useful notes
- Roles and routing are defined in `ui/domain/UserRole.kt` and orchestrated by `MainActivity`/`MainViewModel`.
- User data persists in `SessionDataStore`; Room caches orders and products.
- Retrofit calls target `/api/...` (base URL `http://<HOST>:<PORT>/api/`); images use `http://<HOST>:<PORT>/`.
- For production consider disabling HTTP logging and using HTTPS/BuildConfig for configurable host/port.
- For more functional details, download and read `Project_Description.pdf` in the repo root.
