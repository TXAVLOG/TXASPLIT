# 🎨 TXASplit - AI Design Prompts
Build by TXA  
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!

## 📱 Prompt cho Logo App (Launcher Icon)

**Yêu cầu:**
- Tạo logo cho ứng dụng quản lý chi tiêu nhóm thông minh "TXASplit"
- Style: Modern, minimalist, professional
- Màu sắc: Gradient từ #FF6B9E (hồng) → #C77DFF (tím) → #9D4EDD (tím đậm)
- Concept: Biểu tượng chia sẻ chi phí, có thể là icon chia đôi, hoặc biểu tượng nhóm người
- Format: PNG với nền trong suốt (RGBA)
- Kích thước: 1024x1024 px (sẽ resize về các density sau)
- Background: Transparent
- Style: Flat design, có thể có shadow nhẹ để tạo depth

**Prompt:**
```
Create a modern, minimalist app logo for "TXASplit" - a smart group expense management app.

Design requirements:
- Style: Modern flat design with subtle depth
- Colors: Gradient from #FF6B9E (pink) → #C77DFF (purple) → #9D4EDD (dark purple)
- Concept: Represent splitting expenses/sharing costs, could be split icon, group of people, or money division symbol
- Format: PNG with transparent background (RGBA)
- Size: 1024x1024 pixels
- Background: Fully transparent
- Style: Clean, professional, suitable for app launcher icon
- Should work well at small sizes (48x48px) and large sizes (192x192px)
- Avoid text, use only symbols/icons
- Make it recognizable and memorable
```

---

## 🎬 Prompt cho Splash Screen

**Yêu cầu:**
- Splash screen cho ứng dụng TXASplit
- Style: Modern, elegant, matching với logo
- Màu sắc: Gradient background từ #FF6B9E → #C77DFF → #9D4EDD
- Logo: Logo app ở giữa, có thể có animation effect (nhưng chỉ cần static image)
- Format: PNG với nền trong suốt hoặc gradient
- Kích thước: 1080x1920 px (Portrait, 9:16 ratio cho phone)
- Text: Có thể có text "TXASplit" dưới logo với font đẹp
- Style: Clean, professional, welcoming

**Prompt:**
```
Create a splash screen for "TXASplit" app - a smart group expense management platform.

Design requirements:
- Style: Modern, elegant, welcoming
- Background: Gradient from #FF6B9E (pink) → #C77DFF (purple) → #9D4EDD (dark purple), or transparent with gradient overlay
- Logo: App logo centered, prominent
- Text: "TXASplit" text below logo (optional, elegant font)
- Format: PNG (RGBA if transparent background)
- Size: 1080x1920 pixels (Portrait, 9:16 ratio)
- Style: Clean, professional, modern
- Should feel premium and trustworthy
- Can include subtle patterns or geometric shapes for visual interest
- Logo should be clearly visible and centered
```

---

## 🔔 Prompt cho Notification Icon

**Yêu cẩu:**
- Icon cho notification của app TXASplit
- Style: Simple, recognizable, monochrome-friendly (sẽ được system tint)
- Màu sắc: Có thể là outline hoặc solid, nhưng phải rõ ràng khi system tint màu
- Format: PNG với nền trong suốt
- Kích thước: 512x512 px (sẽ resize về các density sau)
- Background: Transparent
- Style: Simple icon, dễ nhận biết ở kích thước nhỏ (24x24px)
- Nên là biểu tượng đơn giản từ logo hoặc icon đặc trưng của app

**Prompt:**
```
Create a notification icon for "TXASplit" app.

Design requirements:
- Style: Simple, clean, recognizable icon
- Concept: Should represent the app (expense splitting/sharing), can be simplified version of logo
- Format: PNG with transparent background (RGBA)
- Size: 512x512 pixels
- Background: Fully transparent
- Style: Simple outline or solid icon, should work well when system applies color tint
- Must be recognizable at very small sizes (24x24px)
- Should be monochrome-friendly (will be tinted by Android system)
- Avoid complex details, focus on clear symbol
- Can be simplified version of app logo or distinctive icon
```

---

## 📋 Hướng dẫn sử dụng

1. **Logo App:**
   - Tạo file `logo.png` (1024x1024px) và đặt ở root project
   - Chạy `python tools\process_assets.py` để tự động resize vào các folder mipmap-*

2. **Splash Screen:**
   - Tạo file `splash.png` (1080x1920px) và đặt ở root project với tên `wc2.png` (hoặc đổi tên)
   - Chạy `python tools\optimize_splash.py` để resize và optimize

3. **Notification Icon:**
   - Tạo file `notif.png` (512x512px) và đặt ở root project với tên `notii.png` (hoặc đổi tên)
   - Chạy `python tools\process_assets.py` để tự động resize vào các folder drawable-*

4. **Kiểm tra kết quả:**
   - Chạy `.\tools\inspect_images.ps1` để xem thông tin chi tiết về các file đã generate

---

## 🎨 Màu sắc chính của app

- **Primary Pink:** #FF6B9E
- **Primary Purple:** #C77DFF  
- **Dark Purple:** #9D4EDD
- **Gradient:** Linear từ pink → purple → dark purple
