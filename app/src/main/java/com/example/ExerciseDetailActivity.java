package com.example;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.databinding.ActivityExerciseDetailBinding;
import com.example.databinding.DialogLogWorkoutBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ExerciseDetailActivity showing exercise instructions, body part, equipment, difficulty,
 * user's personal best (queried via SQL subquery), and an Intent to launch the direct YouTube link.
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
        // Intent to launch YouTube Link
        binding.btnOpenYoutube.setOnClickListener(v -> {
            String link = currentExercise.getYoutubeLink();
            if (link != null && !link.isEmpty()) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Unable to open YouTube video link", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No video link available for this exercise", Toast.LENGTH_SHORT).show();
            }
        });

        // Quick Log Workout for this Exercise
        binding.btnLogThisExercise.setOnClickListener(v -> showLogDialog());
    }

    private void showLogDialog() {
        DialogLogWorkoutBinding dialogBinding = DialogLogWorkoutBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogBinding.tilExerciseSelect.setVisibility(View.GONE);
        dialogBinding.tvExercisePrompt.setText("Logging set for: " + currentExercise.getTitle());

        dialogBinding.btnCancelWorkout.setOnClickListener(v -> dialog.dismiss());

        dialogBinding.btnSaveWorkout.setOnClickListener(v -> {
            String setsStr = dialogBinding.etSets.getText().toString().trim();
            String repsStr = dialogBinding.etReps.getText().toString().trim();
            String weightStr = dialogBinding.etWeight.getText().toString().trim();
            String notes = dialogBinding.etNotes.getText().toString().trim();

            if (setsStr.isEmpty() || repsStr.isEmpty() || weightStr.isEmpty()) {
                Toast.makeText(this, "Please enter sets, reps, and weight", Toast.LENGTH_SHORT).show();
                return;
            }

            int sets = Integer.parseInt(setsStr);
            int reps = Integer.parseInt(repsStr);
            double weight = Double.parseDouble(weightStr);

            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            WorkoutLog log = new WorkoutLog(0, 1, currentExercise.getId(), currentExercise.getTitle(),
                    today, sets, reps, weight, notes);

            long insertedId = dbHelper.insertWorkoutLog(log);
            if (insertedId > 0) {
                Toast.makeText(this, "Workout logged! Added to daily volume view.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadPersonalBest();
            } else {
                Toast.makeText(this, "Failed to save workout log", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}
