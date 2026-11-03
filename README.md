# TikGrab

App Android (tối ưu cho Samsung Galaxy Z Fold 7) dùng để lưu video TikTok **HD, không watermark**.

- **Cách nhanh nhất:** trong TikTok bấm **Chia sẻ → TikGrab**. Video tự tải ngầm, thanh thông báo hiện tiến độ, bạn vẫn ở lại TikTok.
- **Hoặc:** mở TikGrab, dán link (app tự nhận link vừa copy), bấm **Tải về**.
- Video lưu vào `Movies/TikGrab` (hoặc `DCIM/TikGrab`), hiện ngay trong Thư viện. Bài đăng dạng ảnh lưu vào `Pictures/TikGrab`; nhạc MP3 (tuỳ chọn) lưu vào `Music/TikGrab`.
- Màn ngoài: 1 cột. Màn trong: 2 cột (bên trái tải, bên phải thư viện). Gập/mở máy không mất trạng thái.
- Tự kiểm tra bản mới trên GitHub Releases, bấm một lần là tải và cài.

## Cài đặt
Vào mục **Releases** của repo, tải file `TikGrab-vX.Y.Z.apk` về điện thoại rồi mở để cài (cho phép "Cài ứng dụng không rõ nguồn" nếu máy hỏi).

## Phát hành bản mới
Tăng `versionName` / `versionCode` trong `app/build.gradle.kts`, cập nhật `app/src/main/assets/whatsnew.json`, rồi push. GitHub Actions tự build APK và tạo Release mới.
Khoá ký app nằm trong 2 GitHub Secret: `TIKGRAB_KEYSTORE` (base64 của file .jks) và `TIKGRAB_KEYSTORE_PASSWORD`.

Created by Mondiro
