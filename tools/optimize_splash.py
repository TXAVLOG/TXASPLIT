"""
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - Splash Screen Optimizer
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
"""

from pathlib import Path
from PIL import Image, ImageOps
import sys

def print_banner():
    print("╔══════════════════════════════════════════════════════════════╗")
    print("║           🎨 TXASplit Splash Screen Optimizer                ║")
    print("║           Build by TXA                                       ║")
    print("╚══════════════════════════════════════════════════════════════╝")
    print()

def print_step(message):
    print(f"▶ {message}")

def print_success(message):
    print(f"✅ {message}")

def print_error(message):
    print(f"❌ {message}")

def print_info(message):
    print(f"ℹ️  {message}")

ROOT = Path(__file__).resolve().parents[1]
SPLASH_INPUT = ROOT / "wc2.png"  # File splash gốc từ AI
SPLASH_OUTPUT = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi" / "splash.png"
# Splash screen: Portrait 1080x1920 (9:16 ratio) - kích thước chuẩn cho phone
SPLASH_TARGET = (1080, 1920)

def optimize_splash():
    """Optimize splash screen image - resize về đúng kích thước 1080x1920"""
    print_banner()
    
    print_step("Kiểm tra file input...")
    if not SPLASH_INPUT.exists():
        print_error(f"Input file not found: {SPLASH_INPUT}")
        print_info("Vui lòng đặt file splash screen tại: wc2.png (root project)")
        return False
    
    print_success(f"Tìm thấy: {SPLASH_INPUT.name}")
    
    # Get original size
    original_size = SPLASH_INPUT.stat().st_size
    original_size_mb = original_size / 1024 / 1024
    print_info(f"📦 Dung lượng gốc: {original_size_mb:.2f} MB")
    
    print_step("Đang xử lý hình ảnh...")
    # Load and optimize
    with Image.open(SPLASH_INPUT).convert("RGBA") as img:
        print_info(f"📐 Kích thước gốc: {img.width} × {img.height} px")
        
        # Resize về đúng kích thước mục tiêu (1080x1920)
        print_step(f"Đang resize về kích thước mục tiêu {SPLASH_TARGET[0]}×{SPLASH_TARGET[1]} px...")
        
        # Sử dụng ImageOps.fit để resize và crop về đúng tỷ lệ, sau đó resize về đúng kích thước
        if img.width / img.height != SPLASH_TARGET[0] / SPLASH_TARGET[1]:
            # Nếu tỷ lệ khác, fit và crop về đúng tỷ lệ trước
            img = ImageOps.fit(img, SPLASH_TARGET, method=Image.LANCZOS, centering=(0.5, 0.5))
        else:
            # Nếu tỷ lệ đúng, chỉ cần resize
            img = img.resize(SPLASH_TARGET, Image.LANCZOS)
        
        print_info(f"✂️  Đã resize: {img.width} × {img.height} px")
        
        # Đảm bảo kích thước chính xác
        if img.size != SPLASH_TARGET:
            canvas = Image.new("RGBA", SPLASH_TARGET, (0, 0, 0, 0))
            x_offset = (SPLASH_TARGET[0] - img.width) // 2
            y_offset = (SPLASH_TARGET[1] - img.height) // 2
            canvas.paste(img, (x_offset, y_offset), img)
            img = canvas
        
        # Save with optimization
        print_step("Đang lưu file đã tối ưu...")
        SPLASH_OUTPUT.parent.mkdir(parents=True, exist_ok=True)
        img.save(SPLASH_OUTPUT, format="PNG", optimize=True, compress_level=9)
    
    # Calculate savings
    output_size = SPLASH_OUTPUT.stat().st_size
    output_size_mb = output_size / 1024 / 1024
    saved_mb = (original_size - output_size) / 1024 / 1024
    saved_percent = (saved_mb / original_size_mb * 100) if original_size_mb > 0 else 0
    
    print()
    print("╔══════════════════════════════════════════════════════════════╗")
    print("║              📊 KẾT QUẢ TỐI ƯU HÓA                           ║")
    print("╚══════════════════════════════════════════════════════════════╝")
    print()
    print_success(f"💾 Output: {SPLASH_OUTPUT}")
    print_info(f"📐 Kích thước cuối: {SPLASH_TARGET[0]} × {SPLASH_TARGET[1]} px")
    print_info(f"📦 Dung lượng sau tối ưu: {output_size_mb:.2f} MB")
    print_success(f"💾 Đã tiết kiệm: {saved_mb:.2f} MB ({saved_percent:.1f}%)")
    
    return True

if __name__ == "__main__":
    success = optimize_splash()
    
    # Update styles.xml if needed
    styles_file = ROOT / "app" / "src" / "main" / "res" / "values" / "styles.xml"
    if styles_file.exists():
        content = styles_file.read_text()
        if "@drawable/ic_splash" in content:
            content = content.replace("@drawable/ic_splash", "@drawable/splash")
            styles_file.write_text(content)
            print_success("📝 Đã cập nhật styles.xml để sử dụng @drawable/splash")
    
    if success:
        print()
        print("╔══════════════════════════════════════════════════════════════╗")
        print("║              ✅ TỐI ƯU HÓA HOÀN TẤT!                          ║")
        print("╚══════════════════════════════════════════════════════════════╝")
        sys.exit(0)
    else:
        print()
        print("╔══════════════════════════════════════════════════════════════╗")
        print("║              ❌ TỐI ƯU HÓA THẤT BẠI!                         ║")
        print("╚══════════════════════════════════════════════════════════════╝")
        sys.exit(1)
