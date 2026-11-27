package com.fittrack.app.data.cloud;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;

import com.fittrack.app.data.local.AppDatabase;
import com.fittrack.app.data.local.entity.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;

import java.io.IOException;

import retrofit2.Response;

/**
 * Toàn bộ logic Cloud Save phía app. Đây là các hàm CHẶN (blocking) -> luôn gọi trên luồng nền
 * (ExecutorService / WorkManager), KHÔNG gọi trên main thread.
 *
 * Nguyên tắc "kiểu Soul Knight":
 *  - Room là bản chính, app chạy offline 100%, không có cloud vẫn dùng bình thường.
 *  - Chỉ khi người dùng bấm (hoặc bật auto) mới gọi các hàm này.
 *  - Cloud và máy lệch nhau -> KHÔNG tự ghi đè, trả CONFLICT để UI cho người dùng chọn.
 */
public class CloudSaveRepository {

    public enum Status {
        OK,             // đã tải lên / đã khôi phục
        UNCHANGED,      // dữ liệu y hệt bản cloud mới nhất, không cần làm gì
        CONFLICT,       // cloud có bản khác -> hỏi người dùng
        NO_CLOUD_DATA,  // cloud chưa có gì (404)
        NOT_LOGGED_IN,
        WRONG_ACCOUNT,  // dữ liệu trên máy thuộc tài khoản Firebase khác
        APP_TOO_OLD,    // bản cloud do phiên bản app mới hơn tạo ra
        NETWORK_ERROR,  // mất mạng / timeout / không lấy được token -> nên thử lại sau
        SERVER_ERROR,   // 5xx hoặc 429 -> nên thử lại sau
        REJECTED        // 4xx khác (dữ liệu bị từ chối) -> thử lại cũng vô ích
    }

    public static final class Result {
        public final Status status;
        public final CloudModels.BackupInfo info; // bản cloud liên quan (có thể null)
        public final String message;

        Result(Status status, CloudModels.BackupInfo info, String message) {
            this.status = status;
            this.info = info;
            this.message = message;
        }
    }

    /** Tóm tắt dữ liệu trên máy, để so sánh với bản cloud trong hộp thoại xung đột. */
    public static final class LocalSummary {
        public int foodLogs, workoutLogs, weightEntries, waterLogs;
        public boolean hasProfile;
        public String lastLogDate; // "yyyy-MM-dd" hoặc null
    }

    private final Context app;
    private final AppDatabase db;
    private final CloudSaveApi api;
    private final CloudPrefs prefs;

    public CloudSaveRepository(Context context) {
        this.app = context.getApplicationContext();
        this.db = AppDatabase.getInstance(app);
        this.api = ApiClient.get();
        this.prefs = new CloudPrefs(app);
    }

    public CloudPrefs prefs() { return prefs; }

    private String currentUid() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return u != null ? u.getUid() : null;
    }

    // ---------------------------------------------------------------------------------------
    //  SAO LƯU: máy -> cloud
    // ---------------------------------------------------------------------------------------

    /**
     * @param force  true = người dùng đã chọn "Giữ dữ liệu trên máy" trong hộp thoại xung đột -> ghi đè.
     * @param isAuto true nếu do WorkManager gọi (không hiện UI).
     */
    public Result backup(boolean force, boolean isAuto) {
        String uid = currentUid();
        if (uid == null) return new Result(Status.NOT_LOGGED_IN, null, "Please log in first.");

        // Chặn trường hợp user A đăng xuất, user B đăng nhập trên cùng máy: Room vẫn còn dữ liệu của A
        // (app hiện không xóa Room khi logout) -> tuyệt đối không đẩy nó lên tài khoản của B.
        UserProfile local = db.userProfileDao().getUserProfileOnce();
        if (local != null && !local.firebaseUid.isEmpty() && !local.firebaseUid.equals(uid)) {
            return new Result(Status.WRONG_ACCOUNT, null,
                    "Data on this device belongs to a different account.");
        }

        CloudModels.UploadRequest req = new CloudModels.UploadRequest();
        req.deviceId = prefs.deviceId();
        req.deviceName = deviceName();
        req.appVersion = appVersion();
        req.schemaVersion = localSchemaVersion();
        req.isAuto = isAuto;
        req.force = force;
        req.baseBackupId = prefs.baseBackupId(uid);
        req.data = SnapshotMapper.fromRoom(db);

        try {
            Response<CloudModels.UploadResponse> res = api.upload(req).execute();

            if (res.isSuccessful() && res.body() != null) { // 201 hoặc 200
                CloudModels.UploadResponse body = res.body();
                prefs.setBaseBackupId(uid, body.backup.id);
                prefs.setConflictPending(false);
                prefs.setLastBackupAtMillis(System.currentTimeMillis());
                boolean unchanged = "unchanged".equals(body.outcome);
                return new Result(unchanged ? Status.UNCHANGED : Status.OK, body.backup, null);
            }

            if (res.code() == 409 && res.errorBody() != null) {
                CloudModels.ConflictResponse c =
                        new Gson().fromJson(res.errorBody().string(), CloudModels.ConflictResponse.class);
                prefs.setConflictPending(true);
                return new Result(Status.CONFLICT, c.latest, c.message);
            }

            return httpFailure(res.code());
        } catch (IOException e) {
            return new Result(Status.NETWORK_ERROR, null, e.getMessage());
        }
    }

    // ---------------------------------------------------------------------------------------
    //  KHÔI PHỤC: cloud -> máy
    // ---------------------------------------------------------------------------------------

    /** Thông tin bản cloud mới nhất (không tải dữ liệu). status = OK kèm info, hoặc NO_CLOUD_DATA. */
    public Result fetchLatestInfo() {
        if (currentUid() == null) return new Result(Status.NOT_LOGGED_IN, null, "Please log in first.");
        try {
            Response<CloudModels.BackupInfo> res = api.latestInfo().execute();
            if (res.code() == 404) return new Result(Status.NO_CLOUD_DATA, null, null);
            if (res.isSuccessful() && res.body() != null) return new Result(Status.OK, res.body(), null);
            return httpFailure(res.code());
        } catch (IOException e) {
            return new Result(Status.NETWORK_ERROR, null, e.getMessage());
        }
    }

    /** Tải bản cloud mới nhất rồi THAY THẾ toàn bộ dữ liệu trên máy (UI phải xác nhận với người dùng trước). */
    public Result restoreLatest() {
        String uid = currentUid();
        if (uid == null) return new Result(Status.NOT_LOGGED_IN, null, "Please log in first.");

        try {
            Response<CloudModels.Download> res = api.downloadLatest().execute();
            if (res.code() == 404) return new Result(Status.NO_CLOUD_DATA, null, null);
            if (!res.isSuccessful() || res.body() == null) return httpFailure(res.code());

            CloudModels.Download dl = res.body();

            // Bản cloud do app mới hơn tạo -> có thể có cột/bảng app này chưa hiểu -> không khôi phục.
            if (dl.info.schemaVersion > localSchemaVersion()) {
                return new Result(Status.APP_TOO_OLD, dl.info,
                        "This cloud save was created by a newer version of the app. Please update the app.");
            }

            SnapshotMapper.replaceLocal(db, dl.data, uid); // 1 transaction, lỗi thì rollback
            prefs.setBaseBackupId(uid, dl.info.id);
            prefs.setConflictPending(false);
            return new Result(Status.OK, dl.info, null);
        } catch (IOException e) {
            return new Result(Status.NETWORK_ERROR, null, e.getMessage());
        }
    }

    // ---------------------------------------------------------------------------------------
    //  Tiện ích
    // ---------------------------------------------------------------------------------------

    public LocalSummary localSummary() {
        LocalSummary s = new LocalSummary();
        String last = null;

        for (com.fittrack.app.data.local.entity.FoodLog x : db.foodLogDao().getAllOnce()) {
            s.foodLogs++;
            last = maxDate(last, x.date);
        }
        for (com.fittrack.app.data.local.entity.WorkoutLog x : db.workoutLogDao().getAllOnce()) {
            s.workoutLogs++;
            last = maxDate(last, x.date);
        }
        for (com.fittrack.app.data.local.entity.WeightEntry x : db.weightEntryDao().getAllOnce()) {
            s.weightEntries++;
            last = maxDate(last, x.date);
        }
        for (com.fittrack.app.data.local.entity.WaterLog x : db.waterLogDao().getAllOnce()) {
            s.waterLogs++;
            last = maxDate(last, x.date);
        }
        s.hasProfile = db.userProfileDao().getUserProfileOnce() != null;
        s.lastLogDate = last;
        return s;
    }

    /** Chuỗi "yyyy-MM-dd" so sánh theo từ điển cũng chính là so sánh theo thời gian. */
    private static String maxDate(String a, String b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.compareTo(b) >= 0 ? a : b;
    }

    private static Result httpFailure(int code) {
        if (code == 429 || code >= 500) {
            return new Result(Status.SERVER_ERROR, null, "Server error (HTTP " + code + "). Try again later.");
        }
        if (code == 401) {
            return new Result(Status.NOT_LOGGED_IN, null, "Session expired. Please log in again.");
        }
        return new Result(Status.REJECTED, null, "Request rejected (HTTP " + code + ").");
    }

    /** Version của Room AppDatabase (hiện là 12) - gửi kèm để biết cấu trúc dữ liệu của snapshot. */
    private int localSchemaVersion() {
        return db.getOpenHelper().getReadableDatabase().getVersion();
    }

    private String appVersion() {
        try {
            PackageInfo pi = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
            String v = pi.versionName != null ? pi.versionName : "unknown";
            return v.length() > 20 ? v.substring(0, 20) : v; // server giới hạn 20 ký tự
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static String deviceName() {
        String name = (Build.MANUFACTURER + " " + Build.MODEL).trim();
        if (name.isEmpty()) name = "Android device";
        return name.length() > 100 ? name.substring(0, 100) : name; // server giới hạn 100 ký tự
    }
}
