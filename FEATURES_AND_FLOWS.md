# Feature và luồng hoạt động của QRScanner

## 1. Sơ đồ điều hướng tổng quát

```text
Home
├── Tạo mã từ văn bản ─────────► QRGeneratorActivity
├── Tạo mã theo biểu mẫu ──────► QRFormGeneratorActivity
├── Quét bằng camera ──────────► CameraScannerActivity
│                                └── CameraCaptureActivity
│                                    └── QRDetailActivity
├── Quét từ thư viện ──────────► QRScannerActivity
├── Lịch sử ───────────────────► HistoryActivity
│                                └── QRDetailActivity
└── Mục gần đây ───────────────► QRDetailActivity
```

## 2. Danh sách feature

| Nhóm | Feature | Trạng thái theo mã nguồn |
|---|---|---|
| Trang chủ | Điều hướng tới 5 chức năng chính | Có |
| Trang chủ | Hiển thị tối đa 2 mục lịch sử gần nhất | Có |
| Tạo mã | Tạo QR từ văn bản tự do | Có |
| Tạo mã | Tạo EAN-13, EAN-8, UPC-A, Code 128, Code 39, PDF417, Data Matrix, Aztec | Có |
| Tạo mã | Tạo QR bằng form Wi-Fi, contact, email, phone, SMS, URL | Có |
| Tạo mã | Preview, lưu PNG vào thư viện, chia sẻ nội dung/ảnh | Có |
| Quét | Quét QR trực tiếp bằng camera | Có |
| Quét | Chọn ảnh trong thư viện và giải mã bằng ZXing MultiFormatReader | Có |
| Phân loại | TEXT, WIFI, LOCATION, CONTACT, EMAIL, PHONE, SMS, URL | Có |
| Hành động | Mở link/Maps, lưu liên hệ, gọi điện, gửi email/SMS, kết nối Wi-Fi | Có qua Android Intent/API |
| Lịch sử | Tự lưu khi tạo/quét thành công | Có |
| Lịch sử | Danh sách mới nhất trước, xem chi tiết, sao chép, xóa | Có |
| Lịch sử | Xóa toàn bộ có xác nhận | Có |
| Lịch sử | Export/import CSV | Có, với giới hạn nêu bên dưới |
| Chia sẻ | Chia sẻ text hoặc ảnh bằng Android Sharesheet | Có |

## 3. Luồng khởi động và trang chủ

1. Android mở `HomeActivity` từ launcher intent.
2. Người dùng chọn một card để đi đến tạo mã, tạo theo form, quét camera, quét ảnh hoặc lịch sử.
3. Mỗi lần `HomeActivity.onResume()`, ứng dụng đọc toàn bộ lịch sử trên background thread.
4. Nếu có dữ liệu, hai mục mới nhất được hiển thị; chạm vào mục sẽ mở `QRDetailActivity` bằng history ID.

## 4. Luồng tạo mã từ nội dung tự do

1. Người dùng mở `QRGeneratorActivity`.
2. Chọn một `BarcodeType`; mặc định là QR Code.
3. Nhập nội dung và bấm tạo.
4. `QRGeneratorViewModel` loại bỏ khoảng trắng đầu/cuối và từ chối chuỗi rỗng.
5. Kích thước ảnh được chọn theo định dạng:
   - QR Code, Data Matrix, Aztec: 1024 x 1024.
   - Các barcode còn lại: 1024 x 300.
6. `ZXingQRCodeProvider` encode nội dung thành bitmap.
7. Nội dung được phân loại qua `QRContentParser`; UI hiển thị preview và action tương ứng.
8. Ảnh và metadata được tự động ghi vào lịch sử với nguồn `GENERATED`.
9. Người dùng có thể:
   - Lưu PNG vào `Pictures/QRApp`.
   - Chia sẻ nội dung hoặc ảnh.
   - Thực hiện action theo loại nội dung nếu là QR có cấu trúc.
10. Nếu sửa input sau khi tạo, mã hiện tại được đánh dấu là cũ và chia sẻ bị khóa cho đến khi tạo lại.

Lưu ý: nội dung có hợp lệ với chuẩn của từng barcode 1D hay không do ZXing kiểm tra; lỗi encode được trả về dưới dạng thông báo tạo mã thất bại.

## 5. Luồng tạo QR theo biểu mẫu

1. Người dùng mở `QRFormGeneratorActivity` và chọn loại form.
2. Ứng dụng hiện các trường tương ứng, xóa input/preview của loại trước.
3. Dữ liệu được validate và chuyển thành chuỗi chuẩn:

| Form | Định dạng đầu ra | Kiểm tra chính |
|---|---|---|
| Wi-Fi | `WIFI:T:...;S:...;P:...;;` | Bắt buộc SSID; hỗ trợ WPA, WEP, nopass |
| Contact | vCard 3.0 | Cần tên hoặc số điện thoại |
| Email | `mailto:` cùng subject/body query | Email hợp lệ |
| Phone | `tel:` | Bắt buộc số điện thoại |
| SMS | `smsto:` cùng body query | Bắt buộc số người nhận |
| URL | HTTP/HTTPS; tự thêm `https://` nếu thiếu | URL hợp lệ |

4. Chuỗi được chuyển cho cùng `QRGeneratorViewModel` để tạo QR Code.
5. Các bước preview, tự lưu lịch sử, lưu thư viện, chia sẻ và action giống luồng tạo tự do.

## 6. Luồng quét trực tiếp bằng camera

1. Người dùng mở `CameraScannerActivity`.
2. Activity lập tức khởi chạy `ScanContract` của ZXing với định dạng mong muốn là QR Code.
3. `CameraCaptureActivity` cung cấp giao diện camera và nút quay lại.
4. Nếu người dùng hủy quét, Activity kết thúc.
5. Nếu quét thành công, ZXing trả nội dung và đường dẫn ảnh barcode.
6. `CameraScannerViewModel` phân loại nội dung, đọc bitmap và lưu lịch sử với nguồn `SCANNED`.
7. Khi lưu thành công, Activity nhận history ID, mở `QRDetailActivity`, rồi tự kết thúc.
8. Nếu xử lý/lưu thất bại, ứng dụng báo lỗi và kết thúc màn hình quét.

## 7. Luồng quét từ ảnh thư viện

1. Người dùng mở `QRScannerActivity` và chọn ảnh qua system picker (`image/*`).
2. URI ảnh được hiển thị ở khu vực preview; nút quét được bật.
3. Khi bấm quét, repository tải bitmap từ URI.
4. `ZXingQRCodeProvider` dùng `MultiFormatReader` để decode.
5. Thành công:
   - Hiển thị nội dung text.
   - Phân loại nội dung và sinh action.
   - Cho phép sao chép/chia sẻ.
   - Tự lưu ảnh, nội dung và nguồn `SCANNED` vào lịch sử.
6. Thất bại: hiển thị thông báo không tìm thấy mã QR hợp lệ trong ảnh.

Màn hình này không tự điều hướng sang chi tiết sau khi quét; kết quả được thao tác ngay tại màn hình.

## 8. Luồng nhận diện nội dung và hành động

`QRContentParser` nhận chuỗi gốc và trả `ParsedQRContent`. `QRActionBinder` dựa trên type để tạo nút:

| Loại | Mẫu được nhận diện | Hành động |
|---|---|---|
| `URL` | URL HTTP/HTTPS hợp lệ | Mở trình duyệt/app xử lý link |
| `WIFI` | Chuỗi `WIFI:` | Mở màn hình thêm mạng, gửi network suggestion hoặc kết nối legacy tùy Android |
| `LOCATION` | URI `geo:` | Mở ứng dụng bản đồ tại tọa độ |
| `CONTACT` | MECARD hoặc vCard | Mở màn hình thêm liên hệ; thêm nút gọi nếu có số |
| `EMAIL` | `mailto:` hoặc `MATMSG:` | Mở ứng dụng email, điền người nhận/subject/body |
| `PHONE` | `tel:` | Mở dialer với số đã điền, không gọi trực tiếp |
| `SMS` | `sms:` hoặc `smsto:` | Mở ứng dụng SMS với người nhận/body |
| `TEXT` | Không khớp các loại trên | Không sinh action chuyên biệt |

Nếu thiết bị không có ứng dụng xử lý Intent tương ứng, ứng dụng hiển thị Toast thay vì crash.

## 9. Luồng lịch sử

### Tự động lưu

- Tạo mã: lưu nội dung, loại đã parse, nguồn `GENERATED`, timestamp hiện tại và snapshot bitmap.
- Quét camera/thư viện: lưu tương tự với nguồn `SCANNED`.
- SQLite trả dữ liệu theo `timestamp DESC`.

### Xem và thao tác danh sách

1. `HistoryActivity` tải lịch sử qua `HistoryViewModel`.
2. `HistoryAdapter` hiển thị nội dung rút gọn, thời gian, loại/nguồn và style theo content type.
3. Người dùng có thể:
   - Chạm mục để mở chi tiết.
   - Sao chép text vào clipboard.
   - Sao chép ảnh QR vào clipboard nếu snapshot còn tồn tại.
   - Xóa một mục sau xác nhận.
   - Xóa toàn bộ sau xác nhận.
4. Xóa bản ghi cũng xóa file snapshot tương ứng.

### Import/export CSV

- Export tạo file với header `id,content,type,source,timestamp,imagePath` qua system document picker.
- Nội dung có dấu nháy kép được escape theo kiểu CSV.
- Import đọc CSV được chọn, bỏ header, khôi phục `content`, `type`, `source` rồi gọi `saveEntry()`.
- Import không giữ ID/timestamp gốc và không nhập lại ảnh; mỗi mục có timestamp mới tại lúc import và `imagePath = null`.
- Parser CSV là triển khai thủ công; file không đúng schema hoặc enum không hợp lệ sẽ làm toàn bộ thao tác báo lỗi định dạng.

## 10. Luồng xem chi tiết

1. `QRDetailActivity` nhận `EXTRA_HISTORY_ID`.
2. `QRDetailViewModel` đọc bản ghi trên background thread, parse lại content và tải bitmap từ `imagePath` nếu có.
3. Màn hình hiển thị nội dung đầy đủ, thời gian, nguồn, loại, ảnh và các action chuyên biệt.
4. Người dùng có thể sao chép text, chia sẻ text/ảnh hoặc xóa mục.
5. Sau khi xóa thành công, Activity kết thúc; trang trước tải lại dữ liệu khi resume hoặc qua ViewModel.

## 11. Luồng chia sẻ và lưu ảnh

### Chia sẻ

1. `ShareUtil` hiện dialog chọn chia sẻ nội dung hoặc ảnh.
2. Chia sẻ nội dung dùng `ACTION_SEND` với MIME `text/plain`.
3. Chia sẻ ảnh ghi tạm `cache/qr_share/qr_share.png`.
4. `FileProvider` chuyển file thành content URI và cấp quyền đọc tạm.
5. Android Sharesheet được mở để người dùng chọn ứng dụng nhận.

### Lưu vào thư viện

- Android 10+: dùng MediaStore với `RELATIVE_PATH = Pictures/QRApp` và cơ chế `IS_PENDING`.
- Android 8/9: ghi vào thư mục Pictures/QRApp, cần quyền ghi external storage và yêu cầu Media Scanner cập nhật thư viện.
- Tên file có dạng `QR_yyyyMMdd_HHmmss.png` cho cả QR và barcode.

## 12. Xử lý nền và trạng thái UI

- Các ViewModel dùng `Executors.newSingleThreadExecutor()` cho encode/decode, SQLite và file I/O.
- Kết quả được đưa về UI qua `LiveData`/`postValue`.
- Activity observe loading để khóa nút và hiện progress indicator.
- Executor được dừng trong `ViewModel.onCleared()`; executor riêng của `HomeActivity` được dừng trong `onDestroy()`.

## 13. Giới hạn và điểm cần kiểm thử thêm

- Chưa có automated test trong repo.
- Cần kiểm thử action Wi-Fi trên API 26-28, 29 và 30+ vì mỗi nhóm dùng API khác nhau; Android hiện đại vẫn cần người dùng xác nhận/tham gia luồng hệ thống.
- Cần kiểm thử Intent Maps, contact, email và SMS trên thiết bị có/không có ứng dụng tương ứng.
- Cần kiểm thử import CSV với dấu phẩy, dấu nháy kép, xuống dòng trong content và file lớn.
- Quét camera chỉ giới hạn QR Code, dù các phần tạo mã và decode ảnh hỗ trợ nhiều barcode.
- Lịch sử phụ thuộc file snapshot trong vùng riêng của app; xóa dữ liệu ứng dụng hoặc mất file sẽ khiến mục lịch sử không còn ảnh nhưng text vẫn có thể còn trong SQLite.
