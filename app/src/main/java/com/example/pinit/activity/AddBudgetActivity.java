package com.example.pinit.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.pinit.R;
import com.example.pinit.database.DatabaseHelper;
import com.example.pinit.model.Budget;

import java.util.Calendar;
import java.util.Locale;

public class AddBudgetActivity extends AppCompatActivity {

    private EditText etTitle, etAmount, etDate, etMemo;
    private RadioGroup rgType;
    private Spinner spinnerCategory;
    private DatabaseHelper dbHelper;
    private int tripId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_budget);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("지출 추가");
        }

        dbHelper = new DatabaseHelper(this);
        tripId = getIntent().getIntExtra("trip_id", -1);

        etTitle = findViewById(R.id.etTitle);
        etAmount = findViewById(R.id.etAmount);
        etDate = findViewById(R.id.etDate);
        etMemo = findViewById(R.id.etMemo);
        rgType = findViewById(R.id.rgType);
        spinnerCategory = findViewById(R.id.spinnerCategory);

        String[] categories = {"식비", "교통", "숙박", "쇼핑", "관광", "기타"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        Calendar cal = Calendar.getInstance();
        etDate.setText(String.format(Locale.KOREA, "%d-%02d-%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)));
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) ->
                    etDate.setText(String.format(Locale.KOREA, "%d-%02d-%02d", year, month + 1, day)),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // 영수증 스캔으로 넘어온 금액 자동 입력
        double autoAmount = getIntent().getDoubleExtra("auto_amount", 0);
        if (autoAmount > 0) {
            etAmount.setText(String.valueOf((int) autoAmount));
            etTitle.requestFocus();
            Toast.makeText(this, "영수증에서 인식된 금액이 자동 입력되었습니다.", Toast.LENGTH_SHORT).show();
        }

        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> saveBudget());
    }

    private void saveBudget() {
        String title = etTitle.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        if (title.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, "제목과 금액을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        Budget b = new Budget();
        b.setTripId(tripId);
        b.setTitle(title);
        b.setAmount(Double.parseDouble(amountStr));
        b.setCategory(spinnerCategory.getSelectedItem().toString());
        b.setDate(etDate.getText().toString());
        b.setType(rgType.getCheckedRadioButtonId() == R.id.rbIncome ? "income" : "expense");
        b.setMemo(etMemo.getText().toString());
        dbHelper.insertBudget(b);
        Toast.makeText(this, "저장되었습니다.", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
