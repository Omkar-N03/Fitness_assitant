package com.example;

import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Utility for attaching Material / Android DatePickerDialog to date input fields.
 * Formats dates consistently as YYYY-MM-DD.
 */
public final class DatePickerUtil {

    private DatePickerUtil() {}

    /**
     * Attaches a DatePickerDialog to an EditText, triggering when clicked or focused.
     * Prevents keyboard popping up and automatically formats output as YYYY-MM-DD.
     */
    public static void attachDatePicker(Context context, EditText editText) {
        if (editText == null || context == null) return;

        // Prevent virtual keyboard from popping up on the date field
        editText.setFocusable(false);
        editText.setClickable(true);
        editText.setCursorVisible(false);

        editText.setOnClickListener(v -> showDatePicker(context, editText));
    }

    public static void showDatePicker(Context context, EditText editText) {
        Calendar calendar = Calendar.getInstance();
        String currentText = editText != null && editText.getText() != null ? editText.getText().toString().trim() : "";

        if (!currentText.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                java.util.Date parsed = sdf.parse(currentText);
                if (parsed != null) {
                    calendar.setTime(parsed);
                }
            } catch (Exception ignored) {}
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                context,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(selectedYear, selectedMonth, selectedDay);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    if (editText != null) {
                        editText.setText(sdf.format(selectedCal.getTime()));
                    }
                },
                year,
                month,
                day
        );

        datePickerDialog.show();
    }
}
