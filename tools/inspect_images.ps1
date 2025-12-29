# ████████ ██   ██  █████   █████  ██████  ██████ 
#    ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
#    ██      ███   ███████ ███████ ██████  ██████ 
#    ██     ██ ██  ██   ██ ██   ██ ██      ██     
#    ██    ██   ██ ██   ██ ██   ██ ██      ██     
#
# TXASplit Image Inspector
# Build by TXA
# Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!

Add-Type -AssemblyName System.Drawing

function Write-Banner {
    Write-Host @"
╔══════════════════════════════════════════════════════════════╗
║           🖼️  TXASplit Image Inspector                       ║
║           Build by TXA                                       ║
╚══════════════════════════════════════════════════════════════╝
"@ -ForegroundColor Cyan
}

function Write-Section {
    param([string]$Title)
    Write-Host "`n$Title" -ForegroundColor Yellow
    Write-Host ("─" * 60) -ForegroundColor DarkGray
}

function Write-ImageInfo {
    param(
        [string]$Path,
        [string]$Status,
        [string]$Info,
        [string]$ExpectedSize = ""
    )
    if ($Status -eq "found") {
        Write-Host "✅ $(Split-Path $Path -Leaf)" -ForegroundColor Green
        Write-Host "   $Info" -ForegroundColor Gray
        if ($ExpectedSize -ne "") {
            $isCorrect = $Info -match $ExpectedSize
            if ($isCorrect) {
                Write-Host "   ✓ Kích thước đúng chuẩn" -ForegroundColor Green
            } else {
                Write-Host "   ⚠️  Kích thước không đúng chuẩn (mong đợi: $ExpectedSize)" -ForegroundColor Yellow
            }
        }
    } else {
        Write-Host "❌ $(Split-Path $Path -Leaf): not found" -ForegroundColor Red
    }
}

Write-Banner

Write-Section "📂 Original Assets (Input Files)"
$originalFiles = @(
    @{Path='C:\Users\Admin\Desktop\TXASPLIT\logo.png'; Expected='1024×1024'; Desc='Logo app'},
    @{Path='c:\Users\Admin\Desktop\TXASPLIT\notii.png'; Expected='512×512'; Desc='Notification icon'},
    @{Path='c:\Users\Admin\Desktop\TXASPLIT\wc2.png'; Expected='1080×1920'; Desc='Splash and welcome screen'}
)

$foundCount = 0
$notFoundCount = 0

foreach ($file in $originalFiles) {
    $path = $file.Path
    if (-Not (Test-Path $path)) {
        Write-ImageInfo -Path $path -Status "not found"
        $notFoundCount++
        continue
    }
    try {
        $img = [System.Drawing.Image]::FromFile($path)
        $sizeKB = [math]::Round((Get-Item $path).Length / 1KB, 2)
        $sizeMB = [math]::Round((Get-Item $path).Length / 1MB, 2)
        $sizeStr = if ($sizeMB -ge 1) { "$sizeMB MB" } else { "$sizeKB KB" }
        $format = $img.RawFormat.ToString()
        $currentSize = "$($img.Width)×$($img.Height)"
        $info = "$currentSize px | $sizeStr | Format: $format | $($file.Desc)"
        Write-ImageInfo -Path $path -Status "found" -Info $info -ExpectedSize $file.Expected
        $img.Dispose()
        $foundCount++
    } catch {
        Write-Host "⚠️  Lỗi khi đọc $path : $_" -ForegroundColor Yellow
        $notFoundCount++
    }
}

Write-Host "`n📊 Tổng kết: $foundCount tìm thấy, $notFoundCount không tìm thấy" -ForegroundColor Cyan

Write-Section "📦 Generated Resources (Resized Outputs)"

# Launcher icons với kích thước mong đợi
$launcherIcons = @(
    @{Path='C:\Users\Admin\Desktop\TXASPLIT\app\src\main\res\mipmap-mdpi\ic_launcher.png'; Expected='48×48'; Desc='mdpi'},
    @{Path='C:\Users\Admin\Desktop\TXASPLIT\app\src\main\res\mipmap-hdpi\ic_launcher.png'; Expected='72×72'; Desc='hdpi'},
    @{Path='C:\Users\Admin\Desktop\TXASPLIT\app\src\main\res\mipmap-xhdpi\ic_launcher.png'; Expected='96×96'; Desc='xhdpi'},
    @{Path='C:\Users\Admin\Desktop\TXASPLIT\app\src\main\res\mipmap-xxhdpi\ic_launcher.png'; Expected='144×144'; Desc='xxhdpi'},
    @{Path='C:\Users\Admin\Desktop\TXASPLIT\app\src\main\res\mipmap-xxxhdpi\ic_launcher.png'; Expected='192×192'; Desc='xxxhdpi'}
)

# Notification icons với kích thước mong đợi
$notificationIcons = @(
    @{Path='C:\Users\Admin\Desktop\TXASPLIT\app\src\main\res\drawable-mdpi\ic_stat_txaboard.png'; Expected='24×24'; Desc='mdpi'},
    @{Path='C:\Users\Admin\Desktop\TXASPLIT\app\src\main\res\drawable-hdpi\ic_stat_txaboard.png'; Expected='36×36'; Desc='hdpi'},
    @{Path='C:\Users\Admin\Desktop\TXASPLIT\app\src\main\res\drawable-xhdpi\ic_stat_txaboard.png'; Expected='48×48'; Desc='xhdpi'},
    @{Path='C:\Users\Admin\Desktop\TXASPLIT\app\src\main\res\drawable-xxhdpi\ic_stat_txaboard.png'; Expected='72×72'; Desc='xxhdpi'},
    @{Path='C:\Users\Admin\Desktop\TXASPLIT\app\src\main\res\drawable-xxxhdpi\ic_stat_txaboard.png'; Expected='96×96'; Desc='xxxhdpi'}
)

# Splash & Welcome với kích thước mong đợi
$splashWelcome = @(
    @{Path='C:\Users\Admin\Desktop\TXASPLIT\app\src\main\res\drawable-nodpi\splash.png'; Expected='1080×1920'; Desc='Splash screen'},
    @{Path='C:\Users\Admin\Desktop\TXASPLIT\app\src\main\res\drawable-nodpi\welcome_primary.png'; Expected='1920×1080'; Desc='Welcome primary'},
    @{Path='C:\Users\Admin\Desktop\TXASPLIT\app\src\main\res\drawable-nodpi\welcome_secondary.png'; Expected='1080×1920'; Desc='Welcome secondary'}
)

$allResources = @()
$allResources += $launcherIcons
$allResources += $notificationIcons
$allResources += $splashWelcome

$foundCount = 0
$notFoundCount = 0
$incorrectSizeCount = 0
$totalSize = 0

Write-Host "`n📱 Launcher Icons:" -ForegroundColor Cyan
foreach ($file in $launcherIcons) {
    $path = $file.Path
    if (-Not (Test-Path $path)) {
        Write-ImageInfo -Path $path -Status "not found"
        $notFoundCount++
        continue
    }
    try {
        $img = [System.Drawing.Image]::FromFile($path)
        $sizeKB = [math]::Round((Get-Item $path).Length / 1KB, 2)
        $totalSize += (Get-Item $path).Length
        $format = $img.RawFormat.ToString()
        $currentSize = "$($img.Width)×$($img.Height)"
        $info = "$currentSize px | $sizeKB KB | Format: $format"
        $isCorrect = ($img.Width -eq [int]($file.Expected -split '×')[0]) -and ($img.Height -eq [int]($file.Expected -split '×')[1])
        Write-ImageInfo -Path $path -Status "found" -Info $info -ExpectedSize $file.Expected
        if (-not $isCorrect) {
            $incorrectSizeCount++
        }
        $img.Dispose()
        $foundCount++
    } catch {
        Write-Host "⚠️  Lỗi khi đọc $path : $_" -ForegroundColor Yellow
        $notFoundCount++
    }
}

Write-Host "`n🔔 Notification Icons:" -ForegroundColor Cyan
foreach ($file in $notificationIcons) {
    $path = $file.Path
    if (-Not (Test-Path $path)) {
        Write-ImageInfo -Path $path -Status "not found"
        $notFoundCount++
        continue
    }
    try {
        $img = [System.Drawing.Image]::FromFile($path)
        $sizeKB = [math]::Round((Get-Item $path).Length / 1KB, 2)
        $totalSize += (Get-Item $path).Length
        $format = $img.RawFormat.ToString()
        $currentSize = "$($img.Width)×$($img.Height)"
        $info = "$currentSize px | $sizeKB KB | Format: $format"
        $isCorrect = ($img.Width -eq [int]($file.Expected -split '×')[0]) -and ($img.Height -eq [int]($file.Expected -split '×')[1])
        Write-ImageInfo -Path $path -Status "found" -Info $info -ExpectedSize $file.Expected
        if (-not $isCorrect) {
            $incorrectSizeCount++
        }
        $img.Dispose()
        $foundCount++
    } catch {
        Write-Host "⚠️  Lỗi khi đọc $path : $_" -ForegroundColor Yellow
        $notFoundCount++
    }
}

Write-Host "`n🎬 Splash & Welcome Screens:" -ForegroundColor Cyan
foreach ($file in $splashWelcome) {
    $path = $file.Path
    if (-Not (Test-Path $path)) {
        Write-ImageInfo -Path $path -Status "not found"
        $notFoundCount++
        continue
    }
    try {
        $img = [System.Drawing.Image]::FromFile($path)
        $sizeKB = [math]::Round((Get-Item $path).Length / 1KB, 2)
        $sizeMB = [math]::Round((Get-Item $path).Length / 1MB, 2)
        $sizeStr = if ($sizeMB -ge 1) { "$sizeMB MB" } else { "$sizeKB KB" }
        $totalSize += (Get-Item $path).Length
        $format = $img.RawFormat.ToString()
        $currentSize = "$($img.Width)×$($img.Height)"
        $info = "$currentSize px | $sizeStr | Format: $format | $($file.Desc)"
        $isCorrect = ($img.Width -eq [int]($file.Expected -split '×')[0]) -and ($img.Height -eq [int]($file.Expected -split '×')[1])
        Write-ImageInfo -Path $path -Status "found" -Info $info -ExpectedSize $file.Expected
        if (-not $isCorrect) {
            $incorrectSizeCount++
        }
        $img.Dispose()
        $foundCount++
    } catch {
        Write-Host "⚠️  Lỗi khi đọc $path : $_" -ForegroundColor Yellow
        $notFoundCount++
    }
}

$totalSizeMB = [math]::Round($totalSize / 1MB, 2)
$totalSizeKB = [math]::Round($totalSize / 1KB, 2)
$totalSizeStr = if ($totalSizeMB -ge 1) { "$totalSizeMB MB" } else { "$totalSizeKB KB" }

Write-Host "`n📊 Tổng kết:" -ForegroundColor Cyan
Write-Host "   Tìm thấy: $foundCount files" -ForegroundColor Green
Write-Host "   Không tìm thấy: $notFoundCount files" -ForegroundColor $(if ($notFoundCount -gt 0) { "Red" } else { "Green" })
Write-Host "   Kích thước sai: $incorrectSizeCount files" -ForegroundColor $(if ($incorrectSizeCount -gt 0) { "Yellow" } else { "Green" })
Write-Host "   Tổng dung lượng: $totalSizeStr" -ForegroundColor Cyan

if ($incorrectSizeCount -gt 0) {
    Write-Host "`n⚠️  Có $incorrectSizeCount file(s) có kích thước không đúng chuẩn!" -ForegroundColor Yellow
    Write-Host "   Vui lòng chạy lại: python tools\process_assets.py" -ForegroundColor White
}

Write-Host @"

╔══════════════════════════════════════════════════════════════╗
║              ✅ INSPECTION HOÀN TẤT!                         ║
╚══════════════════════════════════════════════════════════════╝
"@ -ForegroundColor Green
