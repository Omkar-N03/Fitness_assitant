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
        binding.rvRecentWorkouts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecentWorkouts.setAdapter(summaryAdapter);

        // Setup click listeners to edit profile & biometrics directly from Dashboard
        View.OnClickListener editProfileClickListener = v -> showEditProfileDialog();
        binding.llHeader.setOnClickListener(editProfileClickListener);
        binding.tvGymBadge.setOnClickListener(editProfileClickListener);
        binding.cardWeightHero.setOnClickListener(editProfileClickListener);
        binding.cardBmi.setOnClickListener(editProfileClickListener);

        // Setup FAB and button click listeners
        binding.fabLogWorkout.setOnClickListener(v -> showLogWorkoutDialog(null));
        binding.btnQuickLogFirst.setOnClickListener(v -> showLogWorkoutDialog(null));

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
        if (profile != null) {
            binding.tvUserName.setText(profile.getName());
            binding.tvCurrentWeight.setText(String.format(Locale.getDefault(), "%.1f kg", profile.getCurrentWeightKg()));
            binding.tvTargetWeight.setText(String.format(Locale.getDefault(), "%.1f kg", profile.getTargetWeightKg()));

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
                goalStatus = String.format(Locale.getDefault(), "%.1f kg to goal (%.1f kg) • SQLite Trigger Synced • Tap to edit", Math.abs(diff), profile.getTargetWeightKg());
                binding.tvGymBadge.setText(String.format(Locale.getDefault(), "Cut: -%.1f kg", Math.abs(diff)));
            } else {
                goalStatus = String.format(Locale.getDefault(), "+%.1f kg to goal (%.1f kg) • SQLite Trigger Synced • Tap to edit", diff, profile.getTargetWeightKg());
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
     * Shows a dialog to log a workout set into the SQLite database with searchable exercise dropdown.
     */
    public void showLogWorkoutDialog(@Nullable Long preselectedExerciseId) {
        if (getContext() == null) return;

        DialogLogWorkoutBinding dialogBinding = DialogLogWorkoutBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogBinding.getRoot())
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        // Setup exercise searchable dropdown list
        List<String> titles = new ArrayList<>();
        int selectedIndex = 0;
        for (int i = 0; i < exerciseList.size(); i++) {
            titles.add(exerciseList.get(i).getTitle());
            if (preselectedExerciseId != null && exerciseList.get(i).getId() == preselectedExerciseId) {
                selectedIndex = i;
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, titles);
        dialogBinding.actvExercise.setAdapter(adapter);

        if (!titles.isEmpty()) {
            // Set default/preselected text without triggering auto-filtering
            dialogBinding.actvExercise.setText(titles.get(selectedIndex), false);
        }

        // Show full list upon clicking or focusing on the field
        dialogBinding.actvExercise.setOnClickListener(v -> dialogBinding.actvExercise.showDropDown());
        dialogBinding.actvExercise.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                dialogBinding.actvExercise.showDropDown();
            }
        });

        dialogBinding.btnCancelWorkout.setOnClickListener(v -> dialog.dismiss());

        dialogBinding.btnSaveWorkout.setOnClickListener(v -> {
            String selectedTitle = dialogBinding.actvExercise.getText().toString().trim();
            String setsStr = dialogBinding.etSets.getText().toString().trim();
            String repsStr = dialogBinding.etReps.getText().toString().trim();
            String weightStr = dialogBinding.etWeight.getText().toString().trim();
            String notes = dialogBinding.etNotes.getText().toString().trim();

            if (selectedTitle.isEmpty() || setsStr.isEmpty() || repsStr.isEmpty() || weightStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int sets = Integer.parseInt(setsStr);
            int reps = Integer.parseInt(repsStr);
            double weight = Double.parseDouble(weightStr);

            // Find matching exercise ID
            long exId = 1;
            for (Exercise ex : exerciseList) {
                if (ex.getTitle().equalsIgnoreCase(selectedTitle)) {
                    exId = ex.getId();
                    break;
                }
            }

            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            WorkoutLog log = new WorkoutLog(0, 1, exId, selectedTitle, today, sets, reps, weight, notes);
            long insertedId = dbHelper.insertWorkoutLog(log);

            if (insertedId > 0) {
                Toast.makeText(requireContext(), "Workout set logged! Volume added to SQLite View.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadDashboardData();
            } else {
                Toast.makeText(requireContext(), "Failed to save log", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
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
                double height = Double.parseDouble(hStr);
                double weight = Double.parseDouble(wStr);
                double targetWeight = tStr.isEmpty() ? weight : Double.parseDouble(tStr);

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