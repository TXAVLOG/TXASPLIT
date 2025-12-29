#
# ████████ ██   ██  █████   █████  ██████  ██████  
#    ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██ 
#    ██      ███   ███████ ███████ ██████  ██████  
#    ██     ██ ██  ██   ██ ██   ██ ██      ██      
#    ██    ██   ██ ██   ██ ██   ██ ██      ██      
#                                                
# TXASplit - Version Manager PowerShell Script
# Build by TXA
# Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
#

param(
    [Parameter(Position=0)]
    [string]$Command = "help",
    [Parameter(Position=1)]
    [string]$TargetVersion
)

# Danh sách lệnh hợp lệ
$ValidCommands = @("patch", "minor", "major", "show", "build", "help", "downgrade")

# Validate command và suggest nếu gõ sai
if ($Command -notin $ValidCommands) {
    Write-Host "❌ Lỗi: Lệnh '$Command' không hợp lệ!" -ForegroundColor Red
    Write-Host ""
    
    # Tìm lệnh gần giống nhất (fuzzy match)
    $suggested = $ValidCommands | Where-Object { 
        $_.StartsWith($Command.Substring(0, [Math]::Min(3, $Command.Length))) -or
        $Command -like "*$($_.Substring(0, [Math]::Min(3, $_.Length)))*"
    } | Select-Object -First 1
    
    if ($suggested) {
        Write-Host "💡 Có phải bạn muốn dùng: '$suggested'?" -ForegroundColor Yellow
        Write-Host ""
    }
    
    Write-Host "📋 Các lệnh hợp lệ:" -ForegroundColor Cyan
    foreach ($cmd in $ValidCommands) {
        Write-Host "  - $cmd" -ForegroundColor White
    }
    Write-Host ""
    Write-Host "💬 Gõ '.\version_manager.ps1 help' để xem hướng dẫn chi tiết." -ForegroundColor Yellow
    exit 1
}

function Show-Help {
    Write-Host "TXASplit Version Manager" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Usage: .\version_manager.ps1 [command]" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Commands:" -ForegroundColor Green
    Write-Host "  patch   - Increment patch version (1.0.0 -> 1.0.1)"
    Write-Host "  minor   - Increment minor version (1.0.0 -> 1.1.0)"
    Write-Host "  major   - Increment major version (1.0.0 -> 2.0.0)"
    Write-Host "  show    - Show current version"
    Write-Host "  build   - Build app with current version"
    Write-Host "  help    - Show this help message"
    Write-Host "  downgrade - Downgrade to specific version"
    Write-Host ""
    Write-Host "Examples:" -ForegroundColor Yellow
    Write-Host "  .\version_manager.ps1 patch"
    Write-Host "  .\version_manager.ps1 minor"
    Write-Host "  .\version_manager.ps1 show"
    Write-Host "  .\version_manager.ps1 downgrade -TargetVersion 1.0.0_txa"
}

function Get-CurrentVersion {
    if (Test-Path "version.txa") {
        $raw = (Get-Content "version.txa" -Raw).Trim()
        return $raw
    } else {
        Write-Host "❌ Lỗi: File version.txa không tồn tại!" -ForegroundColor Red
        return $null
    }
}

function Parse-Version($raw) {
    if ($raw -match "^(\d+)\.(\d+)\.(\d+)(?:_(.+))?$") {
        return @{
            Major = [int]$matches[1]
            Minor = [int]$matches[2]
            Patch = [int]$matches[3]
            Suffix = if ($matches[4]) { $matches[4] } else { "txa" }
            Raw = $raw
        }
    }
    return $null
}

function Format-Version($v) {
    $base = "$($v.Major).$($v.Minor).$($v.Patch)"
    if ($v.Suffix -and $v.Suffix -ne "") {
        return "${base}_$($v.Suffix)"
    }
    return "${base}_txa"
}

function Write-VersionFile($versionString) {
    $versionString = $versionString.Trim()
    Set-Content -Path "version.txa" -Value $versionString -NoNewline
    Write-Host "✅ Đã cập nhật file version.txa: $versionString" -ForegroundColor Green
}

function Show-Version {
    $version = Get-CurrentVersion
    if ($version) {
        Write-Host "Current version: $version" -ForegroundColor Green
    }
}

function Downgrade-Version($target) {
    if ([string]::IsNullOrWhiteSpace($target)) {
        Write-Host "❌ Lỗi: Cần chỉ định phiên bản đích!" -ForegroundColor Red
        Write-Host ""
        Write-Host "📝 Cách dùng:" -ForegroundColor Yellow
        Write-Host "  .\version_manager.ps1 downgrade -TargetVersion 1.0.0_txa" -ForegroundColor White
        Write-Host ""
        Write-Host "💡 Ví dụ:" -ForegroundColor Cyan
        Write-Host "  .\version_manager.ps1 downgrade -TargetVersion 1.1.0_txa" -ForegroundColor White
        return
    }

    # Validate format
    $target = $target.Trim()
    if ($target -notmatch "^\d+\.\d+\.\d+(?:_(.+))?$") {
        Write-Host "❌ Lỗi: Định dạng phiên bản không hợp lệ: $target" -ForegroundColor Red
        Write-Host ""
        Write-Host "📝 Định dạng đúng: X.Y.Z_txa" -ForegroundColor Yellow
        Write-Host "💡 Ví dụ: 1.0.0_txa, 1.1.0_txa, 2.0.0_txa" -ForegroundColor Cyan
        return
    }

    # Đảm bảo có suffix _txa
    if ($target -notmatch "_txa$") {
        $target = "${target}_txa"
    }

    Write-Host "🔄 Đang downgrade về phiên bản: $target..." -ForegroundColor Yellow
    
    # Cập nhật trực tiếp file version.txa
    Write-VersionFile $target
    
    # Đồng bộ với Gradle (để đảm bảo build system cũng nhận biết)
    $gradleArgs = @("downgradeVersion", "-PtargetVersion=$target")
    & ".\gradlew.bat" $gradleArgs | Out-Null

    Write-Host ""
    Write-Host "✅ Downgrade thành công!" -ForegroundColor Cyan
    Show-Version
}

function Increment-Version($type) {
    $current = Get-CurrentVersion
    if (-not $current) {
        Write-Host "❌ Không thể đọc phiên bản hiện tại!" -ForegroundColor Red
        return
    }

    $v = Parse-Version $current
    if (-not $v) {
        Write-Host "❌ Lỗi: Không thể parse phiên bản: $current" -ForegroundColor Red
        return
    }

    Write-Host "🔄 Đang tăng $type version..." -ForegroundColor Yellow
    
    # Tính toán phiên bản mới
    $newVersion = switch ($type) {
        "major" { 
            @{ Major = $v.Major + 1; Minor = 0; Patch = 0; Suffix = $v.Suffix }
        }
        "minor" { 
            @{ Major = $v.Major; Minor = $v.Minor + 1; Patch = 0; Suffix = $v.Suffix }
        }
        "patch" { 
            @{ Major = $v.Major; Minor = $v.Minor; Patch = $v.Patch + 1; Suffix = $v.Suffix }
        }
    }

    $newVersionString = Format-Version $newVersion
    
    # Cập nhật trực tiếp file version.txa
    Write-VersionFile $newVersionString
    
    # Đồng bộ với Gradle (để đảm bảo build system cũng nhận biết)
    $gradleFlag = switch ($type) {
        "major" { "-Pmajor" }
        "minor" { "-Pminor" }
        "patch" { "-Ppatch" }
    }
    $gradleArgs = @("incrementVersion", $gradleFlag)
    & ".\gradlew.bat" $gradleArgs | Out-Null
    
    Write-Host ""
    Write-Host "✅ Phiên bản mới:" -ForegroundColor Green
    Show-Version
    Write-Host "✅ Cập nhật thành công!" -ForegroundColor Cyan
}

function Build-App {
    Write-Host "Building TXASplit with current version..." -ForegroundColor Yellow
    
    Invoke-Expression ".\gradlew.bat updateVersion assembleDebug"
    
    Write-Host ""
    Write-Host "Build completed!" -ForegroundColor Green
    Write-Host "APK location: app/build/outputs/apk/debug/" -ForegroundColor Cyan
}

# Main execution
switch ($Command) {
    "help" { Show-Help }
    "show" { Show-Version }
    "patch" { Increment-Version "patch" }
    "minor" { Increment-Version "minor" }
    "major" { Increment-Version "major" }
    "build" { Build-App }
    "downgrade" { Downgrade-Version $TargetVersion }
    default { Show-Help }
}
