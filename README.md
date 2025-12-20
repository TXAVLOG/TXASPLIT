# ████████ ██   ██  █████   █████  ██████  ██████  
#    ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██ 
#    ██      ███   ███████ ███████ ██████  ██████  
#    ██     ██ ██  ██   ██ ██   ██ ██      ██      
#    ██    ██   ██ ██   ██ ██   ██ ██      ██      

# TXASplit

**TXASplit** là ứng dụng Android quản lý chi tiêu nhóm, hỗ trợ tạo hóa đơn, thanh toán VietQR, thống kê và xuất báo cáo Excel.

---

## 📥 Tải APK

Tải phiên bản mới nhất tại [**Releases**](https://github.com/TXAVLOG/TXASPLIT/releases).

---

## 🛠️ Setup & Build từ Source

### Yêu cầu hệ thống

- **JDK**: 17 trở lên
- **Android SDK**: API 35 (compileSdk), minSdk 26
- **Build Tools**: 35.0.0
- **Kotlin**: 2.0.21
- **Gradle**: 8.13.2 (wrapper)

### Bước 1: Clone repository

```bash
git clone https://github.com/TXAVLOG/TXASPLIT.git
cd TXASPLIT
```

### Bước 2: Cài đặt Android SDK (nếu chưa có)

Sử dụng Android Studio hoặc command-line tools:

```bash
# Cài đặt SDK platform & build-tools cần thiết
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

### Bước 3: Build APK

#### Sử dụng Gradle Wrapper (khuyến nghị)

```bash
# Windows
.\gradlew.bat assembleDebug

# Linux/macOS
./gradlew assembleDebug
```

APK sẽ được tạo tại: `app/build/outputs/apk/debug/app-debug.apk`

#### Build Release (signed)

```bash
.\gradlew.bat assembleRelease
```

> **Lưu ý**: Để build release, cần cấu hình signing key trong `app/build.gradle.kts` hoặc `keystore.properties`.

---

## 📦 Cấu trúc Project

```
TXASPLIT/
├── app/                          # Module ứng dụng chính
│   ├── src/main/
│   │   ├── java/ke/txasplit/vk/  # Source code Kotlin
│   │   │   ├── core/             # Utilities (HTTP, VietQR, Format...)
│   │   │   ├── data/             # Room Database, DAO, Entity
│   │   │   ├── domain/           # Use cases
│   │   │   ├── ui/               # Activities, Fragments, Adapters
│   │   │   ├── update/           # Auto-update logic
│   │   │   └── ...
│   │   ├── res/                  # Resources (layouts, drawables, strings)
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts          # App-level Gradle config
│   └── schemas/                  # Room database schemas
├── build.gradle.kts              # Root-level Gradle config
├── version.txa                   # Version management (1.3.0_txa)
├── gradle.properties             # Gradle settings
└── settings.gradle.kts           # Project settings
```

---

## 🚀 Tính năng chính

- **Quản lý nhóm**: Tạo/tham gia nhóm, phân quyền thành viên (Admin/Member)
- **Hóa đơn & Thanh toán**: Tạo bill, ghi nhận payment, tích hợp VietQR
- **Thống kê**: Biểu đồ chi tiêu theo thành viên/thời gian (MPAndroidChart)
- **Xuất Excel**: Export báo cáo chi tiết (Apache POI)
- **Auto-update**: Tự động kiểm tra & cài đặt bản cập nhật mới
- **Notification**: Nhắc nhở hóa đơn quá hạn, xác minh thanh toán

---

## 🧰 Dependencies chính

- **Kotlin**: 2.0.21
- **Room**: 2.6.1 (Database)
- **Hilt**: 2.52 (Dependency Injection)
- **OkHttp**: 4.12.0 (HTTP client)
- **Kotlinx Serialization**: 1.7.3 (JSON parsing)
- **Glide**: 4.16.0 (Image loading cho VietQR)
- **MPAndroidChart**: 3.1.0 (Biểu đồ thống kê)
- **Apache POI**: 5.2.5 (Excel export)

---

## 📝 Version Management

Project sử dụng file `version.txa` để quản lý version:

```bash
# Xem version hiện tại
.\gradlew.bat updateVersion

# Tăng patch version (1.3.0 -> 1.3.1)
.\gradlew.bat incrementVersion -Ppatch

# Tăng minor version (1.3.0 -> 1.4.0)
.\gradlew.bat incrementVersion -Pminor

# Tăng major version (1.3.0 -> 2.0.0)
.\gradlew.bat incrementVersion -Pmajor
```

---

## 🐛 Troubleshooting

### Lỗi "SDK location not found"

Tạo file `local.properties` trong thư mục root:

```properties
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
```

### Lỗi build-tools version

Kiểm tra version build-tools đã cài:

```bash
sdkmanager --list | findstr build-tools
```

Cài đặt version cần thiết:

```bash
sdkmanager "build-tools;35.0.0"
```

---

## 📧 Liên hệ

- **Facebook**: [vlog.txa.2311](https://fb.com/vlog.txa.2311)
- **Email**: txavlog7@gmail.com

---

## 📄 License

Dự án này được phát triển bởi **TXA** cho mục đích học tập và phi thương mại.
