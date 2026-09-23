package com.example;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.databinding.FragmentProfileBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ProfileFragment containing the form to view and update user Name, Height, and Weight,
 * as well as managing the user's vertical profile photo and active session.
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private GymDatabaseHelper dbHelper;
    private UserProfile currentProfile;

    private File pendingCameraFile;
    private Uri pendingCameraUri;

    // Camera picture launcher
    private final ActivityResultLauncher<Uri> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            isSuccess -> {
                if (isSuccess && pendingCameraFile != null && pendingCameraFile.exists()) {
                    // Ensure the captured profile photo is saved vertically (upright portrait)
                    ImageRotationHelper.fixPhotoVerticalOrientation(pendingCameraFile);
                    saveProfilePhotoPath(pendingCameraFile.getAbsolutePath());
                } else {
                    Toast.makeText(getContext(), "Photo capture cancelled", Toast.LENGTH_SHORT).show();
                }
            }
    );

    // Camera permission launcher
    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    launchCamera();
                } else {
                    Toast.makeText(getContext(), "Camera permission is required to capture a profile photo", Toast.LENGTH_SHORT).show();
                }
            }
    );

    // Gallery picker launcher
    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    handleGalleryImage(uri);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = GymDatabaseHelper.getInstance(requireContext());

        loadProfile();
        setupProfilePhotoActions();
        setupLiveBmiCalculation();
        setupInputFocusHandling();
        setupSaveButton();
        setupAccountSession();
    }

    private void setupInputFocusHandling() {
        // Automatically ensure Goal Weight and inputs smoothly scroll into full view when focused with the soft keyboard
        View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
            if (hasFocus && binding != null) {
                binding.scrollProfile.postDelayed(() -> {
                    if (binding != null) {
                        binding.scrollProfile.smoothScrollTo(0, v.getBottom() + 120);
                    }
                }, 200);
            }
        };

        binding.etProfileTargetWeight.setOnFocusChangeListener(focusListener);
        binding.etProfileWeight.setOnFocusChangeListener(focusListener);
        binding.etProfileHeight.setOnFocusChangeListener(focusListener);

        binding.etProfileTargetWeight.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                binding.btnSaveProfile.performClick();
                return true;
            }
            return false;
        });
    }

    private void loadProfile() {
        currentProfile = dbHelper.getPrimaryUserProfile();
        if (currentProfile == null) {
            String savedName = AuthManager.getUserName(requireContext());
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            currentProfile = new UserProfile(1, (savedName != null && !savedName.isEmpty()) ? savedName : "Athlete", 175.0, 72.0, 70.0, today, today, null);
            dbHelper.insertUserProfile(currentProfile);
        }

        binding.etProfileName.setText(currentProfile.getName());
        binding.etProfileHeight.setText(String.format(Locale.getDefault(), "%.1f", currentProfile.getHeightCm()));
        binding.etProfileWeight.setText(String.format(Locale.getDefault(), "%.1f", currentProfile.getCurrentWeightKg()));
        binding.etProfileTargetWeight.setText(String.format(Locale.getDefault(), "%.1f", currentProfile.getTargetWeightKg()));

        loadProfilePhotoUI();
        updateBmiPreview(currentProfile.getHeightCm(), currentProfile.getCurrentWeightKg());
    }

    private void loadProfilePhotoUI() {
        if (currentProfile == null || getContext() == null) return;

        String path = currentProfile.getProfileImagePath();
        if (path != null && !path.trim().isEmpty()) {
            File photoFile = new File(path);
            if (photoFile.exists()) {
                Bitmap bitmap = ImageRotationHelper.loadOrientedBitmap(path, 300, 300);
                if (bitmap != null) {
                    binding.ivProfileAvatar.setImageBitmap(bitmap);
                    binding.ivProfileAvatar.setVisibility(View.VISIBLE);
                    binding.tvProfileInitials.setVisibility(View.GONE);
                    return;
                }
            }
        }

        // Fallback to initials
        binding.ivProfileAvatar.setVisibility(View.GONE);
        binding.tvProfileInitials.setVisibility(View.VISIBLE);
        binding.tvProfileInitials.setText(currentProfile.getInitials());
    }

    private void setupProfilePhotoActions() {
        // Tapping avatar views photo in high-resolution viewer if present, or prompts to add one
        View.OnClickListener avatarViewClickListener = v -> {
            String path = currentProfile != null ? currentProfile.getProfileImagePath() : null;
            if (path != null && new File(path).exists()) {
                PhotoViewerHelper.showProfilePhotoViewer(
                        requireContext(),
                        path,
                        currentProfile.getName(),
                        this::removeProfilePhoto
                );
            } else {
                showPhotoPickerChoiceDialog();
            }
        };

        binding.cardProfileAvatar.setOnClickListener(avatarViewClickListener);
        binding.ivProfileAvatar.setOnClickListener(avatarViewClickListener);

        // Camera badge button to add or update profile picture
        binding.btnChangeProfilePhoto.setOnClickListener(v -> showPhotoPickerChoiceDialog());
    }

    private void showPhotoPickerChoiceDialog() {
        String[] options;
        boolean hasExisting = currentProfile != null && currentProfile.getProfileImagePath() != null
                && new File(currentProfile.getProfileImagePath()).exists();

        if (hasExisting) {
            options = new String[]{"Take Photo with Camera", "Choose from Gallery", "View Full Photo", "Remove Photo"};
        } else {
            options = new String[]{"Take Photo with Camera", "Choose from Gallery"};
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndLaunch();
                    } else if (which == 1) {
                        pickImageLauncher.launch("image/*");
                    } else if (which == 2 && hasExisting) {
                        PhotoViewerHelper.showProfilePhotoViewer(
                                requireContext(),
                                currentProfile.getProfileImagePath(),
                                currentProfile.getName(),
                                this::removeProfilePhoto
                        );
                    } else if (which == 3 && hasExisting) {
                        removeProfilePhoto();
                    }
                })
                .show();
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        try {
            File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (storageDir == null) {
                storageDir = new File(requireContext().getFilesDir(), "profile_photos");
            }
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            pendingCameraFile = File.createTempFile("PROFILE_" + timeStamp + "_", ".jpg", storageDir);

            pendingCameraUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    pendingCameraFile
            );

            takePictureLauncher.launch(pendingCameraUri);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Unable to prepare camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleGalleryImage(@NonNull Uri uri) {
        try {
            File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (storageDir == null) {
                storageDir = new File(requireContext().getFilesDir(), "profile_photos");
            }
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File destinationFile = new File(storageDir, "PROFILE_GALLERY_" + timeStamp + ".jpg");

            InputStream in = requireContext().getContentResolver().openInputStream(uri);
            if (in != null) {
                FileOutputStream out = new FileOutputStream(destinationFile);
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                in.close();
                out.flush();
                out.close();

                ImageRotationHelper.fixPhotoVerticalOrientation(destinationFile);
                saveProfilePhotoPath(destinationFile.getAbsolutePath());
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error importing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProfilePhotoPath(String path) {
        if (currentProfile != null) {
            currentProfile.setProfileImagePath(path);
            dbHelper.saveOrUpdateUserProfile(currentProfile, false);
            loadProfilePhotoUI();
            Toast.makeText(requireContext(), "Profile photo updated vertically!", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeProfilePhoto() {
        if (currentProfile != null) {
            String existingPath = currentProfile.getProfileImagePath();
            if (existingPath != null) {
                try {
                    File f = new File(existingPath);
                    if (f.exists()) f.delete();
                } catch (Exception ignored) {}
            }
            currentProfile.setProfileImagePath(null);
            dbHelper.saveOrUpdateUserProfile(currentProfile, false);
            loadProfilePhotoUI();
            Toast.makeText(requireContext(), "Profile photo removed", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupAccountSession() {
        String identifier = AuthManager.getUserEmail(requireContext());
        binding.tvUserAccountEmail.setText(identifier.isEmpty() ? "Personal Athlete Account" : identifier);

        // Separate Log Out action
        binding.btnSignOut.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to log out of your account?")
                    .setPositiveButton("Log Out", (dialog, which) -> {
                        AuthManager.logout(requireContext());
                        Intent intent = new Intent(requireContext(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Distinct Delete Account action with confirmation dialog
        binding.btnDeleteAccount.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Account Permanently?")
                    .setMessage("Are you sure you want to permanently delete your account (" + (identifier.isEmpty() ? "current user" : identifier) + ")?\n\nThis will permanently delete all your workout logs, logged exercises, progress photos, and biometric stats from this device. This action cannot be undone.")
                    .setPositiveButton("Delete Permanently", (dialog, which) -> {
                        dbHelper.deleteUserAccountAndData(identifier);
                        AuthManager.clearAll(requireContext());
                        Toast.makeText(requireContext(), "Account and personal fitness data deleted.", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(requireContext(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void setupLiveBmiCalculation() {
        TextWatcher bmiWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateLiveBmi();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        binding.etProfileHeight.addTextChangedListener(bmiWatcher);
        binding.etProfileWeight.addTextChangedListener(bmiWatcher);
    }

    private void calculateLiveBmi() {
        try {
            String hStr = binding.etProfileHeight.getText().toString().trim();
            String wStr = binding.etProfileWeight.getText().toString().trim();

            if (!hStr.isEmpty() && !wStr.isEmpty()) {
                double height = Double.parseDouble(hStr);
                double weight = Double.parseDouble(wStr);
                updateBmiPreview(height, weight);
            }
        } catch (NumberFormatException ignored) {}
    }

    private void updateBmiPreview(double heightCm, double weightKg) {
        if (heightCm <= 0 || weightKg <= 0 || getContext() == null) return;

        double heightM = heightCm / 100.0;
        double bmi = weightKg / (heightM * heightM);

        String category;
        int colorRes;
        String advice;

        if (bmi < 18.5) {
            category = "Underweight";
            colorRes = R.color.bmi_underweight_color;
            advice = "Your BMI is under 18.5. Consider a modest caloric surplus with strength training.";
        } else if (bmi < 25.0) {
            category = "Normal";
            colorRes = R.color.bmi_normal_color;
            advice = "Your BMI is in the healthy zone (18.5 – 24.9). Ideal for athletic performance.";
        } else if (bmi < 30.0) {
            category = "Overweight";
            colorRes = R.color.bmi_overweight_color;
            advice = "Your BMI is 25.0 – 29.9. For lifters with high muscle mass, BMI may skew higher.";
        } else {
            category = "Obese";
            colorRes = R.color.bmi_obese_color;
            advice = "Your BMI is 30+. Prioritize progressive cardiovascular conditioning and caloric balance.";
        }

        binding.tvProfileBmiBadge.setText(String.format(Locale.getDefault(), "%s (%.1f)", category, bmi));
        binding.tvProfileBmiBadge.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
        binding.tvProfileBmiDesc.setText(advice);
    }

    private void setupSaveButton() {
        binding.btnSaveProfile.setOnClickListener(v -> {
            String name = binding.etProfileName.getText().toString().trim();
            String heightStr = binding.etProfileHeight.getText().toString().trim();
            String weightStr = binding.etProfileWeight.getText().toString().trim();
            String targetStr = binding.etProfileTargetWeight.getText().toString().trim();

            if (name.isEmpty() || heightStr.isEmpty() || weightStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in your name, height, and weight", Toast.LENGTH_SHORT).show();
                return;
            }

            double height;
            double weight;
            double targetWeight;
            try {
                height = Double.parseDouble(heightStr.replace(',', '.'));
                weight = Double.parseDouble(weightStr.replace(',', '.'));
                targetWeight = targetStr.isEmpty() ? weight : Double.parseDouble(targetStr.replace(',', '.'));
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Please enter valid numeric values for height and weight", Toast.LENGTH_SHORT).show();
                return;
            }

            currentProfile.setName(name);
            currentProfile.setHeightCm(height);
            currentProfile.setCurrentWeightKg(weight);
            currentProfile.setTargetWeightKg(targetWeight);

            dbHelper.saveOrUpdateUserProfile(currentProfile, true);
            AuthManager.setLoggedIn(requireContext(), true, AuthManager.getUserEmail(requireContext()), name);
            loadProfilePhotoUI();
            Toast.makeText(requireContext(), "Profile biometrics updated successfully!", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
