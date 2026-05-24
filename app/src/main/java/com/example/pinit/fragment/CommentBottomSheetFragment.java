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
import com.example.pinit.data.MyComment;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class CommentBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_NICKNAME = "nickname";
    private static final String DEFAULT_NICKNAME = "User_1234567";
    private static final String LEGACY_DEFAULT_NICKNAME = "\uB0C9\uB3D9\uB41C \uBE14\uB8E8\uBCA0\uB9AC";

    private static final String POST_ID = MyComment.POST_ID_SHANGHAI;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_bottom_sheet_comment, container, false);

        EditText etCommentInput = view.findViewById(R.id.etCommentInput);
        Button btnSendComment = view.findViewById(R.id.btnSendComment);
        LinearLayout layoutCommentList = view.findViewById(R.id.layoutCommentList);

        List<String> comments = MyComment.getComments(requireContext(), POST_ID);
        if (!comments.isEmpty()) {
            layoutCommentList.removeAllViews();
            String myNickname = getMyNickname();
            for (String comment : comments) {
                addCommentViewToLayout(layoutCommentList, myNickname, comment);
            }
        }

        btnSendComment.setOnClickListener(v -> {
            String comment = etCommentInput.getText().toString().trim();
            if (comment.isEmpty()) {
                Toast.makeText(getContext(), "\uB313\uAE00\uC744 \uC785\uB825\uD574\uC8FC\uC138\uC694.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (MyComment.getComments(requireContext(), POST_ID).isEmpty()) {
                layoutCommentList.removeAllViews();
            }

            String myNickname = getMyNickname();
            MyComment.addComment(requireContext(), POST_ID, comment);
            addCommentViewToLayout(layoutCommentList, myNickname, comment);

            etCommentInput.setText("");
            Toast.makeText(getContext(), "\uB313\uAE00\uC774 \uB4F1\uB85D\uB418\uC5C8\uC2B5\uB2C8\uB2E4.", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

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

    private String getMyNickname() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String nickname = prefs.getString(KEY_NICKNAME, DEFAULT_NICKNAME);
        if (nickname == null || nickname.trim().isEmpty() || LEGACY_DEFAULT_NICKNAME.equals(nickname)) {
            prefs.edit().putString(KEY_NICKNAME, DEFAULT_NICKNAME).apply();
            return DEFAULT_NICKNAME;
        }
        return nickname;
    }

}
