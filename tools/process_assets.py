"""
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - Asset Processor
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!

Utility to resize/copy TXASplit assets into Android resource folders.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

from PIL import Image, ImageOps

def print_banner():
    print("╔══════════════════════════════════════════════════════════════╗")
    print("║           🎨 TXASplit Asset Processor                        ║")
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
APP_RES = ROOT / "app" / "src" / "main" / "res"
ASSETS = {
    "logo": ROOT / "logo.png",
    "notif": ROOT / "notif.png",
    "welcome_primary": ROOT / "wc2.png",
    "welcome_secondary": ROOT / "wc2.png",
}

# Launcher icon sizes (dp → px cho từng density)
LAUNCHER_SIZES = {
    "mdpi": 48,    # 1x
    "hdpi": 72,    # 1.5x
    "xhdpi": 96,   # 2x
    "xxhdpi": 144, # 3x
    "xxxhdpi": 192, # 4x
}

# Notification icon sizes (dp → px cho từng density)
NOTIFICATION_SIZES = {
    "mdpi": 24,    # 1x
    "hdpi": 36,    # 1.5x
    "xhdpi": 48,   # 2x
    "xxhdpi": 72,  # 3x
    "xxxhdpi": 96, # 4x
}

# Welcome screen: Full HD landscape
WELCOME_TARGET = (1920, 1080)  # Full HD welcome screen
# Splash screen: Portrait phone (9:16 ratio)
SPLASH_TARGET = (1080, 1920)    # Portrait splash (9:16 ratio cho phone)


def ensure_exists(path: Path) -> None:
    if not path.exists():
        raise FileNotFoundError(f"Missing asset: {path}")


def save_png(image: Image.Image, dest: Path, optimize: bool = True) -> None:
    """Save PNG với optimization"""
    dest.parent.mkdir(parents=True, exist_ok=True)
    image.save(dest, format="PNG", optimize=optimize, compress_level=9)


def process_launcher() -> None:
    """Resize logo thành launcher icons cho các density"""
    print_step("Xử lý Launcher Icons...")
    ensure_exists(ASSETS["logo"])
    
    with Image.open(ASSETS["logo"]).convert("RGBA") as img:
        original_size = f"{img.width}×{img.height}"
        print_info(f"Logo gốc: {original_size} px")
        
        count = 0
        for density, size in LAUNCHER_SIZES.items():
            # Resize về đúng kích thước (size x size)
            target = img.resize((size, size), Image.LANCZOS)
            base = APP_RES / f"mipmap-{density}"
            save_png(target, base / "ic_launcher.png")
            save_png(target, base / "ic_launcher_round.png")
            print_info(f"  {density}: {size}×{size} px")
            count += 2
        print_success(f"Launcher icons đã được cập nhật ({count} files)")


def process_notification() -> None:
    """Resize notification icon thành các density"""
    print_step("Xử lý Notification Icons...")
    ensure_exists(ASSETS["notif"])
    
    with Image.open(ASSETS["notif"]).convert("RGBA") as img:
        original_size = f"{img.width}×{img.height}"
        print_info(f"Notification icon gốc: {original_size} px")
        
        count = 0
        for density, size in NOTIFICATION_SIZES.items():
            # Resize về đúng kích thước (size x size)
            target = img.resize((size, size), Image.LANCZOS)
            base = APP_RES / f"drawable-{density}"
            save_png(target, base / "ic_stat_txaboard.png")
            print_info(f"  {density}: {size}×{size} px")
            count += 1
        print_success(f"Notification icons đã được cập nhật ({count} files)")


def process_welcome() -> None:
    """Resize welcome và splash screens"""
    print_step("Xử lý Welcome & Splash Assets...")
    ensure_exists(ASSETS["welcome_primary"])
    ensure_exists(ASSETS["welcome_secondary"])
    
    # Welcome primary: 1920x1080 (landscape)
    with Image.open(ASSETS["welcome_primary"]).convert("RGBA") as img:
        original_size = f"{img.width}×{img.height}"
        print_info(f"Welcome primary gốc: {original_size} px")
        
        # Resize về đúng kích thước 1920x1080
        if img.size != WELCOME_TARGET:
            target = ImageOps.fit(img, WELCOME_TARGET, method=Image.LANCZOS, centering=(0.5, 0.5))
        else:
            target = img
        
        save_png(target, APP_RES / "drawable-nodpi" / "welcome_primary.png")
        print_success(f"welcome_primary: {WELCOME_TARGET[0]}×{WELCOME_TARGET[1]} px")
    
    # Welcome secondary / Splash: 1080x1920 (portrait)
    with Image.open(ASSETS["welcome_secondary"]).convert("RGBA") as img:
        original_size = f"{img.width}×{img.height}"
        print_info(f"Splash/welcome_secondary gốc: {original_size} px")
        
        # Resize về đúng kích thước 1080x1920
        if img.size != SPLASH_TARGET:
            target = ImageOps.fit(img, SPLASH_TARGET, method=Image.LANCZOS, centering=(0.5, 0.5))
        else:
            target = img
        
        save_png(target, APP_RES / "drawable-nodpi" / "splash.png")
        save_png(target, APP_RES / "drawable-nodpi" / "welcome_secondary.png")
        print_success(f"splash/welcome_secondary: {SPLASH_TARGET[0]}×{SPLASH_TARGET[1]} px")
    
    print_success("Welcome & splash assets đã được cập nhật (3 files)")


def main(argv: list[str]) -> int:
    print_banner()
    
    parser = argparse.ArgumentParser(description="Process TXASplit visual assets")
    parser.add_argument(
        "--skip",
        choices=["launcher", "notif", "welcome"],
        action="append",
        help="Skip selected stages",
    )
    args = parser.parse_args(argv)
    skip = set(args.skip or [])
    
    print_info(f"Bỏ qua: {', '.join(skip) if skip else 'Không có'}")
    print()
    
    try:
        if "launcher" not in skip:
            process_launcher()
        if "notif" not in skip:
            process_notification()
        if "welcome" not in skip:
            process_welcome()
        
        print()
        print("╔══════════════════════════════════════════════════════════════╗")
        print("║              ✅ XỬ LÝ ASSETS HOÀN TẤT!                        ║")
        print("╚══════════════════════════════════════════════════════════════╝")
        print()
        print_success("Tất cả assets đã được resize và xử lý thành công!")
        return 0
    except FileNotFoundError as e:
        print()
        print("╔══════════════════════════════════════════════════════════════╗")
        print("║              ❌ XỬ LÝ ASSETS THẤT BẠI!                        ║")
        print("╚══════════════════════════════════════════════════════════════╝")
        print()
        print_error(str(e))
        print_info("Vui lòng đảm bảo các file sau tồn tại ở root project:")
        print_info("  - logo.png (1024x1024px)")
        print_info("  - notii.png (512x512px)")
        print_info("  - wc1.png (1920x1080px)")
        print_info("  - wc2.png (1080x1920px)")
        return 1
    except Exception as e:
        print()
        print("╔══════════════════════════════════════════════════════════════╗")
        print("║              ❌ XỬ LÝ ASSETS THẤT BẠI!                        ║")
        print("╚══════════════════════════════════════════════════════════════╝")
        print()
        print_error(f"Lỗi không mong đợi: {e}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
