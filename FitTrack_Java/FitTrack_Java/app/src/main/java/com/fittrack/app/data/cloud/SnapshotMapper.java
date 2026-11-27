package com.fittrack.app.data.cloud;

import com.fittrack.app.data.local.AppDatabase;
import com.fittrack.app.data.local.entity.*;
import com.fittrack.app.util.TDEECalculator;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

import static com.fittrack.app.data.cloud.CloudModels.*;

final class SnapshotMapper {
    private SnapshotMapper() {}

    static Data read(AppDatabase db) {
        Data data = empty();
        UserProfile p = db.userProfileDao().getUserProfileOnce();
        if (p != null) {
            Profile profile = new Profile();
            profile.name = p.name;
            profile.age = p.age;
            profile.gender = p.gender;
            profile.height = p.heightCm;
            profile.activityLevel = p.activityLevel;
            data.userProfile = profile;
        }
        for (FoodLog row : db.foodLogDao().getAllOnce()) {
            Food f = new Food();
            f.foodName = row.name;
            f.calories = (int) Math.round(row.calories);
            f.protein = row.proteinG;
            f.carbs = row.carbG;
            f.fat = row.fatG;
            f.loggedAt = timestamp(row.date, 0);
            data.foodLogs.add(f);
        }
        for (WorkoutLog row : db.workoutLogDao().getAllOnce()) {
            Workout w = new Workout();
            w.exerciseName = row.exerciseName;
            w.caloriesBurned = (int) Math.round(row.caloriesBurned);
            w.loggedAt = timestamp(row.date, 0);
            data.workoutLogs.add(w);
        }
        for (WaterLog row : db.waterLogDao().getAllOnce()) {
            Water w = new Water();
            w.amountMl = row.amountMl;
            w.loggedAt = timestamp(row.date, row.hour);
            data.waterLogs.add(w);
        }
        for (WeightEntry row : db.weightEntryDao().getAllOnce()) {
            Weight w = new Weight();
            w.weight = row.weightKg;
            w.recordedDate = timestamp(row.date, 0);
            data.weightEntries.add(w);
        }
        return data;
    }

    static String day(String timestamp) {
        return LocalDate.parse(timestamp.substring(0, 10)).toString();
    }

    static int hour(String timestamp) {
        return Integer.parseInt(timestamp.substring(11, 13));
    }

    // Compare only fields representable on both sides. Counts preserve repeated identical meals.
    private static <T> List<T> missing(List<T> source, List<T> existing, Function<T, List<?>> key) {
        Map<List<?>, Integer> counts = new HashMap<>();
        for (T row : existing) counts.merge(key.apply(row), 1, Integer::sum);
        List<T> result = new ArrayList<>();
        for (T row : source) {
            List<?> fields = key.apply(row);
            int count = counts.getOrDefault(fields, 0);
            if (count == 0) result.add(row);
            else counts.put(fields, count - 1);
        }
        return result;
    }

    static Data difference(Data source, Data existing) {
        Data result = empty();
        result.userProfile = source.userProfile;
        result.foodLogs = missing(source.foodLogs, existing.foodLogs,
                f -> Arrays.asList(day(f.loggedAt), f.foodName, f.calories, f.protein, f.carbs, f.fat));
        result.workoutLogs = missing(source.workoutLogs, existing.workoutLogs,
                w -> Arrays.asList(day(w.loggedAt), w.exerciseName, w.caloriesBurned));
        result.waterLogs = missing(source.waterLogs, existing.waterLogs,
                w -> Arrays.asList(day(w.loggedAt), hour(w.loggedAt), w.amountMl));
        Map<String, Weight> target = dailyWeights(existing.weightEntries);
        for (Map.Entry<String, Weight> entry : dailyWeights(source.weightEntries).entrySet()) {
            Weight other = target.get(entry.getKey());
            if (other == null || other.weight != entry.getValue().weight) result.weightEntries.add(entry.getValue());
        }
        return result;
    }

    private static Map<String, Weight> dailyWeights(List<Weight> rows) {
        Map<String, Weight> daily = new LinkedHashMap<>();
        for (Weight row : rows) daily.put(day(row.recordedDate), row);
        return daily;
    }

    static void merge(AppDatabase db, Data cloud) {
        db.runInTransaction(() -> {
            Data delta = difference(cloud, read(db));
            for (Food f : delta.foodLogs) db.foodLogDao().insert(new FoodLog(f.foodName, f.calories,
                    f.protein, f.carbs, f.fat, "Snack", day(f.loggedAt)));
            for (Workout w : delta.workoutLogs) db.workoutLogDao().insert(new WorkoutLog(w.exerciseName,
                    0, 0, 0, w.caloriesBurned, day(w.loggedAt)));
            for (Water w : delta.waterLogs) db.waterLogDao().insert(new WaterLog(w.amountMl, hour(w.loggedAt), day(w.loggedAt)));
            for (Weight w : delta.weightEntries) db.weightEntryDao().insert(new WeightEntry(w.weight, day(w.recordedDate)));
            UserProfile local = db.userProfileDao().getUserProfileOnce();
            Profile remote = cloud.userProfile;
            if (local != null && remote != null) {
                if (remote.name != null) local.name = remote.name;
                if (remote.age != null) local.age = remote.age;
                if (remote.height != null) local.heightCm = remote.height;
                if (remote.gender != null) local.gender = "Nam".equalsIgnoreCase(remote.gender) ? "Male" :
                        ("Nữ".equalsIgnoreCase(remote.gender) ? "Female" : remote.gender);
                if (remote.activityLevel != null) local.activityLevel = remote.activityLevel;
                local.bmr = TDEECalculator.calculateBMR(local.gender, local.weightKg, local.heightCm, local.age);
                local.tdee = TDEECalculator.calculateTDEE(local.bmr, local.activityLevel);
                local.calorieGoal = TDEECalculator.calculateCalorieGoal(local.tdee, local.goal);
                TDEECalculator.MacroResult macros = TDEECalculator.calculateMacros(local.calorieGoal);
                local.proteinGoalG = macros.proteinG;
                local.carbGoalG = macros.carbG;
                local.fatGoalG = macros.fatG;
                db.userProfileDao().insertOrUpdate(local);
            }
        });
    }
}
