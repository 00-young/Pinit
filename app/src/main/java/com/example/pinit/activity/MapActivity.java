package com.example.pinit.activity;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pinit.R;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng selectedLatLng;
    private String selectedPlaceName = ""; // 실제 구현시 역지오코딩(주소 변환)을 쓰거나 임시 명칭 사용

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), com.example.pinit.BuildConfig.GOOGLE_MAPS_API_KEY);
        }

        // 1. 지도 프래그먼트 연결
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        if (autocompleteFragment != null) {
            autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG));
            autocompleteFragment.setCountries("KR");

            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    if (place.getLatLng() != null) {
                        selectedLatLng = place.getLatLng();

                        if (place.getAddress() != null) {
                            selectedPlaceName = cleanUpAddress(place.getAddress());
                        } else {
                            selectedPlaceName = getRoadAddress(selectedLatLng);
                        }

                        if (mMap != null) {
                            mMap.clear();
                            mMap.addMarker(new MarkerOptions().position(selectedLatLng).title(place.getName()));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 16f));
                        }
                    }
                }

                @Override
                public void onError(@NonNull Status status) {
                    Toast.makeText(MapActivity.this, "검색 에러: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        // 2. 선택 완료 버튼 이벤트
        Button btnConfirm = findViewById(R.id.btnConfirmPlace);
        btnConfirm.setOnClickListener(v -> {
            if (selectedLatLng != null && !selectedPlaceName.isEmpty()) {
                Intent resultIntent = new Intent();
                // 이전 화면으로 장소 이름과 좌표 전달
                resultIntent.putExtra("place_name", selectedPlaceName);
                resultIntent.putExtra("latitude", selectedLatLng.latitude);
                resultIntent.putExtra("longitude", selectedLatLng.longitude);
                setResult(RESULT_OK, resultIntent);
                finish(); // 지도 화면 닫기
            } else {
                Toast.makeText(this, "지도를 클릭하거나 장소를 검색하여 선택해 주세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // 기본 위치 설정 (예: 서울 중심부)
        LatLng seoul = new LatLng(37.5665, 126.9780);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(seoul, 15f));

        // 지도를 클릭했을 때 마커 표시하기
        mMap.setOnMapClickListener(latLng -> {
            mMap.clear(); // 기존 마커 지우기
            selectedLatLng = latLng;

            selectedPlaceName = getRoadAddress(latLng);

            // 클릭한 좌표에 마커 추가
            mMap.addMarker(new MarkerOptions().position(latLng).title("선택한 위치"));
        });
    }

    private String getRoadAddress(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.KOREA);
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String fullAddress = address.getAddressLine(0);
                return cleanUpAddress(fullAddress);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return String.format(Locale.KOREA, "선택한 위치 (%.4f, %.4f)", latLng.latitude, latLng.longitude);
    }

    private String cleanUpAddress(String address) {
        if (address != null && address.startsWith("대한민국 ")) {
            return address.replace("대한민국 ", "").trim();
        }
        return address;
    }
}