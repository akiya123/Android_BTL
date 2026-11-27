package com.fittrack.app.auth;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.fittrack.app.data.cloud.CloudSaveRepository;
import com.fittrack.app.data.local.AppDatabase;

public class AuthViewModel extends AndroidViewModel {
    static final class AuthOutcome {
        final boolean success;
        final String errorMessage;
        AuthOutcome(String error) {
            success = error == null;
            errorMessage = error;
        }
    }

    private final MutableLiveData<AuthOutcome> result = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public AuthViewModel(Application app) { super(app); }
    LiveData<AuthOutcome> getAuthResult() { return result; }
    LiveData<Boolean> getIsLoading() { return loading; }
    String currentUser() { return AccountSession.localId(getApplication()); }

    void login(String username, String password) { submit(username, password, false); }

    void register(String username, String password, String confirm) {
        if (!password.equals(confirm)) {
            result.setValue(new AuthOutcome("Passwords do not match."));
            return;
        }
        submit(username, password, true);
    }

    private void submit(String username, String password, boolean register) {
        if (Boolean.TRUE.equals(loading.getValue())) return;
        if (username.isEmpty() || password.trim().isEmpty()) {
            result.setValue(new AuthOutcome("Enter your cloud username and password."));
            return;
        }
        loading.setValue(true);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                String id = CloudSaveRepository.authenticate(username, password, register);
                AccountSession.start(getApplication(), id, username);
                result.postValue(new AuthOutcome(null));
            } catch (java.net.UnknownHostException | java.net.SocketTimeoutException e) {
                result.postValue(new AuthOutcome("Cannot reach android.akiya.io.vn. Check your internet connection."));
            } catch (java.io.IOException e) {
                result.postValue(new AuthOutcome(e.getLocalizedMessage()));
            } catch (RuntimeException e) {
                result.postValue(new AuthOutcome("Invalid response from the account server."));
            } finally {
                loading.postValue(false);
            }
        });
    }
}
