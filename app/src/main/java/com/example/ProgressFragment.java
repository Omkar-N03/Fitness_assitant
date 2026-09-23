package com.example;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.databinding.DialogAddWeightBinding;
import com.example.databinding.FragmentProgressBinding;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ProgressFragment displaying weight history (with SQLite Trigger updates)
 * and Camera Integration using modern ActivityResultLauncher API to capture physique progress photos.
 */
public class ProgressFragment extends Fragment {

    private FragmentProgressBinding binding;
    private GymDatabaseHelper dbHelper;
    private WeightHistoryAdapter weightAdapter;
    private ProgressPhotoAdapter photoAdapter;

    private Uri pendingPhotoUri;
    private File pendingPhotoFile;

    // Modern ActivityResultLauncher for taking photo using FileProvider
    private final ActivityResultLauncher<Uri> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            isSuccess -> {
                if (isSuccess && pendingPhotoFile != null && pendingPhotoFile.exists()) {
                    // Ensure photo is physically saved vertically / upright on disk
                    ImageRotationHelper.fixPhotoVerticalOrientation(pendingPhotoFile);
                    onPhotoCaptured(pendingPhotoFile.getAbsolutePath());
                } else {
                    Toast.makeText(getContext(), "Photo capture cancelled", Toast.LENGTH_SHORT).show();
                }
            }
    );

    // Permission launcher for Camera
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    launchCamera();
                } else {
                    Toast.makeText(getContext(), "Camera permission is required to capture progress photos", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProgressBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = GymDatabaseHelper.getInstance(requireContext());

        setupWeightHistoryRecycler();
        setupProgressPhotosRecycler();
        setupClickListeners();
        loadAllProgressData();
    }

    private void setupWeightHistoryRecycler() {
        weightAdapter = new WeightHistoryAdapter(requireContext(), entry -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Weigh-In")
                    .setMessage("Remove this weight entry from history?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        dbHelper.deleteWeightEntry(entry.getId());
                        loadAllProgressData();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        binding.rvWeightHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvWeightHistory.setAdapter(weightAdapter);
    }

    private void setupProgressPhotosRecycler() {
        photoAdapter = new ProgressPhotoAdapter(requireContext(), new ProgressPhotoAdapter.OnPhotoActionListener() {
            @Override
            public void onPhotoView(ProgressPhoto photo) {
                PhotoViewerHelper.showProgressPhotoViewer(requireContext(), photo, () -> deleteProgressPhoto(photo));
            }

            @Override
            public void onPhotoDelete(ProgressPhoto photo) {
                confirmDeleteProgressPhoto(photo);
            }
        });
        binding.rvProgressPhotos.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvProgressPhotos.setAdapter(photoAdapter);
    }

    private void confirmDeleteProgressPhoto(ProgressPhoto photo) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Progress Photo")
                .setMessage("Remove this photo entry from your physique journal?")
                .setPositiveButton("Delete", (dialog, which) -> deleteProgressPhoto(photo))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProgressPhoto(ProgressPhoto photo) {
        dbHelper.deleteProgressPhoto(photo.getId());
        try {
            File f = new File(photo.getImagePath());
            if (f.exists()) {
                f.delete();
            }
        } catch (Exception ignored) {}
        loadAllProgressData();
    }

    private void setupClickListeners() {
        binding.btnAddWeightEntry.setOnClickListener(v -> showAddWeightDialog());

        View.OnClickListener cameraClickListener = v -> checkCameraPermissionAndLaunch();
        binding.btnCapturePhotoHeader.setOnClickListener(cameraClickListener);
        binding.btnEmptyCapture.setOnClickListener(cameraClickListener);
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        try {
            File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (storageDir == null) {
                storageDir = new File(requireContext().getFilesDir(), "progress_photos");
            }
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            pendingPhotoFile = File.createTempFile("PROGRESS_" + timeStamp + "_", ".jpg", storageDir);

            pendingPhotoUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    pendingPhotoFile
            );

            takePictureLauncher.launch(pendingPhotoUri);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Error preparing image file", Toast.LENGTH_SHORT).show();
        }
    }

    private void onPhotoCaptured(String imagePath) {
        UserProfile userProfile = dbHelper.getPrimaryUserProfile();
        double currentWeight = userProfile != null ? userProfile.getCurrentWeightKg() : 70.0;
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        ProgressPhoto photo = new ProgressPhoto(0, 1, imagePath, today, currentWeight, "Physique check-in");
        long id = dbHelper.insertProgressPhoto(photo);

        if (id > 0) {
            Toast.makeText(requireContext(), "Progress photo saved to journal!", Toast.LENGTH_SHORT).show();
            loadAllProgressData();
        }
    }

    private void loadAllProgressData() {
        if (getContext() == null || binding == null) return;

        // Load Weight History
        List<WeightHistory> weightList = dbHelper.getWeightHistory(1);
        weightAdapter.setEntries(weightList);

        if (!weightList.isEmpty()) {
            binding.tvLatestWeightDisplay.setText(String.format(Locale.getDefault(), "%.1f kg", weightList.get(0).getWeight()));
        } else {
            UserProfile profile = dbHelper.getPrimaryUserProfile();
            if (profile != null) {
                binding.tvLatestWeightDisplay.setText(String.format(Locale.getDefault(), "%.1f kg", profile.getCurrentWeightKg()));
            }
        }

        // Load Progress Photos
        List<ProgressPhoto> photos = dbHelper.getAllProgressPhotos(1);
        photoAdapter.setPhotos(photos);
        binding.tvPhotoCountBadge.setText(String.format(Locale.getDefault(), "%d Photos", photos.size()));

        if (photos.isEmpty()) {
            binding.cardEmptyPhotos.setVisibility(View.VISIBLE);
            binding.rvProgressPhotos.setVisibility(View.GONE);
        } else {
            binding.cardEmptyPhotos.setVisibility(View.GONE);
            binding.rvProgressPhotos.setVisibility(View.VISIBLE);
        }
    }

    private void showAddWeightDialog() {
        DialogAddWeightBinding dialogBinding = DialogAddWeightBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogBinding.getRoot())
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        // Prepopulate with latest weight
        UserProfile profile = dbHelper.getPrimaryUserProfile();
        if (profile != null) {
            dialogBinding.etInputWeight.setText(String.valueOf(profile.getCurrentWeightKg()));
        }

        dialogBinding.btnCancelWeight.setOnClickListener(v -> dialog.dismiss());

        dialogBinding.btnSubmitWeight.setOnClickListener(v -> {
            String weightStr = dialogBinding.etInputWeight.getText().toString().trim();
            String notes = dialogBinding.etInputNotes.getText().toString().trim();

            if (weightStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your weight", Toast.LENGTH_SHORT).show();
                return;
            }

            double weight;
            try {
                weight = Double.parseDouble(weightStr.replace(',', '.'));
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Please enter a valid numeric weight", Toast.LENGTH_SHORT).show();
                return;
            }
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            WeightEntry entry = new WeightEntry(0, 1, weight, today, notes);
            // This INSERT automatically executes the SQLite Trigger `trg_update_user_current_weight`
            long insertedId = dbHelper.insertWeightEntry(entry);

            if (insertedId > 0) {
                Toast.makeText(requireContext(), "Weight entry saved successfully!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadAllProgressData();
            } else {
                Toast.makeText(requireContext(), "Failed to record weight", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
