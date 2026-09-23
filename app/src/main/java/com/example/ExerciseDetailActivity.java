package com.example;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.databinding.ActivityExerciseDetailBinding;
import com.example.databinding.DialogLogWorkoutBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ExerciseDetailActivity showing exercise instructions, body part, equipment, difficulty,
 * user's personal best (queried via SQL subquery), and rich in-app YouTube video demonstration.
 */
public class ExerciseDetailActivity extends AppCompatActivity {

    private ActivityExerciseDetailBinding binding;
    private GymDatabaseHelper dbHelper;
    private Exercise currentExercise;
    private long exerciseId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExerciseDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = GymDatabaseHelper.getInstance(this);
        exerciseId = getIntent().getLongExtra("exercise_id", 1);

        currentExercise = dbHelper.getExerciseById(exerciseId);
        if (currentExercise == null) {
            Toast.makeText(this, "Exercise not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        populateExerciseData();
        setupActions();
    }

    private void setupToolbar() {
        binding.toolbarDetail.setTitle(currentExercise.getTitle());
        binding.toolbarDetail.setNavigationOnClickListener(v -> finish());
    }

    private void populateExerciseData() {
        binding.tvDetailTitle.setText(currentExercise.getTitle());
        binding.tvDetailBodyPart.setText(currentExercise.getBodyPart());
        binding.tvDetailEquipment.setText(currentExercise.getEquipment());
        binding.tvDetailLevel.setText(currentExercise.getLevel());
        binding.tvDetailDesc.setText(currentExercise.getDescription());

        if (currentExercise.isExactMatch()) {
            binding.tvDetailExactBadge.setVisibility(View.VISIBLE);
        } else {
            binding.tvDetailExactBadge.setVisibility(View.GONE);
        }

        // YouTube Video Info
        if (currentExercise.getVideoTitle() != null && !currentExercise.getVideoTitle().isEmpty()) {
            binding.tvVideoTitle.setText(currentExercise.getVideoTitle());
        } else {
            binding.tvVideoTitle.setText(currentExercise.getTitle() + " Form Tutorial");
        }

        if (currentExercise.getYoutubeChannel() != null && !currentExercise.getYoutubeChannel().isEmpty()) {
            binding.tvYoutubeChannel.setText("Instructor: " + currentExercise.getYoutubeChannel());
        } else {
            binding.tvYoutubeChannel.setText("Certified Trainer Video Demonstration");
        }

        // Check Personal Best for this exercise using SQL subquery
        loadPersonalBest();
    }

    private void loadPersonalBest() {
        double maxWeight = dbHelper.getPersonalBestForExercise(exerciseId);
        if (maxWeight > 0) {
            binding.tvDetailPb.setText(String.format(Locale.getDefault(), "%.1f kg (All-Time Record)", maxWeight));
        } else {
            binding.tvDetailPb.setText("No recorded lifts yet for this exercise");
        }
    }

    private void setupActions() {
        String link = currentExercise.getYoutubeLink();
        String videoId = VideoHelper.extractVideoId(link);

        View.OnClickListener playVideoListener = v -> {
            if (videoId != null) {
                binding.flVideoPlaceholder.setVisibility(View.GONE);
                binding.wvDetailVideo.setVisibility(View.VISIBLE);
                VideoHelper.setupVideoWebView(binding.wvDetailVideo, binding.pbDetailVideoLoading, videoId);
                binding.btnPlayInAppVideo.setText("Watch in Pop-up Player");
                binding.btnPlayInAppVideo.setOnClickListener(v2 -> {
                    VideoHelper.showInAppVideoDialog(ExerciseDetailActivity.this,
                            link,
                            currentExercise.getTitle(),
                            currentExercise.getYoutubeChannel());
                });
            } else if (link != null && !link.isEmpty()) {
                VideoHelper.openExternalVideo(ExerciseDetailActivity.this, link, currentExercise.getTitle(), currentExercise.getYoutubeChannel());
            } else {
                Toast.makeText(ExerciseDetailActivity.this, "No video link available for this exercise", Toast.LENGTH_SHORT).show();
            }
        };

        binding.flVideoPlaceholder.setOnClickListener(playVideoListener);
        binding.btnPlayInAppVideo.setOnClickListener(playVideoListener);

        // Open in external YouTube App or Browser (with safe fallback to in-app player)
        binding.btnOpenYoutube.setOnClickListener(v -> {
            if (link != null && !link.isEmpty()) {
                VideoHelper.openExternalVideo(ExerciseDetailActivity.this,
                        link,
                        currentExercise.getTitle(),
                        currentExercise.getYoutubeChannel());
            } else {
                Toast.makeText(this, "No video link available for this exercise", Toast.LENGTH_SHORT).show();
            }
        });

        // Quick Log Workout for this Exercise
        binding.btnLogThisExercise.setOnClickListener(v -> showLogDialog());

        // Favorite / Like Exercise Button
        updateFavoriteButtonState();
        binding.btnDetailFavorite.setOnClickListener(v -> {
            boolean newState = !currentExercise.isFavorite();
            currentExercise.setFavorite(newState);
            dbHelper.setExerciseFavorite(currentExercise.getId(), newState);
            updateFavoriteButtonState();
            if (newState) {
                // Exercise liked: Open prompt to add to previous existing workout or create a new split
                showLogDialog();
            }
        });
    }

    private void updateFavoriteButtonState() {
        if (currentExercise.isFavorite()) {
            binding.btnDetailFavorite.setImageResource(R.drawable.ic_favorite_filled);
            binding.btnDetailFavorite.setImageTintList(null);
        } else {
            binding.btnDetailFavorite.setImageResource(R.drawable.ic_favorite_border);
            binding.btnDetailFavorite.setImageTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.text_secondary)));
        }
    }

    private void showLogDialog() {
        DialogLogWorkoutBinding dialogBinding = DialogLogWorkoutBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }

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
        String bodyPart = currentExercise.getBodyPart() != null ? currentExercise.getBodyPart().toLowerCase(Locale.ROOT) : "";
        if (bodyPart.contains("chest") || bodyPart.contains("shoulder") || bodyPart.contains("tricep")) {
            defaultSplit = "Push Day (Chest, Shoulders, Triceps)";
        } else if (bodyPart.contains("back") || bodyPart.contains("bicep") || bodyPart.contains("trap")) {
            defaultSplit = "Pull Day (Back, Biceps, Traps)";
        } else if (bodyPart.contains("leg") || bodyPart.contains("quad") || bodyPart.contains("calf") || bodyPart.contains("glute")) {
            defaultSplit = "Leg Day (Quads, Hamstrings, Calves)";
        }

        ArrayAdapter<String> splitAdapter = new ArrayAdapter<>(this,
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
        DatePickerUtil.attachDatePicker(this, dialogBinding.etDate);

        // 3. Setup exercise list for autocomplete
        List<Exercise> allExercises = dbHelper.getAllExercises();
        List<String> titles = new ArrayList<>();
        for (Exercise ex : allExercises) {
            titles.add(ex.getTitle());
        }
        ArrayAdapter<String> exerciseAdapter = new ArrayAdapter<>(this,
                R.layout.item_dropdown_entry, titles);
        dialogBinding.actvExercise.setAdapter(exerciseAdapter);

        // 4. Track selected exercises, pre-adding currentExercise
        List<Exercise> selectedExercises = new ArrayList<>();
        selectedExercises.add(currentExercise);

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
                        com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(ExerciseDetailActivity.this);
                        chip.setText(ex.getTitle());
                        chip.setCloseIconVisible(true);
                        chip.setChipBackgroundColorResource(R.color.surface_item);
                        chip.setTextColor(ContextCompat.getColor(ExerciseDetailActivity.this, R.color.text_primary));
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
                Toast.makeText(this, "Please select or enter an exercise name", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "'" + target.getTitle() + "' is already in this split", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Please add at least one exercise to your split", Toast.LENGTH_SHORT).show();
                return;
            }

            String notes = dialogBinding.etNotes.getText() != null ? dialogBinding.etNotes.getText().toString().trim() : "";
            String chosenDate = dialogBinding.etDate.getText() != null ? dialogBinding.etDate.getText().toString().trim() : "";
            if (chosenDate.isEmpty()) {
                chosenDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            }

            long insertedId = dbHelper.logWorkoutSession(splitName, chosenDate, selectedExercises, notes);
            if (insertedId > 0) {
                Toast.makeText(this, splitName + " logged successfully for " + chosenDate + "!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Failed to save workout session", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.setOnDismissListener(d -> {
            View focus = dialog.getCurrentFocus();
            if (focus != null) {
                focus.clearFocus();
                android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
                }
            }
        });

        dialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (binding != null && binding.wvDetailVideo != null) {
                binding.wvDetailVideo.onPause();
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        try {
            if (binding != null && binding.wvDetailVideo != null) {
                binding.wvDetailVideo.stopLoading();
                binding.wvDetailVideo.destroy();
            }
        } catch (Exception ignored) {}
        super.onDestroy();
    }
}
