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
import java.util.ArrayList;

// 일반 Fragment가 아니라 BottomSheetDialogFragment를 상속받아야 바닥에서 올라옵니다!
public class TagBottomSheetFragment extends BottomSheetDialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_tag_bottom_sheet, container, false);

        //  1. 바텀시트 레이아웃에 있는 '적용하기' 또는 '확인' 버튼을 찾아옵니다.
        Button btnApplyTag = view.findViewById(R.id.btnApplyTags);

        // 버튼이 정상적으로 존재할 때 실행할 클릭 이벤트 리스너를 달아줍니다.
        if (btnApplyTag != null) {
            btnApplyTag.setOnClickListener(v -> {

                // 🌟 [추가] 2. 글쓰기 화면으로 보낼 태그 가방(ArrayList)을 만듭니다.
                ArrayList<String> selectedTags = new ArrayList<>();

                // 💡 [시뮬레이션] 지금은 가짜 데이터로 이미지에 있던 3개 태그를 강제로 넣어줍니다.
                // 추후 기능팀이 체크박스나 칩 클릭 리스너를 완성하면 선택된 값들이 들어가게 바꾸면 됩니다.
                selectedTags.add("감성");
                selectedTags.add("우정 여행");
                selectedTags.add("1박 2일");

                // 🌟 [추가] 3. 택배 상자(Bundle)에 태그 리스트를 이쁘게 담아줍니다.
                Bundle bundle = new Bundle();
                bundle.putStringArrayList("selectedTags", selectedTags);

                // 🌟 [추가] 4. 아까 CreatePostFragment에 만들어둔 "tagResult" 문으로 택배를 쏩니다!
                getParentFragmentManager().setFragmentResult("tagResult", bundle);

                // 🌟 [추가] 5. 임무를 완료했으니 태그 바텀시트 창을 닫아줍니다.
                dismiss();
            });
        }

        return view;
    }
}