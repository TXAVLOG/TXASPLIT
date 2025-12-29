# ████████ ██   ██  █████   █████  ██████  ██████  
#    ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██ 
#    ██      ███   ███████ ███████ ██████  ██████  
#    ██     ██ ██  ██   ██ ██   ██ ██      ██      
#    ██    ██   ██ ██   ██ ██   ██ ██      ██      
#
# Build Fix Script for TXASplit
# Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com

param(
    [switch]$Deep,
    [switch]$Release,
    [switch]$Help
)

$ErrorActionPreference = "Stop"

function Show-Help {
    Write-Host @"
╔══════════════════════════════════════════════════════════════╗
║           🔧 TXASplit Build Fix Script                        ║
║           Build by TXA                                       ║
╚══════════════════════════════════════════════════════════════╝

📖 Sử dụng:
  .\fix_build.ps1              # Clean và build debug
  .\fix_build.ps1 -Deep        # Deep clean (xóa .gradle)
  .\fix_build.ps1 -Release     # Build release
  .\fix_build.ps1 -Help        # Hiển thị hướng dẫn

🔧 Các bước thực hiện:
  1. Dừng Gradle daemon
  2. Clean build cache
  3. Xóa build folders
  4. Rebuild project

"@ -ForegroundColor Cyan
    exit 0
}

function Write-Step {
    param([string]$Message)
    Write-Host "`n▶ $Message" -ForegroundColor Yellow
}

function Write-Success {
    param([string]$Message)
    Write-Host "✅ $Message" -ForegroundColor Green
}

function Write-Error-Custom {
    param([string]$Message)
    Write-Host "❌ $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "ℹ️  $Message" -ForegroundColor Gray
}

if ($Help) {
    Show-Help
}

# Kiểm tra đang ở root project
if (-not (Test-Path "gradlew.bat")) {
    Write-Error-Custom "Không tìm thấy gradlew.bat. Vui lòng chạy script từ thư mục root của project."
    exit 1
}

Write-Host @"
╔══════════════════════════════════════════════════════════════╗
║           🔧 TXASplit Build Fix Script                      ║
║           Build by TXA                                       ║
╚══════════════════════════════════════════════════════════════╝
"@ -ForegroundColor Cyan

# Bước 1: Dừng Gradle daemon
Write-Step "Dừng Gradle daemon..."
try {
    & .\gradlew.bat --stop 2>&1 | Out-Null
    Write-Success "Gradle daemon đã dừng"
    # Đợi một chút để đảm bảo daemon đã giải phóng tất cả file
    Start-Sleep -Seconds 2
} catch {
    # Ignore errors khi dừng daemon
    Write-Info "Không có daemon đang chạy hoặc đã dừng"
}

Write-Step "Dung Java loi..."
taskkill /IM java.exe /F

# Bước 1.5: Xử lý cache corrupted (luôn xóa kotlin-dsl để tránh lỗi metadata)
Write-Step "Xóa cache Kotlin DSL (để tránh lỗi metadata)..."
$gradleCacheBase = "$env:USERPROFILE\.gradle\caches"

# Xóa kotlin-dsl cache (thường gây lỗi metadata)
$kotlinDslPaths = @()
if (Test-Path $gradleCacheBase) {
    $gradleVersionDirs = Get-ChildItem -Path $gradleCacheBase -Directory -ErrorAction SilentlyContinue
    foreach ($versionDir in $gradleVersionDirs) {
        $kotlinDslPath = Join-Path $versionDir.FullName "kotlin-dsl"
        if (Test-Path $kotlinDslPath) {
            $kotlinDslPaths += $kotlinDslPath
        }
    }
}

if ($kotlinDslPaths.Count -gt 0) {
    Write-Info "Phát hiện $($kotlinDslPaths.Count) Kotlin DSL cache, đang xóa..."
    foreach ($path in $kotlinDslPaths) {
        try {
            Remove-Item -Recurse -Force $path -ErrorAction SilentlyContinue
        } catch {
            # Ignore errors
        }
    }
    Write-Success "Đã xóa Kotlin DSL cache"
} else {
    Write-Info "Không tìm thấy Kotlin DSL cache"
}

# Xóa transforms cache (thường gây lỗi metadata.bin)
# Xóa toàn bộ thư mục transforms và tất cả thư mục con bên trong
$transformsPaths = @()
if (Test-Path $gradleCacheBase) {
    $gradleVersionDirs = Get-ChildItem -Path $gradleCacheBase -Directory -ErrorAction SilentlyContinue
    foreach ($versionDir in $gradleVersionDirs) {
        $transformsPath = Join-Path $versionDir.FullName "transforms"
        if (Test-Path $transformsPath) {
            $transformsPaths += $transformsPath
        }
    }
}

if ($transformsPaths.Count -gt 0) {
    Write-Info "Phát hiện $($transformsPaths.Count) transforms cache, đang xóa..."
    foreach ($path in $transformsPaths) {
        try {
            # Xóa tất cả thư mục con trước
            $subDirs = Get-ChildItem -Path $path -Directory -ErrorAction SilentlyContinue
            foreach ($subDir in $subDirs) {
                try {
                    Remove-Item -Recurse -Force $subDir.FullName -ErrorAction SilentlyContinue
                } catch {
                    # Ignore individual errors
                }
            }
            # Sau đó xóa thư mục transforms
            Remove-Item -Recurse -Force $path -ErrorAction SilentlyContinue
        } catch {
            # Ignore errors
        }
    }
    Write-Success "Đã xóa transforms cache"
} else {
    Write-Info "Không tìm thấy transforms cache"
}

# Xóa cache journal corrupted (nếu có)
$gradleCacheJournal = "$gradleCacheBase\journal-1"
if (Test-Path "$gradleCacheJournal\file-access.bin") {
    Write-Info "Phát hiện cache journal có thể bị corrupted, đang xóa..."
    try {
        Remove-Item -Recurse -Force "$gradleCacheJournal\*" -ErrorAction SilentlyContinue
        Write-Success "Đã xóa cache journal corrupted"
    } catch {
        Write-Info "Không thể xóa cache journal"
    }
}

# Bước 2: Deep clean TRƯỚC (nếu được yêu cầu) - phải làm trước clean để tránh lỗi
if ($Deep) {
    Write-Step "Deep clean - Xóa .gradle cache..."
    if (Test-Path ".gradle") {
        Remove-Item -Recurse -Force .gradle -ErrorAction SilentlyContinue
        Write-Success "Đã xóa .gradle cache"
    }
    
    # Xóa toàn bộ thư mục caches (xóa hết để tránh lỗi metadata)
    $gradleUserHome = "$env:USERPROFILE\.gradle\caches"
    if (Test-Path $gradleUserHome) {
        Write-Step "Xóa toàn bộ Gradle user cache..."
        
        # Xóa từng thư mục cache để tránh lỗi permission
        $cacheDirs = Get-ChildItem -Path $gradleUserHome -Directory -ErrorAction SilentlyContinue
        $deletedCount = 0
        foreach ($dir in $cacheDirs) {
            try {
                Remove-Item -Recurse -Force $dir.FullName -ErrorAction SilentlyContinue
                $deletedCount++
            } catch {
                # Ignore individual errors nhưng vẫn thử xóa
                Write-Info "Không thể xóa: $($dir.Name)"
            }
        }
        if ($deletedCount -gt 0) {
            Write-Success "Đã xóa $deletedCount thư mục cache"
        } else {
            Write-Info "Không có cache để xóa"
        }
    }
    
    # Xóa daemon cache
    $gradleDaemon = "$env:USERPROFILE\.gradle\daemon"
    if (Test-Path $gradleDaemon) {
        Write-Step "Xóa Gradle daemon cache..."
        Remove-Item -Recurse -Force "$gradleDaemon\*" -ErrorAction SilentlyContinue
        Write-Success "Đã xóa Gradle daemon cache"
    }
    
    # Đợi một chút để đảm bảo file đã được giải phóng
    Start-Sleep -Seconds 1
}

# Bước 3: Clean project (bỏ qua nếu đã deep clean vì cache đã bị xóa)
if (-not $Deep) {
    Write-Step "Clean project..."
    try {
        & .\gradlew.bat clean
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle clean trả về mã lỗi $LASTEXITCODE"
        }
        Write-Success "Project đã được clean"
    } catch {
        Write-Error-Custom "Lỗi khi clean project: $_"
        exit 1
    }
} else {
    Write-Info "Bỏ qua clean project vì đã deep clean"
}

# Bước 4: Xóa build folders
Write-Step "Xóa build folders..."
$foldersToDelete = @(
    "app\build",
    "build"
)

foreach ($folder in $foldersToDelete) {
    if (Test-Path $folder) {
        Remove-Item -Recurse -Force $folder -ErrorAction SilentlyContinue
        Write-Success "Đã xóa $folder"
    }
}

# Bước 5: Rebuild project
Write-Step "Rebuild project..."
$buildType = if ($Release) { "assembleRelease" } else { "assembleDebug" }

Write-Host "`n📦 Build type: $buildType" -ForegroundColor Cyan
Write-Host "🚀 Đang build... (có thể mất vài phút)`n" -ForegroundColor Yellow

try {
    # Build với flag để đảm bảo rebuild metadata sau khi xóa cache
    & .\gradlew.bat $buildType --no-build-cache --refresh-dependencies --stacktrace
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build trả về mã lỗi $LASTEXITCODE"
    }
    
    Write-Host @"

╔══════════════════════════════════════════════════════════════╗
║              ✅ BUILD THÀNH CÔNG!                            ║
╚══════════════════════════════════════════════════════════════╝

"@ -ForegroundColor Green

    # Hiển thị thông tin APK
    $apkPath = if ($Release) {
        "app\build\outputs\apk\release\app-release.apk"
    } else {
        "app\build\outputs\apk\debug\app-debug.apk"
    }
    
    if (Test-Path $apkPath) {
        $apkSize = (Get-Item $apkPath).Length / 1MB
        Write-Host "APK location: $apkPath" -ForegroundColor Cyan
        Write-Host "APK size: $([math]::Round($apkSize, 2)) MB" -ForegroundColor Cyan
    }
    
} catch {
    Write-Host @"

╔══════════════════════════════════════════════════════════════╗
║              ❌ BUILD THẤT BẠI!                              ║
╚══════════════════════════════════════════════════════════════╝

"@ -ForegroundColor Red

    Write-Host "❌ Lỗi: $_" -ForegroundColor Red
    Write-Host "`n💡 Gợi ý:" -ForegroundColor Yellow
    Write-Host "   1. Chạy lại với -Deep để deep clean" -ForegroundColor White
    Write-Host "   2. Kiểm tra BUILD_FIX_GUIDE.md để xem hướng dẫn chi tiết" -ForegroundColor White
    Write-Host "   3. Kiểm tra file gradle.properties và build.gradle" -ForegroundColor White
    Write-Host "   4. Liên hệ: FB: https://fb.com/vlog.txa.2311" -ForegroundColor White
    
    exit 1
}

Write-Host "`n✅ Hoàn tất!" -ForegroundColor Green
