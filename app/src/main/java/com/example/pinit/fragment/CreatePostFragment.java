package com.example.pinit.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;

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
import com.example.pinit.adapter.ScheduleDetailAdapter;
import com.example.pinit.database.PlacesApiHelper;
import com.example.pinit.manager.FirebaseManager;
import com.example.pinit.model.DailySchedule;
import com.example.pinit.model.MyPlan;
import com.example.pinit.model.User;
import com.example.pinit.model.post.ContentBlock;
import com.example.pinit.model.post.Post;
import com.example.pinit.repository.PostRepository;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore; // DB 조회를 위해 추가됨

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CreatePostFragment extends Fragment {

    // 기능팀 지도 API 키 가져오기
    private static final String API_KEY = PlacesApiHelper.API_KEY;

    private ActivityResultLauncher<Intent> travelSettingLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> placeSearchLauncher;

    private LinearLayout layoutDynamicContent;
    private LinearLayout layoutImportedBudget;
    private LinearLayout layoutTagsContainer;
    private LinearLayout layoutTravelSettingTagsContainer;
    private ImageView ivSelectedPhoto;

    private TextView tvTotalBudget;
    private EditText etBudgetFood, etBudgetTransport, etBudgetAccom, etBudgetShopping, etBudgetSightseeing, etBudgetEtc;

    // 비동기 콜백 인터페이스
    interface GeocodeCallback { void onResult(LatLng latLng); }

    // 게시글 제목
    private EditText etPostTitle;

    // 업로드 버튼
    private Button btnUpload;

    // Firebase Repository
    private final PostRepository postRepository = new PostRepository();

    // 대표 이미지 Uri
    private Uri thumbnailUri;
    private ProgressDialog progressDialog;

    // 수정 모드를 위한 변수 추가
    private String editPostId = null;
    private boolean isEditMode = false;

    // PostDetailFragment에서 수정하러 넘어올 때 사용하는 생성자
    public static CreatePostFragment newInstanceForEdit(String postId) {
        CreatePostFragment fragment = new CreatePostFragment();
        Bundle args = new Bundle();
        args.putString("postId", postId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 넘어온 postId가 있다면 수정 모드(Edit Mode)로 설정
        if (getArguments() != null && getArguments().containsKey("postId")) {
            editPostId = getArguments().getString("postId");
            isEditMode = true;
        }

        travelSettingLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        if (layoutTravelSettingTagsContainer != null) {
                            layoutTravelSettingTagsContainer.removeAllViews();
                            addTravelSettingTag(data.getStringExtra("selectedDate"));
                            addTravelSettingTag(data.getStringExtra("selectedCountry"));
                            addTravelSettingTag(data.getStringExtra("selectedPeople"));
                        }
                    }
                }
        );

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null && ivSelectedPhoto != null) {
                            thumbnailUri = imageUri;
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
                        MapView mapView = dayBlockView.findViewById(R.id.mapTemplateView);
                        Button btnReadMore = dayBlockView.findViewById(R.id.btnReadMore);

                        tvTitle.setText(currentDay.getDayTitle() + " (" + currentDay.getDate() + ")");

                        if (mapView != null) {
                            mapView.onCreate(null);
                            mapView.onResume();
                            mapView.getMapAsync(googleMap -> {
                                googleMap.getUiSettings().setAllGesturesEnabled(false);
                                showPinsForPlaces(googleMap, currentDay.getPlaces());
                            });
                        }

                        rvPlaces.setLayoutManager(new LinearLayoutManager(getContext()));
                        rvPlaces.setNestedScrollingEnabled(false);
                        rvPlaces.setAdapter(new ScheduleDetailAdapter(getContext(), currentDay.getScheduleObjects(),
                                schedule -> {}, id -> {}, schedule -> {}));

                        rvPlaces.setVisibility(View.GONE);
                        if (btnReadMore != null) {
                            btnReadMore.setOnClickListener(v -> {
                                if (rvPlaces.getVisibility() == View.VISIBLE) {
                                    rvPlaces.setVisibility(View.GONE);
                                    btnReadMore.setText("더보기 ▼");
                                } else {
                                    rvPlaces.setVisibility(View.VISIBLE);
                                    btnReadMore.setText("접기 ▲");
                                }
                            });
                        }

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
                }
            }
        });

        getParentFragmentManager().setFragmentResultListener("tagResult", this, (requestKey, bundle) -> {
            ArrayList<String> selectedTags = bundle.getStringArrayList("selectedTags");
            if (selectedTags != null && layoutTagsContainer != null) {
                layoutTagsContainer.removeAllViews();
                for (String tag : selectedTags) {
                    TextView tvTag = new TextView(getContext());
                    tvTag.setText("#" + tag);
                    tvTag.setTextColor(0xFF000000);
                    tvTag.setBackgroundColor(0xFFEEEEEE);
                    tvTag.setPadding(32, 12, 32, 12);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.setMargins(0, 0, 16, 0);
                    tvTag.setLayoutParams(params);
                    layoutTagsContainer.addView(tvTag);
                }
            }
        });

        getParentFragmentManager().setFragmentResultListener("budgetResult", this, (requestKey, bundle) -> {
            if (layoutImportedBudget != null) {
                layoutImportedBudget.setVisibility(View.VISIBLE);
                etBudgetFood.setText(String.valueOf(bundle.getInt("budgetFood", 0)));
                etBudgetTransport.setText(String.valueOf(bundle.getInt("budgetTransport", 0)));
                etBudgetAccom.setText(String.valueOf(bundle.getInt("budgetAccom", 0)));
                etBudgetShopping.setText(String.valueOf(bundle.getInt("budgetShopping", 0)));
                etBudgetSightseeing.setText(String.valueOf(bundle.getInt("budgetSightseeing", 0)));
                etBudgetEtc.setText(String.valueOf(bundle.getInt("budgetEtc", 0)));
                calculateTotalBudget();
            }
        });
    }

    private void showPinsForPlaces(GoogleMap googleMap, List<String> places) {
        if (places == null || places.isEmpty()) return;

        int total = places.size();
        LatLng[] orderedPositions = new LatLng[total];
        int[] done = {0};

        for (int i = 0; i < total; i++) {
            final int index = i;
            final String placeName = places.get(i);

            geocode(placeName, latLng -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (latLng != null && googleMap != null) {
                            orderedPositions[index] = latLng;
                            googleMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title((index + 1) + ". " + placeName)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                        }
                        done[0]++;
                        if (done[0] == total) onAllGeocodeDone(googleMap, orderedPositions);
                    });
                }
            });
        }
    }

    private void onAllGeocodeDone(GoogleMap googleMap, LatLng[] orderedPositions) {
        List<LatLng> validPositions = new ArrayList<>();
        for (LatLng pos : orderedPositions) {
            if (pos != null) validPositions.add(pos);
        }
        if (validPositions.size() >= 2) {
            googleMap.addPolyline(new PolylineOptions()
                    .addAll(validPositions)
                    .width(8f)
                    .color(Color.parseColor("#FF6B35"))
                    .geodesic(true));
        }
        fitCameraToPins(googleMap, validPositions);
    }

    private void fitCameraToPins(GoogleMap googleMap, List<LatLng> positions) {
        if (googleMap == null) return;
        if (positions.isEmpty()) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.5665, 126.9780), 10f));
            return;
        }
        if (positions.size() == 1) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(positions.get(0), 15f));
        } else {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng pos : positions) builder.include(pos);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 120));
        }
    }

    private void geocode(String address, GeocodeCallback callback) {
        new Thread(() -> {
            try {
                String url = "https://maps.googleapis.com/maps/api/geocode/json?address="
                        + java.net.URLEncoder.encode(address, "UTF-8")
                        + "&language=ko&key=" + API_KEY;
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                String body = response.body().string();
                JSONObject json = new JSONObject(body);
                JSONArray results = json.optJSONArray("results");
                if (results != null && results.length() > 0) {
                    JSONObject loc = results.getJSONObject(0).getJSONObject("geometry").getJSONObject("location");
                    callback.onResult(new LatLng(loc.getDouble("lat"), loc.getDouble("lng")));
                } else {
                    callback.onResult(null);
                }
            } catch (Exception e) {
                callback.onResult(null);
            }
        }).start();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(
                R.layout.fragment_create_post,
                container,
                false
        );

        layoutDynamicContent = view.findViewById(R.id.layoutDynamicContent);
        layoutImportedBudget = view.findViewById(R.id.layoutImportedBudget);
        layoutTagsContainer = view.findViewById(R.id.layoutTagsContainer);
        layoutTravelSettingTagsContainer = view.findViewById(R.id.layoutTravelSettingTagsContainer);
        ivSelectedPhoto = view.findViewById(R.id.ivSelectedPhoto);
        bindHeaderMyPageButton(view);

        tvTotalBudget = view.findViewById(R.id.tvTotalBudget);
        etBudgetAccom = view.findViewById(R.id.etBudgetAccom);
        etBudgetTransport = view.findViewById(R.id.etBudgetTransport);
        etBudgetFood = view.findViewById(R.id.etBudgetFood);
        etBudgetShopping = view.findViewById(R.id.etBudgetShopping);
        etBudgetSightseeing = view.findViewById(R.id.etBudgetSightseeing);
        etBudgetEtc = view.findViewById(R.id.etBudgetEtc);

        etPostTitle = view.findViewById(R.id.etPostTitle);
        btnUpload = view.findViewById(R.id.btnRegister);

        btnUpload.setOnClickListener(v -> uploadPost());

        TextWatcher budgetWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { calculateTotalBudget(); }
        };

        etBudgetFood.addTextChangedListener(budgetWatcher);
        etBudgetTransport.addTextChangedListener(budgetWatcher);
        etBudgetAccom.addTextChangedListener(budgetWatcher);
        etBudgetShopping.addTextChangedListener(budgetWatcher);
        etBudgetSightseeing.addTextChangedListener(budgetWatcher);
        etBudgetEtc.addTextChangedListener(budgetWatcher);

        Spinner spinnerVisibility = view.findViewById(R.id.spinnerVisibility);
        String[] visibilityItems = {"전체공개", "나만보기"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, visibilityItems);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVisibility.setAdapter(spinnerAdapter);

        view.findViewById(R.id.btnTravelSetting).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), PostTravelSettingActivity.class);
            travelSettingLauncher.launch(intent);
        });

        view.findViewById(R.id.btnLoadBudget).setOnClickListener(v -> {
            BudgetBottomSheetFragment bottomSheet = new BudgetBottomSheetFragment();
            bottomSheet.show(getParentFragmentManager(), "BudgetBottomSheet");
        });

        view.findViewById(R.id.btnLoadMyPlan).setOnClickListener(v -> {
            MyPlansBottomSheetFragment bottomSheet = new MyPlansBottomSheetFragment();
            bottomSheet.show(getParentFragmentManager(), "MyPlansBottomSheet");
        });

        view.findViewById(R.id.btnInsertTag).setOnClickListener(v -> {
            TagBottomSheetFragment bottomSheet = new TagBottomSheetFragment();
            bottomSheet.show(getParentFragmentManager(), "TagBottomSheet");
        });

        view.findViewById(R.id.ivMenuPhoto).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        view.findViewById(R.id.ivMenuLocation).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), PlaceSearchActivity.class);
            intent.putExtra("isPickingMode", true);
            placeSearchLauncher.launch(intent);
        });

        // 수정 모드일 경우 버튼 텍스트 변경 및 기존 데이터 불러오기
        if (isEditMode) {
            btnUpload.setText("수정하기");
            loadExistingPostData();
        }

        return view;
    }

    // 기존 게시물 데이터를 불러오는 함수
    private void loadExistingPostData() {
        FirebaseFirestore.getInstance().collection("posts").document(editPostId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Post post = documentSnapshot.toObject(Post.class);
                        if (post != null) {
                            etPostTitle.setText(post.getTitle());
                            // TODO: 필요시 본문(ContentBlocks)이나 예산, 태그 등을 불러와서 UI에 세팅하는 로직 확장 가능
                        }
                    }
                });
    }

    private void uploadPost() {
        String title = etPostTitle.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(getContext(), "게시글 제목을 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage(isEditMode ? "게시물을 수정하는 중입니다...\n잠시만 기다려주세요." : "게시물을 업로드하는 중입니다...\n잠시만 기다려주세요.");
        progressDialog.setCancelable(false);
        progressDialog.show();

        if (thumbnailUri == null) {
            thumbnailUri = Uri.parse("android.resource://" + requireContext().getPackageName() + "/" + R.mipmap.ic_launcher);
        }

        String email = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getEmail()
                : "";

        if (email.isEmpty()) {
            if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
            Toast.makeText(getContext(), "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseManager.getInstance().getUserData(email, new FirebaseManager.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {
                String nickname = user.getNickname();
                executeUpload(title, uid, nickname);
            }

            @Override
            public void onFailure(Exception e) {
                String fallbackNickname = email.split("@")[0];
                executeUpload(title, uid, fallbackNickname);
            }
        });
    }

    private void executeUpload(String title, String uid, String nickname) {
        // 수정 모드면 기존 ID(editPostId)를 그대로 쓰고, 아니면 새 ID를 발급
        String finalPostId = isEditMode ? editPostId : UUID.randomUUID().toString();

        Post post = new Post(
                finalPostId,
                uid,
                nickname,
                title,
                "image",
                "",
                Collections.singletonList("여행"),
                0, 0, 0, 0,
                false,
                Timestamp.now()
        );

        List<ContentBlock> blocks = new ArrayList<>();
        int childCount = layoutDynamicContent.getChildCount();
        int sequence = 0;

        for (int i = 0; i < childCount; i++) {
            View child = layoutDynamicContent.getChildAt(i);

            if (child instanceof EditText) {
                String commentText = ((EditText) child).getText().toString().trim();
                if (!commentText.isEmpty()) {
                    ContentBlock textBlock = new ContentBlock(
                            UUID.randomUUID().toString(),
                            "text",
                            commentText,
                            "",
                            null,
                            sequence++
                    );
                    blocks.add(textBlock);
                }
            }
            else if (child.getId() == R.id.tvInsertedPlaceName) {
                TextView tvPlaceName = child.findViewById(R.id.tvInsertedPlaceName);
                if (tvPlaceName != null) {
                    ContentBlock placeBlock = new ContentBlock(
                            UUID.randomUUID().toString(),
                            "place",
                            tvPlaceName.getText().toString(),
                            "",
                            null,
                            sequence++
                    );
                    blocks.add(placeBlock);
                }
            }
        }

        if (blocks.isEmpty()) {
            blocks.add(new ContentBlock(UUID.randomUUID().toString(), "text", "등록된 본문 내용이 없습니다.", "", null, 0));
        }

        postRepository.uploadPost(
                post,
                thumbnailUri,
                blocks,
                () -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(getContext(), isEditMode ? "수정 성공!" : "업로드 성공!", Toast.LENGTH_SHORT).show();
                    if (getActivity() != null) {
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }
                    return null;
                },
                e -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(getContext(), (isEditMode ? "수정 실패: " : "업로드 실패: ") + e.getMessage(), Toast.LENGTH_SHORT).show();
                    return null;
                }
        );
    }

    private void bindHeaderMyPageButton(View rootView) {
        if (!(rootView instanceof LinearLayout)) return;
    }

    private void addTravelSettingTag(String text) {
        if (text == null
                || text.trim().isEmpty()
                || text.equals("날짜를 선택하세요")) return;
        TextView tvTag = new TextView(getContext());
        tvTag.setText(text);
        tvTag.setTextColor(0xFF333333);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(0xFFFFFFFF);
        drawable.setStroke(2, 0xFFDDDDDD);
        drawable.setCornerRadius(40f);
        tvTag.setBackground(drawable);
        tvTag.setPadding(32, 12, 32, 12);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 0, 16, 0);
        tvTag.setLayoutParams(params);
        layoutTravelSettingTagsContainer.addView(tvTag);
    }

    private void calculateTotalBudget() {
        int food = parseBudgetNumber(etBudgetFood.getText().toString());
        int transport = parseBudgetNumber(etBudgetTransport.getText().toString());
        int accom = parseBudgetNumber(etBudgetAccom.getText().toString());
        int shopping = parseBudgetNumber(etBudgetShopping.getText().toString());
        int sightseeing = parseBudgetNumber(etBudgetSightseeing.getText().toString());
        int etc = parseBudgetNumber(etBudgetEtc.getText().toString());
        int total = food + transport + accom + shopping + sightseeing + etc;
        tvTotalBudget.setText("총 " + total + "만원");
    }

    private int parseBudgetNumber(String text) {
        try { return (text == null || text.trim().isEmpty()) ? 0 : Integer.parseInt(text.trim()); }
        catch (NumberFormatException e) { return 0; }
    }
}