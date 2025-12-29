# TXASplit Ubuntu VPS Build Script

Script build tự động cho TXASplit trên VPS Ubuntu, có khả năng push kết quả lên Git.

## 🚀 Cách sử dụng

### 1. Chuẩn bị VPS Ubuntu
```bash
# Clone repository
git clone https://github.com/TXAVLOG/TXASPLIT.git
cd TXASPLIT

# Chạy script build (chmod executable trước)
chmod +x tools/build_ubuntu.sh
```

### 2. Build Debug APK
```bash
# Build debug (mặc định)
./tools/build_ubuntu.sh

# Hoặc chỉ định rõ
./tools/build_ubuntu.sh debug
```

### 3. Build Release APK
```bash
# Build release với ký tự số
./tools/build_ubuntu.sh release
```

## 📋 Tính năng

### ✅ Tự động cài đặt môi trường
- OpenJDK 17
- Git, wget, curl
- Cấu hình JAVA_HOME

### ✅ Build process
- Clean build trước khi build
- Hỗ trợ cả Debug và Release
- Tự động tạo keystore cho Release
- Copy APK vào thư mục `TXABUILD/`

### ✅ Git Integration
- **Build thành công:** Push APK và log lên Git
- **Build thất bại:** Push log lỗi lên Git
- Commit message có timestamp và thông tin chi tiết

### ✅ Error handling
- Log toàn bộ process vào `build.log`
- Hiển thị lỗi rõ ràng với màu sắc
- Exit code phù hợp

## 📁 Output

### Build thành công
```
TXABUILD/
├── TXASplit-debug.apk     # Debug APK
└── TXASplit-release.apk   # Release APK
```

### Git commits
- **Thành công:** `build: Successful debug build - APK generated (X.X MB) [YYYYMMDD_HHMMSS]`
- **Thất bại:** `build: Failed debug build - Lỗi chi tiết [YYYYMMDD_HHMMSS]`

## 🔧 Cấu hình

### Environment variables
- `JAVA_HOME`: Tự động set cho OpenJDK 17
- `PATH`: Tự động thêm Java bin

### Keystore (Release)
- File: `keystore/txasplit.keystore`
- Store password: `txasplit-store`
- Key alias: `txasplit`
- Key password: `txasplit-key`

## 🎯 Output examples

### Build thành công
```
╔══════════════════════════════════════════════════════════════╗
║              ✅ BUILD THÀNH CÔNG!                            ║
╚══════════════════════════════════════════════════════════════╝

📦 APK đã được tạo thành công!
📁 Đường dẫn: TXABUILD/TXASplit-debug.apk
💾 Dung lượng: 12.5 MB
```

### Build thất bại
```
╔══════════════════════════════════════════════════════════════╗
║              ❌ BUILD THẤT BẠI!                              ║
╚══════════════════════════════════════════════════════════════╝

❌ Lỗi: Gradle build thất bại
❌ Exit code: 1
```

## 🔄 CI/CD Integration

Script có thể tích hợp vào:
- GitHub Actions
- GitLab CI
- Jenkins
- Cron jobs

### Cron example (build hàng ngày)
```bash
# Build debug lúc 2AM hàng ngày
0 2 * * * cd /path/to/TXASPLIT && ./tools/build_ubuntu.sh debug
```

## 🛠️ Troubleshooting

### Common issues
1. **Permission denied:** `chmod +x tools/build_ubuntu.sh`
2. **Java not found:** Script sẽ tự cài đặt OpenJDK 17
3. **Git auth error:** Cấu hình Git credentials trước
4. **Gradle daemon stuck:** Script tự dừng daemon trước/after build

### Logs
- Console output: Real-time với màu sắc
- File log: `build.log` (push lên Git khi thất bại)

## 📞 Support

- **Facebook:** https://fb.com/vlog.txa.2311
- **Email:** txavlog7@gmail.com
- **GitHub:** https://github.com/TXAVLOG/TXASPLIT
