package com.example;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.databinding.ItemProgressPhotoBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for displaying physique progress photos.
 */
public class ProgressPhotoAdapter extends RecyclerView.Adapter<ProgressPhotoAdapter.PhotoViewHolder> {

    public interface OnPhotoClickListener {
        void onPhotoClick(ProgressPhoto photo);
    }

    public interface OnPhotoActionListener {
        void onPhotoView(ProgressPhoto photo);
        void onPhotoDelete(ProgressPhoto photo);
    }

    private final List<ProgressPhoto> photoList = new ArrayList<>();
    private OnPhotoClickListener clickListener;
    private OnPhotoActionListener actionListener;

    public ProgressPhotoAdapter(OnPhotoClickListener listener) {
        this.clickListener = listener;
    }

    public ProgressPhotoAdapter(android.content.Context context, OnPhotoClickListener listener) {
        this.clickListener = listener;
    }

    public ProgressPhotoAdapter(android.content.Context context, OnPhotoActionListener listener) {
        this.actionListener = listener;
    }

    public void setActionListener(OnPhotoActionListener listener) {
        this.actionListener = listener;
    }

    public void setPhotos(List<ProgressPhoto> photos) {
        photoList.clear();
        if (photos != null) {
            photoList.addAll(photos);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProgressPhotoBinding binding = ItemProgressPhotoBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new PhotoViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        holder.bind(photoList.get(position));
    }

    @Override
    public int getItemCount() {
        return photoList.size();
    }

    class PhotoViewHolder extends RecyclerView.ViewHolder {
        private final ItemProgressPhotoBinding binding;

        PhotoViewHolder(ItemProgressPhotoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ProgressPhoto photo) {
            binding.tvPhotoDate.setText(photo.getDate());
            if (photo.getWeight() > 0) {
                binding.tvPhotoWeight.setVisibility(View.VISIBLE);
                binding.tvPhotoWeight.setText(String.format(java.util.Locale.US, "%.1f kg", photo.getWeight()));
            } else {
                binding.tvPhotoWeight.setVisibility(View.GONE);
            }

            if (photo.getNotes() != null && !photo.getNotes().trim().isEmpty()) {
                binding.tvPhotoNotes.setVisibility(View.VISIBLE);
                binding.tvPhotoNotes.setText(photo.getNotes());
            } else {
                binding.tvPhotoNotes.setVisibility(View.GONE);
            }

            // High quality oriented vertical thumbnail loading
            String path = photo.getImagePath();
            if (path != null && !path.isEmpty()) {
                File file = new File(path);
                if (file.exists()) {
                    Bitmap thumbnail = ImageRotationHelper.loadOrientedBitmap(file.getAbsolutePath(), 600, 600);
                    if (thumbnail != null) {
                        binding.ivProgressPhoto.setImageBitmap(thumbnail);
                    } else {
                        binding.ivProgressPhoto.setImageResource(R.drawable.ic_camera);
                    }
                } else {
                    binding.ivProgressPhoto.setImageResource(R.drawable.ic_camera);
                }
            } else {
                binding.ivProgressPhoto.setImageResource(R.drawable.ic_camera);
            }

            // Tapping on photo card opens the interactive full-screen photo viewer!
            View.OnClickListener viewListener = v -> {
                if (actionListener != null) {
                    actionListener.onPhotoView(photo);
                } else if (clickListener != null) {
                    clickListener.onPhotoClick(photo);
                }
            };
            binding.getRoot().setOnClickListener(viewListener);
            binding.ivProgressPhoto.setOnClickListener(viewListener);

            // Tapping on trash icon initiates delete confirmation
            binding.btnDeletePhoto.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onPhotoDelete(photo);
                } else if (clickListener != null) {
                    clickListener.onPhotoClick(photo);
                }
            });
        }
    }
}
