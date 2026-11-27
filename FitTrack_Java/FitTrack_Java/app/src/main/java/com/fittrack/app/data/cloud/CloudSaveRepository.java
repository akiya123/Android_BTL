package com.fittrack.app.data.cloud;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.fittrack.app.BuildConfig;
import com.fittrack.app.data.local.AppDatabase;
import com.google.gson.Gson;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;
import static com.fittrack.app.data.cloud.CloudModels.*;

public final class CloudSaveRepository {
    private static final Object UPLOAD_LOCK = new Object();
    private final AppDatabase db;
    private final SharedPreferences accounts;
    private final String uid;
    private final Gson gson = new Gson();

    public CloudSaveRepository(Context context, String uid) {
        this.uid = uid;
        db = AppDatabase.forAccount(context, uid);
        accounts = context.getSharedPreferences("akiya_accounts", Context.MODE_PRIVATE);
    }

    public boolean linked() { return accounts.contains(uid); }

    public void unlink() { accounts.edit().remove(uid).apply(); }

    public void link(String username, String password, boolean register) throws IOException {
        Reply reply = call(register ? "auth/register" : "auth/login", new Credentials(username, password), Reply.class);
        if (!reply.success || reply.userId == null || !reply.userId.matches("USR_[A-Za-z0-9]+")) {
            throw new IOException("Cloud login failed.");
        }
        accounts.edit().putString(uid, reply.userId).apply();
    }

    private String cloudId() throws IOException {
        String id = accounts.getString(uid, null);
        if (id == null) throw new IOException("Connect your cloud account first.");
        return id;
    }

    private Download download(String id) throws IOException {
        Download data = call("sync/download/" + Uri.encode(id), null, Download.class);
        if (!data.success || data.foodLogs == null || data.waterLogs == null ||
                data.workoutLogs == null || data.weightEntries == null) throw new IOException("Incomplete cloud response.");
        return data;
    }

    public void backup() throws IOException {
        synchronized (UPLOAD_LOCK) {
            String id = cloudId();
            Data cloud = download(id);
            Data local = db.runInTransaction(() -> SnapshotMapper.read(db));
            Data delta = SnapshotMapper.difference(local, cloud);
            if (delta.userProfile == null) throw new IOException("Complete your profile before backing up.");
            delta.userId = id;
            if (cloud.userProfile != null) delta.userProfile.targetWeight = cloud.userProfile.targetWeight;
            Reply reply = call("sync/upload", delta, Reply.class);
            if (!reply.success) throw new IOException("Backup was not accepted.");
        }
    }

    public void syncFromCloud() throws IOException {
        Data cloud = download(cloudId());
        if (cloud.userProfile == null && cloud.foodLogs.isEmpty() && cloud.waterLogs.isEmpty()
                && cloud.weightEntries.isEmpty() && cloud.workoutLogs.isEmpty()) {
            throw new IOException("There is no cloud data for this account.");
        }
        SnapshotMapper.merge(db, cloud);
    }

    private <T> T call(String path, Object body, Class<T> type) throws IOException {
        URL url = new URL(BuildConfig.CLOUD_API_URL.replaceAll("/+$", "") + "/" + path);
        if (!"https".equals(url.getProtocol())) throw new IOException("The cloud API requires HTTPS.");
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        try {
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(90000);
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(false);
            if (body != null) {
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setChunkedStreamingMode(8192);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (Writer out = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
                    gson.toJson(body, out);
                }
            }
            int status = connection.getResponseCode();
            if (status == 401) throw new IOException("Incorrect cloud username or password.");
            if (status < 200 || status >= 300) throw new IOException("Cloud request failed (HTTP " + status + ").");
            try (Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                T result = gson.fromJson(reader, type);
                if (result == null) throw new IOException("Empty cloud response.");
                return result;
            }
        } finally {
            connection.disconnect();
        }
    }
}
