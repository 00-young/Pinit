package com.example.pinit.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.pinit.R;
import com.example.pinit.database.DatabaseHelper;
import com.example.pinit.model.Schedule;
import com.example.pinit.database.FirestoreRepository;
import com.example.pinit.model.Trip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;
import java.util.Locale;

public class AddScheduleActivity extends AppCompatActivity {

    private EditText etTitle, etDate, etTime, etPlaceName, etMemo;
    private DatabaseHelper dbHelper;
    private int tripId;
    private int editScheduleId = -1;
    private ActivityResultLauncher<Intent> mapActivityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_schedule);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dbHelper = new DatabaseHelper(this);
        tripId = getIntent().getIntExtra("trip_id", -1);
        editScheduleId = getIntent().getIntExtra("schedule_id", -1);
        originalDate =
                getIntent().getStringExtra(
                        "schedule_date"
                );

        etTitle = findViewById(R.id.etTitle);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etPlaceName = findViewById(R.id.etPlaceName);
        etMemo = findViewById(R.id.etMemo);

        boolean isEditMode = editScheduleId != -1;

        if (isEditMode) {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("일정 수정");
            ((android.widget.Button) findViewById(R.id.btnSave)).setText("수정 완료");
            etTitle.setText(getIntent().getStringExtra("schedule_title"));
            etDate.setText(getIntent().getStringExtra("schedule_date"));
            etTime.setText(getIntent().getStringExtra("schedule_time"));
            etPlaceName.setText(getIntent().getStringExtra("schedule_place"));
            etMemo.setText(getIntent().getStringExtra("schedule_memo"));
        } else {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("일정 추가");
            String defaultDate = getIntent().getStringExtra("default_date");
            if (defaultDate != null) {
                etDate.setText(defaultDate);
            } else {
                Calendar cal = Calendar.getInstance();
                etDate.setText(String.format(Locale.KOREA, "%d-%02d-%02d",
                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)));
            }
        }

        // 1. 날짜 선택 리스너
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) ->
                    etDate.setText(String.format(Locale.KOREA, "%d-%02d-%02d", year, month + 1, day)),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // 저장 버튼 클릭 리스너
        findViewById(R.id.btnSave).setOnClickListener(v -> saveSchedule());

        // 2. 시간 선택 리스너
        etTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            new android.app.TimePickerDialog(this, (view, selectedHour, selectedMinute) -> {
                etTime.setText(String.format(Locale.KOREA, "%02d:%02d", selectedHour, selectedMinute));
            }, hour, minute, false).show();
        });

        // 3. 지도 콜백 등록
        mapActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            String placeName = result.getData().getStringExtra("place_name");
                            etPlaceName.setText(placeName);
                        }
                    }
                }
        );

        // 4. 장소 선택 리스너
        etPlaceName.setOnClickListener(v -> {
            Intent intent = new Intent(AddScheduleActivity.this, MapActivity.class);
            mapActivityResultLauncher.launch(intent);
        });
    }

    private void saveSchedule() {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "일정 제목을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        Schedule s = new Schedule();
        s.setTitle(title);
        s.setDate(etDate.getText().toString());
        s.setTime(etTime.getText().toString());
        s.setPlaceName(etPlaceName.getText().toString());
        s.setMemo(etMemo.getText().toString());
        s.setLatitude(
                getIntent().getDoubleExtra(
                        "schedule_latitude",
                        0
                )
        );

        s.setLongitude(
                getIntent().getDoubleExtra(
                        "schedule_longitude",
                        0
                )
        );

        s.setColor(
                getIntent().getStringExtra(
                        "schedule_color"
                )
        );

        s.setCategory(
                getIntent().getStringExtra(
                        "schedule_category"
                )
        );

        s.setGooglePlaceId(
                getIntent().getStringExtra(
                        "schedule_google_place_id"
                )
        );

        if (editScheduleId != -1) {
            s.setId(editScheduleId);
            dbHelper.updateSchedule(s);

            // =========================
            // Firestore 수정
            // =========================
            FirebaseUser user =
                    FirebaseAuth.getInstance()
                            .getCurrentUser();

            if (user != null) {
                FirestoreRepository repository = new FirestoreRepository();
                String scheduleId = user.getUid() + "_" + tripId;
                String newDayId = s.getDate();

                String itemId =
                        "item" + editScheduleId;

                // =========================
                // 날짜 변경 없는 경우
                // =========================

                if (originalDate != null
                        && originalDate.equals(newDayId)) {

                    repository.updateItem(
                            scheduleId,
                            newDayId,
                            itemId,
                            s
                    );

                } else {

                    // =========================
                    // 기존 item 삭제
                    // =========================

                    repository.deleteItem(
                            scheduleId,
                            originalDate,
                            itemId
                    );

                    // =========================
                    // 새로운 day에 item 생성
                    // =========================

                    repository.uploadDay(
                            scheduleId,
                            newDayId,
                            1,
                            newDayId
                    );

                    repository.uploadItem(
                            scheduleId,
                            newDayId,
                            itemId,
                            s
                    );
                }
            }
            Toast.makeText(
                    this,
                    "일정이 수정되었습니다!",
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            s.setTripId(tripId);
            s.setColor("#FFDA44");

            long insertedId =
                    dbHelper.insertSchedule(s);

            s.setId((int) insertedId);

            // =========================
            // Firestore 업로드
            // =========================

            FirebaseUser user =
                    FirebaseAuth.getInstance()
                            .getCurrentUser();

            if (user != null) {

                Trip trip =
                        dbHelper.getTripById(tripId);

                if (trip != null) {

                    FirestoreRepository repository =
                            new FirestoreRepository();

                    String scheduleId =
                            user.getUid() + "_"
                                    + tripId;

                    String dayId =
                            s.getDate();

                    String itemId =
                            "item" + insertedId;

                    repository.uploadDay(
                            scheduleId,
                            dayId,
                            1,
                            dayId
                    );

                    repository.uploadItem(
                            scheduleId,
                            dayId,
                            itemId,
                            s
                    );
                }
            }
            Toast.makeText(this, "일정이 추가되었습니다!", Toast.LENGTH_SHORT).show();
        }
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}