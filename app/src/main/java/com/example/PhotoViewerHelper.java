package com.example;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.databinding.DialogViewPhotoBinding;

import java.io.File;

/**
 * Helper to display progress and profile photos in an interactive, vertical viewer dialog.
 */
public final class PhotoViewerHelper {

    private PhotoViewerHelper() {}

    public interface OnPhotoDeleteListener {
        void onDeletePhoto();
    }

    /**
     * Shows a progress photo in full-screen modal with orientation correction,
     * details, and delete option.
     */
    public static void showProgressPhotoViewer(@NonNull Context context,
                                               @NonNull ProgressPhoto photo,
                                               @Nullable OnPhotoDeleteListener deleteListener) {
        DialogViewPhotoBinding binding = DialogViewPhotoBinding.inflate(LayoutInflater.from(context));
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(binding.getRoot())
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        binding.tvViewPhotoDate.setText(photo.getDate());
        if (photo.getWeight() > 0) {
            binding.tvViewPhotoWeight.setVisibility(View.VISIBLE);
            binding.tvViewPhotoWeight.setText(String.format(java.util.Locale.US, "%.1f kg", photo.getWeight()));
        } else {
            binding.tvViewPhotoWeight.setVisibility(View.GONE);
        }

        if (photo.getNotes() != null && !photo.getNotes().trim().isEmpty()) {
            binding.tvViewPhotoNotes.setVisibility(View.VISIBLE);
            binding.tvViewPhotoNotes.setText(photo.getNotes());
        } else {
            binding.tvViewPhotoNotes.setVisibility(View.GONE);
        }

        // Load oriented bitmap
        binding.pbViewPhotoLoading.setVisibility(View.VISIBLE);
        Bitmap bitmap = ImageRotationHelper.loadOrientedBitmap(photo.getImagePath(), 1080, 1080);
        binding.pbViewPhotoLoading.setVisibility(View.GONE);

        if (bitmap != null) {
            binding.ivViewPhotoMain.setImageBitmap(bitmap);
        } else {
            binding.ivViewPhotoMain.setImageResource(R.drawable.ic_camera);
        }

        binding.ibViewPhotoClose.setOnClickListener(v -> dialog.dismiss());
        binding.btnViewPhotoClose.setOnClickListener(v -> dialog.dismiss());

        if (deleteListener != null) {
            binding.ibViewPhotoDelete.setVisibility(View.VISIBLE);
            binding.ibViewPhotoDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Delete Progress Photo")
                        .setMessage("Are you sure you want to permanently remove this physique check-in?")
                        .setPositiveButton("Delete", (d, w) -> {
                            deleteListener.onDeletePhoto();
                            dialog.dismiss();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        } else {
            binding.ibViewPhotoDelete.setVisibility(View.GONE);
        }

        dialog.show();
    }

    /**
     * Shows a profile avatar/photo in full-screen modal with orientation correction.
     */
    public static void showProfilePhotoViewer(@NonNull Context context,
                                              @NonNull String photoPath,
                                              @NonNull String userName,
                                              @Nullable OnPhotoDeleteListener deleteListener) {
        DialogViewPhotoBinding binding = DialogViewPhotoBinding.inflate(LayoutInflater.from(context));
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(binding.getRoot())
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        binding.tvViewPhotoDate.setText(userName);
        binding.tvViewPhotoWeight.setVisibility(View.VISIBLE);
        binding.tvViewPhotoWeight.setText("Profile Picture");
        binding.tvViewPhotoNotes.setVisibility(View.GONE);

        binding.pbViewPhotoLoading.setVisibility(View.VISIBLE);
        Bitmap bitmap = ImageRotationHelper.loadOrientedBitmap(photoPath, 1080, 1080);
        binding.pbViewPhotoLoading.setVisibility(View.GONE);

        if (bitmap != null) {
            binding.ivViewPhotoMain.setImageBitmap(bitmap);
        } else {
            binding.ivViewPhotoMain.setImageResource(R.drawable.ic_person);
        }

        binding.ibViewPhotoClose.setOnClickListener(v -> dialog.dismiss());
        binding.btnViewPhotoClose.setOnClickListener(v -> dialog.dismiss());

        if (deleteListener != null) {
            binding.ibViewPhotoDelete.setVisibility(View.VISIBLE);
            binding.ibViewPhotoDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Remove Profile Photo")
                        .setMessage("Remove your current profile photo and reset to default initials?")
                        .setPositiveButton("Remove", (d, w) -> {
                            deleteListener.onDeletePhoto();
                            dialog.dismiss();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        } else {
            binding.ibViewPhotoDelete.setVisibility(View.GONE);
        }

        dialog.show();
    }
}
