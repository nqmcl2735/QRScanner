# Software Requirements Specification (SRS)
## Ứng dụng Android: QR Generator & Scanner

**Phiên bản:** 1.0
**Ngày:** 24/07/2026
**Trạng thái:** Bản nháp

---

## 1. Giới thiệu

### 1.1 Mục đích
Tài liệu này mô tả các yêu cầu chức năng và phi chức năng cho ứng dụng Android cho phép người dùng tạo mã QR, lưu mã QR vào bộ nhớ ngoài, chọn ảnh QR từ bộ nhớ ngoài và quét ảnh đó để giải mã thành văn bản. Tài liệu dùng làm cơ sở cho đội phát triển, kiểm thử và các bên liên quan thống nhất phạm vi công việc.

### 1.2 Phạm vi
Ứng dụng là một app Android độc lập (native), hoạt động offline (không yêu cầu kết nối mạng cho các chức năng cốt lõi), gồm 3 nhóm chức năng chính:
1. Sinh mã QR từ văn bản người dùng nhập và lưu vào bộ nhớ ngoài (external storage).
2. Chọn ảnh chứa mã QR có sẵn trong bộ nhớ ngoài (thư viện ảnh/thiết bị).
3. Quét/giải mã ảnh QR đã chọn thành văn bản và hiển thị kết quả cho người dùng.

Ngoài ra, giai đoạn khởi tạo dự án bao gồm việc khảo sát, lựa chọn và tích hợp (import) thư viện quét mã QR phù hợp vào project.

### 1.3 Định nghĩa, từ viết tắt
| Từ viết tắt | Ý nghĩa |
|---|---|
| QR | Quick Response (mã phản hồi nhanh) |
| SRS | Software Requirements Specification |
| SDK | Software Development Kit |
| API | Application Programming Interface |
| APK | Android Package (gói cài đặt ứng dụng Android) |
| Scoped Storage | Cơ chế lưu trữ có phạm vi của Android (từ Android 10 trở lên) |

### 1.4 Tài liệu tham khảo
- Android Developers – Data and file storage overview
- Tài liệu thư viện ZXing (zxing-android-embedded)
- Tài liệu thư viện ML Kit Barcode Scanning (Google)

---

## 2. Mô tả tổng quan

### 2.1 Bối cảnh sản phẩm
Ứng dụng phục vụ nhu cầu cá nhân/nội bộ: tạo nhanh mã QR (ví dụ chứa link, thông tin liên hệ, chuỗi văn bản...) và giải mã lại các ảnh QR sẵn có trên thiết bị mà không cần dùng camera trực tiếp trong luồng chính (luồng chính là chọn ảnh có sẵn để quét).

### 2.2 Chức năng sản phẩm (tổng quan)
- **F1 – Lựa chọn & tích hợp thư viện QR**: Khảo sát, so sánh và chọn thư viện quét/sinh mã QR phù hợp (ví dụ: ZXing, ML Kit Barcode Scanning, ZBar...).
- **F2 – Import thư viện**: Thêm dependency vào project (Gradle), cấu hình quyền và cấu hình build cần thiết.
- **F3 – Tạo mã QR (QR Generator)**: Người dùng nhập nội dung, ứng dụng sinh ảnh QR và lưu vào bộ nhớ ngoài của thiết bị.
- **F4 – Chọn ảnh QR từ bộ nhớ ngoài**: Người dùng chọn một ảnh QR có sẵn từ bộ nhớ ngoài (Gallery/File picker).
- **F5 – Quét ảnh QR ra văn bản**: Ứng dụng đọc ảnh đã chọn, giải mã QR và hiển thị nội dung văn bản giải mã được.

### 2.3 Đối tượng người dùng
Người dùng phổ thông có nhu cầu tạo và đọc mã QR nhanh trên thiết bị Android cá nhân; không yêu cầu kiến thức kỹ thuật.

### 2.4 Ràng buộc
- Ứng dụng chạy trên Android, tối thiểu **Android 8.0 (API 26)** trở lên (có thể điều chỉnh theo yêu cầu thực tế).
- Từ Android 10 (API 29) trở lên phải tuân thủ cơ chế **Scoped Storage**; với Android 13+ (API 33) cần dùng quyền ảnh chi tiết (`READ_MEDIA_IMAGES`) thay cho `READ_EXTERNAL_STORAGE`.
- Thư viện quét QR được chọn phải là mã nguồn mở hoặc miễn phí, hỗ trợ giải mã từ ảnh tĩnh (không chỉ từ camera).

### 2.5 Giả định và phụ thuộc
- Thiết bị có bộ nhớ ngoài khả dụng và người dùng đã cấp quyền truy cập bộ nhớ/ảnh.
- Ảnh được chọn để quét chứa mã QR hợp lệ, rõ nét.

---

## 3. Yêu cầu chức năng chi tiết (Functional Requirements)

### FR-1: Lựa chọn thư viện Scan QR
| Mục | Nội dung |
|---|---|
| Mô tả | Đội phát triển khảo sát và lựa chọn thư viện quét/sinh mã QR (ví dụ: ZXing – `zxing-android-embedded`, Google ML Kit Barcode Scanning, ZBar) dựa trên tiêu chí: độ chính xác, hiệu năng, kích thước thư viện, khả năng hoạt động offline, giấy phép sử dụng, mức độ hỗ trợ/bảo trì. |
| Đầu vào | Không có (bước chuẩn bị kỹ thuật) |
| Đầu ra | Quyết định thư viện được chọn, có tài liệu so sánh ngắn |
| Ưu tiên | Cao |

### FR-2: Import thư viện vào project
| Mục | Nội dung |
|---|---|
| Mô tả | Thêm dependency của thư viện đã chọn vào file `build.gradle`, đồng bộ project, khai báo các quyền cần thiết trong `AndroidManifest.xml` (ví dụ quyền đọc/ghi bộ nhớ, quyền camera nếu cần). |
| Điều kiện tiên quyết | Đã hoàn thành FR-1 |
| Đầu ra | Project build thành công với thư viện đã tích hợp, không phát sinh lỗi xung đột dependency |
| Ưu tiên | Cao |

### FR-3: Tạo mã QR (QR Generator) và lưu vào bộ nhớ ngoài
| Mục | Nội dung |
|---|---|
| Mô tả | Người dùng nhập nội dung văn bản vào ô nhập liệu; nhấn nút "Tạo mã QR"; ứng dụng sinh ảnh QR tương ứng và hiển thị preview. Người dùng có thể lưu ảnh QR vào bộ nhớ ngoài (thư mục Pictures hoặc thư mục con của app). |
| Đầu vào | Chuỗi văn bản (text, URL...) do người dùng nhập |
| Xử lý | Sinh mã QR bằng thư viện đã tích hợp (FR-2); mã hóa nội dung thành ảnh QR (định dạng PNG khuyến nghị) |
| Đầu ra | Ảnh QR hiển thị trên màn hình; file ảnh được lưu vào bộ nhớ ngoài; thông báo lưu thành công kèm đường dẫn/tên file |
| Ngoại lệ | Nếu nội dung nhập rỗng → hiển thị cảnh báo yêu cầu nhập nội dung; nếu không có quyền ghi bộ nhớ → yêu cầu cấp quyền hoặc hiển thị lỗi |
| Ưu tiên | Cao |

### FR-4: Chọn ảnh QR từ bộ nhớ ngoài
| Mục | Nội dung |
|---|---|
| Mô tả | Người dùng nhấn nút "Chọn ảnh"; hệ thống mở trình chọn ảnh (Gallery/System Picker) để người dùng chọn một ảnh QR có sẵn trong bộ nhớ ngoài của thiết bị. |
| Đầu vào | Thao tác chọn ảnh của người dùng |
| Xử lý | Ứng dụng nhận URI/đường dẫn ảnh được chọn; hiển thị ảnh preview trên màn hình |
| Đầu ra | Ảnh đã chọn được hiển thị, sẵn sàng để đưa vào bước quét (FR-5) |
| Ngoại lệ | Nếu người dùng không cấp quyền truy cập ảnh → hiển thị thông báo yêu cầu cấp quyền; nếu người dùng hủy chọn → không thực hiện thêm hành động |
| Ưu tiên | Cao |

### FR-5: Quét ảnh QR thành văn bản và hiển thị
| Mục | Nội dung |
|---|---|
| Mô tả | Sau khi có ảnh QR (từ FR-4), ứng dụng dùng thư viện đã tích hợp để giải mã nội dung QR trong ảnh và hiển thị văn bản kết quả cho người dùng. |
| Đầu vào | Ảnh QR đã chọn (từ FR-4) |
| Xử lý | Đọc dữ liệu ảnh → giải mã bằng thư viện QR → trích xuất chuỗi văn bản |
| Đầu ra | Văn bản giải mã được hiển thị trên màn hình (dạng text, có thể sao chép) |
| Ngoại lệ | Nếu ảnh không chứa mã QR hợp lệ hoặc không giải mã được → hiển thị thông báo lỗi "Không tìm thấy mã QR hợp lệ trong ảnh" | 
| Ưu tiên | Cao |

---

## 4. Yêu cầu phi chức năng (Non-functional Requirements)

| Mã | Loại | Mô tả |
|---|---|---|
| NFR-1 | Hiệu năng | Thời gian tạo mã QR và thời gian giải mã ảnh QR không vượt quá 2 giây với ảnh có kích thước thông thường (≤ 5MB). |
| NFR-2 | Khả dụng | Ứng dụng hoạt động offline hoàn toàn cho các chức năng tạo/quét QR (không cần internet). |
| NFR-3 | Bảo mật/Quyền riêng tư | Chỉ yêu cầu các quyền bộ nhớ/ảnh cần thiết tối thiểu; không thu thập hay gửi dữ liệu người dùng ra ngoài thiết bị. |
| NFR-4 | Khả năng tương thích | Hỗ trợ tối thiểu Android 8.0 (API 26) trở lên; tương thích cơ chế Scoped Storage (Android 10+) và quyền ảnh chi tiết (Android 13+). |
| NFR-5 | Khả năng sử dụng | Giao diện đơn giản, thao tác tối đa 2-3 bước cho mỗi chức năng chính. |
| NFR-6 | Độ tin cậy | Xử lý và thông báo lỗi rõ ràng cho các trường hợp: thiếu quyền, ảnh không hợp lệ, không giải mã được. |

---

## 5. Yêu cầu giao diện (UI Requirements)

Ứng dụng gồm tối thiểu các màn hình sau:

1. **Màn hình chính (Home)**: 2 lựa chọn – "Tạo mã QR" và "Quét ảnh QR".
2. **Màn hình Tạo mã QR**:
   - Ô nhập văn bản
   - Nút "Tạo mã QR"
   - Vùng hiển thị preview ảnh QR
   - Nút "Lưu vào bộ nhớ"
3. **Màn hình Quét ảnh QR**:
   - Nút "Chọn ảnh từ bộ nhớ"
   - Vùng hiển thị ảnh đã chọn
   - Nút "Quét" (hoặc tự động quét sau khi chọn ảnh)
   - Vùng hiển thị kết quả văn bản giải mã (có nút "Sao chép")

---

## 6. Yêu cầu hệ thống & quyền truy cập

- **Ngôn ngữ/Nền tảng:** Android (Kotlin hoặc Java), Android Studio.
- **Quyền cần khai báo trong Manifest (tùy phiên bản Android):**
  - `READ_EXTERNAL_STORAGE` (Android ≤ 12)
  - `READ_MEDIA_IMAGES` (Android 13+)
  - `WRITE_EXTERNAL_STORAGE` (chỉ cần với Android ≤ 9, các phiên bản sau dùng MediaStore API để ghi vào bộ nhớ dùng chung)
- **Thư viện dự kiến sử dụng (một trong các lựa chọn ở FR-1):**
  - ZXing (`journeyapps:zxing-android-embedded` hoặc `com.google.zxing:core`)
  - Google ML Kit Barcode Scanning

---

## 7. Luồng nghiệp vụ chính (Main Flows)

**Luồng 1 – Tạo mã QR:**
Người dùng mở app → chọn "Tạo mã QR" → nhập văn bản → nhấn "Tạo" → xem preview → nhấn "Lưu" → hệ thống lưu ảnh vào bộ nhớ ngoài → hiển thị thông báo thành công.

**Luồng 2 – Quét ảnh QR có sẵn:**
Người dùng mở app → chọn "Quét ảnh QR" → nhấn "Chọn ảnh" → chọn ảnh từ bộ nhớ ngoài → hệ thống giải mã ảnh → hiển thị văn bản kết quả.

---

## 8. Tiêu chí nghiệm thu (Acceptance Criteria)

- [ ] Thư viện QR được chọn và tích hợp thành công, project build không lỗi.
- [ ] Người dùng nhập văn bản và tạo được ảnh QR chính xác (quét lại bằng ứng dụng khác cho ra đúng nội dung đã nhập).
- [ ] Ảnh QR được lưu thành công vào bộ nhớ ngoài, có thể tìm thấy trong thư viện ảnh của thiết bị.
- [ ] Người dùng chọn được ảnh QR từ bộ nhớ ngoài thông qua trình chọn ảnh hệ thống.
- [ ] Ứng dụng giải mã đúng nội dung văn bản từ ảnh QR hợp lệ và hiển thị lên màn hình.
- [ ] Ứng dụng hiển thị thông báo lỗi phù hợp khi ảnh không chứa QR hợp lệ hoặc thiếu quyền truy cập.

---

## 9. Phạm vi ngoài dự án (Out of Scope)
- Quét QR trực tiếp qua camera theo thời gian thực (không thuộc phạm vi mô tả ban đầu; có thể bổ sung ở phiên bản sau).
- Đồng bộ dữ liệu lên cloud/server.
- Đăng nhập/tài khoản người dùng.
