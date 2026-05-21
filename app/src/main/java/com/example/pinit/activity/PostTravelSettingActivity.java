package com.example.pinit.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pinit.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostTravelSettingActivity extends AppCompatActivity {

    public static final String EXTRA_START_DATE = "post_setting_start_date";
    public static final String EXTRA_END_DATE = "post_setting_end_date";
    public static final String EXTRA_COUNTRY = "post_setting_country";
    public static final String EXTRA_PEOPLE = "post_setting_people";

    private Spinner spinnerYear;
    private Spinner spinnerMonth;
    private GridLayout calendarGrid;
    private TextView selectedDateRange;
    private EditText countryEditText;
    private ChipGroup peopleChipGroup;

    private Calendar startDate;
    private Calendar endDate;
    private String searchQuery = "";
    private String savedCountry = "";
    private String selectedPeople = "";
    private final String[] peopleOptions = {"혼자", "2명", "3~4명", "가족"};
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.KOREAN);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_travel_setting);

        searchQuery = getIntent().getStringExtra(PostSearchActivity.EXTRA_SEARCH_QUERY);
        if (searchQuery == null) searchQuery = "";

        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerMonth = findViewById(R.id.spinnerMonth);
        calendarGrid = findViewById(R.id.calendarGrid);
        selectedDateRange = findViewById(R.id.tvSelectedDateRange);
        countryEditText = findViewById(R.id.etCountry);
        peopleChipGroup = findViewById(R.id.peopleChipGroup);

        //기존 코드 부분
        //findViewById(R.id.btnCloseSetting).setOnClickListener(v -> openSearchScreen());

        // ✨ [내가 추가한 코드] 닫기(X) 버튼
        findViewById(R.id.btnCloseSetting).setOnClickListener(v -> {
            if (getCallingActivity() != null) {
                finish();
            } else {
                openSearchScreen();
            }
        });

        //원래 있던 기존 코드 부분
        findViewById(R.id.btnSaveDate).setOnClickListener(v ->
                Toast.makeText(this, selectedDateRange.getText().toString(), Toast.LENGTH_SHORT).show());

        // 기존 코드 부분
        // findViewById(R.id.btnApplySetting).setOnClickListener(v -> openSearchScreen());

        // ✨ [내가 추가한 코드] 적용 버튼
        findViewById(R.id.btnApplySetting).setOnClickListener(v -> {
            if (getCallingActivity() != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selectedDate", selectedDateRange.getText().toString());
                resultIntent.putExtra("selectedCountry", savedCountry);
                resultIntent.putExtra("selectedPeople", selectedPeople);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            } else {
                openSearchScreen();
            }
        });

        //여기서부터 기존 코드 부분
        Button btnSaveCountry = findViewById(R.id.btnSaveCountry);
        ImageButton btnCountrySearch = findViewById(R.id.btnCountrySearch);
        View.OnClickListener saveCountry = v -> saveCountry();
        btnSaveCountry.setOnClickListener(saveCountry);
        btnCountrySearch.setOnClickListener(saveCountry);
        countryEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveCountry();
                return true;
            }
            return false;
        });

        setupSpinners();
        setupPeopleChips();
        setupBottomNavigation();
        updateSelectedDateRange();
        drawCalendar();
        countryEditText.clearFocus();
        findViewById(R.id.btnDateSetting).requestFocus();
    }

    private void setupSpinners() {
        Calendar today = Calendar.getInstance();
        List<String> years = new ArrayList<>();
        for (int year = today.get(Calendar.YEAR) - 2; year <= today.get(Calendar.YEAR) + 5; year++) {
            years.add(year + "년");
        }

        List<String> months = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            months.add(String.format(Locale.KOREAN, "%02d월", month));
        }

        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, years);
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, months);
        spinnerYear.setAdapter(yearAdapter);
        spinnerMonth.setAdapter(monthAdapter);
        spinnerYear.setSelection(2);
        spinnerMonth.setSelection(today.get(Calendar.MONTH));

        spinnerYear.setOnItemSelectedListener(new SimpleItemSelectedListener(this::drawCalendar));
        spinnerMonth.setOnItemSelectedListener(new SimpleItemSelectedListener(this::drawCalendar));
    }

    private void drawCalendar() {
        if (calendarGrid == null || spinnerYear.getSelectedItem() == null || spinnerMonth.getSelectedItem() == null) {
            return;
        }

        calendarGrid.removeAllViews();
        String[] weekdays = {"일", "월", "화", "수", "목", "금", "토"};
        for (String weekday : weekdays) {
            TextView cell = createCalendarCell(weekday, true);
            cell.setBackgroundColor(Color.rgb(255, 211, 0));
            calendarGrid.addView(cell);
        }

        int year = Integer.parseInt(spinnerYear.getSelectedItem().toString().replace("년", ""));
        int month = Integer.parseInt(spinnerMonth.getSelectedItem().toString().replace("월", ""));
        Calendar firstDay = Calendar.getInstance();
        firstDay.set(year, month - 1, 1);
        int firstWeekday = firstDay.get(Calendar.DAY_OF_WEEK) - 1;
        int lastDay = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < firstWeekday; i++) {
            calendarGrid.addView(createCalendarCell("", false));
        }

        for (int day = 1; day <= lastDay; day++) {
            Calendar date = Calendar.getInstance();
            date.set(year, month - 1, day, 0, 0, 0);
            date.set(Calendar.MILLISECOND, 0);

            TextView cell = createCalendarCell(String.valueOf(day), false);
            cell.setOnClickListener(v -> selectDate(date));
            if (isSelectedOrInRange(date)) {
                cell.setBackgroundColor(Color.rgb(255, 235, 143));
            }
            calendarGrid.addView(cell);
        }
    }

    private TextView createCalendarCell(String text, boolean header) {
        TextView cell = new TextView(this);
        cell.setText(text);
        cell.setGravity(Gravity.CENTER);
        cell.setTextColor(Color.rgb(20, 20, 20));
        cell.setTextSize(header ? 16 : 20);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = getResources().getDisplayMetrics().widthPixels / 7 - dp(6);
        params.height = header ? dp(40) : dp(62);
        cell.setLayoutParams(params);
        return cell;
    }

    private void selectDate(Calendar date) {
        Calendar selected = (Calendar) date.clone();
        if (startDate == null || endDate != null) {
            startDate = selected;
            endDate = null;
        } else if (selected.before(startDate)) {
            endDate = startDate;
            startDate = selected;
        } else {
            endDate = selected;
        }
        updateSelectedDateRange();
        drawCalendar();
    }

    private boolean isSelectedOrInRange(Calendar date) {
        if (startDate == null) return false;
        Date target = date.getTime();
        if (endDate == null) return dateFormat.format(target).equals(dateFormat.format(startDate.getTime()));
        return !target.before(startDate.getTime()) && !target.after(endDate.getTime());
    }

    private void updateSelectedDateRange() {
        if (startDate == null) {
            selectedDateRange.setText("날짜를 선택하세요");
        } else if (endDate == null) {
            selectedDateRange.setText(dateFormat.format(startDate.getTime()));
        } else {
            selectedDateRange.setText(dateFormat.format(startDate.getTime()) + " ~ " + dateFormat.format(endDate.getTime()));
        }
    }

    private void saveCountry() {
        savedCountry = countryEditText.getText().toString().trim();
        if (savedCountry.isEmpty()) {
            Toast.makeText(this, "나라 또는 도시를 입력해주세요.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, savedCountry + " 저장", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupPeopleChips() {
        peopleChipGroup.removeAllViews();
        for (String option : peopleOptions) {
            Chip chip = new Chip(this);
            chip.setText(option);
            chip.setTextColor(Color.rgb(17, 17, 17));
            chip.setTextSize(14);
            chip.setSingleLine(true);
            chip.setCheckable(true);
            chip.setChipBackgroundColor(new ColorStateList(
                    new int[][]{
                            new int[]{android.R.attr.state_checked},
                            new int[]{}
                    },
                    new int[]{
                            Color.rgb(255, 245, 200),
                            Color.WHITE
                    }
            ));
            chip.setChipStrokeColor(ColorStateList.valueOf(Color.rgb(40, 40, 40)));
            chip.setChipStrokeWidth(1);
            chip.setOnClickListener(v -> selectedPeople = chip.isChecked() ? option : "");
            peopleChipGroup.addView(chip);
        }
    }

    private void openSearchScreen() {
        Intent intent = new Intent(this, PostSearchActivity.class);
        intent.putExtra(PostSearchActivity.EXTRA_SEARCH_QUERY, searchQuery);
        if (startDate != null) {
            intent.putExtra(EXTRA_START_DATE, dateFormat.format(startDate.getTime()));
        }
        if (endDate != null) {
            intent.putExtra(EXTRA_END_DATE, dateFormat.format(endDate.getTime()));
        }
        if (!savedCountry.isEmpty()) {
            intent.putExtra(EXTRA_COUNTRY, savedCountry);
        }
        if (!selectedPeople.isEmpty()) {
            intent.putExtra(EXTRA_PEOPLE, selectedPeople);
        }
        startActivity(intent);
        finish();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_community);
        bottomNav.setOnItemSelectedListener(item -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("selected_nav", item.getItemId());
            startActivity(intent);
            finish();
            return true;
        });
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class SimpleItemSelectedListener implements android.widget.AdapterView.OnItemSelectedListener {
        private final Runnable onSelected;

        SimpleItemSelectedListener(Runnable onSelected) {
            this.onSelected = onSelected;
        }

        @Override
        public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
            onSelected.run();
        }

        @Override
        public void onNothingSelected(android.widget.AdapterView<?> parent) {
        }
    }
}
