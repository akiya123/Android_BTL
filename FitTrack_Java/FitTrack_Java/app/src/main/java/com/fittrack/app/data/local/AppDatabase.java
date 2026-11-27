package com.fittrack.app.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.nio.charset.StandardCharsets;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.fittrack.app.data.local.dao.FoodLogDao;
import com.fittrack.app.data.local.dao.UserProfileDao;
import com.fittrack.app.data.local.dao.WaterLogDao;
import com.fittrack.app.data.local.dao.WeightEntryDao;
import com.fittrack.app.data.local.dao.WorkoutLogDao;
import com.fittrack.app.data.local.entity.FoodLog;
import com.fittrack.app.data.local.entity.UserProfile;
import com.fittrack.app.data.local.entity.WaterLog;
import com.fittrack.app.data.local.entity.WeightEntry;
import com.fittrack.app.data.local.entity.WorkoutLog;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
        entities = {UserProfile.class, FoodLog.class, WorkoutLog.class, WeightEntry.class, WaterLog.class},
        version = 12,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserProfileDao userProfileDao();

    public abstract FoodLogDao foodLogDao();

    public abstract WorkoutLogDao workoutLogDao();

    public abstract WeightEntryDao weightEntryDao();

    public abstract WaterLogDao waterLogDao();

    private static volatile AppDatabase INSTANCE;

    /** Shared executor to run Room read/write operations off the main thread (replacing coroutines). */
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(4);

    private static final String LEGACY_NAME = "fittrack_database";
    private static String activeName;

    // Existing installations keep the original Room file for its recorded owner.
    // Called on the routing executor before opening any account-specific repositories.
    public static synchronized void identifyLegacyOwner(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("accounts", Context.MODE_PRIVATE);
        if (prefs.contains("legacyOwner") || !context.getDatabasePath(LEGACY_NAME).exists()) return;
        AppDatabase legacy = open(context, LEGACY_NAME);
        try {
            UserProfile profile = legacy.userProfileDao().getUserProfileOnce();
            if (profile != null) prefs.edit().putString("legacyOwner", profile.firebaseUid).apply();
        } finally {
            legacy.close();
        }
    }

    public static AppDatabase getInstance(@NonNull Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) throw new IllegalStateException("Sign in to access FitTrack data.");
        return forAccount(context, user.getUid());
    }

    public static synchronized AppDatabase forAccount(Context context, String uid) {
        String owner = context.getSharedPreferences("accounts", Context.MODE_PRIVATE)
                .getString("legacyOwner", "");
        String name = uid.equals(owner) ? LEGACY_NAME : "fittrack_" + accountKey(uid);
        if (!name.equals(activeName)) {
            INSTANCE = open(context, name);
            activeName = name;
        }
        return INSTANCE;
    }

    public static String accountKey(String uid) {
        return Base64.encodeToString(uid.getBytes(StandardCharsets.UTF_8),
                Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    private static AppDatabase open(Context context, String name) {
        return Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, name)
                .fallbackToDestructiveMigration().build();
    }
}
