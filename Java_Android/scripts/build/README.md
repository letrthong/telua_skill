# build_apk.sh — Build & Deploy APK hệ thống cho AOSP

Script tự động hóa vòng lặp phát triển app hệ thống (`priv-app`) trên AOSP / Android Automotive (cây Qualcomm QSSI):

**lunch → build module → root & remount → push APK → reboot / restart nhanh → lấy logcat**

| File | Vai trò |
|---|---|
| `build_apk.sh` | Script chính (logic) |
| `apk.config` | Cấu hình: chọn app, lunch target, deploy, log, test |

---

## 1. Yêu cầu

- Máy build Linux có cây AOSP, cấu trúc thư mục dạng `.../android/qssi/` (có `build/envsetup.sh`).
- `adb` (platform-tools) có trong `PATH`.
- Thiết bị chạy bản **`userdebug`** hoặc **`eng`**. Bản `user` không cho `adb root` / `adb remount`, script sẽ dừng.
- Cắm **một** thiết bị adb.
- Script có quyền chạy và dùng xuống dòng kiểu Linux (LF):
  ```bash
  chmod +x build_apk.sh
  dos2unix build_apk.sh apk.config   # chỉ cần nếu copy file từ Windows không qua git
  ```

---

## 2. Bắt đầu nhanh (một workspace)

Đặt `build_apk.sh` + `apk.config` ở **bất kỳ đâu bên trong** thư mục `android/` của cây AOSP, rồi:

```bash
vi apk.config          # chọn ACTIVE_PROJECT, sửa CONFIG_TARGET_PRODUCT / CONFIG_TARGET_BUILD_VARIANT
./build_apk.sh --info  # kiểm tra cấu hình + trạng thái thiết bị
./build_apk.sh         # build + deploy + reboot
```

> Script tìm cây AOSP bằng cách đi ngược từ thư mục chứa nó lên đến thư mục tên `android`,
> rồi dùng `ANDROID_TOP=<...>/android/qssi`.

---

## 3. Nhiều workspace với `--softlink`

Dùng **một bản script gốc** cho nhiều cây AOSP (nhiều workspace / nhánh). Mỗi workspace có config riêng.

```
d:/code/telua_skill/.../build/          ← bản gốc (sửa ở đây)
├── build_apk.sh
└── apk.config                          ← mẫu

~/ws_main/android/qssi/tools/
├── build_apk.sh  -> (symlink về bản gốc)
├── apk.config        (ACTIVE_PROJECT="oem_service", lunch target A)
├── OemService.apk    (APK build ra nằm ở đây)
└── deployment_log.txt

~/ws_release/android/qssi/tools/
├── build_apk.sh  -> (symlink về bản gốc)
├── apk.config        (ACTIVE_PROJECT="car_audio", lunch target B)
└── ...
```

### Cài vào một workspace

```bash
# Chạy từ bản gốc; thư mục đích phải tồn tại và nằm trong cây android/
mkdir -p ~/ws_main/android/qssi/tools
./build_apk.sh --softlink ~/ws_main/android/qssi/tools

mkdir -p ~/ws_release/android/qssi/tools
./build_apk.sh --softlink ~/ws_release/android/qssi/tools
```

`--softlink <path>` sẽ:

1. Tạo `<path>/build_apk.sh` là **symlink** về script gốc.
2. **Copy** `apk.config` (file cạnh script đang chạy) sang `<path>/apk.config`.
3. **Ghi đè** nếu hai file đã tồn tại.

### Dùng trong từng workspace

```bash
cd ~/ws_main/android/qssi/tools
vi apk.config            # ACTIVE_PROJECT="oem_service", lunch target A
./build_apk.sh --info
./build_apk.sh

cd ~/ws_release/android/qssi/tools
vi apk.config            # ACTIVE_PROJECT="car_audio", lunch target B
./build_apk.sh
```

### Vì sao cách này hoạt động

- **Script dùng chung**: sửa bản gốc một lần → mọi workspace dùng bản mới ngay.
- **Config riêng**: mỗi workspace có `ACTIVE_PROJECT`, lunch target, debug tag… riêng.
- **Đúng cây AOSP**: script tìm `apk.config` và thư mục `android/` theo **vị trí của link**, không phải file gốc. Chạy link trong `ws_main` → build `ws_main`.
- **Output tách riêng**: APK và `deployment_log.txt` nằm trong thư mục của link.

### Lưu ý

- Chạy lại `--softlink` vào cùng thư mục sẽ **ghi đè `apk.config`** của workspace đó. Backup trước nếu đã sửa.
- Biến mới thêm vào `apk.config` gốc **không tự có** trong các bản copy cũ. Script vẫn chạy nhờ giá trị mặc định; muốn đổi thì thêm tay vào bản copy.
- Đặt link **ngoài** thư mục `android/` thì script cảnh báo và build sẽ không tìm thấy cây AOSP.
- Không deploy từ hai workspace cùng lúc lên cùng một thiết bị.

---

## 4. Tùy chọn dòng lệnh

| Tùy chọn | Ý nghĩa |
|---|---|
| *(không có)* | Build + deploy (nếu `ENABLE_DEPLOY="true"`) + reboot đầy đủ |
| `--info` | In cấu hình, module, đường dẫn, trạng thái thiết bị rồi thoát |
| `--start-deploy true\|false` | Ghi đè `ENABLE_DEPLOY` trong config |
| `--no-reboot` | Restart nhanh framework (`stop && start`, ~10–20s) thay vì reboot cả máy |
| `--softlink <path>` | Symlink script + copy config vào `<path>` (xem mục 3) |
| `--test-mode true\|false` | Chạy test script thay vì build |
| `-h`, `--help` | Trợ giúp |

Ví dụ:

```bash
./build_apk.sh --start-deploy false   # chỉ build, không deploy
./build_apk.sh --no-reboot            # deploy nhanh khi chỉ sửa code Java
./build_apk.sh --test-mode true       # chạy test
```

> **Khi nào không dùng `--no-reboot`:** khi thay đổi `AndroidManifest.xml`, quyền (permission),
> SELinux policy, hoặc thêm app mới. Lúc đó nên reboot đầy đủ.

---

## 5. Cấu hình `apk.config`

### 5.1 Chọn app (`ACTIVE_PROJECT`)

```bash
ACTIVE_PROJECT="oem_service"   # "oem_service" | "car_audio" | "vehicle_service"
```

Mỗi profile trong khối `case` định nghĩa:

| Biến | Ý nghĩa |
|---|---|
| `MODULE_NAME` | Tên module để build (`m <MODULE_NAME>`). Lấy từ `name:` trong `Android.bp` hoặc `LOCAL_PACKAGE_NAME` trong `Android.mk`. Bỏ trống → dùng tên APK bỏ `.apk` |
| `SOURCE_CODE_RELATIVE_PATH` | Thư mục source, tính từ `ANDROID_TOP` |
| `APK_OUTPUT_RELATIVE_PATH` | Thư mục chứa APK sau build, tính từ `ANDROID_TOP` (**bị xóa trước mỗi lần build**) |
| `APK_FILE_NAME` | Tên file APK |
| `APK_DEPLOY_PATH` | Thư mục trên thiết bị, ví dụ `/system/priv-app/OemService` |
| `DEFAULT_LOGCAT_FILTER` | Regex lọc logcat mặc định của profile |
| `DEFAULT_DEBUG_LOG_TAGS` | Danh sách tag bật DEBUG mặc định của profile |

Thêm app mới: copy một khối `"..." ) ... ;;`, đổi tên và các giá trị, rồi đặt `ACTIVE_PROJECT` theo tên mới.

### 5.2 Môi trường build

| Biến | Ý nghĩa |
|---|---|
| `CONFIG_TARGET_PRODUCT` | Product cho `lunch`, ví dụ `abc_xyz_in` |
| `CONFIG_TARGET_BUILD_VARIANT` | `userdebug` hoặc `eng` |
| `BUILD_JOBS` | Số luồng build. Để `""` → tự dùng `nproc` (mặc định 8 nếu không có `nproc`) |

> Nếu shell đã `lunch` sẵn (`TARGET_PRODUCT` đã có), script **dùng luôn môi trường đó** và bỏ qua
> giá trị trong config.

### 5.3 Deploy & log

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `ENABLE_DEPLOY` | `"true"` | Có push APK lên thiết bị không |
| `ENABLE_DEBUG_LOG` | `"true"` | Có bật mức log cho các tag không |
| `DEBUG_LOG_LEVEL` | `"DEBUG"` | `DEBUG` hoặc `VERBOSE` |
| `DEBUG_LOG_TAGS` | theo profile | Các tag cách nhau bởi dấu cách |
| `ENABLE_LOGCAT` | `"false"` | Có lấy logcat sau deploy không |
| `LOGCAT_FILTER` | theo profile | Regex `grep -E`. Để `""` → lấy toàn bộ log |
| `LOGCAT_SETTLE_SECONDS` | `"10"` | Số giây chờ app khởi động trước khi lấy log |

Debug log được bật bằng `setprop persist.log.tag.<TAG> <LEVEL>` (giữ qua reboot), tương ứng với
`Log.isLoggable(TAG, Log.DEBUG)` trong code Java.

### 5.4 Test

| Biến | Ý nghĩa |
|---|---|
| `TEST_RELATIVE_PATH` | Thư mục test, tính từ thư mục chứa script (hoặc link) |
| `TEST_BUILD_SCRIPT` | Script test trong thư mục đó |

---

## 6. Script làm gì khi chạy

1. Đọc `apk.config`, kiểm tra các biến bắt buộc.
2. Tìm `ANDROID_TOP`, `source build/envsetup.sh` + `lunch` (nếu chưa có môi trường).
3. *(`--test-mode true`)* chạy test rồi thoát.
4. Xóa thư mục output cũ, chạy `m <MODULE_NAME> -j<N>`, copy APK ra cạnh script.
5. Deploy (nếu bật):
   1. Chờ thiết bị; dừng nếu là bản `user`.
   2. `adb root` và xác nhận `uid=0`.
   3. `adb remount` và **ghi thử** vào thư mục đích. Lần remount đầu (overlayfs) tự reboot rồi remount lại.
   4. Push APK, `chmod 644`, xóa cache `oat/` cũ, `sync`.
   5. Bật debug log tag; xóa buffer logcat (nếu `ENABLE_LOGCAT`).
   6. Reboot và chờ `sys.boot_completed=1` — hoặc với `--no-reboot`: `stop && start` và chờ `system_server` mới + PackageManager sẵn sàng.
6. Chờ `LOGCAT_SETTLE_SECONDS`, lưu log vào `deployment_log.txt`.

---

## 7. Xử lý sự cố

| Hiện tượng | Nguyên nhân / cách xử lý |
|---|---|
| `Could not find the 'android' root directory` | Script (hoặc link) không nằm trong cây `android/`. Đặt lại vị trí, kiểm tra bằng `--info` |
| `Cannot find 'build/envsetup.sh'` | Cây không có dạng `android/qssi/`. `ANDROID_TOP` đang cố định là `android/qssi` |
| `lunch` lỗi | Sai `CONFIG_TARGET_PRODUCT` / `CONFIG_TARGET_BUILD_VARIANT` |
| `APK not found ... after build` | `MODULE_NAME` hoặc `APK_OUTPUT_RELATIVE_PATH` sai. Xem `name:` trong `Android.bp` |
| `Device is running a 'user' build` | Flash bản `userdebug` / `eng` |
| `adbd is not running as root` | Bản build không cho root, hoặc adb bị kẹt: `adb kill-server` rồi thử lại |
| `... is still read-only after 'adb remount'` | Thường do verity: `adb disable-verity && adb reboot`, rồi chạy lại |
| `error: more than one device/emulator` | Rút bớt thiết bị (script chỉ hỗ trợ một thiết bị) |
| `Fast restart failed` | Chạy lại không có `--no-reboot` |
| `deployment_log.txt` rỗng | Tăng `LOGCAT_SETTLE_SECONDS`, kiểm tra `LOGCAT_FILTER` và `DEBUG_LOG_TAGS` |
| Code mới không có hiệu lực | Thay đổi cần reboot đầy đủ (manifest, quyền, SELinux) — bỏ `--no-reboot` |
| `bad interpreter: /bin/bash^M` | File có xuống dòng Windows: `dos2unix build_apk.sh apk.config` |
