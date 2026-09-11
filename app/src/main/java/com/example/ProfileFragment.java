package com.example;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.databinding.FragmentProfileBinding;

import java.util.Locale;

/**
 * ProfileFragment containing the form to view and update user Name, Height, and Weight.
 * Provides live interactive BMI calculations as numbers are typed.
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private GymDatabaseHelper dbHelper;
    private UserProfile currentProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = GymDatabaseHelper.getInstance(requireContext());

        loadProfile();
        setupLiveBmiCalculation();
        setupSaveButton();
    }

    private void loadProfile() {
        currentProfile = dbHelper.getPrimaryUserProfile();
        if (currentProfile == null) {
            currentProfile = new UserProfile(1, "Alex Rivera", 178.0, 74.5, 72.0, "2026-09-06", "2026-09-06");
            dbHelper.insertUserProfile(currentProfile);
        }

        binding.etProfileName.setText(currentProfile.getName());
        binding.etProfileHeight.setText(String.format(Locale.getDefault(), "%.1f", currentProfile.getHeightCm()));
        binding.etProfileWeight.setText(String.format(Locale.getDefault(), "%.1f", currentProfile.getCurrentWeightKg()));
        binding.etProfileTargetWeight.setText(String.format(Locale.getDefault(), "%.1f", currentProfile.getTargetWeightKg()));

        updateBmiPreview(currentProfile.getHeightCm(), currentProfile.getCurrentWeightKg());
    }

    private void setupLiveBmiCalculation() {
        TextWatcher bmiWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateLiveBmi();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        binding.etProfileHeight.addTextChangedListener(bmiWatcher);
        binding.etProfileWeight.addTextChangedListener(bmiWatcher);
    }

    private void calculateLiveBmi() {
        try {
            String hStr = binding.etProfileHeight.getText().toString().trim();
            String wStr = binding.etProfileWeight.getText().toString().trim();

            if (!hStr.isEmpty() && !wStr.isEmpty()) {
                double height = Double.parseDouble(hStr);
                double weight = Double.parseDouble(wStr);
                updateBmiPreview(height, weight);
            }
        } catch (NumberFormatException ignored) {}
    }

    private void updateBmiPreview(double heightCm, double weightKg) {
        if (heightCm <= 0 || weightKg <= 0 || getContext() == null) return;

        double heightM = heightCm / 100.0;
        double bmi = weightKg / (heightM * heightM);

        String category;
        int colorRes;
        String advice;

        if (bmi < 18.5) {
            category = "Underweight";
            colorRes = R.color.bmi_underweight_color;
            advice = "Your BMI is under 18.5. Consider a modest caloric surplus with strength training.";
        } else if (bmi < 25.0) {
            category = "Normal";
            colorRes = R.color.bmi_normal_color;
            advice = "Your BMI is in the healthy zone (18.5 – 24.9). Ideal for athletic performance.";
        } else if (bmi < 30.0) {
            category = "Overweight";
            colorRes = R.color.bmi_overweight_color;
            advice = "Your BMI is 25.0 – 29.9. For lifters with high muscle mass, BMI may skew higher.";
        } else {
            category = "Obese";
            colorRes = R.color.bmi_obese_color;
            advice = "Your BMI is 30+. Prioritize progressive cardiovascular conditioning and caloric balance.";
        }

        binding.tvProfileBmiBadge.setText(String.format(Locale.getDefault(), "%s (%.1f)", category, bmi));
        binding.tvProfileBmiBadge.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
        binding.tvProfileBmiDesc.setText(advice);
    }

    private void setupSaveButton() {
        binding.btnSaveProfile.setOnClickListener(v -> {
            String name = binding.etProfileName.getText().toString().trim();
            String heightStr = binding.etProfileHeight.getText().toString().trim();
            String weightStr = binding.etProfileWeight.getText().toString().trim();
            String targetStr = binding.etProfileTargetWeight.getText().toString().trim();

            if (name.isEmpty() || heightStr.isEmpty() || weightStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in your name, height, and weight", Toast.LENGTH_SHORT).show();
                return;
            }

            double height = Double.parseDouble(heightStr);
            double weight = Double.parseDouble(weightStr);
            double targetWeight = targetStr.isEmpty() ? weight : Double.parseDouble(targetStr);

            currentProfile.setName(name);
            currentProfile.setHeightCm(height);
            currentProfile.setCurrentWeightKg(weight);
            currentProfile.setTargetWeightKg(targetWeight);

            dbHelper.saveOrUpdateUserProfile(currentProfile, true);
            Toast.makeText(requireContext(), "Profile biometrics updated successfully!", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
