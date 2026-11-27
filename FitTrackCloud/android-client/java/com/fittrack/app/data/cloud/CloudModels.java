package com.fittrack.app.data.cloud;

import java.util.ArrayList;
import java.util.List;

/**
 * Các lớp POJO khớp 1-1 với JSON của API (Gson dùng đúng tên field -> camelCase giống ASP.NET Core).
 * Ngày là chuỗi "yyyy-MM-dd" - đúng định dạng đang lưu trong Room.
 */
public final class CloudModels {
    private CloudModels() {}

    // ---------------- Dữ liệu snapshot ----------------
    public static class Profile {
        public String name;
        public int age;
        public String gender;
        public float heightCm;
        public float weightKg;
        public String activityLevel;
        public String goal;
        public double bmr, tdee, calorieGoal, proteinGoalG, carbGoalG, fatGoalG;
    }

    public static class FoodLog {
        public long localId;
        public String name;
        public double calories, proteinG, carbG, fatG;
        public String mealType;
        public String date;
    }

    public static class WorkoutLog {
        public long localId;
        public String exerciseName;
        public int sets, reps;
        public float weightKg;
        public double caloriesBurned;
        public String date;
    }

    public static class WeightEntry {
        public long localId;
        public float weightKg;
        public String date;
    }

    public static class WaterLog {
        public long localId;
        public int amountMl;
        public int hour;
        public String date;
    }

    public static class SnapshotData {
        public Profile profile; // null nếu chưa có hồ sơ
        public List<FoodLog> foodLogs = new ArrayList<>();
        public List<WorkoutLog> workoutLogs = new ArrayList<>();
        public List<WeightEntry> weightEntries = new ArrayList<>();
        public List<WaterLog> waterLogs = new ArrayList<>();
    }

    // ---------------- Request ----------------
    public static class UploadRequest {
        public String deviceId;
        public String deviceName;
        public String appVersion;
        public int schemaVersion;
        public String label;          // tuỳ chọn
        public boolean isAuto;
        public Long baseBackupId;     // null = máy này chưa từng đồng bộ (Gson bỏ qua field null)
        public boolean force;
        public SnapshotData data;
    }

    // ---------------- Response ----------------
    public static class BackupInfo {
        public long id;
        public String createdAtUtc;  // ISO-8601, vd "2026-11-11T08:58:35.485+00:00"
        public String deviceId;
        public String deviceName;
        public String appVersion;
        public int schemaVersion;
        public String label;
        public boolean isAuto;
        public boolean hasProfile;
        public String profileName;
        public Float latestWeightKg;
        public int foodLogCount, workoutLogCount, weightEntryCount, waterLogCount;
        public String firstLogDate;  // "yyyy-MM-dd" hoặc null
        public String lastLogDate;
    }

    public static class UploadResponse {
        public String outcome;       // "created" | "unchanged"
        public BackupInfo backup;
        public int prunedBackups;
    }

    public static class ConflictResponse {
        public String code;
        public String message;
        public BackupInfo latest;
    }

    public static class Download {
        public BackupInfo info;
        public SnapshotData data;
    }
}
