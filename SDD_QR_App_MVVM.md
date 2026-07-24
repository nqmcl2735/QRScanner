# Software Design Document (SDD)
## Ứng dụng Android: QR Generator & Scanner
### Kiến trúc: MVVM | Ngôn ngữ: Java | Giao diện: XML

**Phiên bản:** 1.0
**Ngày:** 24/07/2026
**Tài liệu tham chiếu:** SRS_QR_App.md (v1.0)

---

## 1. Giới thiệu

### 1.1 Mục đích tài liệu
Tài liệu này mô tả thiết kế kỹ thuật (kiến trúc, cấu trúc thư mục, các lớp, luồng dữ liệu) cho ứng dụng Android hiện thực đúng các chức năng đã nêu trong SRS: chọn/tích hợp thư viện QR, tạo mã QR và lưu vào bộ nhớ ngoài, chọn ảnh QR từ bộ nhớ ngoài, quét ảnh QR ra văn bản và hiển thị.

### 1.2 Phạm vi
Thiết kế áp dụng cho bản build đầu tiên (v1.0), sử dụng kiến trúc **MVVM (Model – View – ViewModel)**, ngôn ngữ **Java**, layout viết bằng **XML**, sử dụng **Android Jetpack** (`ViewModel`, `LiveData`, `ActivityResultLauncher`).

### 1.3 Công nghệ & thư viện sử dụng
| Thành phần | Lựa chọn |
|---|---|
| Ngôn ngữ | Java |
| Kiến trúc | MVVM |
| UI | XML layout + ViewBinding |
| Quản lý vòng đời/dữ liệu | `androidx.lifecycle:lifecycle-viewmodel`, `androidx.lifecycle:lifecycle-livedata` |
| Thư viện sinh/quét QR | ZXing core (`com.google.zxing:core`) + `journeyapps:zxing-android-embedded` (wrapper tiện dụng) |
| Chọn ảnh | `ActivityResultContracts.GetContent` (Android Photo Picker / System Picker) |
| Lưu ảnh vào bộ nhớ ngoài | `MediaStore API` (Android 10+) / `File API` (Android ≤ 9) |
| Bất đồng bộ | `ExecutorService` (background thread) + `LiveData` post lên main thread |

---

## 2. Kiến trúc tổng quan – MVVM

### 2.1 Sơ đồ kiến trúc

```
┌─────────────────────┐        ┌──────────────────────┐        ┌───────────────────────────┐
│        VIEW          │        │       VIEWMODEL       │        │           MODEL            │
│  (Activity / XML)     │        │                        │        │  (Repository + Data Source) │
│                        │        │                        │        │                             │
│  HomeActivity          │        │                        │        │                             │
│  QRGeneratorActivity   │◄──────►│  QRGeneratorViewModel  │◄──────►│  QRGeneratorRepository       │
│   - EditText input     │  Live  │   - LiveData<Bitmap>    │  gọi   │   - QRCodeHelper (ZXing)      │
│   - Button "Tạo"        │  Data  │   - LiveData<Uri>       │ hàm    │   - StorageHelper (MediaStore)│
│   - ImageView preview   │        │   - LiveData<String>err │        │                             │
│   - Button "Lưu"         │        │                        │        │                             │
│                        │        │                        │        │                             │
│  QRScannerActivity     │◄──────►│  QRScannerViewModel    │◄──────►│  QRScannerRepository         │
│   - Button "Chọn ảnh"    │        │   - LiveData<Uri>       │        │   - QRCodeHelper (ZXing)      │
│   - ImageView preview   │        │   - LiveData<String>text│        │   - ImageDecoder helper       │
│   - TextView kết quả     │        │   - LiveData<String>err │        │                             │
└─────────────────────┘        └──────────────────────┘        └───────────────────────────┘
```

### 2.2 Vai trò từng lớp trong MVVM

| Lớp | Vai trò | Ghi chú |
|---|---|---|
| **View** (Activity/Fragment + XML) | Hiển thị UI, nhận thao tác người dùng (click, nhập liệu), quan sát (`observe`) LiveData từ ViewModel để cập nhật giao diện. **Không** chứa logic nghiệp vụ. | Dùng ViewBinding để truy cập view, tránh `findViewById` |
| **ViewModel** | Giữ trạng thái UI (qua `LiveData`), nhận sự kiện từ View, gọi Repository để xử lý, không tham chiếu trực tiếp đến Context/View (tránh leak). Tồn tại độc lập với vòng đời Activity (sống sót qua xoay màn hình). | Kế thừa `AndroidViewModel` nếu cần `Application Context` (ví dụ để truy cập `ContentResolver`) |
| **Model (Repository + Data Source)** | Chứa logic nghiệp vụ thực sự: sinh mã QR, giải mã QR, đọc/ghi file vào bộ nhớ ngoài. Repository là lớp trung gian, ẩn chi tiết triển khai (thư viện QR, MediaStore) khỏi ViewModel. | Tách riêng `QRCodeHelper` (bọc thư viện ZXing) để dễ thay thư viện khác sau này (đáp ứng FR-1/FR-2) |

### 2.3 Luồng dữ liệu (Data Binding qua LiveData)

```
User Action (View)
      │
      ▼
ViewModel.method()  ──────────────►  Repository.method()  ──────────────►  Helper/DataSource
      │                                                                          │
      │  (chạy trên background thread qua ExecutorService)                       │
      │◄─────────────────────────────────────────────────────────────────────────┘
      ▼
LiveData.postValue(result)
      │
      ▼ (Observer trên main thread)
View.observe() → cập nhật UI (ImageView, TextView, Toast...)
```

---

## 3. Kiến trúc thư mục dự án (Project Structure)

```
QRApp/
│
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   │
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           │
│           ├── java/com/example/qrapp/
│           │   │
│           │   ├── QRApplication.java                     # Application class (nếu cần khởi tạo global)
│           │   │
│           │   ├── data/                                  # ===== MODEL LAYER =====
│           │   │   ├── model/
│           │   │   │   ├── QRGenerateRequest.java          # Model input cho việc tạo QR
│           │   │   │   └── QRScanResult.java               # Model kết quả quét QR
│           │   │   │
│           │   │   ├── repository/
│           │   │   │   ├── QRGeneratorRepository.java      # Nghiệp vụ tạo & lưu QR
│           │   │   │   └── QRScannerRepository.java        # Nghiệp vụ đọc & giải mã QR
│           │   │   │
│           │   │   └── source/
│           │   │       ├── qrlib/
│           │   │       │   ├── IQRCodeProvider.java        # Interface bọc thư viện QR (FR-1/FR-2)
│           │   │       │   └── ZXingQRCodeProvider.java    # Implement bằng thư viện ZXing
│           │   │       │
│           │   │       └── storage/
│           │   │           ├── IStorageDataSource.java     # Interface đọc/ghi bộ nhớ ngoài
│           │   │           └── MediaStoreDataSource.java   # Implement bằng MediaStore API
│           │   │
│           │   ├── ui/                                     # ===== VIEW + VIEWMODEL LAYER =====
│           │   │   ├── base/
│           │   │   │   └── BaseActivity.java               # Xử lý chung: permission, toast, loading
│           │   │   │
│           │   │   ├── home/
│           │   │   │   └── HomeActivity.java                # Màn hình chính, điều hướng 2 chức năng
│           │   │   │
│           │   │   ├── generator/
│           │   │   │   ├── QRGeneratorActivity.java          # View - màn hình tạo QR
│           │   │   │   └── QRGeneratorViewModel.java         # ViewModel tương ứng
│           │   │   │
│           │   │   └── scanner/
│           │   │       ├── QRScannerActivity.java            # View - màn hình quét QR từ ảnh
│           │   │       └── QRScannerViewModel.java           # ViewModel tương ứng
│           │   │
│           │   ├── viewmodel/
│           │   │   └── ViewModelFactory.java                # Factory khởi tạo ViewModel kèm Repository
│           │   │
│           │   └── util/
│           │       ├── Constants.java                       # Hằng số (request code, folder name...)
│           │       ├── PermissionUtils.java                 # Xử lý xin quyền runtime
│           │       ├── ImageUtils.java                       # Convert Bitmap <-> Uri, resize ảnh
│           │       └── ResultEvent.java                      # Wrapper "sự kiện dùng 1 lần" cho LiveData
│           │
│           └── res/
│               ├── layout/
│               │   ├── activity_home.xml
│               │   ├── activity_qr_generator.xml
│               │   └── activity_qr_scanner.xml
│               │
│               ├── values/
│               │   ├── strings.xml
│               │   ├── colors.xml
│               │   ├── dimens.xml
│               │   └── themes.xml
│               │
│               ├── drawable/
│               │   ├── ic_qr_generate.xml
│               │   ├── ic_qr_scan.xml
│               │   └── bg_button_rounded.xml
│               │
│               ├── mipmap/                                  # Icon ứng dụng
│               │
│               └── xml/
│                   └── file_paths.xml                        # Cấu hình FileProvider (nếu cần chia sẻ ảnh)
│
├── build.gradle                                              # Project-level Gradle
├── settings.gradle
└── gradle.properties
```

**Nguyên tắc tổ chức thư mục:**
- `data/` chỉ chứa logic nghiệp vụ và truy cập dữ liệu (Model layer trong MVVM) — không phụ thuộc Android UI.
- `ui/` chia theo **feature/module** (`home`, `generator`, `scanner`), mỗi module tự chứa View + ViewModel của mình → dễ mở rộng, dễ bảo trì.
- `util/` chứa các lớp dùng chung, không thuộc riêng module nào.
- Việc bọc thư viện QR trong `data/source/qrlib/IQRCodeProvider.java` giúp đáp ứng đúng yêu cầu FR-1 (chọn thư viện) và FR-2 (import thư viện): nếu sau này đổi từ ZXing sang ML Kit, chỉ cần viết thêm 1 class implement `IQRCodeProvider`, không ảnh hưởng đến Repository/ViewModel/View.

---

## 4. Thiết kế chi tiết theo module

### 4.1 Module `home`

| File | Trách nhiệm |
|---|---|
| `HomeActivity.java` | Hiển thị 2 nút: "Tạo mã QR" và "Quét ảnh QR"; điều hướng (Intent) sang `QRGeneratorActivity` hoặc `QRScannerActivity`. Không có ViewModel riêng vì không có logic/state cần lưu. |

`activity_home.xml`: `ConstraintLayout` chứa 2 `Button` (hoặc `MaterialCardView`) để điều hướng.

---

### 4.2 Module `generator` (đáp ứng FR-3)

**Sơ đồ lớp:**

```
QRGeneratorActivity (View)
   │  observe
   ▼
QRGeneratorViewModel
   - LiveData<Bitmap> qrBitmapResult
   - LiveData<Uri> savedFileUri
   - LiveData<String> errorMessage
   - LiveData<Boolean> loading
   + generateQRCode(String text)
   + saveQRCodeToStorage()
   │  gọi
   ▼
QRGeneratorRepository
   + generateQRBitmap(String text, int size): Bitmap
   + saveBitmapToExternalStorage(Bitmap bitmap, String fileName): Uri
   │  sử dụng
   ▼
IQRCodeProvider (ZXingQRCodeProvider)        IStorageDataSource (MediaStoreDataSource)
   + encode(String content, int size): Bitmap    + saveImage(Bitmap, String fileName): Uri
```

**Luồng xử lý (Sequence – Tạo & lưu QR):**

```
1. User nhập text vào EditText, nhấn "Tạo mã QR"
2. QRGeneratorActivity gọi viewModel.generateQRCode(text)
3. ViewModel gọi repository.generateQRBitmap(text, size) trên background thread
4. Repository gọi ZXingQRCodeProvider.encode(text, size) → trả về Bitmap
5. ViewModel.postValue(qrBitmapResult) → Activity observe → hiển thị ImageView preview
6. User nhấn "Lưu"
7. QRGeneratorActivity gọi viewModel.saveQRCodeToStorage()
8. ViewModel gọi repository.saveBitmapToExternalStorage(bitmap, fileName)
9. Repository gọi MediaStoreDataSource.saveImage() → ghi file vào thư mục Pictures/QRApp
10. ViewModel.postValue(savedFileUri) → Activity hiển thị Toast "Đã lưu ảnh QR thành công"
```

`activity_qr_generator.xml` gồm:
- `TextInputLayout` + `EditText` nhập nội dung
- `Button` "Tạo mã QR"
- `ImageView` preview mã QR
- `Button` "Lưu vào bộ nhớ" (disable đến khi có ảnh QR)
- `ProgressBar` (hiển thị khi loading)

---

### 4.3 Module `scanner` (đáp ứng FR-4, FR-5)

**Sơ đồ lớp:**

```
QRScannerActivity (View)
   │  observe
   ▼
QRScannerViewModel
   - LiveData<Uri> selectedImageUri
   - LiveData<String> decodedText
   - LiveData<String> errorMessage
   - LiveData<Boolean> loading
   + setSelectedImage(Uri uri)
   + scanSelectedImage()
   │  gọi
   ▼
QRScannerRepository
   + decodeQRFromUri(Uri uri): String
   │  sử dụng
   ▼
IQRCodeProvider (ZXingQRCodeProvider)      IStorageDataSource (MediaStoreDataSource)
   + decode(Bitmap bitmap): String            + loadImageAsBitmap(Uri uri): Bitmap
```

**Luồng xử lý (Sequence – Chọn ảnh & quét QR):**

```
1. User nhấn "Chọn ảnh từ bộ nhớ"
2. QRScannerActivity mở ActivityResultLauncher (GetContent, MIME type "image/*")
3. Hệ thống trả về Uri ảnh đã chọn → Activity gọi viewModel.setSelectedImage(uri)
4. ViewModel.postValue(selectedImageUri) → Activity hiển thị ảnh preview lên ImageView
5. Activity gọi viewModel.scanSelectedImage() (tự động hoặc khi nhấn nút "Quét")
6. ViewModel gọi repository.decodeQRFromUri(uri) trên background thread
7. Repository gọi MediaStoreDataSource.loadImageAsBitmap(uri) → Bitmap
8. Repository gọi ZXingQRCodeProvider.decode(bitmap) → chuỗi text (hoặc lỗi nếu không đọc được)
9. ViewModel.postValue(decodedText) hoặc postValue(errorMessage)
10. Activity observe → hiển thị text kết quả lên TextView (hoặc Toast lỗi "Không tìm thấy mã QR hợp lệ")
```

`activity_qr_scanner.xml` gồm:
- `Button` "Chọn ảnh từ bộ nhớ"
- `ImageView` preview ảnh đã chọn
- `Button` "Quét mã" (tuỳ chọn nếu không tự động quét)
- `TextView` hiển thị kết quả văn bản
- `Button` "Sao chép" (copy text vào Clipboard)
- `ProgressBar`

---

## 5. Thiết kế lớp Model (data/model)

**`QRGenerateRequest.java`**
```java
public class QRGenerateRequest {
    private String content;
    private int size; // kích thước ảnh QR (px), mặc định 512
    // constructor, getter/setter
}
```

**`QRScanResult.java`**
```java
public class QRScanResult {
    private String decodedText;
    private boolean success;
    private String errorMessage;
    // constructor, getter/setter
}
```

---

## 6. Thiết kế Repository & lớp bọc thư viện QR

**`IQRCodeProvider.java`** (interface – tách biệt để dễ thay thư viện, đúng tinh thần FR-1/FR-2)
```java
public interface IQRCodeProvider {
    Bitmap encode(String content, int width, int height) throws Exception;
    String decode(Bitmap bitmap) throws Exception;
}
```

**`ZXingQRCodeProvider.java`** – implement bằng thư viện ZXing (`com.google.zxing.MultiFormatWriter` để encode, `com.google.zxing.MultiFormatReader` để decode).

**`QRGeneratorRepository.java`**
```java
public class QRGeneratorRepository {
    private final IQRCodeProvider qrCodeProvider;
    private final IStorageDataSource storageDataSource;

    public Bitmap generateQRBitmap(String content, int size) throws Exception {
        return qrCodeProvider.encode(content, size, size);
    }

    public Uri saveBitmapToExternalStorage(Bitmap bitmap, String fileName) throws IOException {
        return storageDataSource.saveImage(bitmap, fileName);
    }
}
```

**`QRScannerRepository.java`**
```java
public class QRScannerRepository {
    private final IQRCodeProvider qrCodeProvider;
    private final IStorageDataSource storageDataSource;

    public String decodeQRFromUri(Uri uri) throws Exception {
        Bitmap bitmap = storageDataSource.loadImageAsBitmap(uri);
        return qrCodeProvider.decode(bitmap);
    }
}
```

---

## 7. Thiết kế lưu trữ (Storage Layer)

**`IStorageDataSource.java`**
```java
public interface IStorageDataSource {
    Uri saveImage(Bitmap bitmap, String fileName) throws IOException;
    Bitmap loadImageAsBitmap(Uri uri) throws IOException;
}
```

**`MediaStoreDataSource.java`** – implement dựa trên phiên bản Android:
- **Android ≥ 10 (API 29):** dùng `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` + `ContentValues` (`DISPLAY_NAME`, `MIME_TYPE`, `RELATIVE_PATH = Pictures/QRApp`) — không cần quyền `WRITE_EXTERNAL_STORAGE`.
- **Android ≤ 9 (API ≤ 28):** ghi trực tiếp vào `Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)` bằng `FileOutputStream`, yêu cầu quyền `WRITE_EXTERNAL_STORAGE`.
- Đọc ảnh (`loadImageAsBitmap`) dùng `ContentResolver.openInputStream(uri)` → `BitmapFactory.decodeStream()`, hoạt động thống nhất trên mọi phiên bản vì luôn nhận `Uri` từ Photo Picker/System Picker (đáp ứng Scoped Storage).

---

## 8. Thiết kế xử lý quyền (Permission Flow)

```
Activity.onCreate()
   │
   ▼
PermissionUtils.checkAndRequestPermission()
   │
   ├── Android 13+  → kiểm tra READ_MEDIA_IMAGES
   ├── Android 10-12 → không cần quyền đọc khi dùng Photo Picker; không cần quyền ghi khi dùng MediaStore
   └── Android ≤ 9   → kiểm tra READ_EXTERNAL_STORAGE + WRITE_EXTERNAL_STORAGE
   │
   ▼
Nếu chưa cấp quyền → ActivityCompat.requestPermissions()
   │
   ▼
onRequestPermissionsResult()
   ├── Granted → tiếp tục thao tác (chọn ảnh / lưu ảnh)
   └── Denied  → hiển thị Dialog giải thích + nút mở Settings
```

---

## 9. Xử lý lỗi (Error Handling)

| Tình huống lỗi | Nơi xử lý | Cách xử lý |
|---|---|---|
| Text nhập rỗng khi tạo QR | `QRGeneratorViewModel` | Validate trước khi gọi Repository, set `errorMessage` = "Vui lòng nhập nội dung" |
| Ảnh không chứa QR hợp lệ | `QRScannerRepository` → bắt exception từ `decode()` | ViewModel set `errorMessage` = "Không tìm thấy mã QR hợp lệ trong ảnh" |
| Không có quyền truy cập bộ nhớ/ảnh | `BaseActivity`/`PermissionUtils` | Hiển thị Dialog yêu cầu cấp quyền |
| Lỗi ghi file (hết dung lượng, I/O Exception) | `MediaStoreDataSource` → throw `IOException` | Repository/ViewModel bắt và set `errorMessage` tương ứng |

Tất cả lỗi được đưa về UI thông qua `LiveData<String> errorMessage`, Activity `observe` và hiển thị bằng `Toast` hoặc `Snackbar`.

---

## 10. Dependency (build.gradle – app level, minh hoạ)

```gradle
dependencies {
    implementation 'androidx.appcompat:appcompat:1.7.0'
    implementation 'com.google.android.material:material:1.12.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

    // MVVM - Jetpack
    implementation 'androidx.lifecycle:lifecycle-viewmodel:2.8.0'
    implementation 'androidx.lifecycle:lifecycle-livedata:2.8.0'
    implementation 'androidx.activity:activity:1.9.0'

    // Thư viện QR (FR-1 / FR-2)
    implementation 'com.google.zxing:core:3.5.3'
    implementation 'com.journeyapps:zxing-android-embedded:4.3.0'
}

android {
    buildFeatures {
        viewBinding true
    }
}
```

---

## 11. Quy ước đặt tên (Coding Convention)

| Đối tượng | Quy ước | Ví dụ |
|---|---|---|
| Activity | PascalCase + hậu tố `Activity` | `QRGeneratorActivity` |
| ViewModel | PascalCase + hậu tố `ViewModel` | `QRGeneratorViewModel` |
| Repository | PascalCase + hậu tố `Repository` | `QRScannerRepository` |
| Interface | Tiền tố `I` | `IQRCodeProvider` |
| Layout file | `snake_case`, tiền tố loại màn hình | `activity_qr_generator.xml` |
| ID trong XML | `snake_case`, tiền tố loại view | `btn_generate`, `iv_qr_preview`, `tv_result` |
| Hằng số | `UPPER_SNAKE_CASE` | `REQUEST_CODE_PICK_IMAGE` |

---

## 12. Tóm tắt ánh xạ Yêu cầu chức năng ↔ Thiết kế

| Yêu cầu (SRS) | Thành phần thiết kế tương ứng |
|---|---|
| FR-1: Chọn thư viện Scan QR | Interface `IQRCodeProvider` – cho phép chọn/thay đổi implement (ZXing) mà không ảnh hưởng tầng trên |
| FR-2: Import thư viện | Khai báo dependency trong `build.gradle` (mục 10), implement `ZXingQRCodeProvider` |
| FR-3: Tạo QR & lưu bộ nhớ ngoài | Module `generator`: `QRGeneratorActivity` → `QRGeneratorViewModel` → `QRGeneratorRepository` → `ZXingQRCodeProvider` + `MediaStoreDataSource` |
| FR-4: Chọn ảnh từ bộ nhớ ngoài | Module `scanner`: `QRScannerActivity` dùng `ActivityResultContracts.GetContent` → `QRScannerViewModel.setSelectedImage()` |
| FR-5: Quét ảnh QR ra text | Module `scanner`: `QRScannerViewModel.scanSelectedImage()` → `QRScannerRepository.decodeQRFromUri()` → `ZXingQRCodeProvider.decode()` |
