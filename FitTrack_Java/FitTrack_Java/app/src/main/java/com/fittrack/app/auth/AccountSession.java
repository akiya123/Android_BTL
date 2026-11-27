package com.fittrack.app.auth;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.Map;

public final class AccountSession {
    private AccountSession() {}

    private static SharedPreferences session(Context context) {
        return context.getSharedPreferences("api_session", Context.MODE_PRIVATE);
    }

    public static String localId(Context context) {
        SharedPreferences state = session(context);
        if (state.getBoolean("signedOut", false)) return null;
        String id = state.getString("localId", null);
        // An existing installation may still be using its cached Firebase session.
        return id != null ? id : FirebaseAuth.getInstance().getUid();
    }

    public static String username(Context context) {
        String name = session(context).getString("username", null);
        if (name != null) return name;
        FirebaseUser legacy = FirebaseAuth.getInstance().getCurrentUser();
        return legacy != null ? legacy.getEmail() : "";
    }

    static void start(Context context, String cloudId, String username) {
        SharedPreferences links = context.getSharedPreferences("akiya_accounts", Context.MODE_PRIVATE);
        String localId = cloudId;
        // Reopen a previously linked Room database only when ownership is unambiguous.
        if (!links.contains(cloudId)) {
            String owner = null;
            for (Map.Entry<String, ?> link : links.getAll().entrySet()) {
                if (!cloudId.equals(link.getValue())) continue;
                if (owner != null) { owner = null; break; }
                owner = link.getKey();
            }
            if (owner != null) localId = owner;
        }
        links.edit().putString(localId, cloudId).apply();
        session(context).edit().remove("signedOut").putString("localId", localId)
                .putString("username", username).apply();
    }

    public static boolean logout(Context context) {
        // Persist the decision before leaving the current task, even if Android stops the process.
        if (!session(context).edit().clear().putBoolean("signedOut", true).commit()) return false;
        FirebaseAuth legacy = FirebaseAuth.getInstance();
        if (legacy.getCurrentUser() != null) legacy.signOut();
        return true;
    }
}
