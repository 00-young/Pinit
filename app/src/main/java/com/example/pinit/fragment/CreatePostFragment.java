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
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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

import com.example.pinit.model.post.ContentBlock;
import com.example.pinit.model.post.Post;

import com.example.pinit.repository.PostRepository;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CreatePostFragment extends Fragment {

    private ActivityResultLauncher<Intent> travelSettingLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> placeSearchLauncher;

    private LinearLayout layoutDynamicContent;
    private LinearLayout layoutImportedBudget;
    private LinearLayout layoutTagsContainer;

    private ImageView ivSelectedPhoto;

    private TextView tvTotalBudget;

    private EditText etBudgetAccom;
    private EditText etBudgetTransport;
    private EditText etBudgetFood;
    private EditText etBudgetEtc;

    // 게시글 제목
    private EditText etPostTitle;

    // 업로드 버튼
    private Button btnUpload;

    // Firebase Repository
    private final PostRepository postRepository = new PostRepository();

    // 대표 이미지 Uri
    private Uri thumbnailUri;

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

                    if (result.getResultCode() == Activity.RESULT_OK
                            && result.getData() != null) {

                        Uri imageUri = result.getData().getData();

                        if (imageUri != null && ivSelectedPhoto != null) {

                            // Firebase 업로드용 저장
                            thumbnailUri = imageUri;

                            ivSelectedPhoto.setImageURI(imageUri);

                            ivSelectedPhoto.setVisibility(View.VISIBLE);
                        }
                    }
                }
        );

        placeSearchLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {}
        );
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

        ivSelectedPhoto = view.findViewById(R.id.ivSelectedPhoto);

        bindHeaderMyPageButton(view);

        tvTotalBudget = view.findViewById(R.id.tvTotalBudget);

        etBudgetAccom = view.findViewById(R.id.etBudgetAccom);

        etBudgetTransport = view.findViewById(R.id.etBudgetTransport);

        etBudgetFood = view.findViewById(R.id.etBudgetFood);

        etBudgetEtc = view.findViewById(R.id.etBudgetEtc);

        // 게시글 제목 EditText
        etPostTitle = view.findViewById(R.id.etPostTitle);

        // 업로드 버튼
        btnUpload = view.findViewById(R.id.btnRegister);

        btnUpload.setOnClickListener(v -> uploadPost());

        TextWatcher budgetWatcher = new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {}

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {}

            @Override
            public void afterTextChanged(Editable s) {

                calculateTotalBudget();
            }
        };

        etBudgetAccom.addTextChangedListener(budgetWatcher);

        etBudgetTransport.addTextChangedListener(budgetWatcher);

        etBudgetFood.addTextChangedListener(budgetWatcher);

        etBudgetEtc.addTextChangedListener(budgetWatcher);

        Spinner spinnerVisibility =
                view.findViewById(R.id.spinnerVisibility);

        String[] visibilityItems = {"전체공개", "나만보기"};

        ArrayAdapter<String> spinnerAdapter =
                new ArrayAdapter<>(
                        getContext(),
                        android.R.layout.simple_spinner_item,
                        visibilityItems
                );

        spinnerAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerVisibility.setAdapter(spinnerAdapter);

        Button btnTravelSetting =
                view.findViewById(R.id.btnTravelSetting);

        btnTravelSetting.setOnClickListener(v -> {

            Intent intent = new Intent(
                    getActivity(),
                    PostTravelSettingActivity.class
            );

            travelSettingLauncher.launch(intent);
        });

        Button btnLoadBudget =
                view.findViewById(R.id.btnLoadBudget);

        btnLoadBudget.setOnClickListener(v -> {

            if (layoutImportedBudget != null) {

                layoutImportedBudget.setVisibility(View.VISIBLE);
            }
        });

        ImageView ivMenuCamera =
                view.findViewById(R.id.ivMenuCamera);

        ivMenuCamera.setOnClickListener(v -> {

            Intent intent = new Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            );

            galleryLauncher.launch(intent);
        });

        ImageView ivMenuLocation =
                view.findViewById(R.id.ivMenuLocation);

        ivMenuLocation.setOnClickListener(v -> {

            Intent intent = new Intent(
                    getActivity(),
                    PlaceSearchActivity.class
            );

            intent.putExtra("isPickingMode", true);

            placeSearchLauncher.launch(intent);
        });

        return view;
    }

    // Firebase 업로드 함수
    private void uploadPost() {

        String title = etPostTitle.getText().toString().trim();

        if (title.isEmpty()) {

            Toast.makeText(
                    getContext(),
                    "게시글 제목을 입력하세요",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (thumbnailUri == null) {

            Toast.makeText(
                    getContext(),
                    "대표 이미지를 선택하세요",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String uid =
                FirebaseAuth.getInstance().getCurrentUser() != null
                        ? FirebaseAuth.getInstance()
                        .getCurrentUser()
                        .getUid()
                        : "";

        if (uid.isEmpty()) {

            Toast.makeText(
                    getContext(),
                    "로그인이 필요합니다",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String postId = UUID.randomUUID().toString();

        Post post = new Post(
                postId,
                uid,
                title,
                "image",
                "",
                Collections.singletonList("여행"),
                0,
                0,
                0,
                0,
                false,
                Timestamp.now()
        );

        ContentBlock textBlock = new ContentBlock(
                UUID.randomUUID().toString(),
                "text",
                "게시글 내용",
                "",
                null,
                0
        );

        List<ContentBlock> blocks =
                Collections.singletonList(textBlock);

        postRepository.uploadPost(

                post,

                thumbnailUri,

                blocks,

                () -> {

                    Toast.makeText(
                            getContext(),
                            "업로드 성공",
                            Toast.LENGTH_SHORT
                    ).show();

                    return null;
                },

                e -> {

                    Toast.makeText(
                            getContext(),
                            "업로드 실패",
                            Toast.LENGTH_SHORT
                    ).show();

                    return null;
                }
        );
    }

    private void bindHeaderMyPageButton(View rootView) {

        if (!(rootView instanceof LinearLayout)) return;

        LinearLayout rootLayout = (LinearLayout) rootView;

        if (rootLayout.getChildCount() == 0
                || !(rootLayout.getChildAt(0)
                instanceof RelativeLayout)) return;

        RelativeLayout headerLayout =
                (RelativeLayout) rootLayout.getChildAt(0);

        if (headerLayout.getChildCount() == 0) return;

        View headerAction =
                headerLayout.getChildAt(
                        headerLayout.getChildCount() - 1
                );

        headerAction.setClickable(true);

        headerAction.setFocusable(true);

        headerAction.setOnClickListener(v ->
                getParentFragmentManager()
                        .beginTransaction()
                        .replace(
                                R.id.fragmentContainer,
                                new MyPageFragment()
                        )
                        .addToBackStack(null)
                        .commit()
        );
    }

    private void calculateTotalBudget() {

        int accom =
                parseBudgetNumber(
                        etBudgetAccom.getText().toString()
                );

        int transport =
                parseBudgetNumber(
                        etBudgetTransport.getText().toString()
                );

        int food =
                parseBudgetNumber(
                        etBudgetFood.getText().toString()
                );

        int etc =
                parseBudgetNumber(
                        etBudgetEtc.getText().toString()
                );

        int total = accom + transport + food + etc;

        tvTotalBudget.setText("총 " + total + "만원");
    }

    private int parseBudgetNumber(String text) {

        try {

            if (text == null || text.trim().isEmpty()) {

                return 0;
            }

            return Integer.parseInt(text.trim());

        } catch (NumberFormatException e) {

            return 0;
        }
    }

    private static class PlaceViewHolder
            extends RecyclerView.ViewHolder {

        TextView textView;

        public PlaceViewHolder(@NonNull View itemView) {

            super(itemView);

            textView =
                    itemView.findViewById(android.R.id.text1);
        }
    }
}
