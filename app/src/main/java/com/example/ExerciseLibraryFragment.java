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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
        exerciseAdapter.setOnFavoriteClickListener((exercise, isLiked) -> {
            dbHelper.setExerciseFavorite(exercise.getId(), isLiked);
            if ("★ Favorites".equalsIgnoreCase(selectedBodyPart) && !isLiked) {
                filterExercises();
            } else if (isLiked) {
                // When an exercise is liked, offer to add to previous workout or create a new split
                showAddToSplitDialog(exercise);
            }
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

        // Ensure dataset is loaded and valid
        if (dbHelper.getExerciseCount() == 0 || dbHelper.hasCorruptExerciseData()) {
            CsvLoaderUtil.loadExercisesIfEmpty(requireContext(), dbHelper);
        }

        setupFilterChips();
        filterExercises();
    }

    @Override
    public void onResume() {
        super.onResume();
        filterExercises();
    }

    private void setupFilterChips() {
        binding.chipGroupBodyParts.removeAllViews();

        // Always add "All" chip first
        addChip("All", true);

        // Add "★ Favorites" filter chip
        addChip("★ Favorites", false);

        // Add "★ Exact Videos" chip right after "All" so the exact video demonstrations view is immediately accessible
        addChip("★ Exact Videos", false);

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

        if ("★ Exact Videos".equalsIgnoreCase(selectedBodyPart)) {
            binding.tvExerciseCountBadge.setText(String.format(Locale.getDefault(), "%d Exact Guides", results.size()));
        } else if ("★ Favorites".equalsIgnoreCase(selectedBodyPart) || "Favorites".equalsIgnoreCase(selectedBodyPart)) {
            binding.tvExerciseCountBadge.setText(String.format(Locale.getDefault(), "%d Favorites", results.size()));
        } else {
            binding.tvExerciseCountBadge.setText(String.format(Locale.getDefault(), "%d Exercises", results.size()));
        }

        if (results.isEmpty()) {
            binding.tvEmptySearch.setVisibility(View.VISIBLE);
            binding.rvExercises.setVisibility(View.GONE);
        } else {
            binding.tvEmptySearch.setVisibility(View.GONE);
            binding.rvExercises.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Dialog allowing user to add a liked exercise to a previous existing workout split or create a new split with a date picker.
     */
    private void showAddToSplitDialog(@NonNull Exercise exercise) {
        if (getContext() == null) return;

        com.example.databinding.DialogLogWorkoutBinding dialogBinding =
                com.example.databinding.DialogLogWorkoutBinding.inflate(getLayoutInflater());
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(requireContext())
                .setView(dialogBinding.getRoot())
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }

        dialogBinding.tvDialogTitle.setText("Add Exercise to Split");
        dialogBinding.tvDialogSubtitle.setText("Add '" + exercise.getTitle() + "' to a previous workout or create a new split.");

        // 1. Populate workout split presets combined with user's previous existing splits
        List<String> combinedSplits = new ArrayList<>();
        List<String> previousUserSplits = dbHelper.getDistinctWorkoutSplitNames();
        for (String prev : previousUserSplits) {
            if (!combinedSplits.contains(prev)) {
                combinedSplits.add(prev);
            }
        }

        String[] splitPresets = new String[]{
                "Push Day (Chest, Shoulders, Triceps)",
                "Pull Day (Back, Biceps, Traps)",
                "Leg Day (Quads, Hamstrings, Calves)",
                "Upper Body Split",
                "Lower Body Split",
                "Full Body Routine",
                "Cardio & Core Session"
        };
        for (String preset : splitPresets) {
            if (!combinedSplits.contains(preset)) {
                combinedSplits.add(preset);
            }
        }

        String defaultSplit = combinedSplits.isEmpty() ? "Full Body Routine" : combinedSplits.get(0);
        String bodyPart = exercise.getBodyPart() != null ? exercise.getBodyPart().toLowerCase(Locale.ROOT) : "";
        if (bodyPart.contains("chest") || bodyPart.contains("shoulder") || bodyPart.contains("tricep")) {
            defaultSplit = "Push Day (Chest, Shoulders, Triceps)";
        } else if (bodyPart.contains("back") || bodyPart.contains("bicep") || bodyPart.contains("trap")) {
            defaultSplit = "Pull Day (Back, Biceps, Traps)";
        } else if (bodyPart.contains("leg") || bodyPart.contains("quad") || bodyPart.contains("calf") || bodyPart.contains("glute")) {
            defaultSplit = "Leg Day (Quads, Hamstrings, Calves)";
        }

        android.widget.ArrayAdapter<String> splitAdapter = new android.widget.ArrayAdapter<>(requireContext(),
                R.layout.item_dropdown_entry, combinedSplits);
        dialogBinding.actvSplit.setAdapter(splitAdapter);
        dialogBinding.actvSplit.setText(defaultSplit, false);

        int maxDropHeight = (int) (160 * getResources().getDisplayMetrics().density);
        dialogBinding.actvSplit.setDropDownHeight(maxDropHeight);
        dialogBinding.actvExercise.setDropDownHeight(maxDropHeight);
        dialogBinding.actvExercise.setThreshold(1);

        dialogBinding.actvSplit.setOnItemClickListener((parent, view, position, id) -> dialogBinding.actvSplit.dismissDropDown());
        dialogBinding.actvExercise.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            dialogBinding.actvExercise.setText(selected, false);
            dialogBinding.actvExercise.dismissDropDown();
        });

        // 2. Setup date with interactive DatePickerDialog
        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        dialogBinding.etDate.setText(todayStr);
        DatePickerUtil.attachDatePicker(requireContext(), dialogBinding.etDate);

        // 3. Setup exercise list for autocomplete
        List<Exercise> allExercises = dbHelper.getAllExercises();
        List<String> titles = new ArrayList<>();
        for (Exercise ex : allExercises) {
            titles.add(ex.getTitle());
        }
        android.widget.ArrayAdapter<String> exerciseAdapter = new android.widget.ArrayAdapter<>(requireContext(),
                R.layout.item_dropdown_entry, titles);
        dialogBinding.actvExercise.setAdapter(exerciseAdapter);

        // 4. Track selected exercises, pre-adding the current liked exercise
        List<Exercise> selectedExercises = new ArrayList<>();
        selectedExercises.add(exercise);

        Runnable updateChips = new Runnable() {
            @Override
            public void run() {
                dialogBinding.chipGroupExercises.removeAllViews();
                if (selectedExercises.isEmpty()) {
                    dialogBinding.tvNoExercisesHint.setVisibility(View.VISIBLE);
                    dialogBinding.tvAddedExercisesCount.setText("Exercises Added (0)");
                } else {
                    dialogBinding.tvNoExercisesHint.setVisibility(View.GONE);
                    dialogBinding.tvAddedExercisesCount.setText("Exercises Added (" + selectedExercises.size() + ")");
                    for (Exercise ex : selectedExercises) {
                        Chip chip = new Chip(requireContext());
                        chip.setText(ex.getTitle());
                        chip.setCloseIconVisible(true);
                        chip.setChipBackgroundColorResource(R.color.surface_item);
                        chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
                        chip.setCloseIconTintResource(R.color.primary);
                        chip.setOnCloseIconClickListener(v -> {
                            selectedExercises.remove(ex);
                            run();
                        });
                        dialogBinding.chipGroupExercises.addView(chip);
                    }
                }
            }
        };

        updateChips.run();

        // Add Exercise button
        dialogBinding.btnAddExerciseToSplit.setOnClickListener(v -> {
            dialogBinding.actvExercise.dismissDropDown();
            String typed = dialogBinding.actvExercise.getText().toString().trim();
            if (typed.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "Please select or enter an exercise name", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            Exercise target = null;
            for (Exercise ex : allExercises) {
                if (ex.getTitle().equalsIgnoreCase(typed)) {
                    target = ex;
                    break;
                }
            }
            if (target == null) {
                target = new Exercise(0, typed, "Full Body", "Gym Equipment", "Intermediate", "Custom logged exercise", "");
                long newId = dbHelper.insertExercise(target);
                target.setId(newId);
                allExercises.add(target);
                titles.add(typed);
                exerciseAdapter.notifyDataSetChanged();
            }

            boolean alreadyAdded = false;
            for (Exercise existing : selectedExercises) {
                if (existing.getId() == target.getId() || existing.getTitle().equalsIgnoreCase(target.getTitle())) {
                    alreadyAdded = true;
                    break;
                }
            }

            if (alreadyAdded) {
                android.widget.Toast.makeText(requireContext(), "'" + target.getTitle() + "' is already in this split", android.widget.Toast.LENGTH_SHORT).show();
            } else {
                selectedExercises.add(target);
                dialogBinding.actvExercise.setText("");
                updateChips.run();
            }
        });

        dialogBinding.btnCancelWorkout.setOnClickListener(v -> dialog.dismiss());

        dialogBinding.btnSaveWorkout.setOnClickListener(v -> {
            String splitName = dialogBinding.actvSplit.getText().toString().trim();
            if (splitName.isEmpty()) {
                splitName = "Workout Split";
            }

            if (selectedExercises.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "Please add at least one exercise to your split", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            String notes = dialogBinding.etNotes.getText() != null ? dialogBinding.etNotes.getText().toString().trim() : "";
            String chosenDate = dialogBinding.etDate.getText() != null ? dialogBinding.etDate.getText().toString().trim() : "";
            if (chosenDate.isEmpty()) {
                chosenDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            }

            long insertedId = dbHelper.logWorkoutSession(splitName, chosenDate, selectedExercises, notes);
            if (insertedId > 0) {
                android.widget.Toast.makeText(requireContext(), splitName + " logged successfully for " + chosenDate + "!", android.widget.Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                android.widget.Toast.makeText(requireContext(), "Failed to save workout session", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        dialog.setOnDismissListener(d -> {
            View focus = dialog.getCurrentFocus();
            if (focus != null && getContext() != null) {
                focus.clearFocus();
                android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
                }
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
