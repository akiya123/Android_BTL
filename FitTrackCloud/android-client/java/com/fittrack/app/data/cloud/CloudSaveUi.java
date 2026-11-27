package com.fittrack.app.data.cloud;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Các luồng giao diện của Cloud Save. Gọi từ ProfileFragment, ví dụ:
 *     binding.btnCloudBackup.setOnClickListener(v -> CloudSaveUi.backup(requireActivity()));
 *     binding.btnCloudRestore.setOnClickListener(v -> CloudSaveUi.restore(requireActivity()));
 *     binding.switchAutoBackup.setOnCheckedChangeListener((b, on) -> CloudSaveUi.setAutoBackup(requireContext(), on));
 */
public final class CloudSaveUi {
    private CloudSaveUi() {}

    /** 1 luồng riêng cho mạng, để không chiếm ExecutorService của Room (databaseWriteExecutor). */
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    // ===================================================================================
    //  SAO LƯU
    // ===================================================================================

    public static void backup(Activity act) {
        runBackup(act, false);
    }

    private static void runBackup(Activity act, boolean force) {
        Context app = act.getApplicationContext();
        toast(act, "Saving to cloud…");

        IO.execute(() -> {
            CloudSaveRepository repo = new CloudSaveRepository(app);
            CloudSaveRepository.Result r = repo.backup(force, false);
            CloudSaveRepository.LocalSummary local =
                    r.status == CloudSaveRepository.Status.CONFLICT ? repo.localSummary() : null;

            act.runOnUiThread(() -> {
                if (act.isFinishing() || act.isDestroyed()) return;
                onBackupResult(act, r, local);
            });
        });
    }

    private static void onBackupResult(Activity act, CloudSaveRepository.Result r,
                                       CloudSaveRepository.LocalSummary local) {
        switch (r.status) {
            case OK:
                toast(act, "Saved to cloud (" + formatTime(r.info.createdAtUtc) + ")");
                break;
            case UNCHANGED:
                toast(act, "Cloud is already up to date");
                break;
            case CONFLICT:
                showConflictDialog(act, r.info, local);
                break;
            default:
                toast(act, errorText(r));
        }
    }

    /**
     * Cloud đang có bản mà máy này chưa đồng bộ (máy khác đã upload, hoặc đây là máy mới/cài lại).
     * Người dùng tự chọn bản nào được giữ - app KHÔNG tự quyết định.
     */
    private static void showConflictDialog(Activity act, CloudModels.BackupInfo cloud,
                                           CloudSaveRepository.LocalSummary local) {
        String msg = "CLOUD SAVE\n" + describe(cloud)
                + "\n\nTHIS DEVICE\n" + describe(local)
                + "\n\nWhich one do you want to keep?";

        new MaterialAlertDialogBuilder(act)
                .setTitle("Cloud save found")
                .setMessage(msg)
                .setPositiveButton("Keep this device", (d, w) -> runBackup(act, true)) // ghi đè cloud (bản cũ vẫn được giữ vài phiên bản)
                .setNegativeButton("Use cloud", (d, w) -> confirmRestore(act, cloud))
                .setNeutralButton("Cancel", null)
                .show();
    }

    // ===================================================================================
    //  KHÔI PHỤC
    // ===================================================================================

    public static void restore(Activity act) {
        Context app = act.getApplicationContext();
        toast(act, "Checking cloud…");

        IO.execute(() -> {
            CloudSaveRepository.Result r = new CloudSaveRepository(app).fetchLatestInfo();
            act.runOnUiThread(() -> {
                if (act.isFinishing() || act.isDestroyed()) return;
                if (r.status == CloudSaveRepository.Status.OK) {
                    confirmRestore(act, r.info);
                } else if (r.status == CloudSaveRepository.Status.NO_CLOUD_DATA) {
                    toast(act, "No cloud save yet");
                } else {
                    toast(act, errorText(r));
                }
            });
        });
    }

    private static void confirmRestore(Activity act, CloudModels.BackupInfo cloud) {
        new MaterialAlertDialogBuilder(act)
                .setTitle("Restore from cloud?")
                .setMessage(describe(cloud)
                        + "\n\nAll data currently on this device will be REPLACED. This cannot be undone.")
                .setPositiveButton("Restore", (d, w) -> doRestore(act))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void doRestore(Activity act) {
        Context app = act.getApplicationContext();
        toast(act, "Restoring…");

        IO.execute(() -> {
            CloudSaveRepository.Result r = new CloudSaveRepository(app).restoreLatest();
            act.runOnUiThread(() -> {
                if (act.isFinishing() || act.isDestroyed()) return;
                // Room tự thông báo cho LiveData -> các màn hình đang mở tự cập nhật.
                toast(act, r.status == CloudSaveRepository.Status.OK ? "Restored from cloud" : errorText(r));
            });
        });
    }

    // ===================================================================================
    //  TỰ ĐỘNG SAO LƯU (tuỳ chọn)
    // ===================================================================================

    public static void setAutoBackup(Context ctx, boolean enabled) {
        new CloudPrefs(ctx).setAutoBackupEnabled(enabled);
        if (enabled) CloudBackupWorker.schedule(ctx); else CloudBackupWorker.cancel(ctx);
    }

    // ===================================================================================
    //  Định dạng hiển thị
    // ===================================================================================

    private static String describe(CloudModels.BackupInfo i) {
        return "Device: " + i.deviceName
                + "\nSaved: " + formatTime(i.createdAtUtc) + (i.isAuto ? " (auto)" : "")
                + "\nFood logs: " + i.foodLogCount + " · Workouts: " + i.workoutLogCount
                + "\nWeights: " + i.weightEntryCount + " · Water: " + i.waterLogCount
                + "\nLast entry: " + (i.lastLogDate != null ? i.lastLogDate : "—");
    }

    private static String describe(CloudSaveRepository.LocalSummary s) {
        if (s == null) return "—";
        return "Food logs: " + s.foodLogs + " · Workouts: " + s.workoutLogs
                + "\nWeights: " + s.weightEntries + " · Water: " + s.waterLogs
                + "\nLast entry: " + (s.lastLogDate != null ? s.lastLogDate : "—");
    }

    private static String formatTime(String iso) {
        try {
            return OffsetDateTime.parse(iso)
                    .atZoneSameInstant(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.US));
        } catch (Exception e) {
            return iso;
        }
    }

    private static String errorText(CloudSaveRepository.Result r) {
        switch (r.status) {
            case NOT_LOGGED_IN:  return "Please log in again";
            case WRONG_ACCOUNT:  return "This device holds another account's data. Cloud backup is disabled to protect it.";
            case APP_TOO_OLD:    return r.message;
            case NETWORK_ERROR:  return "No connection. Your data is safe on this device — try again later.";
            case SERVER_ERROR:   return "Server is busy. Please try again later.";
            default:             return r.message != null ? r.message : "Something went wrong";
        }
    }

    private static void toast(Activity act, String text) {
        Toast.makeText(act, text, Toast.LENGTH_LONG).show();
    }
}
