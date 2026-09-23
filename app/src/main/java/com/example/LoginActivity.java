package com.example;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.databinding.ActivityLoginBinding;

import java.util.Locale;

/**
 * Authentication screen supporting local account registration and sign-in
 * using Gmail, custom email, or mobile number.
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private GymDatabaseHelper dbHelper;
    private boolean isSignUpMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already authenticated
        if (AuthManager.isLoggedIn(this)) {
            navigateToMain();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = GymDatabaseHelper.getInstance(this);

        setupAuthToggle();
        setupLiveBmiCalculator();
        setupActionButtons();

        // Check if there is a last registered identifier to pre-fill
        String lastIdentifier = AuthManager.getLastRegisteredIdentifier(this);
        if (!lastIdentifier.isEmpty()) {
            binding.etLoginEmail.setText(lastIdentifier);
        }

        // Default to Sign In mode
        setAuthMode(false);

        // Predictive back handler: handles IME keyboard dismissal and toggles mode gracefully
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                hideSoftKeyboard();
                if (isSignUpMode) {
                    setAuthMode(false);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void setupAuthToggle() {
        binding.toggleAuthMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnTabSignIn) {
                    setAuthMode(false);
                } else if (checkedId == R.id.btnTabSignUp) {
                    setAuthMode(true);
                }
            }
        });

        binding.tvSwitchModePrompt.setOnClickListener(v -> {
            setAuthMode(!isSignUpMode);
        });
    }

    private void setAuthMode(boolean signUp) {
        hideSoftKeyboard();
        this.isSignUpMode = signUp;

        // Clear previous error messages
        binding.tilLoginName.setError(null);
        binding.tilLoginEmail.setError(null);
        binding.tilLoginPassword.setError(null);
        binding.tilLoginConfirmPassword.setError(null);

        if (signUp) {
            binding.toggleAuthMode.check(R.id.btnTabSignUp);
            binding.tilLoginName.setVisibility(View.VISIBLE);
            binding.tilLoginConfirmPassword.setVisibility(View.VISIBLE);
            binding.llBodyDetailsSection.setVisibility(View.VISIBLE);
            binding.llOptionsRow.setVisibility(View.GONE);

            binding.btnLogin.setText("Create Account");
            binding.btnLogin.setIconResource(R.drawable.ic_check);
            binding.tvSwitchModePrompt.setText("Already have an account? Sign In");

            binding.tvLoginSubtitle.setText("Create your personal fitness profile. No verification needed — once created, you'll sign in directly.");
        } else {
            binding.toggleAuthMode.check(R.id.btnTabSignIn);
            binding.tilLoginName.setVisibility(View.GONE);
            binding.tilLoginConfirmPassword.setVisibility(View.GONE);
            binding.llBodyDetailsSection.setVisibility(View.GONE);
            binding.llOptionsRow.setVisibility(View.VISIBLE);

            binding.btnLogin.setText("Sign In");
            binding.btnLogin.setIconResource(R.drawable.ic_check);
            binding.tvSwitchModePrompt.setText("Don't have an account? Create Account");

            binding.tvLoginSubtitle.setText("Sign in with the Gmail or mobile number used to create your account.");
        }
    }

    private void setupActionButtons() {
        binding.btnLogin.setOnClickListener(v -> {
            if (isSignUpMode) {
                handleCreateAccount();
            } else {
                handleSignIn();
            }
        });
    }

    private void handleSignIn() {
        hideSoftKeyboard();
        String identifier = getTrimmedText(binding.etLoginEmail);
        String password = getTrimmedText(binding.etLoginPassword);

        binding.tilLoginEmail.setError(null);
        binding.tilLoginPassword.setError(null);

        if (identifier.isEmpty()) {
            binding.tilLoginEmail.setError("Please enter your registered Gmail or mobile number");
            binding.etLoginEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            binding.tilLoginPassword.setError("Please enter your password");
            binding.etLoginPassword.requestFocus();
            return;
        }

        setLoading(true);

        GymDatabaseHelper.AuthResult result = dbHelper.authenticateUser(identifier, password);
        setLoading(false);

        if (result.isSuccess() && result.getAccount() != null) {
            UserAccount account = result.getAccount();
            AuthManager.setLoggedIn(this, true, account.getIdentifier(), account.getName());
            Toast.makeText(this, "Welcome back, " + account.getName() + "!", Toast.LENGTH_SHORT).show();
            navigateToMain();
        } else if (result.getStatus() == GymDatabaseHelper.AuthStatus.USER_NOT_FOUND) {
            binding.tilLoginEmail.setError("No account found with this Gmail or mobile number");
            new AlertDialog.Builder(this)
                    .setTitle("Account Not Found")
                    .setMessage("No registered account was found with: \"" + identifier + "\".\n\nWould you like to create an account now?")
                    .setPositiveButton("Create Account", (dialog, which) -> {
                        setAuthMode(true);
                        binding.etLoginEmail.setText(identifier);
                        binding.etLoginName.requestFocus();
                    })
                    .setNegativeButton("Try Again", null)
                    .show();
        } else if (result.getStatus() == GymDatabaseHelper.AuthStatus.WRONG_PASSWORD) {
            binding.tilLoginPassword.setError("Incorrect password. Please try again.");
            binding.etLoginPassword.requestFocus();
        } else {
            Toast.makeText(this, result.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void handleCreateAccount() {
        hideSoftKeyboard();
        String name = getTrimmedText(binding.etLoginName);
        String identifier = getTrimmedText(binding.etLoginEmail);
        String password = getTrimmedText(binding.etLoginPassword);
        String confirmPassword = getTrimmedText(binding.etLoginConfirmPassword);

        binding.tilLoginName.setError(null);
        binding.tilLoginEmail.setError(null);
        binding.tilLoginPassword.setError(null);
        binding.tilLoginConfirmPassword.setError(null);

        if (name.isEmpty()) {
            binding.tilLoginName.setError("Please enter your full name");
            binding.etLoginName.requestFocus();
            return;
        }

        if (identifier.isEmpty()) {
            binding.tilLoginEmail.setError("Please enter a Gmail address or mobile number");
            binding.etLoginEmail.requestFocus();
            return;
        }

        if (identifier.length() < 4 || identifier.contains(" ")) {
            binding.tilLoginEmail.setError("Please enter a valid Gmail address or mobile number without spaces");
            binding.etLoginEmail.requestFocus();
            return;
        }

        if (password.isEmpty() || password.length() < 6) {
            binding.tilLoginPassword.setError("Password must be at least 6 characters");
            binding.etLoginPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            binding.tilLoginConfirmPassword.setError("Passwords do not match");
            binding.etLoginConfirmPassword.requestFocus();
            return;
        }

        // Check if account already exists
        if (dbHelper.isAccountRegistered(identifier)) {
            binding.tilLoginEmail.setError("This Gmail or mobile number is already registered. Please sign in.");
            new AlertDialog.Builder(this)
                    .setTitle("Account Already Exists")
                    .setMessage("An account with \"" + identifier + "\" is already registered. Please sign in.")
                    .setPositiveButton("Go to Sign In", (dialog, which) -> {
                        redirectToSignIn(identifier);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        // Optional baseline metrics
        double height = parseDoubleOrZero(getTrimmedText(binding.etLoginHeight));
        double weight = parseDoubleOrZero(getTrimmedText(binding.etLoginWeight));
        double targetWeight = parseDoubleOrZero(getTrimmedText(binding.etLoginTargetWeight));

        setLoading(true);

        boolean registered = dbHelper.registerAccount(name, identifier, password, height, weight, targetWeight);
        setLoading(false);

        if (registered) {
            AuthManager.setLastRegisteredIdentifier(this, identifier);

            // Redirect user to Sign In page as explicitly instructed
            new AlertDialog.Builder(this)
                    .setTitle("Account Created Successfully!")
                    .setMessage("Your account has been created for " + name + ".\n\nPlease sign in with your credentials to continue.")
                    .setCancelable(false)
                    .setPositiveButton("Sign In Now", (dialog, which) -> {
                        redirectToSignIn(identifier);
                    })
                    .show();
        } else {
            Toast.makeText(this, "Failed to create account. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Redirects to the Sign In screen with the created Gmail / mobile number pre-filled.
     */
    private void redirectToSignIn(@NonNull String registeredIdentifier) {
        setAuthMode(false);
        binding.etLoginEmail.setText(registeredIdentifier);
        binding.etLoginPassword.setText("");
        binding.etLoginConfirmPassword.setText("");
        binding.etLoginPassword.requestFocus();
        Toast.makeText(this, "Please enter your password to sign in", Toast.LENGTH_SHORT).show();
    }

    private void setupLiveBmiCalculator() {
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

        binding.etLoginHeight.addTextChangedListener(bmiWatcher);
        binding.etLoginWeight.addTextChangedListener(bmiWatcher);
    }

    private void calculateLiveBmi() {
        String heightStr = getTrimmedText(binding.etLoginHeight);
        String weightStr = getTrimmedText(binding.etLoginWeight);

        if (heightStr.isEmpty() || weightStr.isEmpty()) {
            binding.tvRegisterBmiValue.setText("Live BMI Calculator");
            binding.tvRegisterBmiDesc.setText("Enter height & body mass above to calculate baseline BMI");
            return;
        }

        try {
            double heightCm = Double.parseDouble(heightStr);
            double weightKg = Double.parseDouble(weightStr);

            if (heightCm > 50 && weightKg > 20) {
                double heightM = heightCm / 100.0;
                double bmi = weightKg / (heightM * heightM);
                String category = getBmiCategory(bmi);

                binding.tvRegisterBmiValue.setText(String.format(Locale.US, "Baseline BMI: %.1f (%s)", bmi, category));
                binding.tvRegisterBmiDesc.setText("Baseline calibrated for training suggestions & volume tracking.");
            }
        } catch (NumberFormatException ignored) {}
    }

    private String getBmiCategory(double bmi) {
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25.0) return "Normal weight";
        if (bmi < 30.0) return "Overweight";
        return "Obese";
    }

    private void setLoading(boolean loading) {
        binding.progressAuth.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideSoftKeyboard();
    }

    private void hideSoftKeyboard() {
        View current = getCurrentFocus();
        if (current != null) {
            current.clearFocus();
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(current.getWindowToken(), 0);
            }
        }
        if (getWindow() != null && getWindow().getDecorView() != null) {
            androidx.core.view.WindowInsetsControllerCompat controller =
                    androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            if (controller != null) {
                controller.hide(androidx.core.view.WindowInsetsCompat.Type.ime());
            }
        }
    }

    private void navigateToMain() {
        hideSoftKeyboard();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @NonNull
    private String getTrimmedText(android.widget.EditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private double parseDoubleOrZero(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0.0;
        }
    }
}
