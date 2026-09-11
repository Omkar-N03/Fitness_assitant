package com.example;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.databinding.ActivityMainBinding;

/**
 * MainActivity hosting BottomNavigationView and Android Navigation Component.
 * Initializes the raw SQLite database and populates exercises from CSV on startup.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Android Navigation Component with BottomNavigationView
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
        }

        // Asynchronously check and populate exercises from res/raw CSV if empty
        GymDatabaseHelper dbHelper = GymDatabaseHelper.getInstance(this);
        CsvLoader.loadExercisesIfEmpty(this, dbHelper, count -> {
            Log.d(TAG, "Gym database ready with " + count + " exercises.");
        });
    }
}
