package com.example.pinit.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;
import com.example.pinit.activity.PlaceDetailActivity;
import com.example.pinit.adapter.PlaceAdapter;
import com.example.pinit.database.PlacesApiHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NearbyFragment extends Fragment {

    private PlacesApiHelper apiHelper;
    private PlaceAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 0, currentLng = 0;
    private static final int PERMISSION_REQUEST = 100;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nearby, container, false);

        apiHelper = new PlacesApiHelper();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        tvLocation = view.findViewById(R.id.tvLocation);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PlaceAdapter(requireContext(), new ArrayList<>(), place -> {
            Intent intent = new Intent(requireContext(), PlaceDetailActivity.class);
            intent.putExtra("place_id", place.get("place_id"));
            intent.putExtra("place_name", place.get("name"));
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.btnRestaurant).setOnClickListener(v -> searchNearby("restaurant", "🍽️ 주변 음식점"));
        view.findViewById(R.id.btnCafe).setOnClickListener(v -> searchNearby("cafe", "☕ 주변 카페"));
        view.findViewById(R.id.btnAttraction).setOnClickListener(v -> searchNearby("tourist_attraction", "🏛️ 주변 관광지"));
        view.findViewById(R.id.btnHotel).setOnClickListener(v -> searchNearby("lodging", "🏨 주변 숙소"));

        getCurrentLocation();
        return view;
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();
                tvLocation.setText("📍 현재 위치: " + String.format("%.4f, %.4f", currentLat, currentLng));
            }
        });
    }

    private void searchNearby(String type, String label) {
        if (currentLat == 0 && currentLng == 0) {
            Toast.makeText(requireContext(), "위치를 가져오는 중입니다. 잠시 후 시도해주세요.", Toast.LENGTH_SHORT).show();
            getCurrentLocation();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        tvLocation.setText(label + " 검색 중...");
        apiHelper.searchNearby(currentLat, currentLng, type, 2000, new PlacesApiHelper.PlacesCallback() {
            @Override
            public void onSuccess(List<Map<String, String>> places) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvLocation.setText(label + " - " + places.size() + "개 발견");
                    adapter.updateList(places);
                });
            }
            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvLocation.setText("검색 실패");
                    Toast.makeText(requireContext(), "오류: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }
}
