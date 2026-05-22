package com.example.pinit.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;
import com.example.pinit.adapter.PlaceAdapter;
import com.example.pinit.database.PlacesApiHelper;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlaceSearchActivity extends AppCompatActivity {

    private PlacesApiHelper apiHelper;
    private PlaceAdapter adapter;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_search);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("장소 검색");
        }

        apiHelper = new PlacesApiHelper();
        progressBar = findViewById(R.id.progressBar);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

//        adapter = new PlaceAdapter(this, new ArrayList<>(), place -> {
//            Intent intent = new Intent(this, PlaceDetailActivity.class);
//            intent.putExtra("place_id", place.get("place_id"));
//            intent.putExtra("place_name", place.get("name"));
//            startActivity(intent);
//        });
        // 🌟 1. 글쓰기 화면에서 장소 '선택'을 위해 이 화면을 열었는지 확인합니다.
        boolean isPickingMode = getIntent().getBooleanExtra("isPickingMode", false);

        adapter = new PlaceAdapter(this, new ArrayList<>(), place -> {
            if (isPickingMode) {
                // 📍 [상황 A] 글쓰기 화면에서 장소 첨부 버튼을 누르고 들어왔을 때 -> 데이터를 들고 돌아갑니다!
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selectedPlaceName", place.get("name"));

                String address = place.containsKey("address") ? place.get("address") : "주소 정보 없음";
                resultIntent.putExtra("selectedPlaceAddress", address);

                setResult(Activity.RESULT_OK, resultIntent);
                finish(); // 창 닫기

            } else {
                // 📍 [상황 B] 원래 기능팀 코드 (일반 장소 검색일 때) -> 상세 페이지로 넘어갑니다!
                Intent intent = new Intent(this, PlaceDetailActivity.class);
                intent.putExtra("place_id", place.get("place_id"));
                intent.putExtra("place_name", place.get("name"));
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);

        TextInputEditText etSearch = findViewById(R.id.etSearch);
        Button btnSearch = findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(this, "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            progressBar.setVisibility(android.view.View.VISIBLE);
            apiHelper.searchPlaces(query, null, new PlacesApiHelper.PlacesCallback() {
                @Override
                public void onSuccess(List<Map<String, String>> places) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(android.view.View.GONE);
                        adapter.updateList(places);
                    });
                }
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(android.view.View.GONE);
                        Toast.makeText(PlaceSearchActivity.this, "오류: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
