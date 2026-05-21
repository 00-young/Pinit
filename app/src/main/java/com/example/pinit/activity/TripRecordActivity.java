package com.example.pinit.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.pinit.R;
import com.example.pinit.database.DatabaseHelper;
import com.example.pinit.model.Record;

import java.util.Calendar;
import java.util.Locale;

public class TripRecordActivity extends AppCompatActivity {

    private EditText etTitle, etDate, etContent, etPlaceName;
    private DatabaseHelper dbHelper;
    private int tripId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_record);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("여행 기록");
        }

        dbHelper = new DatabaseHelper(this);
        tripId = getIntent().getIntExtra("trip_id", -1);

        etTitle = findViewById(R.id.etTitle);
        etDate = findViewById(R.id.etDate);
        etContent = findViewById(R.id.etContent);
        etPlaceName = findViewById(R.id.etPlaceName);

        Calendar cal = Calendar.getInstance();
        etDate.setText(String.format(Locale.KOREA, "%d-%02d-%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH)));
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) ->
                    etDate.setText(String.format(Locale.KOREA, "%d-%02d-%02d", year, month+1, day)),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> saveRecord());
    }

    private void saveRecord() {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "제목을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        Record r = new Record();
        r.setTripId(tripId);
        r.setTitle(title);
        r.setDate(etDate.getText().toString());
        r.setContent(etContent.getText().toString());
        r.setPlaceName(etPlaceName.getText().toString());
        dbHelper.insertRecord(r);
        Toast.makeText(this, "기록이 저장되었습니다!", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
