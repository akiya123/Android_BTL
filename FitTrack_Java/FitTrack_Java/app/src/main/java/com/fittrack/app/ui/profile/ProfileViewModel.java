package com.fittrack.app.ui.profile;

import android.app.Application;
import androidx.lifecycle.MutableLiveData;

import com.fittrack.app.data.cloud.CloudSaveRepository;
import com.fittrack.app.data.local.AppDatabase;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.fittrack.app.data.local.entity.UserProfile;
import com.fittrack.app.data.repository.UserRepository;
import com.fittrack.app.auth.AccountSession;

public class ProfileViewModel extends AndroidViewModel {

    private final UserRepository repo;
    private final LiveData<UserProfile> userProfile;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        cloud = new CloudSaveRepository(application, AccountSession.localId(application));
        repo = new UserRepository(application);
        userProfile = repo.getUserProfile();
    }

    public LiveData<UserProfile> getUserProfile() {
        return userProfile;
    }

    public String currentEmail() {
        return AccountSession.username(getApplication());
    }

    public void updateAvatar(String uri) {
        repo.updateAvatar(uri);
    }

    private final MutableLiveData<String> status = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final CloudSaveRepository cloud;

    LiveData<String> status() { return status; }
    LiveData<Boolean> busy() { return busy; }
    boolean linked() { return cloud.linked(); }

    void sync(boolean upload, String name, String password, boolean register) {
        if (Boolean.TRUE.equals(busy.getValue())) return;
        busy.setValue(true);
        status.setValue(upload ? "Backing up…" : "Syncing from cloud…");
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                if (!cloud.linked()) cloud.link(name, password, register);
                if (upload) cloud.backup();
                else cloud.syncFromCloud();
                status.postValue(upload ? "Backup saved." : "Cloud data synced.");
            } catch (java.io.IOException error) {
                status.postValue("Sync failed. " + error.getLocalizedMessage());
            } catch (RuntimeException error) {
                status.postValue("Could not process cloud data.");
            } finally {
                busy.postValue(false);
            }
        });
    }

    public boolean logout() {
        if (AccountSession.logout(getApplication())) return true;
        status.setValue("Could not save logout on this device. Please try again.");
        return false;
    }
}
