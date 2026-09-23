package com.example;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.databinding.ItemWorkoutSummaryBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for displaying workout split sessions and their constituent exercises.
 */
public class WorkoutSummaryAdapter extends RecyclerView.Adapter<WorkoutSummaryAdapter.SummaryViewHolder> {

    public interface OnWorkoutSummaryActionListener {
        void onEditSplit(WorkoutSummary summary);
        void onDeleteSplit(WorkoutSummary summary);
    }

    private final List<WorkoutSummary> summaryList = new ArrayList<>();
    private OnWorkoutSummaryActionListener actionListener;

    public WorkoutSummaryAdapter() {
    }

    public WorkoutSummaryAdapter(android.content.Context context) {
    }

    public void setOnActionListener(OnWorkoutSummaryActionListener listener) {
        this.actionListener = listener;
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
        holder.bind(summaryList.get(position), actionListener);
    }

    @Override
    public int getItemCount() {
        return summaryList.size();
    }

    static class SummaryViewHolder extends RecyclerView.ViewHolder {
        private final ItemWorkoutSummaryBinding binding;

        SummaryViewHolder(ItemWorkoutSummaryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(WorkoutSummary summary, OnWorkoutSummaryActionListener listener) {
            binding.tvSplitName.setText(summary.getSplitName());
            binding.tvSummaryDate.setText(summary.getWorkoutDate());
            binding.tvExercisesCount.setText(summary.getExercisesCount() + (summary.getExercisesCount() == 1 ? " Exercise" : " Exercises"));

            // Format exercises cleanly with bullet points
            String names = summary.getExerciseNames();
            if (names != null && !names.trim().isEmpty()) {
                String[] parts = names.split(",");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < parts.length; i++) {
                    String trimmed = parts[i].trim();
                    if (!trimmed.isEmpty()) {
                        sb.append("• ").append(trimmed);
                        if (i < parts.length - 1) {
                            sb.append("\n");
                        }
                    }
                }
                binding.tvExercisesList.setText(sb.toString());
            } else {
                binding.tvExercisesList.setText("• Workout Session Completed");
            }

            // Notes display
            String notes = summary.getNotes();
            if (notes != null && !notes.trim().isEmpty()) {
                binding.tvSummaryNotes.setText("Notes: " + notes.trim());
                binding.tvSummaryNotes.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.tvSummaryNotes.setVisibility(android.view.View.GONE);
            }

            // Action button handlers
            if (listener != null) {
                binding.btnEditSplit.setOnClickListener(v -> listener.onEditSplit(summary));
                binding.btnDeleteSplit.setOnClickListener(v -> listener.onDeleteSplit(summary));
                binding.getRoot().setOnClickListener(v -> listener.onEditSplit(summary));
            } else {
                binding.btnEditSplit.setOnClickListener(null);
                binding.btnDeleteSplit.setOnClickListener(null);
                binding.getRoot().setOnClickListener(null);
            }
        }
    }
}
