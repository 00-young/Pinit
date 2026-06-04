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

    private static final String ARG_PRESELECTED = "preselectedTags";

    /** 미리 선택된 태그를 전달해 화면을 여는 팩토리 */
    public static TagBottomSheetFragment newInstance(ArrayList<String> preselected) {
        TagBottomSheetFragment fragment = new TagBottomSheetFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_PRESELECTED, preselected != null ? preselected : new ArrayList<>());
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_tag_bottom_sheet, container, false);

        ChipGroup groupTogether = view.findViewById(R.id.chipGroupWith);
        ChipGroup groupDuration = view.findViewById(R.id.chipGroupDuration);
        ChipGroup groupTheme = view.findViewById(R.id.chipGroupTheme);

        // 전달받은 미리 선택 태그들을 칩에 반영 (이미 선택된 상태로 표시)
        ArrayList<String> preselected = getArguments() != null
                ? getArguments().getStringArrayList(ARG_PRESELECTED) : null;
        if (preselected != null && !preselected.isEmpty()) {
            applyPreselection(groupTogether, preselected);
            applyPreselection(groupDuration, preselected);
            applyPreselection(groupTheme, preselected);
        }

        Button btnApplyTag = view.findViewById(R.id.btnApplyTags);
        if (btnApplyTag != null) {
            btnApplyTag.setOnClickListener(v -> {
                ArrayList<String> selectedTags = new ArrayList<>();
                addCheckedChipsToList(groupTogether, selectedTags);
                addCheckedChipsToList(groupDuration, selectedTags);
                addCheckedChipsToList(groupTheme, selectedTags);

                Bundle bundle = new Bundle();
                bundle.putStringArrayList("selectedTags", selectedTags);
                getParentFragmentManager().setFragmentResult("tagResult", bundle);
                dismiss();
            });
        }

        return view;
    }

    /** preselected 목록에 있는 칩을 체크 상태로 만든다 (# 유무 무관하게 비교) */
    private void applyPreselection(ChipGroup chipGroup, ArrayList<String> preselected) {
        if (chipGroup == null) return;
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                String chipText = chip.getText().toString().replace("#", "").trim();
                for (String tag : preselected) {
                    if (chipText.equals(tag.replace("#", "").trim())) {
                        chip.setChecked(true);
                        break;
                    }
                }
            }
        }
    }

    private void addCheckedChipsToList(ChipGroup chipGroup, ArrayList<String> list) {
        if (chipGroup == null) return;
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (chip.isChecked()) {
                    list.add(chip.getText().toString().replace("#", ""));
                }
            }
        }
    }
}