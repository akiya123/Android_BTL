package com.fittrack.app.data.cloud;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CloudModels {
    private CloudModels() {}

    static class Reply {
        boolean success;
        String message;
        String userId;
    }

    static class Credentials {
        final String username, password;
        Credentials(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    static class Data {
        String userId;
        Profile userProfile;
        List<Food> foodLogs;
        List<Water> waterLogs;
        List<Weight> weightEntries;
        List<Workout> workoutLogs;
    }

    static class Download extends Data { boolean success; }

    static class Profile {
        String name, gender, activityLevel;
        Integer age;
        Float height, targetWeight;
    }

    static class Food {
        String foodName, loggedAt;
        int calories;
        double protein, carbs, fat;
    }

    static class Water {
        int amountMl;
        String loggedAt;
    }

    static class Weight {
        float weight;
        String recordedDate;
    }

    static class Workout {
        String exerciseName, loggedAt;
        int durationMinutes, caloriesBurned;
    }

    static Data empty() {
        Data data = new Data();
        data.foodLogs = new ArrayList<>();
        data.waterLogs = new ArrayList<>();
        data.weightEntries = new ArrayList<>();
        data.workoutLogs = new ArrayList<>();
        return data;
    }

    static String timestamp(String date, int hour) {
        return String.format(Locale.ROOT, "%sT%02d:00:00", date, hour);
    }
}
