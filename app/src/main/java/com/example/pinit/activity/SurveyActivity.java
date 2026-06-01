package com.example.pinit.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pinit.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SurveyActivity extends AppCompatActivity {

    private ChipGroup chipGroupAge, chipGroupCompanion, chipGroupTheme, chipGroupBudget;
    private Button btnDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);

        chipGroupAge = findViewById(R.id.chipGroupAge);
        chipGroupCompanion = findViewById(R.id.chipGroupCompanion);
        chipGroupTheme = findViewById(R.id.chipGroupTheme);
        chipGroupBudget = findViewById(R.id.chipGroupBudget);
        btnDone = findViewById(R.id.btnSurveyDone);

        btnDone.setOnClickListener(v -> submitSurvey());
    }

    private void submitSurvey() {
        String ageGroup = getCheckedChipText(chipGroupAge);
        String companion = getCheckedChipText(chipGroupCompanion);
        String theme = getCheckedThemes();
        String budgetType = getCheckedChipText(chipGroupBudget);

        if (ageGroup == null || companion == null || theme.isEmpty() || budgetType == null) {
            Toast.makeText(this, "모든 항목을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnDone.setEnabled(false);

        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        Map<String, Object> data = new HashMap<>();
        data.put("ageGroup", ageGroup);
        data.put("companion", companion);
        data.put("theme", theme);
        data.put("budgetType", budgetType);

        FirebaseFirestore.getInstance()
                .collection("users").document(email)
                .update(data)
                .addOnSuccessListener(aVoid -> {
                    SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                    prefs.edit().putBoolean("survey_done_" + email, true).apply();

                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnDone.setEnabled(true);
                    Toast.makeText(this, "저장 실패, 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                });
    }

    private String getCheckedChipText(ChipGroup group) {
        int checkedId = group.getCheckedChipId();
        if (checkedId == -1) return null;
        Chip chip = group.findViewById(checkedId);
        return chip != null ? chip.getText().toString() : null;
    }

    private String getCheckedThemes() {
        List<Integer> ids = chipGroupTheme.getCheckedChipIds();
        List<String> themes = new ArrayList<>();
        for (int id : ids) {
            Chip chip = chipGroupTheme.findViewById(id);
            if (chip != null) themes.add(chip.getText().toString());
        }
        return String.join(",", themes);
    }
}
