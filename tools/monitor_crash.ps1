param(
    [string]$PackageName = "kc.txaboard.vn",
    [switch]$Api
)

# Hàm hiển thị text màu sắc
function Write-ColorText {
    param(
        [string]$Text,
        [string]$Color = "White"
    )
    Write-Host $Text -ForegroundColor $Color
}

# Hàm kiểm tra ADB connection
function Test-ADBConnection {
    Write-ColorText "🔍 Kiểm tra kết nối ADB..." "Yellow"
    
    try {
        $devices = adb devices
        if ($devices -match "device$" -and $devices.Count -gt 1) {
            Write-ColorText "✅ ADB đã kết nối với thiết bị" "Green"
            return $true
        } else {
            Write-ColorText "❌ Không tìm thấy thiết bị ADB đã kết nối" "Red"
            Write-ColorText "   Vui lòng kết nối thiết bị và bật USB Debugging" "Yellow"
            return $false
        }
    } catch {
        Write-ColorText "❌ Lỗi khi kiểm tra ADB: $($_.Exception.Message)" "Red"
        return $false
    }
}

# Hàm kiểm tra app đã cài đặt chưa
function Test-AppInstalled {
    param([string]$Package)
    
    Write-ColorText "📱 Kiểm tra app $Package đã cài đặt chưa..." "Yellow"
    
    try {
        $result = adb shell pm list packages $Package
        if ($result -match "package:$Package") {
            Write-ColorText "✅ App đã được cài đặt trên thiết bị" "Green"
            return $true
        } else {
            Write-ColorText "❌ App chưa được cài đặt trên thiết bị" "Red"
            Write-ColorText "   Vui lòng cài đặt app trước khi monitor crash logs" "Yellow"
            return $false
        }
    } catch {
        Write-ColorText "❌ Lỗi khi kiểm tra app: $($_.Exception.Message)" "Red"
        return $false
    }
}

# Hàm format crash log cho dễ đọc
function Format-CrashLog {
    param([string]$LogLine)
    
    if ($LogLine -match "(\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3})\s+(\d+)\s+(\d+)\s+E AndroidRuntime: (.+)") {
        $timestamp = $matches[1]
        $processId = $matches[2]
        $threadId = $matches[3]
        $message = $matches[4]
        
        # Format timestamp
        $time = [datetime]::ParseExact($timestamp, "MM-dd HH:mm:ss.fff", $null)
        $formattedTime = $time.ToString("HH:mm:ss.fff")
        
        # Color coding cho các loại lỗi
        if ($message -match "FATAL EXCEPTION") {
            return "`n" + "="*80 + "`n🔴 FATAL CRASH DETECTED`n" + "="*80 + "`n⏰ Time: $formattedTime`n🆔 PID: $processId | TID: $threadId`n📝 Message: $message`n"
        } elseif ($message -match "Process:") {
            return "📱 Process: $message"
        } elseif ($message -match "Caused by:") {
            return "⚠️  Caused by: $message"
        } elseif ($message -match "at ") {
            return "    📍 $message"
        } else {
            return "ℹ️  $message"
        }
    } elseif ($LogLine -match "--------- beginning of crash") {
        return "`n" + "🔥"*40 + " CRASH LOG START " + "🔥"*40 + "`n"
    } else {
        return $LogLine
    }
}

# Hàm monitor crash logs
function Start-CrashMonitor {
    param([string]$Package)
    
    Write-ColorText "`n🚀 Bắt đầu monitor crash logs cho app $Package..." "Cyan"
    Write-ColorText "   Nhấn Ctrl+C để dừng monitoring`n" "Gray"
    
    try {
        # Clear crash buffer trước khi bắt đầu
        adb logcat -b crash -c
        
        # Bắt đầu monitoring với format dễ đọc
        adb logcat -b crash | ForEach-Object {
            $formatted = Format-CrashLog -LogLine $_
            Write-Host $formatted
        }
    } catch {
        Write-ColorText "❌ Lỗi khi monitor crash logs: $($_.Exception.Message)" "Red"
    }
}

# Hàm monitor API logs
function Start-ApiMonitor {
    param([string]$Package)
    
    Write-ColorText "`n🌐 Bắt đầu monitor API logs cho app $Package..." "Cyan"
    Write-ColorText "   Nhấn Ctrl+C để dừng monitoring`n" "Gray"
    
    try {
        # Clear main buffer trước khi bắt đầu
        adb logcat -b main -c
        
        # Bắt đầu monitoring với filter cho API tags
        adb logcat -s TXABoardApi:* TXATranslation:* TXABoardApi:* | Where-Object { $_ -match $Package -or $_ -match "TXABoardApi" -or $_ -match "translation" -or $_ -match "api" } | ForEach-Object {
            $formatted = Format-ApiLog -LogLine $_
            Write-Host $formatted
        }
    } catch {
        Write-ColorText "❌ Lỗi khi monitor API logs: $($_.Exception.Message)" "Red"
    }
}

# Hàm format API log cho dễ đọc
function Format-ApiLog {
    param([string]$LogLine)
    
    if ($LogLine -match "(\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3})\s+(\d+)\s+(\d+)\s+([VDIWEFS])\s+(\S+):\s+(.+)") {
        $timestamp = $matches[1]
        $pid = $matches[2]
        $tid = $matches[3]
        $level = $matches[4]
        $tag = $matches[5]
        $message = $matches[6]
        
        # Format timestamp
        $time = [datetime]::ParseExact($timestamp, "MM-dd HH:mm:ss.fff", $null)
        $formattedTime = $time.ToString("HH:mm:ss.fff")
        
        # Color coding cho các level
        $levelColor = switch ($level) {
            "E" { "Red" }
            "W" { "Yellow" }
            "I" { "Cyan" }
            "D" { "Gray" }
            "V" { "White" }
            default { "White" }
        }
        
        $levelText = switch ($level) {
            "E" { "ERROR" }
            "W" { "WARN " }
            "I" { "INFO " }
            "D" { "DEBUG" }
            "V" { "VERB " }
            default { $level }
        }
        
        # Special formatting cho TXABoard API calls
        if ($tag -match "TXABoardApi|translation") {
            return "🌐 [$formattedTime] $levelText [$tag] $message"
        } else {
            return "ℹ️  [$formattedTime] $levelText [$tag] $message"
        }
    } else {
        return $LogLine
    }
}

# Main execution
Write-Host @"
╔══════════════════════════════════════════════════════════════╗
║           🔍 TXASplit Log Monitor                            ║
║           Build by TXA                                       ║
╚══════════════════════════════════════════════════════════════╝
"@ -ForegroundColor Cyan

if ($Api) {
    Write-Host "🌐 Mode: API Log Monitor" -ForegroundColor Cyan
} else {
    Write-Host "🔧 Mode: Crash Log Monitor" -ForegroundColor Cyan
}
Write-Host ""

# Step 1: Kiểm tra ADB connection
if (-not (Test-ADBConnection)) {
    Write-ColorText "`n❌ Thoát do không có kết nối ADB" "Red"
    exit 1
}

# Step 2: Kiểm tra app đã cài đặt chưa
if (-not (Test-AppInstalled -Package $PackageName)) {
    Write-ColorText "`n❌ Thoát do app chưa được cài đặt" "Red"
    exit 1
}

# Step 3: Bắt đầu monitor logs
if ($Api) {
    Start-ApiMonitor -Package $PackageName
    Write-ColorText "`n✅ Monitor API logs đã dừng" "Green"
} else {
    Start-CrashMonitor -Package $PackageName
    Write-ColorText "`n✅ Monitor crash logs đã dừng" "Green"
}
