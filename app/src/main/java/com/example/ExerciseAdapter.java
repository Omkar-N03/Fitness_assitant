package com.example;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.databinding.ItemExerciseBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView Adapter for displaying exercises from the CSV dataset.
 */
public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder> {

    public interface OnExerciseClickListener {
        void onExerciseClick(Exercise exercise);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(Exercise exercise, boolean isLiked);
    }

    private final List<Exercise> exerciseList = new ArrayList<>();
    private final OnExerciseClickListener listener;
    private OnFavoriteClickListener favoriteListener;

    public ExerciseAdapter(OnExerciseClickListener listener) {
        this.listener = listener;
    }

    public ExerciseAdapter(Context context, OnExerciseClickListener listener) {
        this.listener = listener;
    }

    public void setOnFavoriteClickListener(OnFavoriteClickListener favoriteListener) {
        this.favoriteListener = favoriteListener;
    }

    public void setExercises(List<Exercise> exercises) {
        exerciseList.clear();
        if (exercises != null) {
            exerciseList.addAll(exercises);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemExerciseBinding binding = ItemExerciseBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ExerciseViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        holder.bind(exerciseList.get(position));
    }

    @Override
    public int getItemCount() {
        return exerciseList.size();
    }

    class ExerciseViewHolder extends RecyclerView.ViewHolder {
        private final ItemExerciseBinding binding;

        ExerciseViewHolder(ItemExerciseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Exercise exercise) {
            Context context = binding.getRoot().getContext();
            binding.tvExerciseTitle.setText(exercise.getTitle());
            binding.tvBodyPart.setText(exercise.getBodyPart());
            binding.tvEquipment.setText(exercise.getEquipment());
            binding.tvExerciseType.setText(exercise.getExerciseType());
            binding.tvRating.setText(String.format(java.util.Locale.getDefault(), "%.1f", exercise.getRating()));
            binding.tvExerciseDesc.setText(exercise.getDescription());

            // Exact Match Tutorial Badge
            if (exercise.isExactMatch()) {
                binding.tvExactBadge.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.tvExactBadge.setVisibility(android.view.View.GONE);
            }

            // Video information preview
            binding.tvVideoTitle.setText(exercise.getVideoTitle());
            String channel = exercise.getYoutubeChannel();
            if (channel != null && !channel.isEmpty()) {
                binding.tvVideoSubtitle.setText(channel + " • Tap to watch form");
            } else {
                binding.tvVideoSubtitle.setText("YouTube Form Tutorial • Tap to watch");
            }

            binding.chipLevel.setText(exercise.getDifficulty());

            // Color code difficulty
            String diff = exercise.getDifficulty() != null ? exercise.getDifficulty().toLowerCase(Locale.ROOT) : "";
            int chipBgColor;
            int textColor;
            if (diff.contains("begin")) {
                chipBgColor = ContextCompat.getColor(context, R.color.bmi_normal_color);
                textColor = Color.parseColor("#003A12");
            } else if (diff.contains("inter")) {
                chipBgColor = ContextCompat.getColor(context, R.color.bmi_underweight_color);
                textColor = Color.parseColor("#3E2723");
            } else {
                chipBgColor = ContextCompat.getColor(context, R.color.bmi_obese_color);
                textColor = Color.WHITE;
            }
            binding.chipLevel.setChipBackgroundColor(ColorStateList.valueOf(chipBgColor));
            binding.chipLevel.setTextColor(textColor);

            // Favorite / Like button state and click handling
            if (exercise.isFavorite()) {
                binding.ibFavorite.setImageResource(R.drawable.ic_favorite_filled);
                binding.ibFavorite.setImageTintList(null);
            } else {
                binding.ibFavorite.setImageResource(R.drawable.ic_favorite_border);
                binding.ibFavorite.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_secondary)));
            }

            binding.ibFavorite.setOnClickListener(v -> {
                boolean newState = !exercise.isFavorite();
                exercise.setFavorite(newState);
                if (newState) {
                    binding.ibFavorite.setImageResource(R.drawable.ic_favorite_filled);
                    binding.ibFavorite.setImageTintList(null);
                } else {
                    binding.ibFavorite.setImageResource(R.drawable.ic_favorite_border);
                    binding.ibFavorite.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_secondary)));
                }
                if (favoriteListener != null) {
                    favoriteListener.onFavoriteClick(exercise, newState);
                }
            });

            // Whole card opens detail view
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onExerciseClick(exercise);
                }
            });

            // Dedicated play button bar launches video pop-up directly
            android.view.View.OnClickListener playVideoListener = v -> {
                String link = exercise.getYoutubeLink();
                if (link != null && !link.trim().isEmpty()) {
                    VideoHelper.showInAppVideoDialog(context, link, exercise.getTitle(), exercise.getYoutubeChannel());
                } else if (listener != null) {
                    listener.onExerciseClick(exercise);
                }
            };

            binding.layoutPlayVideo.setOnClickListener(playVideoListener);
            binding.ivPlayVideo.setOnClickListener(playVideoListener);
            binding.tvWatchAction.setOnClickListener(playVideoListener);
        }
    }
}
