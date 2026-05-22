package com.example.pinit.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pinit.R;
import com.example.pinit.adapter.MyPlansAdapter;
import com.example.pinit.model.MyPlan;
import com.example.pinit.model.DailySchedule;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.ArrayList;
import java.util.List;

public class MyPlansBottomSheetFragment extends BottomSheetDialogFragment {

    private MyPlansAdapter adapter;
    private List<MyPlan> myPlanList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_bottom_sheet_my_plans, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewMyPlans);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // =========================================================
        //  데이터 만드는 곳: 가방 안에 파우치를 넣습니다.
        // =========================================================

        // 1. 세부 일정(장소) 데이터 만들기
        List<String> day1Places = new ArrayList<>();
        day1Places.add("상하이 푸둥 국제 공항");
        day1Places.add("Shanghai Royal Garden Hotel");
        day1Places.add("상하이 디즈니랜드");

        List<String> day2Places = new ArrayList<>();
        day2Places.add("동방명주");
        day2Places.add("대한민국 임시정부");

        // 2. 날짜별 파우치(DailySchedule) 만들기
        DailySchedule day1 = new DailySchedule("DAY 1", "5월 1일", day1Places);
        DailySchedule day2 = new DailySchedule("DAY 2", "5월 2일", day2Places);

        // 3. 여행 가방(MyPlan)에 파우치 담기
        myPlanList = new ArrayList<>();

        // 상황 A: [일정 전체 담기] (DAY1, DAY2 모두 있음)
        List<DailySchedule> fullSchedule = new ArrayList<>();
        fullSchedule.add(day1);
        fullSchedule.add(day2);
        myPlanList.add(new MyPlan("PLAN_001", "1박 2일 상하이 (전체)", "2026/05/01 ~ 05/02", "상하이", fullSchedule));

        // 상황 B: [이 날짜의 일정만 담기] (DAY1 하나만 있음!)
        List<DailySchedule> partialSchedule = new ArrayList<>();
        partialSchedule.add(day1);
        myPlanList.add(new MyPlan("PLAN_002", "상하이 (DAY1만 담음)", "2026/05/01", "상하이", partialSchedule));

        // =========================================================

        //  OnPlanSelectedListener에서 '선택' 기능만 수행
        adapter = new MyPlansAdapter(myPlanList, new MyPlansAdapter.OnPlanSelectedListener() {
            @Override
            public void onPlanSelected(MyPlan plan) {
                // 선택된 여행 데이터를 통째로 쏴줍니다!
                Bundle result = new Bundle();
                result.putSerializable("selectedPlan", plan);
                getParentFragmentManager().setFragmentResult("planResult", result);

                dismiss(); // 바텀 시트 닫기
            }

        });

        recyclerView.setAdapter(adapter);
        return view;
    }
}