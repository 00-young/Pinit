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

// [화면] 장소 검색 전용 Activity (현재 직접 진입 경로는 없음)
// PlaceFragment 내의 장소 검색 패널(panelSearch)과 기능이 중복됨
// PlaceAdapter로 검색 결과를 RecyclerView에 표시, 항목 클릭 시 PlaceDetailActivity로 이동
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

        // isPickingMode: 게시물 작성 시 장소 첨부용으로 진입할 때 true
        // true이면 클릭 시 선택한 장소 이름/주소를 결과로 반환, false이면 PlaceDetailActivity로 이동
        boolean isPickingMode = getIntent().getBooleanExtra("isPickingMode", false);
        adapter = new PlaceAdapter(this, new ArrayList<>(), place -> {
            if (isPickingMode) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selectedPlaceName", place.get("name"));
                String address = place.containsKey("address") ? place.get("address") : "주소 정보 없음";
                resultIntent.putExtra("selectedPlaceAddress", address);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            } else {
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
            // Google Places 텍스트 검색 실행 (백그라운드 스레드, 결과는 UI 스레드에서 반영)
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
