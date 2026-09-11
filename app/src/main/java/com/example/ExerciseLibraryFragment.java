package com.example;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.databinding.FragmentExerciseLibraryBinding;
import com.google.android.material.chip.Chip;

import java.util.List;
import java.util.Locale;

/**
 * Exercise Library Fragment displaying 100 gym exercises loaded from gym_exercises_100_direct_youtube_videos_3.csv.
 * Features dynamic muscle group chip filtering and real-time search.
 */
public class ExerciseLibraryFragment extends Fragment {

    private FragmentExerciseLibraryBinding binding;
    private GymDatabaseHelper dbHelper;
    private ExerciseAdapter exerciseAdapter;
    private String selectedBodyPart = "All";
    private String currentQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentExerciseLibraryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = GymDatabaseHelper.getInstance(requireContext());

        // Setup RecyclerView
        exerciseAdapter = new ExerciseAdapter(requireContext(), exercise -> {
            Intent intent = new Intent(requireContext(), ExerciseDetailActivity.class);
            intent.putExtra("exercise_id", exercise.getId());
            startActivity(intent);
        });
        binding.rvExercises.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvExercises.setAdapter(exerciseAdapter);

        // Setup Search Listener
        binding.etSearchExercise.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString().trim();
                filterExercises();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        setupFilterChips();
        filterExercises();
    }

    private void setupFilterChips() {
        binding.chipGroupBodyParts.removeAllViews();

        // Always add "All" chip first
        addChip("All", true);

        // Dynamically query distinct body parts from SQLite (indexed on body_part)
        List<String> bodyParts = dbHelper.getDistinctBodyParts();
        for (String bp : bodyParts) {
            addChip(bp, false);
        }
    }

    private void addChip(String label, boolean isChecked) {
        Chip chip = new Chip(requireContext());
        chip.setText(label);
        chip.setCheckable(true);
        chip.setChecked(isChecked);
        chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        chip.setChipBackgroundColorResource(R.color.surface_card);

        chip.setOnCheckedChangeListener((buttonView, checked) -> {
            if (checked) {
                selectedBodyPart = label;
                filterExercises();
            }
        });

        binding.chipGroupBodyParts.addView(chip);
    }

    private void filterExercises() {
        if (dbHelper == null || binding == null) return;

        List<Exercise> results = dbHelper.searchExercises(currentQuery, selectedBodyPart);
        exerciseAdapter.setExercises(results);

        binding.tvExerciseCountBadge.setText(String.format(Locale.getDefault(), "%d Exercises", results.size()));

        if (results.isEmpty()) {
            binding.tvEmptySearch.setVisibility(View.VISIBLE);
            binding.rvExercises.setVisibility(View.GONE);
        } else {
            binding.tvEmptySearch.setVisibility(View.GONE);
            binding.rvExercises.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
