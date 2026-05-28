package com.example.pinit.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.pinit.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;

public class TagBottomSheetFragment extends BottomSheetDialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_tag_bottom_sheet, container, false);

        // 1. 적용하기 버튼 찾기
        Button btnApplyTag = view.findViewById(R.id.btnApplyTags);

        // 버튼 클릭 이벤트
        if (btnApplyTag != null) {
            btnApplyTag.setOnClickListener(v -> {

                ArrayList<String> selectedTags = new ArrayList<>();


                ChipGroup groupTogether = view.findViewById(R.id.chipGroupWith);
                ChipGroup groupDuration = view.findViewById(R.id.chipGroupDuration);
                ChipGroup groupTheme = view.findViewById(R.id.chipGroupTheme);

                // 만들어둔 도우미 함수를 이용해 체크된 칩의 글자만 쏙쏙 빼서 selectedTags에 담습니다.
                addCheckedChipsToList(groupTogether, selectedTags);
                addCheckedChipsToList(groupDuration, selectedTags);
                addCheckedChipsToList(groupTheme, selectedTags);

                // 택배 상자(Bundle)에 담아서 CreatePostFragment로 쏘기
                Bundle bundle = new Bundle();
                bundle.putStringArrayList("selectedTags", selectedTags);
                getParentFragmentManager().setFragmentResult("tagResult", bundle);

                // 바텀시트 닫기
                dismiss();
            });
        }

        return view;
    }

    // 칩 그룹을 넘겨주면, 그 안에서 체크된(V) 칩들의 글자만 찾아주는 도우미 함수입니다.
    private void addCheckedChipsToList(ChipGroup chipGroup, ArrayList<String> list) {
        if (chipGroup == null) return;

        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (chip.isChecked()) {
                    // 글자 앞에 '#'이 붙어있다면 떼어내고 깔끔하게 글자만 담아줍니다.
                    list.add(chip.getText().toString().replace("#", ""));
                }
            }
        }
    }
}