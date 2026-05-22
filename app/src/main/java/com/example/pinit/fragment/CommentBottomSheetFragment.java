package com.example.pinit.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.pinit.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;

public class CommentBottomSheetFragment extends BottomSheetDialogFragment {

    // 창이 닫혀도 데이터가 날아가지 않도록 기억해주는 '공용 메모장(static)' 입니다!
    private static ArrayList<String> savedComments = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_bottom_sheet_comment, container, false);

        EditText etCommentInput = view.findViewById(R.id.etCommentInput);
        Button btnSendComment = view.findViewById(R.id.btnSendComment);
        LinearLayout layoutCommentList = view.findViewById(R.id.layoutCommentList);

        // 폰 내부 금고에서 닉네임 꺼내오기
        SharedPreferences prefs = getContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String myNickname = prefs.getString("nickname", "알 수 없는 유저");

        // ====================================================================
        //  1. 바텀시트를 새로 열었을 때, 메모장에 적힌 댓글이 있다면 쫙 그려줍니다.
        // ====================================================================
        if (!savedComments.isEmpty()) {
            layoutCommentList.removeAllViews(); // "가장 먼저..." 문구 지우기

            // 기억해둔 댓글 개수만큼 반복해서 화면에 추가합니다.
            for (String savedText : savedComments) {
                addCommentViewToLayout(layoutCommentList, myNickname, savedText);
            }
        }

        // ====================================================================
        // 2. 게시 버튼을 눌렀을 때의 동작
        // ====================================================================
        btnSendComment.setOnClickListener(v -> {
            String comment = etCommentInput.getText().toString().trim();

            if (!comment.isEmpty()) {
                // 첫 댓글이라면 "가장 먼저..." 문구를 지웁니다.
                if (savedComments.isEmpty()) {
                    layoutCommentList.removeAllViews();
                }

                // 나중에 창을 다시 열 때를 대비해서 메모장에 텍스트를 저장해 둡니다.
                savedComments.add(comment);

                // 방금 쓴 댓글을 화면 맨 아래에 즉시 그립니다.
                addCommentViewToLayout(layoutCommentList, myNickname, comment);

                etCommentInput.setText("");
                Toast.makeText(getContext(), "댓글이 등록되었습니다.", Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(getContext(), "댓글을 입력해주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    //  댓글 UI를 조립해서 화면에 붙여주는 전용 함수
    private void addCommentViewToLayout(LinearLayout container, String nickname, String commentText) {
        LinearLayout commentWrapper = new LinearLayout(getContext());
        commentWrapper.setOrientation(LinearLayout.VERTICAL);
        commentWrapper.setPadding(0, 0, 0, 48);

        TextView tvUserName = new TextView(getContext());
        tvUserName.setText(nickname);
        tvUserName.setTextSize(12);
        tvUserName.setTextColor(0xFF888888);
        tvUserName.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvComment = new TextView(getContext());
        tvComment.setText(commentText);
        tvComment.setTextSize(14);
        tvComment.setTextColor(0xFF000000);
        tvComment.setPadding(0, 8, 0, 0);

        commentWrapper.addView(tvUserName);
        commentWrapper.addView(tvComment);

        container.addView(commentWrapper);
    }
}