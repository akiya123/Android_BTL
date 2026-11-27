package com.fittrack.app.data.cloud;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

/**
 * Tự động sao lưu (TUỲ CHỌN, mặc định TẮT): mỗi ngày 1 lần khi có Wi-Fi và pin không yếu.
 * Tự động KHÔNG BAO GIỜ ghi đè cloud: nếu cloud có bản khác (máy khác đã upload) thì chỉ đánh dấu
 * conflictPending, lần sau người dùng mở app sẽ được hỏi chọn bản nào (giống Soul Knight).
 */
public class CloudBackupWorker extends Worker {

    public static final String UNIQUE_NAME = "fittrack_cloud_auto_backup";

    public CloudBackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public androidx.work.ListenableWorker.Result doWork() {
        CloudSaveRepository repo = new CloudSaveRepository(getApplicationContext());
        if (!repo.prefs().autoBackupEnabled()) return androidx.work.ListenableWorker.Result.success();

        CloudSaveRepository.Result r = repo.backup(false, true);
        switch (r.status) {
            case NETWORK_ERROR:
            case SERVER_ERROR:
                return androidx.work.ListenableWorker.Result.retry();   // WorkManager tự backoff
            default:
                // OK, UNCHANGED, CONFLICT (đã đánh dấu), NOT_LOGGED_IN, WRONG_ACCOUNT, REJECTED...
                // -> không retry, đợi chu kỳ sau / đợi người dùng can thiệp.
                return androidx.work.ListenableWorker.Result.success();
        }
    }

    public static void schedule(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)   // chỉ Wi-Fi, tránh tốn 4G
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                CloudBackupWorker.class, 24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    public static void cancel(Context context) {
        WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(UNIQUE_NAME);
    }
}
