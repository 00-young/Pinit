package com.example.pinit.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;
import com.example.pinit.database.DatabaseHelper;
import com.example.pinit.model.DailySchedule;
import com.example.pinit.model.MyPlan;
import com.example.pinit.model.Schedule;
import com.example.pinit.model.Trip;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MyPlansBottomSheetFragment extends BottomSheetDialogFragment {

    private DatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_bottom_sheet_my_plans, container, false); // XML 이름 맞게 수정하세요

        RecyclerView rv = view.findViewById(R.id.recyclerViewMyPlans);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        //DB에서 여행 목록 가져오기
        dbHelper = new DatabaseHelper(requireContext());
        List<Trip> tripList = dbHelper.getAllTrips();

        rv.setAdapter(new RecyclerView.Adapter<PlanViewHolder>() {
            @NonNull
            @Override
            public PlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                // XML 아이템 이름 맞게 수정 (기능팀의 item_trip.xml이나 별도 아이템)
                View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
                return new PlanViewHolder(v);
            }

            @Override
            public void onBindViewHolder(@NonNull PlanViewHolder holder, int position) {
                Trip trip = tripList.get(position);
                holder.textView.setText(trip.getTitle() + "\n(" + trip.getStartDate() + " ~ " + trip.getEndDate() + ")");

                holder.itemView.setOnClickListener(v -> {
                    //  클릭 시 해당 여행의 모든 일정을 가져오기
                    List<Schedule> rawSchedules = dbHelper.getSchedulesByTrip(trip.getId());

                    // 기능팀과 똑같이 '시간순'으로 먼저 정렬
                    rawSchedules.sort((a, b) -> {
                        String ta = a.getTime() == null ? "" : a.getTime();
                        String tb = b.getTime() == null ? "" : b.getTime();
                        String[] pa = ta.split(":");
                        String[] pb = tb.split(":");
                        try {
                            int ha = pa.length > 0 ? Integer.parseInt(pa[0]) : 0;
                            int ma = pa.length > 1 ? Integer.parseInt(pa[1]) : 0;
                            int hb = pb.length > 0 ? Integer.parseInt(pb[0]) : 0;
                            int mb = pb.length > 1 ? Integer.parseInt(pb[1]) : 0;
                            return ha != hb ? ha - hb : ma - mb;
                        } catch (NumberFormatException e) {
                            return ta.compareTo(tb);
                        }
                    });
                    //  List<String> 대신 List<Schedule>을 담도록 바구니 변경
                    Map<String, List<Schedule>> grouped = new TreeMap<>();
                    for (Schedule s : rawSchedules) {
                        String date = s.getDate() != null ? s.getDate() : "날짜 미상";
                        grouped.putIfAbsent(date, new ArrayList<>());

                        // 장소명(글자)만 빼서 넣던 코드를 지우고, Schedule 객체를 통째로 넣습니다
                        grouped.get(date).add(s);
                    }

                    // CreatePostFragment로 보낼 데이터 구조 만들기
                    List<DailySchedule> dailySchedules = new ArrayList<>();
                    int dayCount = 1;

                    // 여기도 Map.Entry<String, List<Schedule>> 로 맞춰줍니다.
                    for (Map.Entry<String, List<Schedule>> entry : grouped.entrySet()) {
                        dailySchedules.add(new DailySchedule("DAY " + dayCount, entry.getKey(), entry.getValue()));
                        dayCount++;
                    }

                    // 4개의 데이터를 채워 넣습니다 (제목, 날짜, 장소, 일정 리스트)
                    MyPlan plan = new MyPlan(
                            trip.getTitle(),
                            trip.getStartDate() + " ~ " + trip.getEndDate(),
                            trip.getDestination(),
                            dailySchedules
                    );

                    Bundle bundle = new Bundle();
                    bundle.putSerializable("selectedPlan", plan);
                    getParentFragmentManager().setFragmentResult("planResult", bundle);
                    dismiss();
                });
            }

            @Override
            public int getItemCount() { return tripList.size(); }
        });

        return view;
    }

    private static class PlanViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        public PlanViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}