package com.fittrack.app.data.cloud;

import com.fittrack.app.data.local.AppDatabase;
import com.fittrack.app.data.local.entity.FoodLog;
import com.fittrack.app.data.local.entity.UserProfile;
import com.fittrack.app.data.local.entity.WaterLog;
import com.fittrack.app.data.local.entity.WeightEntry;
import com.fittrack.app.data.local.entity.WorkoutLog;

import java.util.ArrayList;
import java.util.List;

/** Chuyển qua lại giữa dữ liệu Room và JSON snapshot. Mọi hàm đều đọc/ghi DB -> CHỈ gọi trên luồng nền. */
public final class SnapshotMapper {
    private SnapshotMapper() {}

    // ------------------------------------------------------------------ Room -> JSON
    public static CloudModels.SnapshotData fromRoom(AppDatabase db) {
        CloudModels.SnapshotData d = new CloudModels.SnapshotData();

        UserProfile p = db.userProfileDao().getUserProfileOnce();
        if (p != null) {
            CloudModels.Profile cp = new CloudModels.Profile();
            cp.name = p.name;
            cp.age = p.age;
            cp.gender = p.gender;
            cp.heightCm = p.heightCm;
            cp.weightKg = p.weightKg;
            cp.activityLevel = p.activityLevel;
            cp.goal = p.goal;
            cp.bmr = p.bmr;
            cp.tdee = p.tdee;
            cp.calorieGoal = p.calorieGoal;
            cp.proteinGoalG = p.proteinGoalG;
            cp.carbGoalG = p.carbGoalG;
            cp.fatGoalG = p.fatGoalG;
            d.profile = cp; // firebaseUid & avatarUri cố ý KHÔNG gửi
        }

        for (FoodLog f : db.foodLogDao().getAllOnce()) {
            CloudModels.FoodLog x = new CloudModels.FoodLog();
            x.localId = f.id;
            x.name = f.name;
            x.calories = f.calories;
            x.proteinG = f.proteinG;
            x.carbG = f.carbG;
            x.fatG = f.fatG;
            x.mealType = f.mealType;
            x.date = f.date;
            d.foodLogs.add(x);
        }

        for (WorkoutLog w : db.workoutLogDao().getAllOnce()) {
            CloudModels.WorkoutLog x = new CloudModels.WorkoutLog();
            x.localId = w.id;
            x.exerciseName = w.exerciseName;
            x.sets = w.sets;
            x.reps = w.reps;
            x.weightKg = w.weightKg;
            x.caloriesBurned = w.caloriesBurned;
            x.date = w.date;
            d.workoutLogs.add(x);
        }

        for (WeightEntry e : db.weightEntryDao().getAllOnce()) {
            CloudModels.WeightEntry x = new CloudModels.WeightEntry();
            x.localId = e.id;
            x.weightKg = e.weightKg;
            x.date = e.date;
            d.weightEntries.add(x);
        }

        for (WaterLog w : db.waterLogDao().getAllOnce()) {
            CloudModels.WaterLog x = new CloudModels.WaterLog();
            x.localId = w.id;
            x.amountMl = w.amountMl;
            x.hour = w.hour;
            x.date = w.date;
            d.waterLogs.add(x);
        }
        return d;
    }

    // ------------------------------------------------------------------ JSON -> Room
    /**
     * THAY THẾ toàn bộ dữ liệu cục bộ bằng snapshot, trong 1 transaction duy nhất:
     * lỗi giữa chừng thì Room tự rollback, dữ liệu cũ trên máy vẫn còn nguyên.
     *
     * @param currentUid UID Firebase đang đăng nhập (ghi vào UserProfile.firebaseUid)
     */
    public static void replaceLocal(AppDatabase db, CloudModels.SnapshotData d, String currentUid) {
        // Dựng sẵn entity TRƯỚC khi mở transaction cho transaction ngắn nhất có thể.
        List<FoodLog> foods = new ArrayList<>();
        for (CloudModels.FoodLog x : d.foodLogs) {
            foods.add(new FoodLog(nz(x.name), x.calories, x.proteinG, x.carbG, x.fatG, nz(x.mealType), x.date));
        }
        List<WorkoutLog> workouts = new ArrayList<>();
        for (CloudModels.WorkoutLog x : d.workoutLogs) {
            workouts.add(new WorkoutLog(nz(x.exerciseName), x.sets, x.reps, x.weightKg, x.caloriesBurned, x.date));
        }
        List<WeightEntry> weights = new ArrayList<>();
        for (CloudModels.WeightEntry x : d.weightEntries) {
            weights.add(new WeightEntry(x.weightKg, x.date));
        }
        List<WaterLog> waters = new ArrayList<>();
        for (CloudModels.WaterLog x : d.waterLogs) {
            waters.add(new WaterLog(x.amountMl, x.hour, x.date));
        }

        db.runInTransaction(() -> {
            UserProfile existing = db.userProfileDao().getUserProfileOnce();
            String keepAvatar = existing != null ? existing.avatarUri : null; // avatar là file cục bộ -> giữ nguyên

            db.foodLogDao().deleteAll();
            db.workoutLogDao().deleteAll();
            db.weightEntryDao().deleteAll();
            db.waterLogDao().deleteAll();

            // Chèn theo thứ tự id tăng dần (server đã sắp theo LocalId) -> thứ tự mới/cũ giống máy gốc.
            db.foodLogDao().insertAll(foods);
            db.workoutLogDao().insertAll(workouts);
            db.weightEntryDao().insertAll(weights);
            db.waterLogDao().insertAll(waters);

            CloudModels.Profile cp = d.profile;
            if (cp != null) {
                UserProfile p = new UserProfile(
                        currentUid, nz(cp.name), cp.age, nz(cp.gender), cp.heightCm, cp.weightKg,
                        nz(cp.activityLevel), nz(cp.goal), cp.bmr, cp.tdee, cp.calorieGoal,
                        cp.proteinGoalG, cp.carbGoalG, cp.fatGoalG);
                p.avatarUri = keepAvatar;
                db.userProfileDao().insertOrUpdate(p);
            } else {
                db.userProfileDao().clear(); // snapshot gốc chưa có hồ sơ -> app sẽ vào Onboarding
            }
        });
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
