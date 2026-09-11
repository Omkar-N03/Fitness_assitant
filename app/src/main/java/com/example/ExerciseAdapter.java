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

/**
 * RecyclerView Adapter for displaying exercises from the CSV dataset.
 */
public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder> {

    public interface OnExerciseClickListener {
        void onExerciseClick(Exercise exercise);
    }

    private final List<Exercise> exerciseList = new ArrayList<>();
    private final OnExerciseClickListener listener;

    public ExerciseAdapter(OnExerciseClickListener listener) {
        this.listener = listener;
    }

    public ExerciseAdapter(Context context, OnExerciseClickListener listener) {
        this.listener = listener;
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
            binding.chipLevel.setText(exercise.getDifficulty());

            // Color code difficulty
            String diff = exercise.getDifficulty() != null ? exercise.getDifficulty().toLowerCase() : "";
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

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onExerciseClick(exercise);
                }
            });
        }
    }
}
