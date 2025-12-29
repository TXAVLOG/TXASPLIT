#!/bin/bash

# ████████ ██   ██  █████   █████  ██████  ██████ 
#    ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
#    ██      ███   ███████ ███████ ██████  ██████ 
#    ██     ██ ██  ██   ██ ██   ██ ██      ██     
#    ██    ██   ██ ██   ██ ██   ██ ██      ██     
#
# TXASplit Ubuntu VPS Build Script (Enhanced Version)
# Build by TXA
# Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
GRAY='\033[0;37m'
NC='\033[0m' # No Color

# Configuration
PROJECT_NAME="TXASplit"
BUILD_DIR="TXABUILD"
LOG_FILE="build.log"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BUILD_TYPE=${1:-"debug"}  # Default to debug if no argument provided

# Functions
function write_banner() {
    echo -e "${CYAN}
╔══════════════════════════════════════════════════════════════╗
║           🏗️  TXASplit Ubuntu VPS Build Script             ║
║           Build by TXA                                      ║
╚══════════════════════════════════════════════════════════════╝
${NC}"
}

function write_step() {
    echo -e "\n${YELLOW}▶ $1${NC}"
}

function write_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

function write_error() {
    echo -e "${RED}❌ $1${NC}"
}

function write_info() {
    echo -e "${GRAY}ℹ️  $1${NC}"
}

function write_build_success() {
    local apk_path=$1
    local apk_size=$2
    echo -e "${GREEN}
╔══════════════════════════════════════════════════════════════╗
║              ✅ BUILD THÀNH CÔNG!                            ║
╚══════════════════════════════════════════════════════════════╝

📦 APK đã được tạo thành công!
📁 Đường dẫn: $apk_path
💾 Dung lượng: $apk_size MB
${NC}"
}

function write_build_failed() {
    local error_message=$1
    local exit_code=$2
    echo -e "${RED}
╔══════════════════════════════════════════════════════════════╗
║              ❌ BUILD THẤT BẠI!                              ║
╚══════════════════════════════════════════════════════════════╝
${NC}"
    
    write_error "Lỗi: $error_message"
    if [ $exit_code -ne 0 ]; then
        write_error "Exit code: $exit_code"
    fi
    
    echo -e "\n${YELLOW}💡 Gợi ý:${NC}"
    echo "   1. Kiểm tra log phía trên để xem chi tiết lỗi"
    echo "   2. Kiểm tra file build.gradle.kts và gradle.properties"
    echo "   3. Liên hệ: FB: https://fb.com/vlog.txa.2311"
    echo ""
}

function setup_environment() {
    write_step "Cài đặt môi trường build..."
    
    # Update package list
    sudo apt update
    
    # Install required packages
    sudo apt install -y openjdk-17-jdk unzip wget git curl
    
    # Set JAVA_HOME
    export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
    echo "export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64" >> ~/.bashrc
    
    # Add JAVA_HOME to PATH
    echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> ~/.bashrc
    source ~/.bashrc
    
    write_success "Môi trường build đã được cài đặt"
}

function stop_gradle_daemon() {
    write_step "Dừng Gradle daemon..."
    ./gradlew --stop 2>/dev/null || true
    write_info "Gradle daemon đã dừng"
}

function create_keystore() {
    local keystore_dir="keystore"
    local keystore_file="$keystore_dir/txasplit.keystore"
    local store_pass="txasplit-store"
    local key_alias="txasplit"
    local key_pass="txasplit-key"
    
    write_step "Kiểm tra keystore cho Release build..."
    
    mkdir -p "$keystore_dir"
    
    if [ ! -f "$keystore_file" ]; then
        write_info "Keystore chưa tồn tại, đang tạo mới..."
        keytool -genkeypair \
            -v \
            -keystore "$keystore_file" \
            -storepass "$store_pass" \
            -keypass "$key_pass" \
            -alias "$key_alias" \
            -keyalg RSA \
            -keysize 2048 \
            -validity 10000 \
            -dname "CN=TXABoard,O=NROTXA,C=VN" 2>/dev/null
        
        if [ $? -ne 0 ]; then
            write_error "Không thể tạo keystore"
            return 1
        fi
        write_success "Keystore đã được tạo"
    else
        write_success "Keystore đã tồn tại"
    fi
}

function copy_apk() {
    local source=$1
    local target_name=$2
    
    if [ ! -f "$source" ]; then
        write_error "Không tìm thấy file APK: $source"
        return 1
    fi
    
    mkdir -p "$BUILD_DIR"
    local dest="$BUILD_DIR/$target_name"
    cp "$source" "$dest"
    
    local size_mb=$(du -m "$dest" | cut -f1)
    write_success "Đã copy APK -> $dest ($size_mb MB)"
    return 0
}

function run_gradle() {
    write_info "Chạy: ./gradlew $@"
    stop_gradle_daemon
    
    echo -e "\n${CYAN}🚀 Đang build... (có thể mất vài phút)${NC}\n"
    
    # Capture output to log file
    ./gradlew "$@" 2>&1 | tee "$LOG_FILE"
    local exit_code=$?
    
    stop_gradle_daemon
    
    if [ $exit_code -ne 0 ]; then
        return 1
    fi
    
    return 0
}

function push_to_git() {
    local status=$1  # "success" or "failed"
    local commit_message=$2
    local files_to_add=$3
    
    write_step "Push kết quả build lên Git..."
    
    # Configure git if not configured
    if ! git config user.name >/dev/null 2>&1; then
        git config user.name "TXA Build Bot"
        git config user.email "txavlog7@gmail.com"
    fi
    
    # Add files
    if [ -n "$files_to_add" ]; then
        git add $files_to_add
    fi
    
    # Commit changes
    git commit -m "$commit_message" || true
    
    # Push to remote
    git push origin master
    
    write_success "Đã push kết quả lên Git"
}

function main() {
    write_banner
    
    # Get project root directory
    local project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
    cd "$project_root"
    
    write_info "Thư mục project: $project_root"
    write_info "Build type: $BUILD_TYPE"
    write_info "Timestamp: $TIMESTAMP"
    
    # Setup environment (only if needed)
    if ! command -v java >/dev/null 2>&1; then
        setup_environment
    fi
    
    # Check gradlew
    if [ ! -f "gradlew" ]; then
        write_error "Không tìm thấy gradlew"
        write_info "Đang cố gắng tạo gradlew từ gradlew.bat..."
        
        # Try to create gradlew from gradlew.bat
        if [ -f "gradlew.bat" ]; then
            write_info "Tìm thấy gradlew.bat, đang tạo gradlew cho Unix..."
            
            # Create simple gradlew for Unix
            cat > gradlew << 'EOF'
#!/bin/sh

# Gradle wrapper for Unix/Linux - Simplified version
# Based on standard Gradle wrapper

# Resolve script location
PRG="$0"
while [ -h "$PRG" ]; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done

# Get APP_HOME
APP_HOME=`dirname "$PRG"`/..

# Set CLASSPATH
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Find Java
if [ -n "$JAVA_HOME" ]; then
    if [ -x "$JAVA_HOME/bin/java" ]; then
        JAVACMD="$JAVA_HOME/bin/java"
    else
        echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
        echo "Please set the JAVA_HOME variable in your environment to match the location of your Java installation."
        exit 1
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || {
        echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
        echo "Please set the JAVA_HOME variable in your environment to match the location of your Java installation."
        exit 1
    }
fi

# Execute Gradle
exec "$JAVACMD" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
EOF
            
            chmod +x gradlew
            write_success "Đã tạo gradlew cho Unix/Linux"
        else
            write_error "Không tìm thấy gradlew.bat để tạo gradlew"
            write_info "Vui lòng chạy: gradle wrapper"
            exit 1
        fi
    fi
    
    # Make gradlew executable
    chmod +x gradlew
    
    # Create build directory
    mkdir -p "$BUILD_DIR"
    
    # Initialize variables
    local build_success=false
    local apk_path=""
    local apk_size=""
    local build_error=""
    local exit_code=0
    
    # Stop gradle daemon before starting
    stop_gradle_daemon
    
    # Build process with error handling
    if [ "$BUILD_TYPE" = "release" ]; then
        write_step "Build Type: RELEASE"
        echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
        
        # Create keystore if needed
        create_keystore
        
        # Build release APK
        if run_gradle clean assembleRelease \
            -Pandroid.injected.signing.store.file=keystore/txasplit.keystore \
            -Pandroid.injected.signing.store.password=txasplit-store \
            -Pandroid.injected.signing.key.alias=txasplit \
            -Pandroid.injected.signing.key.password=txasplit-key; then
            
            local source_apk="app/build/outputs/apk/release/app-release.apk"
            if [ -f "$source_apk" ]; then
                if copy_apk "$source_apk" "TXASplit-release.apk"; then
                    apk_path="$BUILD_DIR/TXASplit-release.apk"
                    apk_size=$(du -m "$apk_path" | cut -f1)
                    build_success=true
                fi
            else
                build_error="APK không được tạo tại: $source_apk"
                exit_code=1
            fi
        else
            build_error="Gradle build thất bại"
            exit_code=1
        fi
    else
        write_step "Build Type: DEBUG"
        echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
        
        # Build debug APK
        if run_gradle clean assembleDebug; then
            local source_apk="app/build/outputs/apk/debug/app-debug.apk"
            if [ -f "$source_apk" ]; then
                if copy_apk "$source_apk" "TXASplit-debug.apk"; then
                    apk_path="$BUILD_DIR/TXASplit-debug.apk"
                    apk_size=$(du -m "$apk_path" | cut -f1)
                    build_success=true
                fi
            else
                build_error="APK không được tạo tại: $source_apk"
                exit_code=1
            fi
        else
            build_error="Gradle build thất bại"
            exit_code=1
        fi
    fi
    
    # Stop gradle daemon after build
    stop_gradle_daemon
    
    # Push results to git
    if [ "$build_success" = true ] && [ -n "$apk_path" ] && [ -f "$apk_path" ]; then
        write_build_success "$apk_path" "$apk_size"
        
        # Push successful build
        local commit_msg="build: Successful $BUILD_TYPE build - APK generated ($apk_size MB) [$TIMESTAMP]"
        push_to_git "success" "$commit_msg" "$BUILD_DIR/*.apk $LOG_FILE"
        
        exit 0
    else
        local error_msg=${build_error:-"Build thất bại nhưng không có thông tin lỗi chi tiết"}
        write_build_failed "$error_msg" $exit_code
        
        # Push failed build with log
        local commit_msg="build: Failed $BUILD_TYPE build - $error_msg [$TIMESTAMP]"
        push_to_git "failed" "$commit_msg" "$LOG_FILE"
        
        exit $exit_code
    fi
}

# Run main function
main "$@"
