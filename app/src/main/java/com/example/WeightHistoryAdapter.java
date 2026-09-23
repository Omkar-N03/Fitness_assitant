package com.example;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.databinding.ItemWeightHistoryBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView Adapter for displaying recorded weight entries.
 */
public class WeightHistoryAdapter extends RecyclerView.Adapter<WeightHistoryAdapter.WeightViewHolder> {

    public interface OnWeightDeleteListener {
        void onDelete(WeightHistory entry);
    }

    private final List<WeightHistory> weightList = new ArrayList<>();
    private final OnWeightDeleteListener deleteListener;

    public WeightHistoryAdapter() {
        this.deleteListener = null;
    }

    public WeightHistoryAdapter(OnWeightDeleteListener listener) {
        this.deleteListener = listener;
    }

    public WeightHistoryAdapter(android.content.Context context, OnWeightDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setWeights(List<? extends WeightHistory> list) {
        weightList.clear();
        if (list != null) {
            weightList.addAll(list);
        }
        notifyDataSetChanged();
    }

    public void setEntries(List<? extends WeightHistory> list) {
        setWeights(list);
    }

    @NonNull
    @Override
    public WeightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWeightHistoryBinding binding = ItemWeightHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new WeightViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull WeightViewHolder holder, int position) {
        holder.bind(weightList.get(position));
    }

    @Override
    public int getItemCount() {
        return weightList.size();
    }

    class WeightViewHolder extends RecyclerView.ViewHolder {
        private final ItemWeightHistoryBinding binding;

        WeightViewHolder(ItemWeightHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(WeightHistory item) {
            binding.tvWeightDate.setText(item.getDate());
            binding.tvWeightValue.setText(String.format(Locale.US, "%.1f kg", item.getWeight()));
            if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
                binding.tvWeightNotes.setVisibility(View.VISIBLE);
                binding.tvWeightNotes.setText(item.getNotes());
            } else {
                binding.tvWeightNotes.setVisibility(View.GONE);
            }

            binding.btnDeleteWeight.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(item);
                }
            });
        }
    }
}
