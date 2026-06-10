package com.example.pinit.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;
import com.example.pinit.model.Schedule;
import com.example.pinit.model.DailySchedule;
import com.example.pinit.model.MyPlan;
import com.example.pinit.model.post.Post;
import com.example.pinit.adapter.ContentBlockAdapter;
import com.example.pinit.model.post.ContentBlock;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class PostDetailFragment extends Fragment {
    private static final String ARG_POST_ID = "postId";
    private FirebaseFirestore db;
    private String postId;
    private TextView tvPostTitle;
    private TextView tvAuthorName;
    private ImageView authorProfileImage;
    private TextView tvPostDate;
    private TextView tvViewCount;

    private TextView btnEditPost;
    private TextView btnDeletePost;

    private ImageView btnActionScrap;
    private ImageView btnActionShare;
    private RecyclerView rvContentBlocks;

    private HorizontalScrollView scrollTravelSettings;
    private LinearLayout layoutDetailTravelSettings;
    private HorizontalScrollView scrollHashtags;
    private LinearLayout layoutDetailHashtags;

    private String srcStartDate = "";
    private String srcEndDate = "";
    private String srcCountry = "";

    public static PostDetailFragment newInstance(String postId) {
        PostDetailFragment fragment = new PostDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_POST_ID, postId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            postId = getArguments().getString(ARG_POST_ID);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_post_detail, container, false);

        btnActionScrap = view.findViewById(R.id.btnActionScrap);
        btnActionShare = view.findViewById(R.id.btnActionShare);
        rvContentBlocks = view.findViewById(R.id.rvContentBlocks);

        TextView btnSaveAllSchedules = view.findViewById(R.id.btnSaveAllSchedules);
        if (btnSaveAllSchedules != null) {
            btnSaveAllSchedules.setOnClickListener(v -> saveEntireScheduleToMyTravel());
        }

        scrollTravelSettings = view.findViewById(R.id.scrollTravelSettings);
        layoutDetailTravelSettings = view.findViewById(R.id.layoutDetailTravelSettings);
        scrollHashtags = view.findViewById(R.id.scrollHashtags);
        layoutDetailHashtags = view.findViewById(R.id.layoutDetailHashtags);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        view.findViewById(R.id.btnOpenMyPage).setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new MyPageFragment())
                        .addToBackStack(null)
                        .commit());

        db = FirebaseFirestore.getInstance();

        tvPostTitle = view.findViewById(R.id.tvPostTitle);
        tvAuthorName = view.findViewById(R.id.tvAuthorName);
        authorProfileImage = view.findViewById(R.id.authorProfileImage);
        tvPostDate = view.findViewById(R.id.tvPostDate);
        tvViewCount = view.findViewById(R.id.tvViewCount);

        btnEditPost = view.findViewById(R.id.btnEditPost);
        btnDeletePost = view.findViewById(R.id.btnDeletePost);

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
                    List<ContentBlock> blocks = queryDocumentSnapshots.toObjects(ContentBlock.class);
                    rvContentBlocks.setLayoutManager(new LinearLayoutManager(getContext()));

                    // 💡 수정된 핵심 구역: expected 1 인자에 딱 맞춰 람다식 매칭 완벽 수선
                    rvContentBlocks.setAdapter(new ContentBlockAdapter(blocks, dayNumber -> {
                        ContentBlockAdapter adapter = (ContentBlockAdapter) rvContentBlocks.getAdapter();
                        if (adapter != null) {
                            List<Schedule> targetSchedules = adapter.getSchedulesForDay(dayNumber);
                            saveSingleDayToMyTravel(dayNumber, targetSchedules);
                        }
                    }));
                });
    }

    private void sharePostByLink() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String shareId = UUID.randomUUID().toString();
        Map<String, Object> shareData = new HashMap<>();
        shareData.put("userId", uid);
        shareData.put("shareType", "copy_link");
        shareData.put("createdAt", Timestamp.now());
        db.collection("posts").document(postId).collection("shares").document(shareId).set(shareData);
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("postLink", "https://pinit.com/post/" + postId);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getContext(), "링크 복사 완료", Toast.LENGTH_SHORT).show();
    }

    private void toggleScrap() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference scrapRef = db.collection("posts").document(postId).collection("scrap").document(uid);
        scrapRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                scrapRef.delete().addOnSuccessListener(unused -> {
                    db.collection("posts").document(postId).update("scrapCount", FieldValue.increment(-1));
                    Toast.makeText(getContext(), "스크랩 취소", Toast.LENGTH_SHORT).show();
                });
            } else {
                Map<String, Object> scrapData = new HashMap<>();
                scrapData.put("createdAt", Timestamp.now());
                scrapData.put("userId", uid);
                scrapRef.set(scrapData).addOnSuccessListener(unused -> {
                    db.collection("posts").document(postId).update("scrapCount", FieldValue.increment(1));
                    Toast.makeText(getContext(), "스크랩 완료", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadPostDetail() {
        db.collection("posts").document(postId).get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) return;
            Post post = documentSnapshot.toObject(Post.class);
            if (post == null) return;

            String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
            boolean isPrivate = "private".equals(post.getVisibility());
            boolean isAuthor = post.getUserId() != null && post.getUserId().equals(currentUid);
            if (isPrivate && !isAuthor) {
                Toast.makeText(getContext(), "비공개 게시물입니다.", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
                return;
            }

            bindPostData(post);
        });
    }

    private void recordView() {
        db.collection("posts").document(postId)
                .update("viewCount", FieldValue.increment(1))
                .addOnSuccessListener(unused -> refreshViewCount());

        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String viewId = UUID.randomUUID().toString();
        Map<String, Object> viewData = new HashMap<>();
        viewData.put("userId", uid);
        viewData.put("device", "android");
        viewData.put("viewedAt", Timestamp.now());
        db.collection("posts").document(postId).collection("views").document(viewId).set(viewData);
    }

    private void refreshViewCount() {
        db.collection("posts").document(postId).get().addOnSuccessListener(doc -> {
            Post post = doc.toObject(Post.class);
            if (post == null) return;
            bindViewCount(post.getViewCount());
        });
    }

    private void bindViewCount(int count) {
        if (tvViewCount != null) {
            tvViewCount.setText("조회 " + count);
        }
    }

    private void bindPostData(Post post) {
        tvPostTitle.setText(post.getTitle());
        tvAuthorName.setText(post.getUserNickname());
        loadAuthorProfileImage(post.getUserEmail());
        bindViewCount(post.getViewCount());

        if (post.getHashtags() != null && !post.getHashtags().isEmpty()) {
            scrollHashtags.setVisibility(View.VISIBLE);
            layoutDetailHashtags.removeAllViews();
            for (String tag : post.getHashtags()) {
                layoutDetailHashtags.addView(createTagView(tag, true));
            }
        }

        db.collection("searchIndexPosts").document(postId).get().addOnSuccessListener(indexDoc -> {
            if (indexDoc.exists()) {
                List<String> travelTags = new ArrayList<>();

                String startDate = indexDoc.getString("startDate");
                String endDate = indexDoc.getString("endDate");

                srcStartDate = (startDate != null && !startDate.isEmpty()) ? startDate : "2026-06-10";
                srcEndDate = (endDate != null && !endDate.isEmpty()) ? endDate : "2026-06-10";
                srcCountry = indexDoc.getString("country") != null ? indexDoc.getString("country") : "제주도";

                if (startDate != null && !startDate.isEmpty()) {
                    if (endDate != null && !endDate.isEmpty() && !startDate.equals(endDate)) {
                        travelTags.add(startDate + " ~ " + endDate);
                    } else {
                        travelTags.add(startDate);
                    }
                }

                String country = indexDoc.getString("country");
                if (country != null && !country.isEmpty()) travelTags.add(country);

                Long travelerCount = indexDoc.getLong("travelerCount");
                if (travelerCount != null) {
                    if (travelerCount == 1L) travelTags.add("혼자");
                    else if (travelerCount == 2L) travelTags.add("2명");
                }

                if (!travelTags.isEmpty()) {
                    scrollTravelSettings.setVisibility(View.VISIBLE);
                    layoutDetailTravelSettings.removeAllViews();
                    for (String tag : travelTags) {
                        layoutDetailTravelSettings.addView(createTagView(tag, false));
                    }
                }
            }
        });

        View authorRow = getView() != null ? getView().findViewById(R.id.authorProfileRow) : null;
        if (authorRow != null) {
            authorRow.setOnClickListener(v -> {
                String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                        FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

                if (post.getUserId() != null && post.getUserId().equals(currentUid)) {
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, new MyPageFragment())
                            .addToBackStack(null)
                            .commit();
                } else {
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, OtherMyPageFragment.newInstance(post.getUserNickname()))
                            .addToBackStack(null).commit();
                }
            });
        }

        if (post.getCreatedAt() != null) {
            String formattedDate = new java.text.SimpleDateFormat("yyyy. MM. dd", java.util.Locale.KOREA)
                    .format(post.getCreatedAt().toDate());
            tvPostDate.setText(formattedDate);
        }

        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        if (post.getUserId().equals(currentUid)) {
            if (btnEditPost != null) btnEditPost.setVisibility(View.VISIBLE);
            if (btnDeletePost != null) btnDeletePost.setVisibility(View.VISIBLE);

            if (btnDeletePost != null) {
                btnDeletePost.setOnClickListener(v -> deleteMyPost());
            }
            if (btnEditPost != null) {
                btnEditPost.setOnClickListener(v -> editMyPost());
            }
        } else {
            if (btnEditPost != null) btnEditPost.setVisibility(View.GONE);
            if (btnDeletePost != null) btnDeletePost.setVisibility(View.GONE);
        }
    }

    private TextView createTagView(String text, boolean isHashtag) {
        TextView tv = new TextView(getContext());
        tv.setText(isHashtag && !text.startsWith("#") ? "#" + text : text);
        tv.setTextColor(Color.parseColor("#333333"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);

        int paddingHorizontal = (int) (12 * getResources().getDisplayMetrics().density);
        int paddingVertical = (int) (6 * getResources().getDisplayMetrics().density);
        tv.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
        tv.setLayoutParams(params);

        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(40f);
        if (isHashtag) {
            gd.setColor(Color.parseColor("#EEEEEE"));
        } else {
            gd.setColor(Color.parseColor("#FFFFFF"));
            gd.setStroke(2, Color.parseColor("#DDDDDD"));
        }
        tv.setBackground(gd);

        return tv;
    }

    private void loadAuthorProfileImage(String email) {
        if (authorProfileImage == null) return;

        authorProfileImage.setImageResource(R.drawable.bg_profile_avatar);
        if (email == null || email.isEmpty()) return;

        db.collection("users").document(email).get()
                .addOnSuccessListener(snapshot -> {
                    String imageUrl = snapshot.getString("profileImageUrl");
                    if (imageUrl != null
                            && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
                        Glide.with(authorProfileImage)
                                .load(imageUrl)
                                .placeholder(R.drawable.bg_profile_avatar)
                                .error(R.drawable.bg_profile_avatar)
                                .into(authorProfileImage);
                    }
                });
    }

    private void setupBottomActions(View view) {
        ImageView btnActionComment = view.findViewById(R.id.btnActionComment);
        btnActionComment.setOnClickListener(v -> {
            CommentBottomSheetFragment commentSheet = CommentBottomSheetFragment.newInstance(postId);
            commentSheet.show(getChildFragmentManager(), "CommentBottomSheet");
        });
    }

    private void deleteMyPost() {
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("게시물 삭제")
                .setMessage("정말로 이 게시물을 삭제하시겠습니까?\n삭제된 게시물은 복구할 수 없습니다.")
                .setPositiveButton("삭제", (dialog, which) -> {
                    db.collection("posts").document(postId).delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "게시물이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                                getParentFragmentManager().popBackStack();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void editMyPost() {
        Toast.makeText(getContext(), "게시물 수정 화면으로 이동합니다.", Toast.LENGTH_SHORT).show();
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, CreatePostFragment.Companion.newInstanceForEdit(postId))
                .addToBackStack(null)
                .commit();
    }

    private void saveEntireScheduleToMyTravel() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String originalTitle = tvPostTitle.getText().toString();
        String newScheduleTitle = originalTitle;

        DocumentReference newScheduleRef = db.collection("schedules").document();
        Map<String, Object> scheduleData = new HashMap<>();
        scheduleData.put("userId", uid);
        scheduleData.put("title", newScheduleTitle);
        scheduleData.put("startDate", srcStartDate);
        scheduleData.put("endDate", srcEndDate);
        scheduleData.put("country", srcCountry);
        scheduleData.put("createdAt", FieldValue.serverTimestamp());

        newScheduleRef.set(scheduleData).addOnSuccessListener(aVoid -> {
            ContentBlockAdapter adapter = (ContentBlockAdapter) rvContentBlocks.getAdapter();
            if (adapter != null) {
                List<Schedule> d1 = adapter.getSchedulesForDay(1);
                List<Schedule> d2 = adapter.getSchedulesForDay(2);

                if (!d1.isEmpty()) saveDayItemsToFirebase(newScheduleRef, 1, d1);
                if (!d2.isEmpty()) saveDayItemsToFirebase(newScheduleRef, 2, d2);
            }
            Toast.makeText(getContext(), "모든 일정이 내 여행에 담겼습니다! ✈️📌", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> Toast.makeText(getContext(), "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveSingleDayToMyTravel(int dayNumber, List<Schedule> targetSchedules) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String originalTitle = tvPostTitle.getText().toString();
        String newScheduleTitle = originalTitle + " - DAY " + dayNumber;

        DocumentReference newScheduleRef = db.collection("schedules").document();
        Map<String, Object> scheduleData = new HashMap<>();
        scheduleData.put("userId", uid);
        scheduleData.put("title", newScheduleTitle);
        scheduleData.put("startDate", srcStartDate);
        scheduleData.put("endDate", srcEndDate);
        scheduleData.put("country", srcCountry);
        scheduleData.put("createdAt", FieldValue.serverTimestamp());

        newScheduleRef.set(scheduleData).addOnSuccessListener(aVoid -> {
            saveDayItemsToFirebase(newScheduleRef, dayNumber, targetSchedules);
            Toast.makeText(getContext(), "DAY " + dayNumber + " 일정이 담겼습니다! 📌", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> Toast.makeText(getContext(), "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private String calculateDateForDay(String startDateStr, int dayNum) {
        if (startDateStr == null || startDateStr.isEmpty()) return "2026-06-10";
        try {
            String cleanDate = startDateStr.replace(".", "-").replaceAll(" ", "");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            Date date = sdf.parse(cleanDate);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.DAY_OF_MONTH, dayNum - 1);
            return sdf.format(cal.getTime());
        } catch (Exception e) {
            return startDateStr;
        }
    }

    private void saveDayItemsToFirebase(DocumentReference scheduleRef, int dayNum, List<Schedule> schedules) {
        if (schedules == null || schedules.isEmpty()) return;

        String targetDateId = calculateDateForDay(srcStartDate, dayNum);

        DocumentReference dayRef = scheduleRef.collection("days").document(targetDateId);
        Map<String, Object> dayData = new HashMap<>();
        dayData.put("dayNumber", dayNum);
        dayData.put("dayTitle", "DAY " + dayNum);

        dayRef.set(dayData).addOnSuccessListener(aVoid -> {
            for (Schedule s : schedules) {
                s.setDate(targetDateId);
                dayRef.collection("items").document().set(s);
            }
        });
    }
}