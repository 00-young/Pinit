package com.example.pinit.fragment;
import com.example.pinit.model.post.Comment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.UUID;

public class CommentBottomSheetFragment extends BottomSheetDialogFragment {

    private FirebaseFirestore db;
    private EditText etCommentInput;
    private Button btnSendComment;
    private LinearLayout layoutCommentList;
    private static final String ARG_POST_ID = "postId";
    private String postId;
    private String currentUserNickname = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (getArguments() != null) {

            postId = getArguments()
                    .getString(ARG_POST_ID);
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

        loadCurrentUserNickname();
        loadComments();

        btnSendComment.setOnClickListener(v -> {
            String content = etCommentInput.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText( getContext(), "댓글을 입력해주세요.", Toast.LENGTH_SHORT ).show();

                return;
            }
            uploadComment(content);
        });
        return view;
    }

    public static CommentBottomSheetFragment newInstance(
            String postId
    ) {

        CommentBottomSheetFragment fragment =
                new CommentBottomSheetFragment();

        Bundle args = new Bundle();

        args.putString(ARG_POST_ID, postId);

        fragment.setArguments(args);

        return fragment;
    }

    private void uploadComment(String content) {

        if (FirebaseAuth.getInstance()
                .getCurrentUser() == null) {
            return;
        }

        String uid = FirebaseAuth
                .getInstance()
                .getCurrentUser()
                .getUid();

        String commentId =
                UUID.randomUUID().toString();

        Comment comment = new Comment(
                commentId,
                uid,
                currentUserNickname,
                "",
                null,
                content,
                Timestamp.now()
        );

        db.collection("posts")
                .document(postId)
                .collection("comments")
                .document(commentId)
                .set(comment)

                .addOnSuccessListener(unused -> {

                    etCommentInput.setText("");

                    loadComments();

                    Toast.makeText(
                            getContext(),
                            "댓글 등록 완료",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }
    private void loadComments() {

        layoutCommentList.removeAllViews();

        db.collection("posts")
                .document(postId)
                .collection("comments")
                .orderBy("createdAt")

                .get()

                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if (queryDocumentSnapshots.isEmpty()) {

                        TextView emptyView = new TextView(getContext());
                        emptyView.setText( "가장 먼저 댓글을 남겨보세요!" );
                        emptyView.setTextColor(0xFF999999);
                        emptyView.setTextSize(14);

                        emptyView.setPadding(
                                0,
                                100,
                                0,
                                0
                        );

                        layoutCommentList.addView(emptyView);

                        return;
                    }

                    for (DocumentSnapshot snapshot
                            : queryDocumentSnapshots) {

                        Comment comment =
                                snapshot.toObject(Comment.class);

                        if (comment == null) {
                            continue;
                        }

                        addCommentView(
                                comment.getNickname(),
                                comment.getContent(),
                                comment.getProfileImageUrl()
                        );
                    }
                });
    }

    private void loadCurrentUserNickname() {

        if (FirebaseAuth.getInstance()
                .getCurrentUser() == null) {
            return;
        }

        String uid = FirebaseAuth
                .getInstance()
                .getCurrentUser()
                .getUid();

        db.collection("users")
                .document(uid)
                .get()

                .addOnSuccessListener(documentSnapshot -> {

                    if (!documentSnapshot.exists()) {
                        return;
                    }

                    String nickname =
                            documentSnapshot.getString(
                                    "Nickname"
                            );

                    if (nickname != null) {

                        currentUserNickname =
                                nickname;
                    }
                });
    }

    private void addCommentView(
            String nickname,
            String content,
            String profileImageUrl
    ) {

        LinearLayout commentLayout =
                new LinearLayout(getContext());

        commentLayout.setOrientation(
                LinearLayout.HORIZONTAL
        );

        commentLayout.setPadding(
                0,
                24,
                0,
                24
        );

        // 프로필 이미지
        ImageView ivProfile =
                new ImageView(getContext());

        LinearLayout.LayoutParams imageParams =
                new LinearLayout.LayoutParams(
                        80,
                        80
                );

        ivProfile.setLayoutParams(imageParams);

        ivProfile.setImageResource(
                R.drawable.ic_profile_default
        );

        // 텍스트 영역
        LinearLayout textLayout =
                new LinearLayout(getContext());

        textLayout.setOrientation(
                LinearLayout.VERTICAL
        );

        textLayout.setPadding(
                24,
                0,
                0,
                0
        );

        // 닉네임
        TextView tvNickname =
                new TextView(getContext());

        tvNickname.setText(nickname);

        tvNickname.setTextSize(14);

        tvNickname.setTypeface(
                null,
                Typeface.BOLD
        );

        // 댓글 내용
        TextView tvContent =
                new TextView(getContext());

        tvContent.setText(content);

        tvContent.setTextSize(14);

        tvContent.setPadding(
                0,
                8,
                0,
                0
        );

        textLayout.addView(tvNickname);

        textLayout.addView(tvContent);

        commentLayout.addView(ivProfile);

        commentLayout.addView(textLayout);

        layoutCommentList.addView(commentLayout);
    }

}
