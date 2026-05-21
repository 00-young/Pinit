package com.example.pinit.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;
import com.example.pinit.activity.PlaceSearchActivity;
import com.example.pinit.activity.PostTravelSettingActivity;
import com.example.pinit.model.DailySchedule;
import com.example.pinit.model.MyPlan;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class CreatePostFragment extends Fragment {

    private ActivityResultLauncher<Intent> travelSettingLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> placeSearchLauncher;

    private LinearLayout layoutDynamicContent;
    private LinearLayout layoutImportedBudget;
    private LinearLayout layoutTagsContainer;
    private ImageView ivSelectedPhoto;

    // 예산 자동 계산을 위한 뷰 선언
    private TextView tvTotalBudget;
    private EditText etBudgetAccom, etBudgetTransport, etBudgetFood, etBudgetEtc;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        travelSettingLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {}
        );

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null && ivSelectedPhoto != null) {
                            ivSelectedPhoto.setImageURI(imageUri);
                            ivSelectedPhoto.setVisibility(View.VISIBLE);
                        }
                    }
                }
        );

        placeSearchLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String placeName = result.getData().getStringExtra("selectedPlaceName");
                        String placeAddress = result.getData().getStringExtra("selectedPlaceAddress");

                        if (placeName != null && layoutDynamicContent != null) {
                            View placeCardView = LayoutInflater.from(getContext()).inflate(R.layout.item_inserted_place, layoutDynamicContent, false);

                            TextView tvName = placeCardView.findViewById(R.id.tvInsertedPlaceName);
                            TextView tvAddress = placeCardView.findViewById(R.id.tvInsertedPlaceAddress);

                            tvName.setText(placeName);
                            tvAddress.setText(placeAddress != null ? placeAddress : "주소 정보 없음");

                            layoutDynamicContent.addView(placeCardView);

                            EditText etNextComment = new EditText(getContext());
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            lp.setMargins(0, 16, 0, 32);
                            etNextComment.setLayoutParams(lp);
                            etNextComment.setHint("장소에 대한 이야기를 적어보세요...");
                            etNextComment.setBackgroundColor(0x00000000);
                            etNextComment.setGravity(android.view.Gravity.TOP);
                            etNextComment.setMinLines(3);

                            layoutDynamicContent.addView(etNextComment);
                            etNextComment.requestFocus();
                        }
                    }
                }
        );

        getParentFragmentManager().setFragmentResultListener("planResult", this, (requestKey, bundle) -> {
            MyPlan selectedPlan = (MyPlan) bundle.getSerializable("selectedPlan");
            if (selectedPlan != null && layoutDynamicContent != null) {

                layoutDynamicContent.removeAllViews();
                List<DailySchedule> schedules = selectedPlan.getSchedules();

                if (schedules != null) {
                    for (int i = 0; i < schedules.size(); i++) {
                        DailySchedule currentDay = schedules.get(i);

                        View dayBlockView = LayoutInflater.from(getContext()).inflate(R.layout.item_imported_day_block, layoutDynamicContent, false);

                        TextView tvTitle = dayBlockView.findViewById(R.id.tvTemplateDayTitle);
                        RecyclerView rvPlaces = dayBlockView.findViewById(R.id.rvTemplatePlaces);
                        Button btnReadMore = dayBlockView.findViewById(R.id.btnReadMore);

                        MapView mapView = dayBlockView.findViewById(R.id.mapTemplateView);
                        if (mapView != null) {
                            mapView.onCreate(null);
                            mapView.onResume();

                            mapView.getMapAsync(googleMap -> {
                                PolylineOptions polylineOptions = new PolylineOptions().color(0xFFFFD54F).width(8);

                                LatLng[] mockCoords = {
                                        new LatLng(31.2397, 121.4996),
                                        new LatLng(31.2423, 121.4924),
                                        new LatLng(31.2355, 121.5063),
                                        new LatLng(31.2212, 121.4800),
                                        new LatLng(31.1416, 121.6621)
                                };

                                int placeCount = currentDay.getPlaces().size();
                                for (int j = 0; j < placeCount; j++) {
                                    LatLng latLng = mockCoords[Math.min(j, mockCoords.length - 1)];

                                    googleMap.addMarker(new MarkerOptions()
                                            .position(latLng)
                                            .title((j + 1) + ". " + currentDay.getPlaces().get(j)));

                                    polylineOptions.add(latLng);
                                }

                                if (placeCount > 0) {
                                    googleMap.addPolyline(polylineOptions);
                                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mockCoords[0], 12));
                                }
                            });
                        }

                        tvTitle.setText(currentDay.getDayTitle());

                        btnReadMore.setOnClickListener(v -> {
                            if (btnReadMore.getText().toString().contains("더보기")) {
                                btnReadMore.setText("접기 ▲");
                            } else {
                                btnReadMore.setText("더보기 ▼");
                            }
                        });

                        rvPlaces.setLayoutManager(new LinearLayoutManager(getContext()));
                        rvPlaces.setAdapter(new RecyclerView.Adapter<PlaceViewHolder>() {
                            @NonNull
                            @Override
                            public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                                View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
                                return new PlaceViewHolder(v);
                            }

                            @Override
                            public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
                                String placeName = currentDay.getPlaces().get(position);
                                holder.textView.setText((position + 1) + ". " + placeName);
                                holder.textView.setTextColor(0xFF000000);
                                holder.textView.setTextSize(14);
                            }

                            @Override
                            public int getItemCount() { return currentDay.getPlaces().size(); }
                        });

                        layoutDynamicContent.addView(dayBlockView);

                        EditText etMiddleComment = new EditText(getContext());
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        lp.setMargins(0, 0, 0, 48);
                        etMiddleComment.setLayoutParams(lp);
                        etMiddleComment.setHint(currentDay.getDayTitle() + " 일정을 마친 후 이야기를 적어보세요...");
                        etMiddleComment.setBackgroundColor(0x00000000);
                        etMiddleComment.setGravity(android.view.Gravity.TOP);
                        etMiddleComment.setMinLines(4);

                        layoutDynamicContent.addView(etMiddleComment);
                    }

                    EditText etFinalComment = new EditText(getContext());
                    LinearLayout.LayoutParams finalLp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    etFinalComment.setLayoutParams(finalLp);
                    etFinalComment.setHint("모든 일정을 마쳤습니다! 전체적인 감상과 사진을 자유롭게 추가해 보세요.");
                    etFinalComment.setBackgroundColor(0x00000000);
                    etFinalComment.setGravity(android.view.Gravity.TOP);
                    etFinalComment.setMinLines(6);

                    layoutDynamicContent.addView(etFinalComment);
                }
            }
        });

        // 🌟 요구사항 2: 태그 바텀시트에서 전달받은 태그 데이터를 그리기
        // (주의: TagBottomSheetFragment에서 setFragmentResult("tagResult", bundle) 형태로 던져주어야 합니다.)
        getParentFragmentManager().setFragmentResultListener("tagResult", this, (requestKey, bundle) -> {
            ArrayList<String> selectedTags = bundle.getStringArrayList("selectedTags");
            if (selectedTags != null && layoutTagsContainer != null) {
                layoutTagsContainer.removeAllViews(); // 기존 태그 초기화

                for (String tag : selectedTags) {
                    TextView tvTag = new TextView(getContext());
                    tvTag.setText("#" + tag);
                    tvTag.setTextColor(0xFF000000);
                    tvTag.setBackgroundColor(0xFFEEEEEE); // 옅은 회색 배경 (디자인에 맞춰 변경 가능)
                    tvTag.setPadding(32, 12, 32, 12);

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.setMargins(0, 0, 16, 0); // 태그 간격 조정
                    tvTag.setLayoutParams(params);

                    layoutTagsContainer.addView(tvTag);
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_post, container, false);

        layoutDynamicContent = view.findViewById(R.id.layoutDynamicContent);
        layoutImportedBudget = view.findViewById(R.id.layoutImportedBudget);
        layoutTagsContainer = view.findViewById(R.id.layoutTagsContainer);
        ivSelectedPhoto = view.findViewById(R.id.ivSelectedPhoto);

        tvTotalBudget = view.findViewById(R.id.tvTotalBudget);
        etBudgetAccom = view.findViewById(R.id.etBudgetAccom);
        etBudgetTransport = view.findViewById(R.id.etBudgetTransport);
        etBudgetFood = view.findViewById(R.id.etBudgetFood);
        etBudgetEtc = view.findViewById(R.id.etBudgetEtc);

        // 🌟 요구사항 1: 예산 자동 합산 TextWatcher 장착
        TextWatcher budgetWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { calculateTotalBudget(); }
        };

        etBudgetAccom.addTextChangedListener(budgetWatcher);
        etBudgetTransport.addTextChangedListener(budgetWatcher);
        etBudgetFood.addTextChangedListener(budgetWatcher);
        etBudgetEtc.addTextChangedListener(budgetWatcher);

        Spinner spinnerVisibility = view.findViewById(R.id.spinnerVisibility);
        String[] visibilityItems = {"전체공개", "나만보기"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, visibilityItems);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVisibility.setAdapter(spinnerAdapter);

        Button btnTravelSetting = view.findViewById(R.id.btnTravelSetting);
        btnTravelSetting.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), PostTravelSettingActivity.class);
            travelSettingLauncher.launch(intent);
        });

        Button btnLoadBudget = view.findViewById(R.id.btnLoadBudget);
        btnLoadBudget.setOnClickListener(v -> {
            if (layoutImportedBudget != null) {
                layoutImportedBudget.setVisibility(View.VISIBLE);
            }
        });

        Button btnLoadMyPlan = view.findViewById(R.id.btnLoadMyPlan);
        btnLoadMyPlan.setOnClickListener(v -> {
            MyPlansBottomSheetFragment bottomSheet = new MyPlansBottomSheetFragment();
            bottomSheet.show(getParentFragmentManager(), "MyPlansBottomSheet");
        });

        Button btnInsertTag = view.findViewById(R.id.btnInsertTag);
        btnInsertTag.setOnClickListener(v -> {
            TagBottomSheetFragment bottomSheet = new TagBottomSheetFragment();
            bottomSheet.show(getParentFragmentManager(), "TagBottomSheet");
        });

        ImageView ivMenuCamera = view.findViewById(R.id.ivMenuCamera);
        ivMenuCamera.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        ImageView ivMenuLocation = view.findViewById(R.id.ivMenuLocation);
        ivMenuLocation.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), PlaceSearchActivity.class);
            intent.putExtra("isPickingMode", true);
            placeSearchLauncher.launch(intent);
        });

        return view;
    }

    // 🌟 요구사항 1: 예산 합산 로직 함수
    private void calculateTotalBudget() {
        int accom = parseBudgetNumber(etBudgetAccom.getText().toString());
        int transport = parseBudgetNumber(etBudgetTransport.getText().toString());
        int food = parseBudgetNumber(etBudgetFood.getText().toString());
        int etc = parseBudgetNumber(etBudgetEtc.getText().toString());

        int total = accom + transport + food + etc;
        tvTotalBudget.setText("총 " + total + "만원");
    }

    // 공백이나 문자가 들어왔을 때 앱이 튕기지 않게 안전하게 0으로 변환해주는 함수
    private int parseBudgetNumber(String text) {
        try {
            if (text == null || text.trim().isEmpty()) return 0;
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static class PlaceViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}