# FitTrack Cloud Save – Hướng dẫn làm API sao lưu (kiểu Soul Knight)

API **C# / ASP.NET Core (.NET 10) + EF Core (LINQ) + SQL Server**, chạy trên Ubuntu bằng **pm2**,
cho app Android **FitTrack** (Java + Room) vẫn dùng offline 100% và **tuỳ chọn** sao lưu lên cloud.

---

## 0. Đọc trước – 3 điều quan trọng

1. **Báo cáo và mã nguồn bạn gửi là hai dự án khác nhau.** File Word là *Lingua* (app học ngôn ngữ, Node.js + MySQL),
   còn file RAR là *FitTrack* (app dinh dưỡng/tập luyện, Room + Firebase Auth). Tôi lấy **FitTrack** làm nguồn dữ liệu
   (đúng với yêu cầu "dùng offline + tuỳ chọn lưu cloud"); báo cáo chỉ được dùng để tham khảo quy ước (REST, JWT/Bearer, `BASE_URL`).
   Nếu bạn thật ra muốn sao lưu dữ liệu của Lingua thì kiến trúc ở dưới vẫn dùng được, chỉ cần đổi bộ bảng.
2. **Chọn .NET 10, không phải .NET 8.** LINQ là của C#. .NET 8 và .NET 9 đều **hết hỗ trợ ngày 10/11/2026**; .NET 10 là bản LTS (hỗ trợ tới 11/2028).
3. **Phạm vi đã kiểm chứng** (chi tiết ở mục 12): code C# biên dịch sạch, tầng HTTP và pm2 đã chạy thật, nhưng
   **chưa chạy với SQL Server thật và Firebase thật** (môi trường soạn thảo không có). Hãy chạy `deploy/smoke-test.sh` ở mục 9 để xác nhận trên hệ thống của bạn.

---

## 1. Thiết kế

### Mô hình "Cloud Save" kiểu Soul Knight

```
 ┌──────────────── Điện thoại ────────────────┐            ┌──────── Server Ubuntu ────────┐
 │  Room (SQLite)  ← BẢN CHÍNH, chạy offline   │            │  nginx (HTTPS)                │
 │        │                                    │  Bearer    │     │                         │
 │  [Save to cloud] ──┐                        │  Firebase  │  pm2 → ASP.NET Core API       │
 │  [Restore]       ──┼─ Retrofit ─────────────┼──ID token─▶│     │  EF Core / LINQ         │
 │  (auto, tuỳ chọn)──┘                        │            │  SQL Server (Backups + 5 bảng)│
 └────────────────────────────────────────────┘            └───────────────────────────────┘
```

| Quyết định | Lý do |
|---|---|
| **Snapshot toàn bộ**, không đồng bộ 2 chiều từng dòng | Đúng tinh thần Soul Knight, và tránh đúng vấn đề README nhóm bạn đã né: conflict. Room **không cần đổi schema** (không thêm uuid/updatedAt). |
| Cloud lệch với máy → **hỏi người dùng** (HTTP 409) | App không bao giờ tự ghi đè âm thầm. Hộp thoại so sánh số bản ghi/ngày cuối/tên máy rồi để người dùng chọn. |
| Giữ **N bản mới nhất** (mặc định 5) | Lỡ bấm nhầm "ghi đè" vẫn còn bản cũ để quay lại. Bản thừa tự xóa. |
| Đăng nhập bằng **Firebase ID token** | App đã có Firebase Auth. Server chỉ kiểm tra chữ ký token, **không lưu mật khẩu**, không cần service-account. |
| **Hash nội dung** (SHA-256, bỏ `id` cục bộ) | Dữ liệu y hệt bản cloud → trả `unchanged`, không sinh bản trùng. Bỏ `id` vì khôi phục sang máy khác Room sẽ đánh lại id. |
| **ROWVERSION** trên bảng Users | 2 máy upload cùng lúc → 1 máy chắc chắn nhận 409, không mất dữ liệu âm thầm. |
| Dữ liệu lưu ở bảng thường (không phải 1 cục JSON) | Đúng yêu cầu LINQ/SQL, truy vấn/thống kê được. |
| Không lưu `avatarUri`, `firebaseUid` | Avatar là đường dẫn file cục bộ (vô nghĩa khi đổi máy); UID server đã biết qua token. |

### Luồng hoạt động

| Tình huống | App | Server |
|---|---|---|
| Dùng hằng ngày | Chỉ đọc/ghi Room, **không gọi mạng** | – |
| Bấm **Save to cloud**, cloud trống hoặc máy này đã đồng bộ bản mới nhất | `POST /cloud-save` (`baseBackupId` = bản đã đồng bộ) | `201 created` |
| Bấm Save nhưng dữ liệu không đổi | như trên | `200 unchanged` |
| Máy mới/cài lại, hoặc máy khác đã upload | như trên | **`409`** + thông tin bản cloud → hộp thoại **Keep this device / Use cloud / Cancel** |
| Chọn *Keep this device* | `POST` với `force=true` | `201`, bản cũ vẫn được giữ vài phiên bản |
| Chọn *Use cloud* hoặc bấm **Restore** | `GET /cloud-save/latest/data` → thay Room trong **1 transaction** | `200` (404 nếu chưa có gì) |
| Bật **Auto backup** (Wi-Fi, 1 lần/ngày) | WorkManager gọi như Save, **không bao giờ force** | Gặp 409 thì chỉ đánh dấu chờ người dùng |

---

## 2. Cấu trúc thư mục

```
FitTrackCloud/
├── HUONG_DAN.md                       ← file này
├── database/
│   ├── 01_FitTrackCloud_SqlServer.sql ← tạo DB + 7 bảng + index + view
│   └── 02_CreateApiLogin.sql          ← tạo login SQL quyền tối thiểu cho API
├── src/FitTrack.Api/                  ← ASP.NET Core Web API
│   ├── Program.cs                     ← cấu hình: JWT Firebase, rate limit, EF Core, pipeline
│   ├── Data/                          ← Entities.cs + FitTrackDbContext.cs
│   ├── Dtos/CloudSaveDtos.cs          ← hợp đồng JSON + validate
│   ├── Services/                      ← CloudSaveService.cs (LINQ) + SnapshotHasher.cs
│   └── Controllers/                   ← CloudSave / Me / Health
├── deploy/
│   ├── ecosystem.config.js            ← pm2
│   ├── api.env.example                ← bí mật (chuỗi kết nối) ngoài thư mục code
│   ├── nginx-fittrack-api.conf
│   ├── deploy.sh                      ← publish + đổi phiên bản + restart + health check + gợi ý rollback
│   └── smoke-test.sh                  ← kiểm thử tự động bằng curl
└── android-client/                    ← code Java cho app (copy vào dự án FitTrack)
    ├── DAO_additions.md
    └── java/com/fittrack/app/data/cloud/*.java
```

---

## 3. Bước 1 – Tạo cơ sở dữ liệu SQL Server

Cần SQL Server 2019+ (Windows hoặc Linux). Chưa có thì cách nhanh nhất là Docker:

```bash
docker run -e ACCEPT_EULA=Y -e 'MSSQL_SA_PASSWORD=<MatKhauSaManh#2026>' -p 127.0.0.1:1433:1433 \
  -v mssql_data:/var/opt/mssql -d --name mssql mcr.microsoft.com/mssql/server:2022-latest
```

> ⚠️ **Không mở cổng 1433 ra Internet.** Ở trên đã bind `127.0.0.1`. Muốn dùng SSMS/Azure Data Studio từ máy khác, dùng SSH tunnel.

Chạy 2 file SQL (SSMS / Azure Data Studio: mở file → Execute; hoặc `sqlcmd` – trên Linux là gói `mssql-tools18`, thêm `-C` nếu dùng chứng chỉ tự ký):

```bash
sqlcmd -S localhost -U sa -P '<MatKhauSa>' -C -i database/01_FitTrackCloud_SqlServer.sql
sqlcmd -S localhost -U sa -P '<MatKhauSa>' -C -i database/02_CreateApiLogin.sql -v API_PASSWORD='MatKhau#API2026'
```

Kết quả: database `FitTrackCloud` với:

| Bảng | Tương ứng Room | Ghi chú |
|---|---|---|
| `Users` | – | 1 dòng / tài khoản Firebase (`FirebaseUid` UNIQUE), có `RowVersion` |
| `Backups` | – | 1 dòng / snapshot: thiết bị, `SchemaVersion`, `ContentHash`, số lượng, ngày đầu/cuối |
| `BackupProfiles` | `user_profile` | tối đa 1 dòng / snapshot |
| `BackupFoodLogs` | `food_log` | `LocalId` = `id` Room, giữ thứ tự |
| `BackupWorkoutLogs` | `workout_log` | |
| `BackupWeightEntries` | `weight_entry` | |
| `BackupWaterLogs` | `water_log` | `HourOfDay` = cột `hour` |
| view `vw_UserCloudUsage` | – | thống kê dung lượng theo người dùng |

Mọi khóa ngoại đều `ON DELETE CASCADE`: xóa 1 `Backups` là xóa sạch dữ liệu con; xóa `Users` là xóa sạch tất cả. File SQL chạy lại nhiều lần được (idempotent).
File 02 tạo login `fittrack_api` chỉ có `db_datareader` + `db_datawriter` – **API không dùng `sa`**.

---

## 4. Bước 2 – Chạy API trên máy dev

Cài .NET 10 SDK, rồi:

```bash
cd src/FitTrack.Api

# Linux/macOS
export ConnectionStrings__Default="Server=localhost,1433;Database=FitTrackCloud;User Id=fittrack_api;Password=MatKhau#API2026;TrustServerCertificate=True;Encrypt=True"
export Firebase__ProjectId="fittrack-bf1cd"     # = "project_id" trong app/google-services.json
export ASPNETCORE_URLS="http://0.0.0.0:5080"    # để Android Emulator gọi được qua 10.0.2.2:5080
dotnet run
```

(PowerShell: `$env:ConnectionStrings__Default = "..."`.) Gọi thử: `curl http://localhost:5080/api/v1/health` → `{"status":"ok","database":true,...}`.

Quy ước cấu hình: biến môi trường dùng `__` thay cho `:` của `appsettings.json`, và **ghi đè** appsettings. Bí mật không bao giờ commit vào git.

---

## 5. API

Mọi endpoint (trừ `health`) cần header `Authorization: Bearer <Firebase ID token>`; thiếu/sai → `401`. Tiền tố `/api/v1` để sau này đổi hợp đồng mà không làm hỏng các bản app cũ.

| Method & đường dẫn | Mục đích | Trả về |
|---|---|---|
| `GET /health` | server + DB còn sống (không cần token) | 200 / 503 |
| `GET /me` | thông tin tài khoản cloud + bản mới nhất (tự tạo user lần đầu) | 200 |
| `DELETE /me` | xóa vĩnh viễn **toàn bộ** dữ liệu cloud của tôi | 204 |
| `GET /cloud-save` | danh sách bản sao lưu (tóm tắt, mới nhất trước) | 200 |
| `GET /cloud-save/latest` | tóm tắt bản mới nhất (cho hộp thoại so sánh) | 200 / 404 |
| `GET /cloud-save/{id}` | tóm tắt 1 bản | 200 / 404 |
| `GET /cloud-save/latest/data` · `/{id}/data` | tải toàn bộ dữ liệu để khôi phục | 200 / 404 |
| `POST /cloud-save` | tải lên snapshot | **201** created · **200** unchanged · **409** conflict · 400 sai dữ liệu · 429 quá nhanh |
| `DELETE /cloud-save/{id}` | xóa 1 bản của chính mình | 204 / 404 |

Thân `POST /cloud-save` (camelCase, ngày `yyyy-MM-dd`):

```json
{
  "deviceId": "uuid-của-máy", "deviceName": "Google Pixel 8", "appVersion": "1.0",
  "schemaVersion": 12, "label": null, "isAuto": false,
  "baseBackupId": null, "force": false,
  "data": {
    "profile": { "name": "An", "age": 25, "gender": "Male", "heightCm": 170.5, "weightKg": 65.2,
                 "activityLevel": "Sedentary", "goal": "Maintain", "bmr": 1600.5, "tdee": 1920.6,
                 "calorieGoal": 1920.6, "proteinGoalG": 144, "carbGoalG": 192, "fatGoalG": 64 },
    "foodLogs":      [{ "localId": 1, "name": "Beef Pho", "calories": 450, "proteinG": 25, "carbG": 60, "fatG": 15, "mealType": "Lunch", "date": "2026-11-10" }],
    "workoutLogs":   [{ "localId": 1, "exerciseName": "Squats", "sets": 3, "reps": 20, "weightKg": 20, "caloriesBurned": 180.5, "date": "2026-11-10" }],
    "weightEntries": [{ "localId": 1, "weightKg": 65.2, "date": "2026-11-10" }],
    "waterLogs":     [{ "localId": 1, "amountMl": 250, "hour": 8, "date": "2026-11-10" }]
  }
}
```

Phản hồi `409`:

```json
{ "code": "CLOUD_HAS_DIFFERENT_DATA", "message": "...", "latest": { "id": 17, "deviceName": "Samsung S23", "createdAtUtc": "...", "foodLogCount": 120, "lastLogDate": "2026-11-10", "...": "..." } }
```

**Quy tắc validate** (cố ý lỏng – đây là dịch vụ *sao lưu*, không được từ chối dữ liệu app tạo ra hợp lệ):
số không âm, `hour` 0–23, chuỗi không quá dài, tối đa 50.000 dòng/bảng/snapshot, thân tối đa 20 MB. **Không** ràng buộc danh sách `mealType`/`goal`… để sau này app thêm giá trị mới thì bản sao lưu không bị từ chối. Chuỗi rỗng được chấp nhận; thiếu `data` hoặc thiếu `date` thì bị `400` (tránh tạo snapshot rỗng đẩy bản tốt ra khỏi giới hạn N bản). Trường lạ trong JSON được bỏ qua (tương thích tiến).

---

## 6. Đọc hiểu code (các điểm LINQ chính)

`Services/CloudSaveService.cs`:

- **Phép chiếu dùng chung** `ToInfo` (`Expression<Func<Backup, BackupInfoDto>>`): EF chỉ `SELECT` các cột của `Backups`, không kéo bảng con → danh sách rất nhẹ.
- **Bản mới nhất:** `.Where(b => b.UserId == id).OrderByDescending(b => b.CreatedAtUtc).ThenByDescending(b => b.Id).Select(...).FirstOrDefaultAsync()`.
- **Giữ N bản:** `.Skip(max).Select(b => b.Id).ToListAsync()` rồi `Where(b => staleIds.Contains(b.Id)).ExecuteDeleteAsync()` (xóa hàng loạt 1 câu lệnh; dòng con tự xóa nhờ cascade SQL).
- **Tải dữ liệu:** mỗi bảng 1 truy vấn riêng `.OrderBy(x => x.LocalId)` + `Select` sang DTO – cố ý **không dùng `Include` lồng nhau** để tránh "cartesian explosion".
- **LINQ-to-Objects** trong `BuildBackup`: gom ngày của 4 bảng (`Concat`, `Min`, `Max`) và cân nặng mới nhất (`OrderByDescending…FirstOrDefault`).
- **Chống ghi đè đồng thời:** `user.LastBackupAtUtc = now` làm EF sinh `UPDATE Users … WHERE Id=@id AND RowVersion=@rv`; trùng thời điểm → `DbUpdateConcurrencyException` → 409. Toàn bộ `SaveChanges` là 1 transaction nên không bao giờ lưu dở.
- **Cô lập người dùng:** mọi truy vấn đều có `UserId == id` của token; người này không thể đọc/xóa dữ liệu người kia.

`Program.cs`: xác thực JWT Firebase (`Authority = https://securetoken.google.com/<projectId>`, kiểm tra issuer/audience/hạn, khóa công khai Google tự tải và tự xoay), **chính sách mặc định "phải đăng nhập"** (endpoint công khai phải ghi `[AllowAnonymous]`), giới hạn 60 request/phút/người dùng, đọc IP thật qua `X-Forwarded-For`, lỗi ngoài ý muốn → 500 `ProblemDetails` không lộ stack trace.

> Server cần ra Internet (HTTPS tới `securetoken.google.com`) ở request đầu tiên để lấy khóa kiểm tra chữ ký.

---

## 7. Bước 3 – Triển khai Ubuntu + pm2 + nginx + HTTPS

**7.1 Cài đặt** (Ubuntu 24.04 có .NET 10 sẵn trong kho; Ubuntu 22.04 thêm `sudo add-apt-repository ppa:dotnet/backports` trước – xem [hướng dẫn Microsoft](https://learn.microsoft.com/dotnet/core/install/linux-ubuntu)):

```bash
sudo apt update
sudo apt install -y dotnet-sdk-10.0 nginx          # SDK vì build ngay trên server; chỉ chạy thì cần aspnetcore-runtime-10.0
sudo apt install -y nodejs npm && sudo npm install -g pm2
```

**7.2 Thư mục + bí mật:**

```bash
sudo mkdir -p /opt/fittrack-api/releases /var/log/fittrack-api /etc/fittrack-api
sudo chown -R $USER: /opt/fittrack-api /var/log/fittrack-api
sudo cp deploy/api.env.example /etc/fittrack-api/api.env
sudo chown $USER: /etc/fittrack-api/api.env && sudo chmod 600 /etc/fittrack-api/api.env
nano /etc/fittrack-api/api.env        # sửa mật khẩu SQL, Firebase__ProjectId
```

**7.3 Build + chạy bằng pm2** (upload thư mục dự án lên server bằng `git clone`/`scp`/`rsync`):

```bash
./deploy/deploy.sh
```

Script: `dotnet publish` vào `/opt/fittrack-api/releases/<thời-gian>`, trỏ symlink `current`, `pm2 restart`, **chờ `/health` OK tối đa 30 giây**; nếu không lên thì in sẵn lệnh rollback và giữ 5 bản gần nhất. Lần đầu xong, bật tự khởi động cùng máy:

```bash
pm2 startup systemd      # copy-paste đúng dòng sudo mà nó in ra
pm2 save
pm2 install pm2-logrotate    # tránh đầy ổ đĩa vì log
```

Điểm cần biết về `ecosystem.config.js`: `script: 'dotnet'` + `interpreter: 'none'` (nếu thiếu, pm2 sẽ cố chạy bằng Node); `exec_mode: 'fork'` (không dùng cluster – .NET tự đa luồng); chỉ nghe `127.0.0.1:5080`, chỉ nginx mới ra ngoài; sửa `api.env` xong phải `pm2 restart fittrack-api --update-env`.
Lệnh hay dùng: `pm2 status`, `pm2 logs fittrack-api`, `pm2 restart fittrack-api`.

**7.4 nginx + HTTPS:**

```bash
sudo cp deploy/nginx-fittrack-api.conf /etc/nginx/sites-available/fittrack-api   # sửa server_name
sudo ln -s /etc/nginx/sites-available/fittrack-api /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
sudo apt install -y certbot python3-certbot-nginx && sudo certbot --nginx -d api.ten-mien-cua-ban.com
```

Chú ý `client_max_body_size 20m;` – mặc định nginx chỉ cho 1 MB, snapshot lớn hơn sẽ bị `413`. Mở firewall cổng 80/443 (`sudo ufw allow 'Nginx Full'`), **không** mở 5080 và 1433.

---

## 8. Bước 4 – Tích hợp vào app Android (Java)

App hiện **chưa có** Retrofit/OkHttp/WorkManager (đã có `INTERNET` permission, Firebase Auth, minSdk 26, viewBinding).

**8.1 `app/build.gradle`** – thêm dependencies (hoặc bản mới hơn):

```gradle
implementation 'com.squareup.retrofit2:retrofit:2.11.0'
implementation 'com.squareup.retrofit2:converter-gson:2.11.0'
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
implementation 'androidx.work:work-runtime:2.9.1'
```

**8.2 Bổ sung DAO** – làm theo `android-client/DAO_additions.md` (thêm `getAllOnce()`, `deleteAll()`, `insertAll()` cho 4 DAO; **không** đổi Entity, **không** tăng version Room, không cần migration).

**8.3 Copy** thư mục `android-client/java/com/fittrack/app/data/cloud/` vào `app/src/main/java/com/fittrack/app/data/cloud/` rồi sửa `BASE_URL` trong `ApiClient.java`:

| File | Vai trò |
|---|---|
| `CloudModels` | POJO khớp JSON của API |
| `CloudSaveApi` | interface Retrofit |
| `ApiClient` | OkHttp + interceptor gắn Firebase ID token (tự làm mới, 401 thì thử lại 1 lần) |
| `CloudPrefs` | deviceId, `baseBackupId` theo tài khoản, bật/tắt auto, cờ xung đột |
| `SnapshotMapper` | Room ⇄ JSON; khôi phục trong **1 transaction** (lỗi giữa chừng tự rollback, dữ liệu cũ còn nguyên) |
| `CloudSaveRepository` | logic backup / restore / conflict (hàm chặn, gọi trên luồng nền) |
| `CloudSaveUi` | các hộp thoại Save / Restore / Conflict |
| `CloudBackupWorker` | tự sao lưu mỗi ngày, chỉ Wi-Fi, pin không yếu |

**8.4 Thêm UI vào `fragment_profile.xml`** (cạnh nút Logout; `?attr/materialButtonOutlinedStyle` chạy được với cả theme MaterialComponents lẫn Material3):

```xml
<LinearLayout
    android:layout_width="match_parent" android:layout_height="wrap_content"
    android:orientation="vertical" android:layout_marginTop="16dp">

    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:text="Cloud Save" android:textStyle="bold" android:textSize="16sp" />
    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:text="Your data always stays on this device. Cloud backup is optional."
        android:layout_marginBottom="8dp" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnCloudBackup" style="?attr/materialButtonOutlinedStyle"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:text="Save to cloud" />
    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnCloudRestore" style="?attr/materialButtonOutlinedStyle"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:text="Restore from cloud" />
    <com.google.android.material.switchmaterial.SwitchMaterial
        android:id="@+id/switchAutoBackup"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:text="Auto backup daily (Wi-Fi only)" />
</LinearLayout>
```

**8.5 Nối vào `ProfileFragment`** (thứ tự `setChecked` trước `setOnCheckedChangeListener` là có chủ đích, tránh kích hoạt listener khi khởi tạo):

```java
binding.btnCloudBackup.setOnClickListener(v -> CloudSaveUi.backup(requireActivity()));
binding.btnCloudRestore.setOnClickListener(v -> CloudSaveUi.restore(requireActivity()));
binding.switchAutoBackup.setChecked(new CloudPrefs(requireContext()).autoBackupEnabled());
binding.switchAutoBackup.setOnCheckedChangeListener(
        (b, on) -> CloudSaveUi.setAutoBackup(requireContext(), on));
```

Gợi ý: trong `onResume()`, nếu `new CloudPrefs(ctx).conflictPending()` là true thì hiện Snackbar "Cloud has a different save – tap *Save to cloud* to choose"; bấm Save sẽ ra lại hộp thoại chọn bản.

**8.6 HTTPS và Emulator.** Android 9+ chặn HTTP thường. Máy thật dùng `https://api.ten-mien.com/api/v1/`. Để thử với API chạy trên máy tính qua Emulator (`http://10.0.2.2:5080/api/v1/`), chỉ cho phép HTTP ở bản **debug**:

`app/src/debug/res/xml/network_security_config.xml`
```xml
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">10.0.2.2</domain>
    </domain-config>
</network-security-config>
```
`app/src/debug/AndroidManifest.xml`
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:networkSecurityConfig="@xml/network_security_config" />
</manifest>
```

### 3 điểm trong app hiện tại ảnh hưởng đến sao lưu (nên sửa)

1. **Logout không xóa Room.** Người A đăng xuất, người B đăng nhập cùng máy → Room còn dữ liệu của A. `CloudSaveRepository.backup()` đã có chốt chặn `WRONG_ACCOUNT` (so `UserProfile.firebaseUid` với UID đang đăng nhập) nên **không** đẩy dữ liệu A lên tài khoản B. Nếu muốn app dùng đa tài khoản đúng nghĩa, khi logout nên hỏi người dùng rồi gọi `AppDatabase.getInstance(ctx).clearAllTables()` trên luồng nền (nhắc họ lưu cloud trước).
2. **`DatabaseSeeder` tự tạo 31 ngày dữ liệu mẫu** (gọi ở `SplashActivity` khi `food_log` rỗng). Dữ liệu giả sẽ bị đưa vào bản sao lưu, và nếu khôi phục một snapshot không có food log thì lần mở app sau sẽ bị "seed" đè. Chỉ seed ở bản debug:
   ```java
   boolean debuggable = (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
   if (debuggable) DatabaseSeeder.seedIfNeeded(getApplicationContext());
   ```
3. **`DateUtils` dùng `Locale.getDefault()`** để tạo chuỗi ngày. Máy đặt ngôn ngữ dùng chữ số không phải ASCII (Ả Rập, Ba Tư, Hindi…) có thể sinh ngày không phải `yyyy-MM-dd` ASCII và server sẽ trả 400. Đổi 4 chỗ `Locale.getDefault()` trong `DateUtils` thành `Locale.US` (với máy tiếng Việt thì không ảnh hưởng).

---

## 9. Kiểm thử

```bash
API_URL=https://api.ten-mien.com TOKEN="<Firebase ID token>" ./deploy/smoke-test.sh
```

Lấy token (thay `API_KEY` bằng `current_key` trong `google-services.json`, email/mật khẩu là tài khoản đã đăng ký trong app):

```bash
curl -s "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=API_KEY" \
  -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","password":"******","returnSecureToken":true}'    # lấy "idToken"
```

Script kiểm tra: health → token hợp lệ → upload `201` → upload lại `unchanged` → máy "mới" nhận `409` → tải dữ liệu khớp → không token nhận `401` → dọn dẹp.

**Checklist thử trên app thật (kịch bản Soul Knight):**
1. Máy A: nhập vài bản ghi → *Save to cloud* → thấy "Saved".
2. Bấm Save lần nữa → "Cloud is already up to date".
3. Máy B (hoặc xóa dữ liệu app rồi đăng nhập lại): bấm Save → ra hộp thoại **Cloud save found** với số liệu hai bên.
4. Chọn *Use cloud* → xác nhận → dữ liệu máy B khớp máy A, thứ tự "mới/cũ" giữ nguyên.
5. Tắt mạng → Save → báo lỗi mạng, **dữ liệu trên máy không bị ảnh hưởng**, app vẫn dùng bình thường.
6. Máy A thêm dữ liệu → Save (không xung đột vì A đã đồng bộ bản mới nhất). Máy B Save lúc chưa tải bản mới → 409.

---

## 10. Vận hành & bảo mật

- **Sao lưu chính database `FitTrackCloud`** định kỳ (`BACKUP DATABASE … TO DISK` hoặc snapshot của nhà cung cấp) – cloud save của người dùng chỉ an toàn bằng chính DB này.
- **Bí mật** chỉ nằm ở `/etc/fittrack-api/api.env` (quyền 600). Firebase API key trong app *không* phải bí mật; mật khẩu SQL mới là bí mật.
- **Quyền riêng tư:** dữ liệu sức khỏe là dữ liệu nhạy cảm. Hãy ghi rõ trong app là bản sao lưu cloud là tuỳ chọn; `DELETE /me` đã có sẵn để làm nút "Delete my cloud data".
- **Dung lượng:** mỗi người dùng tối đa N snapshot × (dữ liệu của họ). Chỉnh N bằng `CloudSave__MaxBackupsPerUser`; xem tổng quan bằng `SELECT * FROM dbo.vw_UserCloudUsage`.
- **Hiệu năng:** upload ghi bằng EF Core (đủ cho vài nghìn–chục nghìn dòng, thường dưới vài giây). Nếu một người dùng có >20.000 dòng/bảng, cân nhắc thay phần chèn bằng `SqlBulkCopy`.
- **Phiên bản gói:** `csproj` dùng `10.0.*` để luôn lấy bản vá mới nhất; muốn build lặp lại được thì ghi cố định số phiên bản sau lần restore đầu.
- Mở rộng sau này: nén gzip thân request (OkHttp interceptor + `AddRequestDecompression`), giới hạn riêng cho auto-backup so với bản thủ công khi giữ N bản, upload avatar (cột `VARBINARY(MAX)`), trang quản trị xem `vw_UserCloudUsage`.

---

## 11. Sự cố thường gặp

| Triệu chứng | Nguyên nhân thường gặp |
|---|---|
| `/health` trả `503` / `database:false` | Sai chuỗi kết nối; SQL Server chưa chạy; sai mật khẩu `fittrack_api`; thiếu `TrustServerCertificate=True` với chứng chỉ tự ký |
| Mọi request `401` | `Firebase__ProjectId` sai (phải bằng `project_id` trong `google-services.json`); token hết hạn (1 giờ); server không ra được `securetoken.google.com`; đồng hồ server lệch (kiểm tra `timedatectl`). Xem `pm2 logs` – JwtBearer ghi rõ lý do |
| Upload `413` | nginx thiếu `client_max_body_size 20m;` |
| `502 Bad Gateway` | pm2 chưa chạy / API chết: `pm2 status`, `pm2 logs fittrack-api` |
| pm2 báo "errored"/restart liên tục | thiếu `/etc/fittrack-api/api.env` (log báo thiếu `ConnectionStrings:Default`); chưa cài runtime .NET 10; sai `cwd` |
| Upload `400` | thân JSON sai: xem trường `errors` trong phản hồi (thường: thiếu `data`/`date`, số âm, ngày sai định dạng) |
| Upload luôn `409` | `baseBackupId` không được lưu – kiểm tra `CloudPrefs`; hoặc người dùng đang thật sự ở trạng thái xung đột → chọn *Keep/Use cloud* |
| Android `CLEARTEXT communication not permitted` | đang gọi `http://` ngoài bản debug – dùng HTTPS (mục 8.6) |
| Android không import được `Tasks` | thêm `implementation 'com.google.android.gms:play-services-tasks:18.2.0'` |
| `SqlException … certificate chain was issued by an authority that is not trusted` | thêm `TrustServerCertificate=True` hoặc cài chứng chỉ hợp lệ cho SQL Server |

---

## 12. Đã kiểm chứng & chưa kiểm chứng (nói thẳng)

| Phần | Mức độ |
|---|---|
| T-SQL (2 file) | ✅ Parse cú pháp đạt. ⚠️ **Chưa chạy trên SQL Server thật.** |
| Code C# | ✅ Biên dịch sạch với trình biên dịch C#/.NET 10 thật (0 lỗi, 0 cảnh báo). ⚠️ EF Core và JwtBearer được thay bằng bản mô phỏng chữ ký vì môi trường soạn không tải được NuGet → **chưa kiểm chứng EF dịch LINQ ra SQL thế nào** và **chưa kiểm chứng với token Firebase thật**. |
| Tầng HTTP (route, JSON, validate, 201/200/409/404/401/429) | ✅ Chạy thật trên Kestrel với Controller/DTO/`Program.cs` của bạn + service giả. Gồm: 11 kịch bản nghiệp vụ, 11 ca validate, payload 40.000 dòng (5,4 MB) bind JSON + validate + băm mất khoảng 0,4–0,75 s (chưa tính ghi DB), hash không phụ thuộc `localId`, rate limit đúng 60/phút. |
| pm2 + dotnet | ✅ Chạy thật: `interpreter: none`, `cwd`, `env` từ file ngoài, **tự hồi sinh sau `kill -9`**. |
| `deploy.sh`, `smoke-test.sh` | ✅ Cú pháp bash đạt; `smoke-test.sh` chạy đạt 8/8 bước với host giả. `deploy.sh` chưa chạy trọn vẹn (cần NuGet + SQL). |
| Code Android (8 file Java) | ✅ `javac` biên dịch sạch với DAO thật của dự án (đã áp dụng `DAO_additions.md`), nhưng dùng stub cho Android/Retrofit/Firebase/WorkManager. ⚠️ **Chưa chạy trên thiết bị/emulator**; chưa kiểm thử Gson ↔ JSON thực tế. |

Lần chạy đầu tiên thật sự trên hệ thống của bạn: `dotnet build` (tải NuGet) → `smoke-test.sh` → thử trên app. Nếu gặp lỗi biên dịch/chạy, gửi lại nguyên văn thông báo lỗi để chỉnh.
