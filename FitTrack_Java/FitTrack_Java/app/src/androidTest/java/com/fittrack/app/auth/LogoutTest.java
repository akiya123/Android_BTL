package com.fittrack.app.auth;

import android.content.Context;
import android.app.Activity;
import android.app.Instrumentation;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.fittrack.app.MainActivity;
import com.fittrack.app.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class LogoutTest {
    @Test public void logoutOpensLoginAndClearsTheSession() {
        Context context = ApplicationProvider.getApplicationContext();
        String fixture = "logout_ui_fixture";
        String active = AccountSession.localId(context);
        assumeTrue("Use a signed-out test device", active == null || fixture.equals(active));
        AccountSession.start(context, fixture, "Logout test");
        Instrumentation runner = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = runner.addMonitor(AuthActivity.class.getName(), null, false);
        try (ActivityScenario<MainActivity> main = ActivityScenario.launch(MainActivity.class)) {
            main.onActivity(activity -> activity.findViewById(R.id.nav_profile).performClick());
            main.onActivity(activity -> activity.findViewById(R.id.btnLogout).performClick());
            Activity login = monitor.waitForActivityWithTimeout(5000);
            assertNotNull(login);
            runner.waitForIdleSync();
            runner.runOnMainSync(() -> assertTrue(login.findViewById(R.id.etUsername).isShown()));
            assertNull(AccountSession.localId(context));
        } finally {
            runner.removeMonitor(monitor);
            AccountSession.logout(context);
            context.getSharedPreferences("akiya_accounts", Context.MODE_PRIVATE).edit().remove(fixture).apply();
        }
    }
}
