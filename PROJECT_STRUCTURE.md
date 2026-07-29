# Cấu trúc dự án QRScanner

## 1. Tổng quan

QRScanner là ứng dụng Android viết bằng Java, gồm một module `app`, dùng mô hình MVVM theo cách thủ công. Ứng dụng tạo và đọc QR/barcode, phân loại nội dung, lưu lịch sử cục bộ và chuyển các nội dung đã nhận diện sang ứng dụng Android phù hợp.

Thông số kỹ thuật chính:

- Namespace/application ID: `com.example.qrapp`
- Ngôn ngữ: Java 17
- Min SDK: 26; Target/Compile SDK: 35
- Giao diện: XML Layout, View Binding, Material Components
- Quản lý trạng thái: AndroidX ViewModel và LiveData
- Mã hóa/giải mã: ZXing Core và ZXing Android Embedded
- Lưu lịch sử: SQLite qua `SQLiteOpenHelper`
- Lưu ảnh: MediaStore; ảnh lịch sử nằm trong bộ nhớ riêng của ứng dụng

## 2. Cây thư mục chính

```text
QRScanner/
├── app/
│   ├── build.gradle                  # Cấu hình module và dependency
│   └── src/main/
│       ├── AndroidManifest.xml       # Permission, Activity, FileProvider
│       ├── java/com/example/qrapp/
│       │   ├── data/
│       │   │   ├── model/            # Model và enum nghiệp vụ
│       │   │   ├── repository/       # Điều phối các nguồn dữ liệu
│       │   │   └── source/           # SQLite, MediaStore, ZXing
│       │   ├── ui/
│       │   │   ├── base/             # Xử lý chung cho Activity
│       │   │   ├── home/             # Trang chủ
│       │   │   ├── generator/        # Tạo QR/barcode
│       │   │   ├── scanner/          # Quét camera và ảnh thư viện
│       │   │   ├── history/          # Danh sách lịch sử
│       │   │   ├── detail/           # Chi tiết một mục lịch sử
│       │   │   └── viewmodel/        # Factory khởi tạo ViewModel
│       │   └── util/                  # Parser, action, share, file, style
│       └── res/
│           ├── layout/               # Layout Activity và item
│           ├── drawable/             # Icon và background
│           ├── menu/                  # Menu lịch sử
│           ├── values/                # String, màu, kích thước, theme
│           └── xml/file_paths.xml     # Phạm vi chia sẻ file qua FileProvider
├── gradle/                            # Gradle Wrapper
├── README.md                          # Ghi chú feature cũ
├── SRS_QR_App.md                      # Đặc tả yêu cầu
└── SDD_QR_App_MVVM.md                 # Tài liệu thiết kế MVVM
```

Các thư mục `.gradle/`, `build/` và `app/build/` là sản phẩm sinh ra trong quá trình build, không chứa mã nguồn nghiệp vụ.

## 3. Kiến trúc và chiều phụ thuộc

```text
XML/View Binding
       │
       ▼
Activity ── observe/call ──► ViewModel
                                │
                                ▼
                           Repository
                         ┌──────┼───────┐
                         ▼      ▼       ▼
                       ZXing  SQLite  MediaStore/files
```

- `Activity` phụ trách render UI, nhận thao tác, mở màn hình/ứng dụng ngoài và observe `LiveData`.
- `ViewModel` giữ trạng thái màn hình, kiểm tra đầu vào ở mức luồng và chạy I/O trên `ExecutorService` đơn luồng.
- `Repository` cung cấp API nghiệp vụ cho tạo, quét và lịch sử.
- `Data source` thực hiện thao tác cụ thể với ZXing, SQLite và hệ thống lưu trữ Android.
- `ViewModelFactory` đóng vai trò dependency injection thủ công. Mỗi Activity tự tạo dependency rồi truyền vào factory.
- `util` chứa logic dùng chung![img.png](img.png) nhưng một số lớp (`QRActionBinder`, `ShareUtil`, `WifiConnectHelper`) phụ thuộc trực tiếp Android UI/platform.

Không có domain layer/use-case riêng và không dùng framework dependency injection.

## 4. Thành phần theo package

### `data/model`

| Thành phần | Vai trò |
|---|---|
| `BarcodeType` | Danh sách định dạng có thể tạo: QR Code, EAN-13, EAN-8, UPC-A, Code 128, Code 39, PDF417, Data Matrix, Aztec. |
| `QRContentType` | Loại nội dung nghiệp vụ: text, Wi-Fi, vị trí, liên hệ, email, điện thoại, SMS, URL. |
| `ParsedQRContent` | Nội dung sau phân tích cùng các trường chuyên biệt như SSID, tọa độ, email. |
| `ScanResult` | Kết quả giải mã gồm text, bitmap nguồn và định dạng barcode. |
| `QRHistoryItem` | Một bản ghi lịch sử gồm ID, nội dung, loại, nguồn, thời gian và đường dẫn ảnh. |
| `HistorySource` | Phân biệt mã được tạo (`GENERATED`) và được quét (`SCANNED`). |

### `data/source`

- `qrlib/IQRCodeProvider`: abstraction cho encode/decode.
- `qrlib/ZXingQRCodeProvider`: dùng `MultiFormatWriter`/`MultiFormatReader`, UTF-8 và `TRY_HARDER` khi decode.
- `storage/IStorageDataSource`: abstraction đọc và ghi bitmap.
- `storage/MediaStoreDataSource`: đọc ảnh từ `Uri`; ghi PNG vào `Pictures/QRApp` qua MediaStore trên Android 10+, có nhánh tương thích Android cũ.
- `history/IHistoryDataSource`: CRUD tối thiểu cho lịch sử.
- `history/HistorySqliteDataSource`: database `qr_history.db`, bảng `history`, sắp xếp bản ghi mới nhất trước.

Schema bảng `history`:

| Cột | Kiểu | Ý nghĩa |
|---|---|---|
| `id` | INTEGER, PK | ID tự tăng |
| `content` | TEXT | Nội dung gốc |
| `type` | TEXT | Tên enum `QRContentType` |
| `source` | TEXT | Tên enum `HistorySource` |
| `timestamp` | INTEGER | Unix timestamp theo millisecond |
| `image_path` | TEXT, nullable | Ảnh snapshot trong vùng riêng của app |

### `data/repository`

- `QRGeneratorRepository`: tạo bitmap qua provider và lưu ảnh qua storage.
- `QRScannerRepository`: tải bitmap từ URI, giải mã rồi trả `ScanResult`.
- `HistoryRepository`: lưu đồng thời ảnh snapshot và bản ghi SQLite; đọc/xóa một hoặc toàn bộ lịch sử.

### `ui`

| Màn hình | Trách nhiệm chính |
|---|---|
| `HomeActivity` | Màn hình launcher, điều hướng tới các chức năng và tải tối đa hai mục gần đây. |
| `QRGeneratorActivity` | Tạo QR hoặc barcode từ chuỗi nhập tự do. |
| `QRFormGeneratorActivity` | Tạo QR đúng định dạng từ form Wi-Fi, contact, email, phone, SMS hoặc URL. |
| `CameraScannerActivity` | Khởi chạy ZXing camera contract và lưu kết quả. |
| `CameraCaptureActivity` | Giao diện capture tùy chỉnh cho ZXing Embedded. |
| `QRScannerActivity` | Chọn ảnh từ thư viện và giải mã mã trong ảnh. |
| `HistoryActivity` | Hiển thị, import/export, sao chép và xóa lịch sử. |
| `QRDetailActivity` | Hiển thị nội dung/ảnh chi tiết, action, chia sẻ và xóa một mục. |
| `BaseActivity` | Thiết lập edge-to-edge và system bar insets. |

Các ViewModel tương ứng là `QRGeneratorViewModel`, `QRScannerViewModel`, `CameraScannerViewModel`, `HistoryViewModel` và `QRDetailViewModel`. `HomeActivity` hiện đọc repository trực tiếp thay vì có `HomeViewModel`.

### `util`

- `QRContentParser`: phân loại và trích xuất dữ liệu từ Wi-Fi, geo, MECARD/vCard, mailto/MATMSG, tel, SMS và URL; không khớp thì trả `TEXT`.
- `QRActionBinder`: tạo nút hành động theo loại nội dung và phát Android Intent phù hợp.
- `WifiConnectHelper`: xử lý kết nối/gợi ý/thêm Wi-Fi theo phiên bản Android.
- `ShareUtil`: chia sẻ text hoặc ảnh PNG qua Android Sharesheet.
- `ImageFileStore`: lưu/xóa snapshot ảnh dùng bởi lịch sử.
- `FileProviderUtil`: tạo content URI an toàn để chia sẻ file.
- `QRTypeStyle`: ánh xạ loại nội dung sang icon và màu hiển thị.

## 5. Tài nguyên và cấu hình Android

`AndroidManifest.xml` khai báo:

- `CAMERA` cho quét camera.
- `WRITE_EXTERNAL_STORAGE` chỉ đến API 28 cho nhánh lưu ảnh cũ.
- `ACCESS_WIFI_STATE` và `CHANGE_WIFI_STATE` cho chức năng Wi-Fi.
- Camera là feature không bắt buộc, vì ứng dụng vẫn dùng được tạo mã/quét ảnh khi thiết bị không có camera.
- `HomeActivity` là launcher duy nhất và được export; các Activity khác không export.
- `FileProvider` dùng authority `${applicationId}.fileprovider` để chia sẻ ảnh cache/private qua URI.

## 6. Luồng dữ liệu dùng chung

### Tạo hoặc quét thành công

1. Activity gửi yêu cầu tới ViewModel.
2. ViewModel chạy encode/decode trong background executor.
3. `QRContentParser` xác định `QRContentType`.
4. ViewModel cập nhật LiveData để Activity render kết quả.
5. `HistoryRepository.saveEntry()` lưu snapshot ảnh rồi insert SQLite.
6. Khi mở lại trang chủ/lịch sử, dữ liệu được đọc theo thời gian giảm dần.

Việc lưu lịch sử trong luồng tạo và quét ảnh là tác vụ phụ: lỗi lưu bị bỏ qua để không làm hỏng kết quả chính. Riêng quét camera cần lưu thành công để có ID điều hướng sang màn hình chi tiết; nếu lưu lỗi, màn hình báo lỗi và kết thúc.

## 7. Ghi chú về hiện trạng mã nguồn

- Chưa có thư mục test (`src/test` hoặc `src/androidTest`) trong mã nguồn hiện tại.
- Các Activity tự lắp ghép dependency nên có lặp đoạn khởi tạo repository/data source.
- SQLite đang ở version 1; `onUpgrade()` xóa và tạo lại bảng, vì vậy nâng version hiện tại sẽ mất lịch sử cũ.
- Export lịch sử ra CSV có đủ metadata, nhưng import chỉ phục hồi nội dung, loại và nguồn; timestamp mới được tạo tại lúc import và ảnh không được phục hồi.
- Camera scanner cấu hình chỉ nhận QR Code; provider dùng cho ảnh thư viện có thể decode nhiều định dạng.
- Tài liệu này mô tả mã đang có tại thời điểm rà soát, không khẳng định mọi nhánh phụ thuộc ứng dụng ngoài (Wi-Fi, danh bạ, email, bản đồ) đã được kiểm thử trên mọi phiên bản Android.
