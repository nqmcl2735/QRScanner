# Cách hệ thống hoạt động theo từng use case người dùng

## 1. Mục đích tài liệu

Tài liệu này mô tả ứng dụng QRScanner theo góc nhìn use case: người dùng làm gì, hệ thống xử lý qua thành phần nào, dữ liệu nào được đọc/ghi, kết quả thành công và các nhánh lỗi có thể xảy ra.

Các tác nhân liên quan:

- **Người dùng:** thao tác trực tiếp với ứng dụng.
- **Android System:** cung cấp camera, bộ chọn file, MediaStore, clipboard, Sharesheet và Intent.
- **Ứng dụng bên ngoài:** trình duyệt, Maps, danh bạ, dialer, email và SMS.
- **SQLite:** lưu metadata lịch sử trong vùng dữ liệu riêng của ứng dụng.
- **ZXing:** tạo và giải mã QR/barcode.

## 2. Luồng hệ thống chung

```text
Người dùng
    │ thao tác
    ▼
Activity / View Binding
    │ gọi hàm, observe LiveData
    ▼
ViewModel
    │ chạy tác vụ nền
    ▼
Repository
    ├── ZXing: tạo/giải mã mã
    ├── SQLite: lưu/đọc lịch sử
    ├── MediaStore: đọc/lưu ảnh công khai
    └── App-private files: lưu ảnh snapshot lịch sử
```

Các tác vụ encode, decode, database và file I/O chủ yếu chạy trên `ExecutorService` để không chặn giao diện. ViewModel trả trạng thái về Activity qua `LiveData`; Activity cập nhật preview, progress indicator, nút thao tác và thông báo lỗi.

## UC-01 — Mở ứng dụng và chọn chức năng

**Mục tiêu:** truy cập chức năng chính hoặc mở lại một mục gần đây.

**Tiền điều kiện:** ứng dụng đã được cài đặt.

**Luồng chính:**

1. Người dùng mở ứng dụng từ launcher.
2. Android khởi chạy `HomeActivity`.
3. Ứng dụng hiển thị các lựa chọn:
   - Tạo mã từ nội dung tự do.
   - Tạo QR theo biểu mẫu.
   - Quét QR bằng camera.
   - Quét mã từ ảnh thư viện.
   - Xem lịch sử.
4. Trong `onResume()`, `HomeActivity` dùng `HistoryRepository` đọc lịch sử trên background thread.
5. Nếu có dữ liệu, hệ thống hiển thị tối đa hai bản ghi mới nhất.
6. Người dùng chọn một chức năng hoặc chạm vào mục gần đây.
7. Android mở Activity tương ứng; nếu chọn mục gần đây, ID lịch sử được truyền sang `QRDetailActivity`.

**Dữ liệu đọc:** bảng SQLite `history`.

**Ngoại lệ:** nếu chưa có lịch sử, phần mục gần đây được ẩn; các chức năng chính vẫn hoạt động bình thường.

## UC-02 — Tạo QR/barcode từ nội dung tự do

**Mục tiêu:** chuyển một chuỗi văn bản thành QR Code hoặc barcode.

**Tiền điều kiện:** người dùng đang ở `QRGeneratorActivity`.

**Luồng chính:**

1. Người dùng chọn loại mã; mặc định là QR Code.
2. Các loại được hỗ trợ gồm QR Code, EAN-13, EAN-8, UPC-A, Code 128, Code 39, PDF417, Data Matrix và Aztec.
3. Người dùng nhập nội dung rồi bấm **Tạo**.
4. Activity chuyển nội dung tới `QRGeneratorViewModel.generateQRCode()`.
5. ViewModel trim nội dung và bật trạng thái loading.
6. `QRGeneratorRepository` gọi `ZXingQRCodeProvider` để encode:
   - QR Code, Data Matrix, Aztec: bitmap 1024 × 1024.
   - Các loại còn lại: bitmap 1024 × 300.
7. `QRContentParser` phân loại nội dung thành TEXT, URL, WIFI, LOCATION, CONTACT, EMAIL, PHONE hoặc SMS.
8. ViewModel cập nhật bitmap và nội dung đã parse qua LiveData.
9. Activity hiển thị preview, bật nút lưu/chia sẻ và tạo action phù hợp nếu nội dung có cấu trúc.
10. ViewModel tự lưu mã vào lịch sử với nguồn `GENERATED`.

**Dữ liệu ghi:**

- Một file snapshot PNG trong vùng riêng của ứng dụng.
- Một bản ghi trong bảng `history` gồm content, type, source, timestamp và image path.

**Nhánh thay thế/lỗi:**

- Nội dung rỗng: hệ thống yêu cầu nhập nội dung và không encode.
- Nội dung không hợp lệ với barcode đã chọn: ZXing báo lỗi; hệ thống hiển thị thông báo tạo mã thất bại.
- Lưu lịch sử thất bại: mã vẫn được hiển thị vì lưu lịch sử là tác vụ phụ của luồng tạo.
- Người dùng sửa nội dung sau khi tạo: preview được đánh dấu là không còn đồng bộ; chia sẻ bị khóa cho tới khi tạo lại.

## UC-03 — Tạo QR từ biểu mẫu có cấu trúc

**Mục tiêu:** tạo nội dung QR đúng định dạng mà không phải tự viết cú pháp.

**Tiền điều kiện:** người dùng đang ở `QRFormGeneratorActivity`.

**Luồng chính:**

1. Người dùng chọn một loại form.
2. Activity chỉ hiện nhóm trường tương ứng và xóa input/preview của loại trước.
3. Người dùng nhập dữ liệu và bấm **Tạo**.
4. Activity kiểm tra dữ liệu và tạo chuỗi chuẩn:

| Loại form | Chuỗi sinh ra | Điều kiện hợp lệ |
|---|---|---|
| Wi-Fi | `WIFI:T:...;S:...;P:...;;` | Có SSID; chọn WPA, WEP hoặc không mật khẩu |
| Liên hệ | vCard 3.0 | Có tên hoặc số điện thoại |
| Email | `mailto:` và query subject/body | Địa chỉ email hợp lệ |
| Điện thoại | `tel:` | Có số điện thoại |
| SMS | `smsto:` và query body | Có số người nhận |
| URL | HTTP/HTTPS | URL hợp lệ; tự thêm `https://` nếu thiếu scheme |

5. Chuỗi đã định dạng được chuyển tới cùng `QRGeneratorViewModel` của UC-02.
6. ZXing tạo QR Code, hệ thống preview, phân loại lại nội dung và tự lưu lịch sử.

**Kết quả:** người dùng nhận QR đúng format, có thể lưu, chia sẻ hoặc sử dụng action tương ứng.

**Ngoại lệ:** nếu trường bắt buộc thiếu hoặc email/URL không hợp lệ, hệ thống hiển thị thông báo và không tạo QR.

## UC-04 — Quét QR trực tiếp bằng camera

**Mục tiêu:** đọc QR Code từ camera thiết bị.

**Tiền điều kiện:** thiết bị có camera hoạt động và người dùng cho phép truy cập camera khi hệ thống yêu cầu.

**Luồng chính:**

1. Người dùng chọn quét bằng camera tại trang chủ.
2. `CameraScannerActivity` khởi chạy `ScanContract` của ZXing.
3. `CameraCaptureActivity` hiển thị camera scanner và toolbar quay lại.
4. ZXing nhận diện QR Code và trả về text cùng đường dẫn ảnh barcode.
5. `CameraScannerViewModel` dùng `QRContentParser` phân loại nội dung.
6. ViewModel đọc ảnh kết quả và gọi `HistoryRepository.saveEntry()` với nguồn `SCANNED`.
7. Repository lưu snapshot và insert SQLite.
8. ViewModel trả history ID cho Activity.
9. Activity mở `QRDetailActivity` để hiển thị kết quả, sau đó kết thúc màn hình camera.

**Dữ liệu ghi:** snapshot riêng của ứng dụng và một bản ghi lịch sử.

**Nhánh thay thế/lỗi:**

- Người dùng hủy hoặc quay lại: màn hình quét kết thúc, không ghi lịch sử.
- Không cấp quyền camera: hệ thống camera không thể bắt đầu bình thường.
- Không lưu được kết quả: ứng dụng báo lỗi và kết thúc; luồng camera cần history ID để mở màn hình chi tiết.
- Camera scanner hiện chỉ cấu hình nhận QR Code, không phải toàn bộ barcode mà ứng dụng có thể tạo.

## UC-05 — Quét QR/barcode từ ảnh trong thư viện

**Mục tiêu:** giải mã một mã có trong ảnh đã lưu trên thiết bị hoặc nguồn file mà system picker cung cấp.

**Tiền điều kiện:** có ứng dụng/system picker cung cấp ảnh qua content URI.

**Luồng chính:**

1. Người dùng mở `QRScannerActivity`.
2. Chọn **Chọn ảnh**.
3. Android mở system picker với MIME `image/*`.
4. Người dùng chọn ảnh; Activity nhận URI và hiển thị preview.
5. Người dùng bấm **Quét**.
6. `QRScannerViewModel` gọi `QRScannerRepository.decodeQRFromUri()`.
7. `MediaStoreDataSource` đọc URI qua `ContentResolver` thành bitmap.
8. `ZXingQRCodeProvider` dùng `MultiFormatReader` để giải mã.
9. ViewModel phân loại nội dung, trả text và loại nội dung qua LiveData.
10. Activity hiển thị kết quả, bật sao chép/chia sẻ và tạo action chuyên biệt.
11. ViewModel tự lưu kết quả vào lịch sử với nguồn `SCANNED`.

**Nhánh thay thế/lỗi:**

- Chưa chọn ảnh: hệ thống yêu cầu chọn ảnh trước.
- Ảnh không chứa mã hợp lệ hoặc không đọc được: hiển thị thông báo không tìm thấy mã hợp lệ.
- Lưu lịch sử thất bại: kết quả quét vẫn được hiển thị.
- Khác với luồng camera, màn hình này không tự chuyển sang chi tiết sau khi quét.

## UC-06 — Lưu ảnh QR/barcode vào thư viện thiết bị

**Mục tiêu:** tạo một bản PNG công khai mà người dùng có thể xem trong Gallery hoặc ứng dụng quản lý file.

**Tiền điều kiện:** đã tạo thành công một QR/barcode và có bitmap hiện tại.

**Luồng chính:**

1. Người dùng bấm **Lưu** tại màn hình tạo mã.
2. Trên Android 8–9, ứng dụng kiểm tra/yêu cầu `WRITE_EXTERNAL_STORAGE`.
3. `QRGeneratorViewModel` tạo tên file `QR_yyyyMMdd_HHmmss.png`.
4. `QRGeneratorRepository` gọi `MediaStoreDataSource.saveImage()`.
5. Trên Android 10+, ảnh được ghi qua MediaStore vào `Pictures/QRApp` bằng cơ chế `IS_PENDING`.
6. Trên Android 8–9, ảnh được ghi trực tiếp vào thư mục Pictures/QRApp và Media Scanner được thông báo.
7. Activity nhận URI kết quả và hiển thị thông báo lưu thành công.

**Dữ liệu ghi:** file PNG trong external/shared storage.

**Phân biệt:** đây là ảnh công khai do người dùng chủ động lưu. Ảnh snapshot của lịch sử nằm trong bộ nhớ riêng và được tạo tự động ở UC-02, UC-03, UC-04 hoặc UC-05.

**Ngoại lệ:** không có bitmap, hết dung lượng, lỗi I/O hoặc từ chối quyền trên Android cũ thì hệ thống thông báo không thể lưu.

## UC-07 — Chia sẻ nội dung hoặc ảnh mã

**Mục tiêu:** gửi nội dung/ảnh sang một ứng dụng khác.

**Tiền điều kiện:** màn hình hiện tại có nội dung; chia sẻ ảnh yêu cầu có bitmap.

**Luồng chính:**

1. Người dùng bấm **Chia sẻ**.
2. `ShareUtil` hiển thị dialog gồm hai lựa chọn:
   - Chia sẻ nội dung.
   - Chia sẻ ảnh QR/barcode.
3. Nếu chọn nội dung, hệ thống tạo `ACTION_SEND`, MIME `text/plain` và đặt text vào `EXTRA_TEXT`.
4. Nếu chọn ảnh:
   - Bitmap được ghi tạm thành `cache/qr_share/qr_share.png`.
   - `FileProvider` chuyển file thành content URI.
   - Intent dùng MIME `image/png`, `EXTRA_STREAM` và quyền đọc URI tạm thời.
5. Android Sharesheet hiển thị các ứng dụng nhận tương thích.
6. Người dùng chọn ứng dụng và tiếp tục thao tác trong ứng dụng đó.

**Ngoại lệ:** không có bitmap hoặc ghi file tạm thất bại thì hệ thống báo không thể chia sẻ ảnh.

## UC-08 — Thực hiện hành động theo loại nội dung

**Mục tiêu:** dùng ngay dữ liệu trong QR thay vì sao chép thủ công.

**Tiền điều kiện:** `QRContentParser` nhận diện được nội dung có cấu trúc.

`QRActionBinder` tạo nút theo loại nội dung:

| Nội dung | Hành động hệ thống |
|---|---|
| URL | Mở `ACTION_VIEW` tới trình duyệt hoặc ứng dụng xử lý link |
| Vị trí `geo:` | Mở ứng dụng bản đồ tại tọa độ |
| Liên hệ MECARD/vCard | Mở màn hình thêm danh bạ; thêm nút gọi nếu có số điện thoại |
| Email `mailto:`/`MATMSG:` | Mở ứng dụng email với người nhận, subject và body |
| Điện thoại `tel:` | Mở dialer với số đã điền; ứng dụng không tự thực hiện cuộc gọi |
| SMS `sms:`/`smsto:` | Mở ứng dụng SMS với số người nhận và nội dung |
| Wi-Fi | Thêm/gợi ý/kết nối mạng tùy phiên bản Android |
| Text thường | Không tạo action chuyên biệt |

**Luồng Wi-Fi theo phiên bản:**

- Android 11+: mở màn hình hệ thống thêm mạng bằng `ACTION_WIFI_ADD_NETWORKS`.
- Android 10: thêm `WifiNetworkSuggestion` và thông báo kết quả thêm suggestion.
- Android 8–9: dùng API `WifiConfiguration` legacy để thêm và kích hoạt mạng.

**Ngoại lệ:** nếu không có ứng dụng xử lý Maps, contact, dialer, email, SMS hoặc browser, ứng dụng bắt `ActivityNotFoundException` và hiển thị Toast.

## UC-09 — Xem danh sách lịch sử

**Mục tiêu:** xem lại các mã đã tạo hoặc quét.

**Luồng chính:**

1. Người dùng mở `HistoryActivity`.
2. `HistoryViewModel` gọi `HistoryRepository.getAll()` trên background thread.
3. `HistorySqliteDataSource` query bảng `history` theo `timestamp DESC`.
4. LiveData trả danh sách cho Activity.
5. `HistoryAdapter` hiển thị mỗi mục với nội dung, thời gian, nguồn, loại và các nút thao tác.

**Kết quả:** bản ghi mới nhất nằm đầu danh sách.

**Ngoại lệ:** nếu không có bản ghi, Activity hiển thị trạng thái danh sách trống.

## UC-10 — Xem chi tiết một mục lịch sử

**Mục tiêu:** xem đầy đủ nội dung, ảnh và hành động của một bản ghi.

**Điểm bắt đầu:** người dùng chạm mục trong danh sách lịch sử hoặc mục gần đây trên trang chủ.

**Luồng chính:**

1. Activity nguồn mở `QRDetailActivity` và truyền `EXTRA_HISTORY_ID`.
2. `QRDetailViewModel` gọi `HistoryRepository.getById()`.
3. SQLite trả bản ghi có ID tương ứng.
4. ViewModel parse lại content và đọc bitmap từ `imagePath` nếu file tồn tại.
5. Activity hiển thị nội dung, nguồn, loại, thời gian, ảnh và các action tương ứng.
6. Người dùng có thể sao chép, chia sẻ, thực hiện action hoặc xóa mục.

**Ngoại lệ:** nếu bản ghi không tồn tại hoặc ảnh đã mất, phần dữ liệu/ảnh tương ứng không thể hiển thị; text vẫn phụ thuộc bản ghi SQLite.

## UC-11 — Sao chép nội dung hoặc ảnh

### Sao chép nội dung

1. Người dùng bấm nút sao chép tại kết quả quét, danh sách lịch sử hoặc màn hình chi tiết.
2. Activity tạo `ClipData` dạng plain text.
3. `ClipboardManager` đặt nội dung vào clipboard.
4. Hệ thống hiển thị thông báo đã sao chép.

### Sao chép ảnh từ lịch sử

1. Người dùng bấm nút sao chép ảnh tại một mục lịch sử.
2. Ứng dụng lấy `imagePath` và tạo content URI qua `FileProvider`.
3. URI được đặt vào clipboard bằng `ClipData.newUri()`.
4. Ứng dụng khác có thể nhận URI theo quyền và khả năng hỗ trợ clipboard của Android.

**Ngoại lệ:** nếu mục không có `imagePath`, hệ thống báo không thể sao chép ảnh.

## UC-12 — Xóa một mục lịch sử

**Mục tiêu:** loại bỏ một bản ghi không còn cần thiết.

**Luồng chính:**

1. Người dùng bấm xóa tại danh sách hoặc màn hình chi tiết.
2. Ứng dụng hiển thị dialog xác nhận.
3. Nếu người dùng đồng ý, ViewModel gọi `HistoryRepository.delete()` trên background thread.
4. Repository xóa dòng SQLite theo ID.
5. Repository xóa file snapshot tại `imagePath` nếu tồn tại.
6. Danh sách được tải lại; nếu xóa từ chi tiết, màn hình chi tiết kết thúc.

**Nhánh thay thế:** người dùng hủy dialog thì không có dữ liệu nào thay đổi.

**Phạm vi xóa:** ảnh mà người dùng từng chủ động lưu trong `Pictures/QRApp` không bị xóa; chỉ snapshot nội bộ gắn với lịch sử bị xóa.

## UC-13 — Xóa toàn bộ lịch sử

**Mục tiêu:** xóa mọi bản ghi và snapshot lịch sử.

**Luồng chính:**

1. Người dùng chọn xóa toàn bộ trên menu của `HistoryActivity`.
2. Ứng dụng hiển thị dialog xác nhận.
3. `HistoryViewModel` gọi `HistoryRepository.deleteAll()`.
4. Repository đọc danh sách hiện tại và xóa lần lượt các file snapshot.
5. Data source xóa toàn bộ dòng trong bảng `history`.
6. LiveData cập nhật danh sách rỗng và Activity hiển thị thông báo thành công.

**Nhánh thay thế:** hủy xác nhận thì không xóa dữ liệu.

**Phạm vi xóa:** không xóa ảnh công khai trong `Pictures/QRApp` và không xóa các file CSV đã export.

## UC-14 — Export lịch sử thành CSV

**Mục tiêu:** xuất metadata lịch sử thành file người dùng có thể lưu/chuyển đi.

**Luồng chính:**

1. Người dùng chọn **Export CSV** trên toolbar lịch sử.
2. Android mở `CreateDocument` với MIME `text/csv` và tên file gợi ý.
3. Người dùng chọn vị trí lưu.
4. Activity chuyển URI đích cho `HistoryViewModel.exportHistory()`.
5. ViewModel đọc toàn bộ lịch sử từ SQLite.
6. Hệ thống ghi header:

```csv
id,content,type,source,timestamp,imagePath
```

7. Mỗi bản ghi được ghi thành một dòng; dấu nháy kép trong content được escape.
8. Activity nhận thông báo export thành công hoặc lỗi.

**Dữ liệu đọc:** SQLite; hệ thống chỉ ghi chuỗi `imagePath`, không đóng gói nội dung file ảnh.

**Giới hạn quan trọng:** CSV hiện tại không phải backup đầy đủ. File ảnh snapshot vẫn nằm trong vùng riêng của ứng dụng và đường dẫn đó không có giá trị khi chuyển sang thiết bị khác hoặc sau khi gỡ ứng dụng.

## UC-15 — Import lịch sử từ CSV

**Mục tiêu:** nhập lại content/type/source từ file CSV đúng định dạng.

**Luồng chính:**

1. Người dùng chọn **Import CSV**.
2. Android mở `OpenDocument` cho file CSV/text.
3. Người dùng chọn file.
4. Activity chuyển URI nguồn cho `HistoryViewModel.importHistory()`.
5. ViewModel bỏ dòng header và đọc từng dòng dữ liệu.
6. Hệ thống tách cột, unescape content, chuyển type/source về enum.
7. Mỗi dòng hợp lệ được lưu bằng `HistoryRepository.saveEntry()` với bitmap bằng `null`.
8. SQLite tạo ID và timestamp mới cho bản ghi nhập.
9. Danh sách được tải lại và hệ thống thông báo số mục đã nhập.

**Dữ liệu được phục hồi:** content, type và source.

**Dữ liệu không được phục hồi:** ID gốc, timestamp gốc và ảnh snapshot.

**Ngoại lệ:** file sai cấu trúc, enum không hợp lệ hoặc lỗi đọc sẽ làm hệ thống báo không thể nhập dữ liệu. Việc import không có transaction toàn bộ, vì vậy các dòng đã insert trước lúc lỗi có thể vẫn tồn tại.

## UC-16 — Quay lại màn hình trước

**Mục tiêu:** rời màn hình hiện tại mà không thực hiện thêm thao tác.

**Luồng chính:**

1. Người dùng bấm nút quay lại trên toolbar hoặc nút Back của Android.
2. Activity hiện tại gọi `finish()` hoặc được Android đưa ra khỏi back stack.
3. Màn hình trước được hiển thị.
4. Khi quay lại `HomeActivity`, `onResume()` tải lại mục gần đây để phản ánh lịch sử mới.

**Trường hợp camera:** bấm quay lại/hủy scan kết thúc luồng camera mà không lưu kết quả.

## 3. Ma trận use case và thành phần xử lý

| Use case | Activity chính | ViewModel | Repository/Data source chính |
|---|---|---|---|
| UC-01 Trang chủ | `HomeActivity` | Không có ViewModel riêng | `HistoryRepository`, `HistorySqliteDataSource` |
| UC-02 Tạo tự do | `QRGeneratorActivity` | `QRGeneratorViewModel` | `QRGeneratorRepository`, ZXing, History |
| UC-03 Tạo từ form | `QRFormGeneratorActivity` | `QRGeneratorViewModel` | `QRGeneratorRepository`, ZXing, History |
| UC-04 Quét camera | `CameraScannerActivity`, `CameraCaptureActivity` | `CameraScannerViewModel` | ZXing Embedded, `HistoryRepository` |
| UC-05 Quét ảnh | `QRScannerActivity` | `QRScannerViewModel` | `QRScannerRepository`, MediaStore, ZXing, History |
| UC-06 Lưu ảnh | Hai Activity tạo mã | `QRGeneratorViewModel` | `QRGeneratorRepository`, `MediaStoreDataSource` |
| UC-07 Chia sẻ | Generator, Scanner, Detail | ViewModel của màn hình | `ShareUtil`, `FileProvider` |
| UC-08 Action nội dung | Generator, Scanner, Detail | ViewModel của màn hình | `QRContentParser`, `QRActionBinder` |
| UC-09 Danh sách | `HistoryActivity` | `HistoryViewModel` | `HistoryRepository`, SQLite |
| UC-10 Chi tiết | `QRDetailActivity` | `QRDetailViewModel` | `HistoryRepository`, SQLite/files |
| UC-11 Sao chép | Scanner, History, Detail | ViewModel của màn hình | Clipboard, `FileProvider` |
| UC-12 Xóa một mục | History, Detail | History/Detail ViewModel | `HistoryRepository`, SQLite/files |
| UC-13 Xóa tất cả | `HistoryActivity` | `HistoryViewModel` | `HistoryRepository`, SQLite/files |
| UC-14 Export CSV | `HistoryActivity` | `HistoryViewModel` | SQLite, `ContentResolver` |
| UC-15 Import CSV | `HistoryActivity` | `HistoryViewModel` | `ContentResolver`, History, SQLite |

## 4. Vòng đời dữ liệu

```text
Tạo/quét thành công
       │
       ├── content/type/source/timestamp ──► SQLite app-private
       │
       └── bitmap ──► snapshot app-private ──► image_path trong SQLite

Người dùng bấm Lưu
       └── bitmap ──► Pictures/QRApp trong shared/external storage

Người dùng Export CSV
       └── metadata ──► file CSV do người dùng chọn
                        (không chứa bytes ảnh)

Xóa lịch sử
       ├── xóa dòng SQLite
       └── xóa snapshot app-private
           (không xóa ảnh đã lưu vào Pictures/QRApp)
```

SQLite và snapshot lịch sử có thể mất khi người dùng xóa dữ liệu ứng dụng hoặc gỡ ứng dụng. Ảnh đã lưu công khai và CSV đã export nằm ngoài vòng đời dữ liệu riêng của ứng dụng nên thường không bị thao tác xóa lịch sử ảnh hưởng.

## 5. Các giới hạn hiện tại ảnh hưởng use case

- Camera chỉ quét QR Code; quét từ ảnh dùng `MultiFormatReader` và có thể nhận nhiều barcode hơn.
- Export CSV không chứa dữ liệu ảnh; import CSV không phục hồi ảnh hoặc timestamp gốc.
- Import CSV chưa chạy trong transaction toàn bộ và parser CSV là triển khai thủ công.
- Chưa có automated test cho các use case trong repository.
- Hành động Wi-Fi, Maps, contact, email và SMS phụ thuộc phiên bản Android, ứng dụng hệ thống có sẵn và xác nhận của người dùng.
- Các tác vụ nền dùng executor trong process, không phải Android Service; nếu process bị hệ thống dừng thì tác vụ đang chạy không được đảm bảo hoàn tất.
