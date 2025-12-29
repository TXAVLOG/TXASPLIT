# ████████ ██   ██  █████   █████  ██████  ██████ 
#    ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
#    ██      ███   ███████ ███████ ██████  ██████ 
#    ██     ██ ██  ██   ██ ██   ██ ██      ██     
#    ██    ██   ██ ██   ██ ██   ██ ██      ██     
#
# TXASplit Build Script
# Build by TXA
# Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!

param(
    [switch]$Release
)

$ErrorActionPreference = "Stop"

function Write-Banner {
    Write-Host @"
╔══════════════════════════════════════════════════════════════╗
║           🏗️  TXASplit Build Script                        ║
║           Build by TXA                                      ║
╚══════════════════════════════════════════════════════════════╝
"@ -ForegroundColor Cyan
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

function Write-BuildSuccess {
    param([string]$ApkPath, [string]$ApkSize)
    Write-Host @"

╔══════════════════════════════════════════════════════════════╗
║              ✅ BUILD THÀNH CÔNG!                            ║
╚══════════════════════════════════════════════════════════════╝

📦 APK đã được tạo thành công!
📁 Đường dẫn: $ApkPath
💾 Dung lượng: $ApkSize MB

"@ -ForegroundColor Green
}

function Write-BuildFailed {
    param([string]$ErrorMessage, [int]$ExitCode)
    Write-Host @"

╔══════════════════════════════════════════════════════════════╗
║              ❌ BUILD THẤT BẠI!                              ║
╚══════════════════════════════════════════════════════════════╝

"@ -ForegroundColor Red
    
    Write-Error-Custom "Lỗi: $ErrorMessage"
    if ($ExitCode -ne 0) {
        Write-Error-Custom "Exit code: $ExitCode"
    }
    
    Write-Host "`n💡 Gợi ý:" -ForegroundColor Yellow
    Write-Host "   1. Kiểm tra log phía trên để xem chi tiết lỗi" -ForegroundColor White
    Write-Host "   2. Chạy .\tools\fix_build.ps1 để clean và rebuild" -ForegroundColor White
    Write-Host "   3. Kiểm tra file build.gradle.kts và gradle.properties" -ForegroundColor White
    Write-Host "   4. Liên hệ: FB: https://fb.com/vlog.txa.2311" -ForegroundColor White
    Write-Host ""
}

Write-Banner

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

Write-Step "Kiểm tra Gradle wrapper..."
if (Test-Path (Join-Path $projectRoot "gradlew")) {
    $gradlew = Join-Path $projectRoot "gradlew"
    Write-Success "Tìm thấy gradlew (Unix)"
} elseif (Test-Path (Join-Path $projectRoot "gradlew.bat")) {
    $gradlew = Join-Path $projectRoot "gradlew.bat"
    Write-Success "Tìm thấy gradlew.bat (Windows)"
} else {
    Write-BuildFailed "Không tìm thấy gradlew ở $projectRoot" 1
    exit 1
}

$buildFolder = Join-Path $projectRoot "TXABUILD"
New-Item -ItemType Directory -Path $buildFolder -Force | Out-Null
Write-Info "Thư mục build: $buildFolder"

function Stop-GradleDaemon {
    try {
        & $gradlew --stop 2>&1 | Out-Null
        Write-Info "Gradle daemon đã dừng"
    } catch {
        Write-Host "⚠️  Không thể dừng Gradle daemon: $_" -ForegroundColor Yellow
    }
}

# Dừng Gradle daemon trước khi bắt đầu build
Write-Step "Dừng Gradle daemon trước khi build..."
Stop-GradleDaemon

$keystoreDir = Join-Path $projectRoot "keystore"
$keystoreFile = Join-Path $keystoreDir "txasplit.keystore"
$storePass = "txasplit-store"
$keyAlias = "txasplit"
$keyPass = "txasplit-key"

if ($Release) {
    Write-Step "Kiểm tra keystore cho Release build..."
    if (-Not (Test-Path $keystoreDir)) {
        New-Item -ItemType Directory -Path $keystoreDir -Force | Out-Null
        Write-Info "Đã tạo thư mục keystore"
    }
    if (-Not (Test-Path $keystoreFile)) {
        Write-Host "🔑 Keystore chưa tồn tại, đang tạo mới..." -ForegroundColor Yellow
        try {
            $dname = "CN=TXABoard,O=NROTXA,C=VN"
            & keytool -genkeypair `
                -v `
                -keystore $keystoreFile `
                -storepass $storePass `
                -keypass $keyPass `
                -alias $keyAlias `
                -keyalg RSA `
                -keysize 2048 `
                -validity 10000 `
                -dname $dname 2>&1 | Out-Null
            
            if ($LASTEXITCODE -ne 0) {
                throw "keytool trả về exit code $LASTEXITCODE"
            }
            Write-Success "Keystore đã được tạo"
        } catch {
            Write-BuildFailed "Không thể tạo keystore: $_" $LASTEXITCODE
            exit 1
        }
    } else {
        Write-Success "Keystore đã tồn tại"
    }
}

function Copy-Apk($source, $targetName) {
    if (-Not (Test-Path $source)) {
        Write-Error-Custom "Không tìm thấy file APK: $source"
        return $false
    }
    try {
        $dest = Join-Path $buildFolder $targetName
        Copy-Item $source $dest -Force
        $sizeMB = [math]::Round((Get-Item $dest).Length / 1MB, 2)
        Write-Success "Đã copy APK -> $dest ($sizeMB MB)"
        return $true
    } catch {
        Write-Error-Custom "Không thể copy APK: $_"
        return $false
    }
}

function Run-Gradle {
    param(
        [Parameter(ValueFromRemainingArguments = $true)]
        $Args
    )
    $command = "$gradlew $($Args -join ' ')"
    Write-Info "Chạy: $command"
    Stop-GradleDaemon
    
    try {
        Write-Host "`n🚀 Đang build... (có thể mất vài phút)`n" -ForegroundColor Cyan
        
        # Capture output để kiểm tra lỗi
        $output = & $gradlew @Args 2>&1
        $exitCode = $LASTEXITCODE
        
        if ($exitCode -ne 0) {
            # Hiển thị output nếu có lỗi
            Write-Host $output
            throw "Gradle build thất bại với exit code $exitCode"
        }
        
        return $true
    } catch {
        Write-Host $output
        throw $_
    } finally {
        Stop-GradleDaemon
    }
}

$buildSuccess = $false
$apkPath = $null
$apkSize = $null
$buildError = $null
$exitCode = 0

try {
    if ($Release) {
        Write-Host "`n📦 Build Type: RELEASE" -ForegroundColor Cyan
        Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
        
        $buildSuccess = Run-Gradle clean assembleRelease `
            -Pandroid.injected.signing.store.file=$keystoreFile `
            -Pandroid.injected.signing.store.password=$storePass `
            -Pandroid.injected.signing.key.alias=$keyAlias `
            -Pandroid.injected.signing.key.password=$keyPass
        
        if ($buildSuccess) {
            $sourceApk = "app\build\outputs\apk\release\app-release.apk"
            if (Test-Path $sourceApk) {
                $copySuccess = Copy-Apk $sourceApk "TXASplit-release.apk"
                if ($copySuccess) {
                    $apkPath = Join-Path $buildFolder "TXASplit-release.apk"
                    $apkSize = [math]::Round((Get-Item $apkPath).Length / 1MB, 2)
                }
            } else {
                throw "APK không được tạo tại: $sourceApk"
            }
        }
    } else {
        Write-Host "`n🔧 Build Type: DEBUG" -ForegroundColor Cyan
        Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
        
        $buildSuccess = Run-Gradle clean assembleDebug
        
        if ($buildSuccess) {
            $sourceApk = "app\build\outputs\apk\debug\app-debug.apk"
            if (Test-Path $sourceApk) {
                $copySuccess = Copy-Apk $sourceApk "TXASplit-debug.apk"
                if ($copySuccess) {
                    $apkPath = Join-Path $buildFolder "TXASplit-debug.apk"
                    $apkSize = [math]::Round((Get-Item $apkPath).Length / 1MB, 2)
                }
            } else {
                throw "APK không được tạo tại: $sourceApk"
            }
        }
    }
} catch {
    $buildSuccess = $false
    $buildError = $_.Exception.Message
    $exitCode = $LASTEXITCODE
    if ($exitCode -eq 0) { $exitCode = 1 }
}

# Dừng Gradle daemon sau khi build xong
Write-Step "Dừng Gradle daemon sau khi build..."
Stop-GradleDaemon

# Hiển thị kết quả
if ($buildSuccess -and $apkPath -and (Test-Path $apkPath)) {
    Write-BuildSuccess $apkPath $apkSize
    exit 0
} else {
    $errorMsg = if ($buildError) { $buildError } else { "Build thất bại nhưng không có thông tin lỗi chi tiết" }
    Write-BuildFailed $errorMsg $exitCode
    exit $exitCode
}
