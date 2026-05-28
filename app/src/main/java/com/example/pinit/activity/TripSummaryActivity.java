package com.example.pinit.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.pinit.R;
import com.example.pinit.database.DatabaseHelper;
import com.example.pinit.model.Budget;
import com.example.pinit.model.Record;
import com.example.pinit.model.Schedule;
import com.example.pinit.model.Trip;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TripSummaryActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private int tripId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_summary);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("여행 요약");
        }

        dbHelper = new DatabaseHelper(this);
        tripId = getIntent().getIntExtra("trip_id", -1);
        loadSummary();
    }

    private void loadSummary() {
        Trip trip = dbHelper.getTripById(tripId);
        if (trip == null) { finish(); return; }

        NumberFormat fmt = NumberFormat.getNumberInstance(Locale.KOREA);

        // 기본 정보
        ((TextView) findViewById(R.id.tvTitle)).setText(trip.getTitle());
        ((TextView) findViewById(R.id.tvDestination)).setText("📍 " + trip.getDestination());
        ((TextView) findViewById(R.id.tvDate)).setText("📅 " + trip.getStartDate() + " ~ " + trip.getEndDate());
        ((TextView) findViewById(R.id.tvDday)).setText(calcDday(trip.getStartDate(), trip.getEndDate()));

        // 예산
        List<Budget> budgets = dbHelper.getBudgetsByTrip(tripId);
        double used = 0, food = 0, transport = 0, lodge = 0, shopping = 0, etc = 0;
        for (Budget b : budgets) {
            if ("expense".equals(b.getType())) {
                used += b.getAmount();
                switch (b.getCategory()) {
                    case "식비": food += b.getAmount(); break;
                    case "교통": transport += b.getAmount(); break;
                    case "숙박": lodge += b.getAmount(); break;
                    case "쇼핑": shopping += b.getAmount(); break;
                    default: etc += b.getAmount(); break;
                }
            }
        }
        double remain = trip.getBudget() - used;
        int percent = trip.getBudget() > 0 ? (int) (used / trip.getBudget() * 100) : 0;

        ((TextView) findViewById(R.id.tvBudgetTotal)).setText(fmt.format(trip.getBudget()) + "원");
        ((TextView) findViewById(R.id.tvBudgetUsed)).setText(fmt.format(used) + "원");

        TextView tvRemain = findViewById(R.id.tvBudgetRemain);
        tvRemain.setText(fmt.format(remain) + "원");
        tvRemain.setTextColor(remain < 0 ? Color.RED : Color.parseColor("#4CAF50"));

        ((ProgressBar) findViewById(R.id.progressBudget)).setProgress(Math.min(percent, 100));
        ((TextView) findViewById(R.id.tvUsedPercent)).setText(percent + "%");

        ((TextView) findViewById(R.id.tvCatFood)).setText(fmt.format(food) + "원");
        ((TextView) findViewById(R.id.tvCatTransport)).setText(fmt.format(transport) + "원");
        ((TextView) findViewById(R.id.tvCatLodge)).setText(fmt.format(lodge) + "원");
        ((TextView) findViewById(R.id.tvCatShopping)).setText(fmt.format(shopping) + "원");
        ((TextView) findViewById(R.id.tvCatEtc)).setText(fmt.format(etc) + "원");

        // 일정
        List<Schedule> schedules = dbHelper.getSchedulesByTrip(tripId);
        ((TextView) findViewById(R.id.tvScheduleCount)).setText(String.valueOf(schedules.size()));

        StringBuilder scheduleSb = new StringBuilder();
        if (schedules.isEmpty()) {
            scheduleSb.append("등록된 일정이 없습니다.");
        } else {
            String lastDate = "";
            for (Schedule s : schedules) {
                if (!s.getDate().equals(lastDate)) {
                    if (scheduleSb.length() > 0) scheduleSb.append("\n");
                    scheduleSb.append("📅 ").append(s.getDate()).append("\n");
                    lastDate = s.getDate();
                }
                scheduleSb.append("  ");
                if (s.getTime() != null && !s.getTime().isEmpty())
                    scheduleSb.append(s.getTime()).append(" ");
                scheduleSb.append("• ").append(s.getTitle());
                if (s.getPlaceName() != null && !s.getPlaceName().isEmpty())
                    scheduleSb.append(" (").append(s.getPlaceName()).append(")");
                scheduleSb.append("\n");
            }
        }
        ((TextView) findViewById(R.id.tvScheduleSummary)).setText(scheduleSb.toString().trim());

        // 기록
        List<Record> records = dbHelper.getRecordsByTrip(tripId);
        ((TextView) findViewById(R.id.tvRecordCount)).setText(String.valueOf(records.size()));

        StringBuilder recordSb = new StringBuilder();
        if (records.isEmpty()) {
            recordSb.append("등록된 기록이 없습니다.");
        } else {
            for (Record r : records) {
                recordSb.append("📌 ").append(r.getTitle());
                if (r.getDate() != null && !r.getDate().isEmpty())
                    recordSb.append(" (").append(r.getDate()).append(")");
                recordSb.append("\n");
                if (r.getContent() != null && !r.getContent().isEmpty())
                    recordSb.append("   ").append(r.getContent()).append("\n");
            }
        }
        ((TextView) findViewById(R.id.tvRecordSummary)).setText(recordSb.toString().trim());
    }

    private String calcDday(String startStr, String endStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            Date start = sdf.parse(startStr);
            Date end = sdf.parse(endStr);
            Date today = new Date();
            long diffStart = (start.getTime() - today.getTime()) / (1000 * 60 * 60 * 24);
            long diffEnd = (end.getTime() - today.getTime()) / (1000 * 60 * 60 * 24);

            if (diffStart > 0) return "D-" + diffStart + " (여행까지 " + diffStart + "일 남음)";
            else if (diffEnd >= 0) return "✈️ 여행 중!";
            else return "D+" + Math.abs(diffStart) + " (여행 종료)";
        } catch (ParseException e) {
            return "";
        }
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}