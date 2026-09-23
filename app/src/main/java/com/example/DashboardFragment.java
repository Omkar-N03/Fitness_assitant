package com.example;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.databinding.DialogEditProfileBinding;
import com.example.databinding.DialogLogWorkoutBinding;
import com.example.databinding.FragmentDashboardBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Dashboard Fragment displaying user greeting, live BMI indicator, Personal Best highlight,
 * and recent workout summary pulled directly from the SQLite VIEW (workout_summary).
 */
public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private GymDatabaseHelper dbHelper;
    private WorkoutSummaryAdapter summaryAdapter;
    private List<Exercise> exerciseList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = GymDatabaseHelper.getInstance(requireContext());

        // Setup RecyclerView for recent workout summaries
        summaryAdapter = new WorkoutSummaryAdapter(requireContext());
        summaryAdapter.setOnActionListener(new WorkoutSummaryAdapter.OnWorkoutSummaryActionListener() {
            @Override
            public void onEditSplit(WorkoutSummary summary) {
                showEditWorkoutDialog(summary);
            }

            @Override
            public void onDeleteSplit(WorkoutSummary summary) {
                showConfirmDeleteWorkoutDialog(summary);
            }
        });
        binding.rvRecentWorkouts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecentWorkouts.setAdapter(summaryAdapter);

        // Setup click listeners to edit profile & biometrics directly from Dashboard
        View.OnClickListener editProfileClickListener = v -> showEditProfileDialog();
        binding.llHeader.setOnClickListener(editProfileClickListener);
        binding.tvGymBadge.setOnClickListener(editProfileClickListener);
        binding.cardDashboardAvatar.setOnClickListener(editProfileClickListener);
        binding.cardWeightHero.setOnClickListener(editProfileClickListener);
        binding.cardBmi.setOnClickListener(editProfileClickListener);

        // Setup FAB and button click listeners
        binding.fabLogWorkout.setOnClickListener(v -> showLogWorkoutDialog(null));
        binding.btnQuickLogFirst.setOnClickListener(v -> showLogWorkoutDialog(null));
        binding.btnAddSplit.setOnClickListener(v -> showLogWorkoutDialog(null));

        // Load exercises in background for dropdown
        new Thread(() -> {
            exerciseList = dbHelper.getAllExercises();
        }).start();

        loadDashboardData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void loadDashboardData() {
        if (getContext() == null || binding == null) return;

        // 1. User Profile & Biometrics
        UserProfile profile = dbHelper.getPrimaryUserProfile();
        if (profile == null) {
            String savedName = AuthManager.getUserName(requireContext());
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            profile = new UserProfile(1, (savedName != null && !savedName.isEmpty()) ? savedName : "Athlete", 175.0, 72.0, 70.0, today, today, null);
            dbHelper.insertUserProfile(profile);
        }
        if (profile != null) {
            binding.tvUserName.setText(profile.getName());
            binding.tvCurrentWeight.setText(String.format(Locale.getDefault(), "%.1f kg", profile.getCurrentWeightKg()));
            binding.tvTargetWeight.setText(String.format(Locale.getDefault(), "%.1f kg", profile.getTargetWeightKg()));

            // Profile photo or initials thumbnail
            String photoPath = profile.getProfileImagePath();
            if (photoPath != null && !photoPath.trim().isEmpty() && new java.io.File(photoPath).exists()) {
                android.graphics.Bitmap avatarBitmap = ImageRotationHelper.loadOrientedBitmap(photoPath, 120, 120);
                if (avatarBitmap != null) {
                    binding.ivDashboardAvatar.setImageBitmap(avatarBitmap);
                    binding.ivDashboardAvatar.setVisibility(View.VISIBLE);
                    binding.tvDashboardInitials.setVisibility(View.GONE);
                } else {
                    binding.ivDashboardAvatar.setVisibility(View.GONE);
                    binding.tvDashboardInitials.setVisibility(View.VISIBLE);
                    binding.tvDashboardInitials.setText(profile.getInitials());
                }
            } else {
                binding.ivDashboardAvatar.setVisibility(View.GONE);
                binding.tvDashboardInitials.setVisibility(View.VISIBLE);
                binding.tvDashboardInitials.setText(profile.getInitials());
            }

            // Time-based greeting
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            String greeting;
            if (hour < 12) {
                greeting = "Good morning";
            } else if (hour < 17) {
                greeting = "Good afternoon";
            } else {
                greeting = "Good evening";
            }
            binding.tvGreeting.setText(greeting);

            // Goal progress badge and trigger note
            double diff = profile.getTargetWeightKg() - profile.getCurrentWeightKg();
            String goalStatus;
            if (Math.abs(diff) < 0.1) {
                goalStatus = String.format(Locale.getDefault(), "Goal achieved (%.1f kg)! • Tap to edit biometrics", profile.getTargetWeightKg());
                binding.tvGymBadge.setText("Goal Reached ★");
            } else if (diff < 0) {
                goalStatus = String.format(Locale.getDefault(), "%.1f kg to goal (%.1f kg) • Auto-Synced • Tap to edit", Math.abs(diff), profile.getTargetWeightKg());
                binding.tvGymBadge.setText(String.format(Locale.getDefault(), "Cut: -%.1f kg", Math.abs(diff)));
            } else {
                goalStatus = String.format(Locale.getDefault(), "+%.1f kg to goal (%.1f kg) • Auto-Synced • Tap to edit", diff, profile.getTargetWeightKg());
                binding.tvGymBadge.setText(String.format(Locale.getDefault(), "Bulk: +%.1f kg", diff));
            }
            binding.tvTriggerNote.setText(goalStatus);

            // 2. BMI Calculation & Color-Coded Polishing
            double bmi = profile.calculateBmi();
            binding.tvBmiScore.setText(String.format(Locale.getDefault(), "%.1f", bmi));

            String category = profile.getBmiCategory();
            binding.tvBmiBadge.setText(category);

            int badgeColor;
            if (bmi < 18.5) {
                badgeColor = ContextCompat.getColor(requireContext(), R.color.bmi_underweight_color);
                binding.tvBmiScaleContext.setText("Below standard range (<18.5 kg/m²). Focus on nutrient-dense nutrition and progressive overload.");
            } else if (bmi < 25.0) {
                badgeColor = ContextCompat.getColor(requireContext(), R.color.bmi_normal_color);
                binding.tvBmiScaleContext.setText("Optimal range (18.5 – 24.9 kg/m²). Great foundation for strength and conditioning.");
            } else if (bmi < 30.0) {
                badgeColor = ContextCompat.getColor(requireContext(), R.color.bmi_overweight_color);
                binding.tvBmiScaleContext.setText("Slightly elevated (25.0 – 29.9 kg/m²). High lean muscle mass can elevate BMI in athletes.");
            } else {
                badgeColor = ContextCompat.getColor(requireContext(), R.color.bmi_obese_color);
                binding.tvBmiScaleContext.setText("Elevated tier (30.0+ kg/m²). Combine caloric deficit with progressive cardio and lifting.");
            }
            binding.tvBmiBadge.setTextColor(badgeColor);

            // Dynamic Segmented Color Indicator Track highlighting
            binding.bmiTrackUnderweight.setAlpha(bmi < 18.5 ? 1.0f : 0.25f);
            binding.bmiTrackNormal.setAlpha(bmi >= 18.5 && bmi < 25.0 ? 1.0f : 0.25f);
            binding.bmiTrackOverweight.setAlpha(bmi >= 25.0 && bmi < 30.0 ? 1.0f : 0.25f);
            binding.bmiTrackObese.setAlpha(bmi >= 30.0 ? 1.0f : 0.25f);

            binding.bmiTrackUnderweight.setScaleY(bmi < 18.5 ? 1.5f : 1.0f);
            binding.bmiTrackNormal.setScaleY(bmi >= 18.5 && bmi < 25.0 ? 1.5f : 1.0f);
            binding.bmiTrackOverweight.setScaleY(bmi >= 25.0 && bmi < 30.0 ? 1.5f : 1.0f);
            binding.bmiTrackObese.setScaleY(bmi >= 30.0 ? 1.5f : 1.0f);
        } else {
            binding.tvUserName.setText("Gym Member");
            binding.tvCurrentWeight.setText("70.0 kg");
            binding.tvTargetWeight.setText("68.0 kg");
            binding.tvBmiScore.setText("22.0");
            binding.tvBmiBadge.setText("Normal");
        }

        // 3. Overall Personal Best using SQLite Subquery
        PersonalBest pbLog = dbHelper.getOverallHeaviestLift();
        if (pbLog != null && pbLog.getMaxWeight() > 0) {
            binding.tvPersonalBestTitle.setText(String.format(Locale.getDefault(), "%s — %.1f kg (%s)",
                    pbLog.getExerciseTitle(), pbLog.getMaxWeight(), pbLog.getDate()));
            binding.cardPersonalBest.setVisibility(View.VISIBLE);
        } else {
            binding.tvPersonalBestTitle.setText("Log your lifts to discover your Personal Best record!");
        }

        // 4. Recent Workout Summaries queried directly from SQLite VIEW
        List<WorkoutSummary> summaries = dbHelper.getRecentWorkoutSummaries();
        if (summaries.isEmpty()) {
            binding.rvRecentWorkouts.setVisibility(View.GONE);
            binding.cardEmptyWorkouts.setVisibility(View.VISIBLE);
        } else {
            binding.rvRecentWorkouts.setVisibility(View.VISIBLE);
            binding.cardEmptyWorkouts.setVisibility(View.GONE);
            summaryAdapter.setSummaries(summaries);
        }
    }

    /**
     * Shows a dialog allowing the user to select their workout split and add exercises into it.
     */
    public void showLogWorkoutDialog(@Nullable Long preselectedExerciseId) {
        if (getContext() == null) return;

        DialogLogWorkoutBinding dialogBinding = DialogLogWorkoutBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
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
                "Chest & Triceps Focus",
                "Back & Biceps Focus",
                "Shoulders & Arms",
                "Full Body Routine",
                "Cardio & Core Session"
        };
        for (String preset : splitPresets) {
            if (!combinedSplits.contains(preset)) {
                combinedSplits.add(preset);
            }
        }

        ArrayAdapter<String> splitAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.item_dropdown_entry, combinedSplits);
        dialogBinding.actvSplit.setAdapter(splitAdapter);

        // 2. Setup exercise list for autocomplete with compact dropdown styling
        List<String> titles = new ArrayList<>();
        for (Exercise ex : exerciseList) {
            titles.add(ex.getTitle());
        }
        ArrayAdapter<String> exerciseAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.item_dropdown_entry, titles);
        dialogBinding.actvExercise.setAdapter(exerciseAdapter);

        // Constrain dropdown popup height to prevent vertical overflow across dialog
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

        // 3. Track selected exercises in this split
        List<Exercise> selectedExercises = new ArrayList<>();

        // If preselected from library, add it right away
        if (preselectedExerciseId != null) {
            for (Exercise ex : exerciseList) {
                if (ex.getId() == preselectedExerciseId) {
                    selectedExercises.add(ex);
                    break;
                }
            }
        }

        // Helper to update chip UI
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
                        com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(requireContext());
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

        // Add Exercise button click listener
        dialogBinding.btnAddExerciseToSplit.setOnClickListener(v -> {
            dialogBinding.actvExercise.dismissDropDown();
            String typed = dialogBinding.actvExercise.getText().toString().trim();
            if (typed.isEmpty()) {
                Toast.makeText(requireContext(), "Please select or enter an exercise name", Toast.LENGTH_SHORT).show();
                return;
            }

            // Find existing or create custom
            Exercise target = null;
            for (Exercise ex : exerciseList) {
                if (ex.getTitle().equalsIgnoreCase(typed)) {
                    target = ex;
                    break;
                }
            }
            if (target == null) {
                // Insert new custom exercise into library
                target = new Exercise(0, typed, "Full Body", "Gym Equipment", "Intermediate", "Custom logged exercise", "");
                long newId = dbHelper.insertExercise(target);
                target.setId(newId);
                exerciseList.add(target);
                titles.add(typed);
                exerciseAdapter.notifyDataSetChanged();
            }

            // Avoid duplicate additions in the same split
            boolean alreadyAdded = false;
            for (Exercise existing : selectedExercises) {
                if (existing.getId() == target.getId() || existing.getTitle().equalsIgnoreCase(target.getTitle())) {
                    alreadyAdded = true;
                    break;
                }
            }

            if (alreadyAdded) {
                Toast.makeText(requireContext(), "'" + target.getTitle() + "' is already in this split", Toast.LENGTH_SHORT).show();
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

            // If user typed an exercise but didn't tap Add, auto-add it
            String pendingTyped = dialogBinding.actvExercise.getText().toString().trim();
            if (!pendingTyped.isEmpty()) {
                Exercise pendingTarget = null;
                for (Exercise ex : exerciseList) {
                    if (ex.getTitle().equalsIgnoreCase(pendingTyped)) {
                        pendingTarget = ex;
                        break;
                    }
                }
                if (pendingTarget == null) {
                    pendingTarget = new Exercise(0, pendingTyped, "Full Body", "Gym Equipment", "Intermediate", "Custom logged exercise", "");
                    long newId = dbHelper.insertExercise(pendingTarget);
                    pendingTarget.setId(newId);
                    exerciseList.add(pendingTarget);
                }
                boolean exists = false;
                for (Exercise existing : selectedExercises) {
                    if (existing.getId() == pendingTarget.getId() || existing.getTitle().equalsIgnoreCase(pendingTarget.getTitle())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    selectedExercises.add(pendingTarget);
                }
            }

            if (selectedExercises.isEmpty()) {
                Toast.makeText(requireContext(), "Please add at least one exercise to your split", Toast.LENGTH_SHORT).show();
                return;
            }

            String notes = dialogBinding.etNotes.getText() != null ? dialogBinding.etNotes.getText().toString().trim() : "";
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            String chosenDate = dialogBinding.etDate.getText() != null ? dialogBinding.etDate.getText().toString().trim() : "";
            if (chosenDate.isEmpty()) {
                chosenDate = today;
            }

            long result = dbHelper.logWorkoutSession(splitName, chosenDate, selectedExercises, notes);
            if (result > 0) {
                Toast.makeText(requireContext(), splitName + " logged with " + selectedExercises.size() + " exercises!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadDashboardData();
            } else {
                Toast.makeText(requireContext(), "Failed to save workout session", Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize session date to today and attach DatePickerDialog
        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        dialogBinding.etDate.setText(todayStr);
        DatePickerUtil.attachDatePicker(requireContext(), dialogBinding.etDate);

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

    /**
     * Shows a dialog allowing the user to modify or update an existing workout split session.
     * When the user modifies anything:
     * - Changing the split name propagates the change across the entire database.
     * - Changing constituent exercises, date, or notes synchronizes the database records.
     * - A dedicated Delete button allows removing this split or removing all occurrences in DB.
     */
    public void showEditWorkoutDialog(@NonNull WorkoutSummary summary) {
        if (getContext() == null) return;

        DialogLogWorkoutBinding dialogBinding = DialogLogWorkoutBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogBinding.getRoot())
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }

        dialogBinding.tvDialogTitle.setText("Edit Workout Split");
        dialogBinding.tvDialogSubtitle.setText("Modify your exercise split, date, exercises, or notes. Renaming updates the split across the entire database.");
        dialogBinding.btnSaveWorkout.setText("Update Split");
        dialogBinding.btnDeleteWorkout.setText("Delete Permanent");
        dialogBinding.btnDeleteWorkout.setVisibility(View.VISIBLE);

        // 1. Populate workout split presets with compact dropdown styling
        String[] splitPresets = new String[]{
                "Push Day (Chest, Shoulders, Triceps)",
                "Pull Day (Back, Biceps, Traps)",
                "Leg Day (Quads, Hamstrings, Calves)",
                "Upper Body Split",
                "Lower Body Split",
                "Chest & Triceps Focus",
                "Back & Biceps Focus",
                "Shoulders & Arms",
                "Full Body Routine",
                "Cardio & Core Session"
        };
        ArrayAdapter<String> splitAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.item_dropdown_entry, splitPresets);
        dialogBinding.actvSplit.setAdapter(splitAdapter);
        dialogBinding.actvSplit.setText(summary.getSplitName(), false);

        // 2. Setup date and notes with interactive DatePickerDialog
        dialogBinding.etDate.setText(summary.getWorkoutDate());
        DatePickerUtil.attachDatePicker(requireContext(), dialogBinding.etDate);
        dialogBinding.etNotes.setText(summary.getNotes());

        // 3. Setup exercise list for autocomplete with compact dropdown styling
        List<String> titles = new ArrayList<>();
        for (Exercise ex : exerciseList) {
            titles.add(ex.getTitle());
        }
        ArrayAdapter<String> exerciseAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.item_dropdown_entry, titles);
        dialogBinding.actvExercise.setAdapter(exerciseAdapter);

        // Constrain dropdown popup height to prevent vertical overflow across dialog
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

        // 4. Load constituent exercises from the database for this session
        List<Exercise> selectedExercises = new ArrayList<>(dbHelper.getExercisesForSession(summary.getSplitName(), summary.getWorkoutDate()));

        // Fallback: match by titles from summary if needed
        if (selectedExercises.isEmpty() && summary.getExerciseNames() != null && !summary.getExerciseNames().isEmpty()) {
            String[] names = summary.getExerciseNames().split(",");
            for (String n : names) {
                String trimmed = n.trim();
                for (Exercise ex : exerciseList) {
                    if (ex.getTitle().equalsIgnoreCase(trimmed)) {
                        selectedExercises.add(ex);
                        break;
                    }
                }
            }
        }

        // Helper to update chip UI
        Runnable updateChips = new Runnable() {
            @Override
            public void run() {
                dialogBinding.chipGroupExercises.removeAllViews();
                if (selectedExercises.isEmpty()) {
                    dialogBinding.tvNoExercisesHint.setVisibility(View.VISIBLE);
                    dialogBinding.tvAddedExercisesCount.setText("Exercises in Split (0)");
                } else {
                    dialogBinding.tvNoExercisesHint.setVisibility(View.GONE);
                    dialogBinding.tvAddedExercisesCount.setText("Exercises in Split (" + selectedExercises.size() + ")");
                    for (Exercise ex : selectedExercises) {
                        com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(requireContext());
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

        // Add Exercise button click listener
        dialogBinding.btnAddExerciseToSplit.setOnClickListener(v -> {
            dialogBinding.actvExercise.dismissDropDown();
            String typed = dialogBinding.actvExercise.getText().toString().trim();
            if (typed.isEmpty()) {
                Toast.makeText(requireContext(), "Please select or enter an exercise name", Toast.LENGTH_SHORT).show();
                return;
            }

            Exercise target = null;
            for (Exercise ex : exerciseList) {
                if (ex.getTitle().equalsIgnoreCase(typed)) {
                    target = ex;
                    break;
                }
            }
            if (target == null) {
                target = new Exercise(0, typed, "Full Body", "Gym Equipment", "Intermediate", "Custom logged exercise", "");
                long newId = dbHelper.insertExercise(target);
                target.setId(newId);
                exerciseList.add(target);
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
                Toast.makeText(requireContext(), "'" + target.getTitle() + "' is already in this split", Toast.LENGTH_SHORT).show();
            } else {
                selectedExercises.add(target);
                dialogBinding.actvExercise.setText("");
                updateChips.run();
            }
        });

        // Delete Split button click listener
        dialogBinding.btnDeleteWorkout.setOnClickListener(v -> {
            dialog.dismiss();
            showConfirmDeleteWorkoutDialog(summary);
        });

        dialogBinding.btnCancelWorkout.setOnClickListener(v -> dialog.dismiss());

        // Update Split Save button click listener
        dialogBinding.btnSaveWorkout.setOnClickListener(v -> {
            String newSplitName = dialogBinding.actvSplit.getText().toString().trim();
            if (newSplitName.isEmpty()) {
                newSplitName = "Workout Split";
            }

            String pendingTyped = dialogBinding.actvExercise.getText().toString().trim();
            if (!pendingTyped.isEmpty()) {
                Exercise pendingTarget = null;
                for (Exercise ex : exerciseList) {
                    if (ex.getTitle().equalsIgnoreCase(pendingTyped)) {
                        pendingTarget = ex;
                        break;
                    }
                }
                if (pendingTarget == null) {
                    pendingTarget = new Exercise(0, pendingTyped, "Full Body", "Gym Equipment", "Intermediate", "Custom logged exercise", "");
                    long newId = dbHelper.insertExercise(pendingTarget);
                    pendingTarget.setId(newId);
                    exerciseList.add(pendingTarget);
                }
                boolean exists = false;
                for (Exercise existing : selectedExercises) {
                    if (existing.getId() == pendingTarget.getId() || existing.getTitle().equalsIgnoreCase(pendingTarget.getTitle())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    selectedExercises.add(pendingTarget);
                }
            }

            if (selectedExercises.isEmpty()) {
                Toast.makeText(requireContext(), "Please keep at least one exercise in your split", Toast.LENGTH_SHORT).show();
                return;
            }

            String newNotes = dialogBinding.etNotes.getText() != null ? dialogBinding.etNotes.getText().toString().trim() : "";
            String newDate = dialogBinding.etDate.getText() != null ? dialogBinding.etDate.getText().toString().trim() : "";
            if (newDate.isEmpty()) {
                newDate = summary.getWorkoutDate();
            }

            boolean success = dbHelper.updateWorkoutSession(
                    summary.getSplitName(),
                    summary.getWorkoutDate(),
                    newSplitName,
                    newDate,
                    selectedExercises,
                    newNotes
            );

            if (success) {
                Toast.makeText(requireContext(), "Split updated across database successfully!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadDashboardData();
            } else {
                Toast.makeText(requireContext(), "Failed to update workout split", Toast.LENGTH_SHORT).show();
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

    /**
     * Confirms and deletes either the specific workout split session or the split across the entire database.
     */
    public void showConfirmDeleteWorkoutDialog(@NonNull WorkoutSummary summary) {
        if (getContext() == null) return;

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Workout Split")
                .setMessage("Would you like to delete this session (" + summary.getSplitName() + " on " + summary.getWorkoutDate() + ") or delete permanently across the entire database?")
                .setPositiveButton("Delete Session", (d, w) -> {
                    int count = dbHelper.deleteWorkoutSession(summary.getSplitName(), summary.getWorkoutDate());
                    if (count > 0) {
                        Toast.makeText(requireContext(), "Workout session deleted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Session already removed", Toast.LENGTH_SHORT).show();
                    }
                    loadDashboardData();
                })
                .setNeutralButton("Delete Permanent", (d, w) -> {
                    int count = dbHelper.deleteSplitAcrossDb(summary.getSplitName());
                    Toast.makeText(requireContext(), "Permanently deleted " + count + " records for '" + summary.getSplitName() + "'", Toast.LENGTH_SHORT).show();
                    loadDashboardData();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Shows a dialog allowing direct editing of user name, height, current weight, and goal weight.
     * All values on the dashboard dynamically recalculate upon saving.
     */
    public void showEditProfileDialog() {
        if (getContext() == null) return;

        UserProfile profile = dbHelper.getPrimaryUserProfile();
        if (profile == null) return;

        DialogEditProfileBinding dialogBinding = DialogEditProfileBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogBinding.getRoot())
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        dialogBinding.etEditName.setText(profile.getName());
        dialogBinding.etEditHeight.setText(String.format(Locale.getDefault(), "%.1f", profile.getHeightCm()));
        dialogBinding.etEditWeight.setText(String.format(Locale.getDefault(), "%.1f", profile.getCurrentWeightKg()));
        dialogBinding.etEditTargetWeight.setText(String.format(Locale.getDefault(), "%.1f", profile.getTargetWeightKg()));

        Runnable updateDialogPreview = () -> {
            try {
                String hStr = dialogBinding.etEditHeight.getText() != null ? dialogBinding.etEditHeight.getText().toString().trim() : "";
                String wStr = dialogBinding.etEditWeight.getText() != null ? dialogBinding.etEditWeight.getText().toString().trim() : "";
                String tStr = dialogBinding.etEditTargetWeight.getText() != null ? dialogBinding.etEditTargetWeight.getText().toString().trim() : "";

                if (!hStr.isEmpty() && !wStr.isEmpty()) {
                    double h = Double.parseDouble(hStr);
                    double w = Double.parseDouble(wStr);
                    if (h > 0 && w > 0) {
                        double hm = h / 100.0;
                        double calculatedBmi = Math.round((w / (hm * hm)) * 10.0) / 10.0;
                        String cat;
                        int colorRes;
                        if (calculatedBmi < 18.5) {
                            cat = "Underweight";
                            colorRes = R.color.bmi_underweight_color;
                        } else if (calculatedBmi < 25.0) {
                            cat = "Normal";
                            colorRes = R.color.bmi_normal_color;
                        } else if (calculatedBmi < 30.0) {
                            cat = "Overweight";
                            colorRes = R.color.bmi_overweight_color;
                        } else {
                            cat = "Obese";
                            colorRes = R.color.bmi_obese_color;
                        }
                        dialogBinding.tvDialogBmiBadge.setText(String.format(Locale.getDefault(), "%s (%.1f)", cat, calculatedBmi));
                        dialogBinding.tvDialogBmiBadge.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
                    }
                }

                if (!wStr.isEmpty() && !tStr.isEmpty()) {
                    double w = Double.parseDouble(wStr);
                    double t = Double.parseDouble(tStr);
                    double diff = t - w;
                    if (Math.abs(diff) < 0.1) {
                        dialogBinding.tvDialogGoalPreview.setText("Target: At goal weight (" + String.format(Locale.getDefault(), "%.1f", t) + " kg)");
                    } else if (diff < 0) {
                        dialogBinding.tvDialogGoalPreview.setText(String.format(Locale.getDefault(), "Target: %.1f kg loss to reach %.1f kg", Math.abs(diff), t));
                    } else {
                        dialogBinding.tvDialogGoalPreview.setText(String.format(Locale.getDefault(), "Target: %.1f kg gain to reach %.1f kg", diff, t));
                    }
                }
            } catch (Exception ignored) {}
        };

        updateDialogPreview.run();

        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateDialogPreview.run();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        };

        dialogBinding.etEditHeight.addTextChangedListener(watcher);
        dialogBinding.etEditWeight.addTextChangedListener(watcher);
        dialogBinding.etEditTargetWeight.addTextChangedListener(watcher);

        dialogBinding.btnCancelEditProfile.setOnClickListener(v -> dialog.dismiss());

        dialogBinding.btnSaveEditProfile.setOnClickListener(v -> {
            String name = dialogBinding.etEditName.getText() != null ? dialogBinding.etEditName.getText().toString().trim() : "";
            String hStr = dialogBinding.etEditHeight.getText() != null ? dialogBinding.etEditHeight.getText().toString().trim() : "";
            String wStr = dialogBinding.etEditWeight.getText() != null ? dialogBinding.etEditWeight.getText().toString().trim() : "";
            String tStr = dialogBinding.etEditTargetWeight.getText() != null ? dialogBinding.etEditTargetWeight.getText().toString().trim() : "";

            if (name.isEmpty() || hStr.isEmpty() || wStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in name, height, and weight", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double height = Double.parseDouble(hStr.replace(',', '.'));
                double weight = Double.parseDouble(wStr.replace(',', '.'));
                double targetWeight = tStr.isEmpty() ? weight : Double.parseDouble(tStr.replace(',', '.'));

                profile.setName(name);
                profile.setHeightCm(height);
                profile.setCurrentWeightKg(weight);
                profile.setTargetWeightKg(targetWeight);

                dbHelper.saveOrUpdateUserProfile(profile, true);

                Toast.makeText(requireContext(), "Profile updated! Dashboard recalculated.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadDashboardData();
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid numbers entered", Toast.LENGTH_SHORT).show();
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
