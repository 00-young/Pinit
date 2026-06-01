package com.example.pinit.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;
import com.example.pinit.model.Schedule;
import com.example.pinit.model.DailySchedule; // 추가됨
import com.example.pinit.model.MyPlan; // 추가됨
import com.example.pinit.model.post.Post;
import com.example.pinit.adapter.ContentBlockAdapter;
import com.example.pinit.model.post.ContentBlock;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PostDetailFragment extends Fragment {
    private static final String ARG_POST_ID = "postId";
    private FirebaseFirestore db;
    private String postId;
    private TextView tvPostTitle;
    private TextView tvAuthorName;
    private TextView tvPostDate;
    private ImageView btnActionScrap;
    private ImageView btnActionShare;

    public static PostDetailFragment newInstance(
            String postId
    ) {

        PostDetailFragment fragment =
                new PostDetailFragment();

        Bundle args = new Bundle();

        args.putString(ARG_POST_ID, postId);

        fragment.setArguments(args);

        return fragment;
    }
    private RecyclerView rvContentBlocks;

    // [수정됨] 담기 버튼에서 접근할 수 있도록 데이터를 클래스 멤버 변수로 선언
    private List<Schedule> day1Schedules = new ArrayList<>();
    private List<Schedule> day2Schedules = new ArrayList<>();

    @Nullable
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (getArguments() != null) {

            postId = getArguments()
                    .getString(ARG_POST_ID);
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_post_detail, container, false);

        // 뷰 바인딩
        btnActionScrap = view.findViewById(R.id.btnActionScrap);
        btnActionShare = view.findViewById(R.id.btnActionShare);
        rvContentBlocks = view.findViewById(R.id.rvContentBlocks);

        // 상단 뒤로가기 및 프로필 버튼 이벤트
        view.findViewById(R.id.btnBack).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        view.findViewById(R.id.btnOpenMyPage).setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new MyPageFragment())
                        .addToBackStack(null)
                        .commit());

        // [핵심] 서버에서 데이터를 불러오는(척하는) 더미 데이터 세팅 함수 호출
        // (버튼 클릭보다 먼저 데이터가 세팅되어 있어야 합니다)
        db = FirebaseFirestore.getInstance();

        tvPostTitle = view.findViewById(R.id.tvPostTitle);
        tvAuthorName = view.findViewById(R.id.tvAuthorName);
        tvPostDate = view.findViewById(R.id.tvPostDate);
        loadPostDetail();
        loadContentBlocks();
        recordView();
        setupBottomActions(view);
        btnActionScrap.setOnClickListener(v -> toggleScrap());
        btnActionShare.setOnClickListener(v -> sharePostByLink());

        return view;
    }

    private void loadContentBlocks() {

        db.collection("posts")
                .document(postId)
                .collection("contentBlocks")
                .orderBy("sortOrder")

                .get()

                .addOnSuccessListener(queryDocumentSnapshots -> {

                    List<ContentBlock> blocks =
                            queryDocumentSnapshots
                                    .toObjects(
                                            ContentBlock.class
                                    );

                    rvContentBlocks.setLayoutManager(
                            new LinearLayoutManager(
                                    getContext()
                            )
                    );

                    rvContentBlocks.setAdapter(
                            new ContentBlockAdapter(blocks)
                    );
                });
    }

    private void sharePostByLink() {

        if (FirebaseAuth.getInstance()
                .getCurrentUser() == null) {
            return;
        }

        String uid = FirebaseAuth
                .getInstance()
                .getCurrentUser()
                .getUid();

        String shareId =
                UUID.randomUUID().toString();

        Map<String, Object> shareData =
                new HashMap<>();

        shareData.put("userId", uid);

        shareData.put(
                "shareType",
                "copy_link"
        );

        shareData.put(
                "createdAt",
                Timestamp.now()
        );

        db.collection("posts")
                .document(postId)
                .collection("shares")
                .document(shareId)
                .set(shareData);

        // 실제 링크 복사
        ClipboardManager clipboard =
                (ClipboardManager)
                        requireContext()
                                .getSystemService(
                                        Context.CLIPBOARD_SERVICE
                                );

        ClipData clip =
                ClipData.newPlainText(
                        "postLink",
                        "https://pinit.com/post/"
                                + postId
                );

        clipboard.setPrimaryClip(clip);

        Toast.makeText(
                getContext(),
                "링크 복사 완료",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void toggleScrap() {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        String uid = FirebaseAuth
                .getInstance()
                .getCurrentUser()
                .getUid();

        DocumentReference scrapRef = db.collection("posts")
                        .document(postId)
                        .collection("scrap")
                        .document(uid);

        scrapRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // 스크랩 취소
                        scrapRef.delete().addOnSuccessListener(unused -> {
                                    db.collection("posts").document(postId)
                                            .update(
                                                    "scrapCount",
                                                    FieldValue.increment(-1)
                                            );
                                    Toast.makeText(
                                            getContext(),
                                            "스크랩 취소",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                });

                    } else {
                        Map<String, Object> scrapData = new HashMap<>();
                        scrapData.put("createdAt", Timestamp.now());
                        scrapData.put("userId", uid);
                        scrapRef.set(scrapData).addOnSuccessListener(unused -> {
                                    db.collection("posts")
                                            .document(postId)
                                            .update(
                                                    "scrapCount",
                                                    FieldValue.increment(1)
                                            );
                                    Toast.makeText(
                                            getContext(),
                                            "스크랩 완료",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                });
                    }
                });
    }

    private void loadPostDetail() {

        db.collection("posts")
                .document(postId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (!documentSnapshot.exists()) {
                        return;
                    }

                    Post post =
                            documentSnapshot.toObject(Post.class);

                    if (post == null) {
                        return;
                    }

                    bindPostData(post);
                });
    }

    private void recordView() {

        if(FirebaseAuth.getInstance().getCurrentUser() == null){
            return;
        }

        String uid = FirebaseAuth
                .getInstance()
                .getCurrentUser()
                .getUid();

        String viewId = UUID.randomUUID().toString();

        Map<String, Object> viewData =
                new HashMap<>();

        viewData.put("userId", uid);

        viewData.put("device", "android");

        viewData.put(
                "viewedAt",
                Timestamp.now()
        );

        db.collection("posts")
                .document(postId)
                .collection("views")
                .document(viewId)
                .set(viewData);
    }

    private void bindPostData(Post post) {

        tvPostTitle.setText(post.getTitle());
        tvAuthorName.setText(post.getUserNickname());

        // 작성자 클릭 시 상대방 마이페이지로 이동 (실제 데이터 연동)
        View authorRow = getView() != null ? getView().findViewById(R.id.authorProfileRow) : null;
        if (authorRow != null) {
            authorRow.setOnClickListener(v -> {
                // Post 객체에 작성자 이메일 정보가 있다고 가정 (userId 필드가 이메일인 경우)
                // 만약 userId가 UID라면 OtherMyPageFragment에서 UID 지원 로직이 필요함.
                // 현재 스키마상 userId는 UID이므로, UID를 기반으로 이동하도록 연동.
                // OtherMyPageFragment.newInstanceWithEmail()가 이메일을 받으므로 UID로 이메일을 먼저 찾거나
                // 스키마 설계를 맞춰야 함. 여기서는 기존 OtherMyPageFragment가 닉네임 또는 이메일을 받는 방식을 활용.
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, OtherMyPageFragment.newInstanceWithEmail(post.getUserId()))
                        .addToBackStack(null)
                        .commit();
            });
        }

        if (post.getCreatedAt() != null) {
            String formattedDate =
                    new java.text.SimpleDateFormat(
                            "yyyy. MM. dd",
                            java.util.Locale.KOREA
                    ).format(
                            post.getCreatedAt().toDate()
                    );
            tvPostDate.setText(formattedDate);
        }
    }

    // [내 여행에 담기] 로직 구현부

    private void saveSingleDayToMyTravel(int dayNumber) {
        MyPlan singleDayPlan = new MyPlan();
        singleDayPlan.setTitle("상하이 여행기 - DAY " + dayNumber + " (스크랩)");
        singleDayPlan.setCountry("상하이");

        List<DailySchedule> selectedDayList = new ArrayList<>();
        DailySchedule targetDay = new DailySchedule();

        if (dayNumber == 1) {
            targetDay.setDayTitle("DAY 1");
            targetDay.setDate("2026.05.23");
            targetDay.setScheduleObjects(day1Schedules);
            singleDayPlan.setDate("2026.05.23"); // 하루짜리 여행이므로 날짜 고정
        } else if (dayNumber == 2) {
            targetDay.setDayTitle("DAY 2");
            targetDay.setDate("2026.05.24");
            targetDay.setScheduleObjects(day2Schedules);
            singleDayPlan.setDate("2026.05.24");
        }

        selectedDayList.add(targetDay);
        singleDayPlan.setSchedules(selectedDayList);

        // TODO: DB 또는 서버에 singleDayPlan 객체 저장하는 코드 작성 위치

        Toast.makeText(getContext(), "DAY " + dayNumber + " 일정이 내 여행에 담겼습니다!", Toast.LENGTH_SHORT).show();
    }
    // ==========================================

    private void setupBottomActions(View view) {

        ImageView btnActionComment = view.findViewById(R.id.btnActionComment);

        btnActionComment.setOnClickListener(v -> {
            CommentBottomSheetFragment commentSheet = CommentBottomSheetFragment.newInstance(postId);
            commentSheet.show(
                    getChildFragmentManager(),
                    "CommentBottomSheet"
            );
        });
    }

    }