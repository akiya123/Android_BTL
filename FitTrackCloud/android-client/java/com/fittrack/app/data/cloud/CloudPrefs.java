package com.fittrack.app.data.cloud;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

/** Trạng thái đồng bộ cloud lưu cục bộ (SharedPreferences). KHÔNG chứa dữ liệu tập luyện/dinh dưỡng. */
public final class CloudPrefs {

    private static final String FILE = "cloud_save";
    private final SharedPreferences sp;

    public CloudPrefs(Context context) {
        sp = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /** UUID định danh máy này, sinh 1 lần. Server dùng để hiển thị "bản sao lưu từ máy nào". */
    public synchronized String deviceId() {
        String id = sp.getString("device_id", null);
        if (id == null) {
            id = UUID.randomUUID().toString();
            sp.edit().putString("device_id", id).apply();
        }
        return id;
    }

    /**
     * Id bản cloud mà máy này đã đồng bộ gần nhất (sau khi upload thành công hoặc sau khi khôi phục).
     * Lưu theo từng tài khoản Firebase. null = chưa từng đồng bộ.
     */
    public Long baseBackupId(String uid) {
        String key = "base_" + uid;
        return sp.contains(key) ? sp.getLong(key, 0L) : null;
    }

    public void setBaseBackupId(String uid, long backupId) {
        sp.edit().putLong("base_" + uid, backupId).apply();
    }

    public boolean autoBackupEnabled() { return sp.getBoolean("auto_backup", false); }
    public void setAutoBackupEnabled(boolean v) { sp.edit().putBoolean("auto_backup", v).apply(); }

    /** true nếu lần tự sao lưu gần nhất gặp xung đột -> cần người dùng mở app chọn bản nào. */
    public boolean conflictPending() { return sp.getBoolean("conflict_pending", false); }
    public void setConflictPending(boolean v) { sp.edit().putBoolean("conflict_pending", v).apply(); }

    public long lastBackupAtMillis() { return sp.getLong("last_backup_at", 0L); }
    public void setLastBackupAtMillis(long t) { sp.edit().putLong("last_backup_at", t).apply(); }
}
