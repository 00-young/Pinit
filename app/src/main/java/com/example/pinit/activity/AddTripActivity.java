package com.example.pinit.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.pinit.R;
import com.example.pinit.model.Trip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddTripActivity extends AppCompatActivity {

    private EditText etTitle, etDestination, etStartDate, etEndDate, etBudget, etMemo;
    private FirebaseFirestore db; // 💡 로컬 DB 대신 파이어베이스 인스턴스 도입
    private String firestoreDocId = null; // 💡 수정할 파이어베이스 문서 ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_trip);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("여행 추가");
        }

        etTitle = findViewById(R.id.etTitle);
        etDestination = findViewById(R.id.etDestination);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        etBudget = findViewById(R.id.etBudget);
        etMemo = findViewById(R.id.etMemo);

        // 인텐트 데이터 수신 (수정 모드 대비)
        firestoreDocId = getIntent().getStringExtra("firestore_doc_id");
        int editTripId = getIntent().getIntExtra("edit_trip_id", -1);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // 💡 방어 코드: 만약 문서 ID가 없는데 정수 ID가 넘어왔다면 규격(uid_tripId)대로 강제 매핑 복원
        if (firestoreDocId == null && user != null && editTripId != -1) {
            firestoreDocId = user.getUid() + "_" + editTripId;
        }

        // =========================================================
        // 💡 [수정 모드] 파이어베이스에서 실시간으로 읽어와 빈칸 채우기
        // =========================================================
        if (firestoreDocId != null) {
            db.collection("schedules").document(firestoreDocId).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle("여행 수정");
                    }

                    etTitle.setText(doc.getString("title"));
                    etDestination.setText(doc.getString("country")); // destination 매칭
                    etStartDate.setText(doc.getString("startDate"));
                    etEndDate.setText(doc.getString("endDate"));

                    Double budget = doc.getDouble("budget");
                    etBudget.setText(budget != null ? String.valueOf(budget.intValue()) : "0");
                    etMemo.setText(doc.getString("memo"));

                    ((Button) findViewById(R.id.btnSave)).setText("수정 완료");
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            });
        }

        // [추가 모드] 기본 날짜 오늘로 세팅
        if (firestoreDocId == null) {
            Calendar cal = Calendar.getInstance();
            String today = String.format(Locale.KOREA, "%d-%02d-%02d",
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
            etStartDate.setText(today);
            etEndDate.setText(today);
        }

        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> saveTripToFirebase());
    }

    private void showDatePicker(EditText et) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) ->
                et.setText(String.format(Locale.KOREA, "%d-%02d-%02d", year, month + 1, day)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    // =========================================================
    // 💡 순수 파이어베이스 저장 및 수정 로직
    // =========================================================
    private void saveTripToFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = etTitle.getText().toString().trim();
        String destination = etDestination.getText().toString().trim();

        if (title.isEmpty() || destination.isEmpty()) {
            Toast.makeText(this, "여행 이름과 목적지를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            Date start = sdf.parse(etStartDate.getText().toString());
            Date end = sdf.parse(etEndDate.getText().toString());
            if (start != null && end != null && start.after(end)) {
                Toast.makeText(this, "출발일이 귀국일보다 늦을 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (ParseException ignored) {}

        // 파이어베이스 업로드용 Map 생성 (HomeFragment 컬렉션 필드와 100% 일치)
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUid());
        data.put("title", title);
        data.put("country", destination); // HomeFragment 가 읽어오는 키 명칭
        data.put("startDate", etStartDate.getText().toString());
        data.put("endDate", etEndDate.getText().toString());
        data.put("budget", etBudget.getText().toString().isEmpty() ? 0.0 : Double.parseDouble(etBudget.getText().toString()));
        data.put("memo", etMemo.getText().toString());

        if (firestoreDocId != null) {
            // 1️ [수정 완료] 버튼 분기 처리
            db.collection("schedules").document(firestoreDocId).update(data)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "여행이 수정되었습니다!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "수정 실패", Toast.LENGTH_SHORT).show());
        } else {
            // 2️ [새 여행 추가] 버튼 분기 처리
            // 다른 액티비티들과의 정수형 ID 호환성을 유지하기 위해 타임스탬프 기반 정수 추출 고유 난수 생성
            int newTripId = (int) (System.currentTimeMillis() % 100000000);
            String syncDocId = user.getUid() + "_" + newTripId;

            db.collection("schedules").document(syncDocId).set(data)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "여행이 추가되었습니다!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "추가 실패", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}