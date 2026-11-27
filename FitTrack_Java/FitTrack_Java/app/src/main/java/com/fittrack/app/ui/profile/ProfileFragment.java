package com.fittrack.app.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.fittrack.app.auth.AuthActivity;
import com.fittrack.app.data.local.AppDatabase;
import com.fittrack.app.databinding.FragmentProfileBinding;
import com.fittrack.app.onboarding.OnboardingActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private androidx.appcompat.app.AlertDialog syncDialog;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    saveAvatarToInternalStorage(uri);
                }
            });

    private void saveAvatarToInternalStorage(Uri sourceUri) {
        if (getContext() == null) return;
        Context appContext = getContext().getApplicationContext();
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try (InputStream inputStream = appContext.getContentResolver().openInputStream(sourceUri)) {
                if (inputStream == null) return;

                File avatarFile = new File(appContext.getFilesDir(), "avatar_" + AppDatabase.accountKey(uid) + ".jpg");
                try (FileOutputStream outputStream = new FileOutputStream(avatarFile)) {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, read);
                    }
                    outputStream.flush();
                }

                Uri internalUri = Uri.fromFile(avatarFile);
                viewModel.updateAvatar(internalUri.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        binding.tvEmail.setText(viewModel.currentEmail());

        viewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile == null) return;
            binding.tvName.setText(profile.name);
            binding.tvAgeGender.setText(String.format(Locale.US, "%d years · %s", profile.age, profile.gender));
            binding.tvHeightWeight.setText(String.format(Locale.US, "%.0f cm · %.1f kg", profile.heightCm, profile.weightKg));
            binding.tvActivityLevel.setText(profile.activityLevel);
            binding.tvGoal.setText(profile.goal);
            binding.tvBmrValue.setText(String.format(Locale.US, "%.0f kcal", profile.bmr));
            binding.tvTdeeValue.setText(String.format(Locale.US, "%.0f kcal", profile.tdee));

            if (profile.avatarUri != null && !profile.avatarUri.isEmpty()) {
                binding.ivAvatar.setImageURI(null);
                binding.ivAvatar.setImageURI(Uri.parse(profile.avatarUri));
            }
        });

        binding.ivAvatar.setOnClickListener(v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        binding.btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), OnboardingActivity.class)));

        binding.btnBackup.setOnClickListener(v -> startCloudAction(true));
        binding.btnSync.setOnClickListener(v -> startCloudAction(false));
        viewModel.busy().observe(getViewLifecycleOwner(), busy -> {
            binding.btnBackup.setEnabled(!busy);
            binding.btnSync.setEnabled(!busy);
            binding.btnLogout.setEnabled(!busy);
        });
        viewModel.status().observe(getViewLifecycleOwner(), text -> {
            binding.tvSyncStatus.setText(text);
            binding.tvSyncStatus.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
        });

        binding.btnLogout.setOnClickListener(v -> confirmLogout());
    }

    private void startCloudAction(boolean upload) {
        if (viewModel.linked()) {
            viewModel.sync(upload, null, null, false);
            return;
        }
        android.widget.LinearLayout fields = new android.widget.LinearLayout(requireContext());
        fields.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        fields.setPadding(padding, 0, padding, 0);
        android.widget.EditText username = new android.widget.EditText(requireContext());
        username.setSingleLine(true);
        username.setHint("Cloud username");
        android.widget.EditText password = new android.widget.EditText(requireContext());
        password.setSingleLine(true);
        password.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setHint("Cloud password");
        fields.addView(username);
        fields.addView(password);
        syncDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Connect cloud account")
                .setMessage("Use your android.akiya.io.vn account to " + (upload ? "back up this device." : "sync data to this device."))
                .setView(fields)
                .setPositiveButton("Sign in", null)
                .setNegativeButton("Create account", null)
                .setNeutralButton("Cancel", null)
                .show();
        for (int button : new int[]{android.content.DialogInterface.BUTTON_POSITIVE, android.content.DialogInterface.BUTTON_NEGATIVE}) {
            boolean register = button == android.content.DialogInterface.BUTTON_NEGATIVE;
            syncDialog.getButton(button).setOnClickListener(v -> {
                String name = username.getText().toString().trim();
                String secret = password.getText().toString();
                if (name.isEmpty()) { username.setError("Enter your cloud username"); return; }
                if (secret.isEmpty()) { password.setError("Enter your cloud password"); return; }
                syncDialog.dismiss();
                viewModel.sync(upload, name, secret, register);
            });
        }
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout from FitTrack?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    viewModel.logout();
                    Intent intent = new Intent(requireContext(), AuthActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (syncDialog != null) syncDialog.dismiss();
        syncDialog = null;
        binding = null;
    }
}
