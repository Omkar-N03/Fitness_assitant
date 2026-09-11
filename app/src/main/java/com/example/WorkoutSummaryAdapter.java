package com.example;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.databinding.ItemWorkoutSummaryBinding;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for displaying aggregated workouts from the SQLite workout_summary VIEW.
 */
public class WorkoutSummaryAdapter extends RecyclerView.Adapter<WorkoutSummaryAdapter.SummaryViewHolder> {

    private final List<WorkoutSummary> summaryList = new ArrayList<>();
    private final DecimalFormat volumeFormat = new DecimalFormat("#,###");

    public WorkoutSummaryAdapter() {
    }

    public WorkoutSummaryAdapter(android.content.Context context) {
    }

    public void setSummaries(List<WorkoutSummary> list) {
        summaryList.clear();
        if (list != null) {
            summaryList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SummaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWorkoutSummaryBinding binding = ItemWorkoutSummaryBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new SummaryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SummaryViewHolder holder, int position) {
        holder.bind(summaryList.get(position));
    }

    @Override
    public int getItemCount() {
        return summaryList.size();
    }

    class SummaryViewHolder extends RecyclerView.ViewHolder {
        private final ItemWorkoutSummaryBinding binding;

        SummaryViewHolder(ItemWorkoutSummaryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(WorkoutSummary summary) {
            binding.tvSummaryDate.setText(summary.getWorkoutDate());
            binding.tvExercisesCount.setText(summary.getExercisesCount() + " Exercises");
            binding.tvVolumeValue.setText(volumeFormat.format(summary.getTotalVolume()) + " kg");
            binding.tvSetsRepsValue.setText(summary.getTotalSets() + " sets • " + summary.getTotalReps() + " reps");
            binding.tvExercisesList.setText(summary.getExerciseNames() != null ? summary.getExerciseNames() : "Gym Workout");
        }
    }
}
