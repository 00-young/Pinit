package com.example.pinit.fragment;

import com.example.pinit.model.post.Comment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Typeface;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.pinit.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CommentBottomSheetFragment extends BottomSheetDialogFragment {

    private FirebaseFirestore db;
    private EditText etCommentInput;
    private Button btnSendComment;
    private LinearLayout layoutCommentList;

    // 대댓글 UI 관련 변수 추가
    private LinearLayout layoutReplyIndicator;
    private TextView tvReplyTo;
    private ImageView btnCancelReply;
    private String selectedParentCommentId = null; // 현재 답글을 달고 있는 부모 댓글 ID

    private static final String ARG_POST_ID = "postId";
    private String postId;
    private String currentUserNickname = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            postId = getArguments().getString(ARG_POST_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_bottom_sheet_comment, container, false);

        db = FirebaseFirestore.getInstance();
        etCommentInput = view.findViewById(R.id.etCommentInput);
        btnSendComment = view.findViewById(R.id.btnSendComment);
        layoutCommentList = view.findViewById(R.id.layoutCommentList);

        // 대댓글 인디케이터 연결
        layoutReplyIndicator = view.findViewById(R.id.layoutReplyIndicator);
        tvReplyTo = view.findViewById(R.id.tvReplyTo);
        btnCancelReply = view.findViewById(R.id.btnCancelReply);

        // 답글 달기 취소 버튼 동작
        btnCancelReply.setOnClickListener(v -> clearReplyState());

        loadCurrentUserNickname();
        loadComments();

        btnSendComment.setOnClickListener(v -> {
            String content = etCommentInput.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(getContext(), "댓글을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            uploadComment(content);
        });
        return view;
    }

    public static CommentBottomSheetFragment newInstance(String postId) {
        CommentBottomSheetFragment fragment = new CommentBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_POST_ID, postId);
        fragment.setArguments(args);
        return fragment;
    }

    // 대댓글 모드 취소 로직
    private void clearReplyState() {
        selectedParentCommentId = null;
        layoutReplyIndicator.setVisibility(View.GONE);
        etCommentInput.setHint("댓글 추가...");
        etCommentInput.setText("");
    }

    private void uploadComment(String content) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String commentId = UUID.randomUUID().toString();

        // Comment 모델 객체 생성
        Comment comment = new Comment(
                commentId,
                uid,
                currentUserNickname,
                "",
                selectedParentCommentId, // 일반 댓글이면 null, 대댓글이면 부모 ID가 들어감
                content,
                Timestamp.now()
        );

        db.collection("posts").document(postId).collection("comments").document(commentId)
                .set(comment)
                .addOnSuccessListener(unused -> {
                    clearReplyState(); // 작성 완료 후 답글 상태 초기화
                    loadComments();
                    Toast.makeText(getContext(), "등록 완료", Toast.LENGTH_SHORT).show();
                    db.collection("posts").document(postId)
                            .update("commentCount", FieldValue.increment(1));
                });
    }

    private void loadComments() {
        layoutCommentList.removeAllViews();

        db.collection("posts").document(postId).collection("comments").orderBy("createdAt")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        TextView emptyView = new TextView(getContext());
                        emptyView.setText("가장 먼저 댓글을 남겨보세요!");
                        emptyView.setTextColor(0xFF999999);
                        emptyView.setTextSize(14);
                        emptyView.setPadding(0, 100, 0, 0);
                        emptyView.setGravity(View.TEXT_ALIGNMENT_CENTER);
                        layoutCommentList.addView(emptyView);
                        return;
                    }

                    // 부모 댓글과 대댓글을 분리해서 정렬하는 로직
                    List<Comment> parentComments = new ArrayList<>();
                    List<Comment> childComments = new ArrayList<>();

                    for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
                        Comment comment = snapshot.toObject(Comment.class);
                        if (comment == null) continue;

                        if (comment.getParentCommentId() == null || comment.getParentCommentId().isEmpty()) {
                            parentComments.add(comment);
                        } else {
                            childComments.add(comment);
                        }
                    }

                    // 1. 부모 댓글을 먼저 그리고, 2. 그 아래에 해당 부모의 대댓글을 그립니다.
                    for (Comment parent : parentComments) {
                        addCommentView(parent.getCommentId(), parent.getNickname(), parent.getContent(), parent.getProfileImageUrl(), false);

                        for (Comment child : childComments) {
                            if (parent.getCommentId().equals(child.getParentCommentId())) {
                                addCommentView(child.getCommentId(), child.getNickname(), child.getContent(), child.getProfileImageUrl(), true);
                            }
                        }
                    }
                });
    }

    private void loadCurrentUserNickname() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nickname = documentSnapshot.getString("Nickname");
                        if (nickname != null) currentUserNickname = nickname;
                    }
                });
    }

    // addCommentView에 commentId와 isReply(대댓글 여부) 파라미터 추가
    private void addCommentView(String commentId, String nickname, String content, String profileImageUrl, boolean isReply) {
        LinearLayout commentLayout = new LinearLayout(getContext());
        commentLayout.setOrientation(LinearLayout.HORIZONTAL);

        // 대댓글이면 왼쪽 여백(들여쓰기)을 크게 줘서 시각적으로 구분
        if (isReply) {
            commentLayout.setPadding(120, 16, 0, 16);
        } else {
            commentLayout.setPadding(0, 24, 0, 24);
        }

        // 프로필 이미지
        ImageView ivProfile = new ImageView(getContext());
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(isReply ? 60 : 80, isReply ? 60 : 80);
        ivProfile.setLayoutParams(imageParams);
        ivProfile.setImageResource(R.drawable.ic_profile_default);

        // 텍스트 영역
        LinearLayout textLayout = new LinearLayout(getContext());
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setPadding(24, 0, 0, 0);

        // 닉네임
        TextView tvNickname = new TextView(getContext());
        tvNickname.setText(nickname);
        tvNickname.setTextSize(14);
        tvNickname.setTypeface(null, Typeface.BOLD);

        // 댓글 내용
        TextView tvContent = new TextView(getContext());
        tvContent.setText(content);
        tvContent.setTextSize(14);
        tvContent.setPadding(0, 8, 0, 0);

        textLayout.addView(tvNickname);
        textLayout.addView(tvContent);

        // 대댓글이 아닐 경우에만 "답글 달기" 버튼 추가 (대댓글의 대댓글 방지)
        if (!isReply) {
            TextView tvReplyBtn = new TextView(getContext());
            tvReplyBtn.setText("답글 달기");
            tvReplyBtn.setTextSize(12);
            tvReplyBtn.setTextColor(0xFF888888);
            tvReplyBtn.setPadding(0, 8, 0, 0);

            // 답글 달기 클릭 이벤트
            tvReplyBtn.setOnClickListener(v -> {
                selectedParentCommentId = commentId; // 부모 ID 저장
                layoutReplyIndicator.setVisibility(View.VISIBLE);
                tvReplyTo.setText(nickname + "님에게 답글 남기는 중...");
                etCommentInput.setHint("답글 추가...");
                etCommentInput.requestFocus();

                // 자동으로 키보드 올리기
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(etCommentInput, InputMethodManager.SHOW_IMPLICIT);
                }
            });
            textLayout.addView(tvReplyBtn);
        }

        commentLayout.addView(ivProfile);
        commentLayout.addView(textLayout);
        layoutCommentList.addView(commentLayout);
    }
}