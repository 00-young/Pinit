package com.example.pinit.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;
import com.example.pinit.database.DatabaseHelper;
import com.example.pinit.model.Budget;
import com.example.pinit.model.BudgetSummary;
import com.example.pinit.model.Trip;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BudgetBottomSheetFragment extends BottomSheetDialogFragment {

    private DatabaseHelper dbHelper;
    private List<BudgetSummary> summaryList = new ArrayList<>();
    private RecyclerView.Adapter<BudgetViewHolder> myAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_budget_bottom_sheet, container, false);

        RecyclerView rv = view.findViewById(R.id.rvMyBudgetList);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        dbHelper = new DatabaseHelper(requireContext());

        loadBudgetSummaries();

        myAdapter = new RecyclerView.Adapter<BudgetViewHolder>() {
            @NonNull
            @Override
            public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_budget_list, parent, false);
                return new BudgetViewHolder(v);
            }

            @Override
            public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
                BudgetSummary item = summaryList.get(position);
                holder.tvTitle.setText(item.getTitle());
                NumberFormat fmt = NumberFormat.getNumberInstance(Locale.KOREA);
                holder.tvSummary.setText("총 " + fmt.format(item.getTotal()) + "만원");

                // 아이템 뷰가 클릭 및 롱클릭을 확실히 인지하도록 활성화
                holder.itemView.setClickable(true);
                holder.itemView.setLongClickable(true);

                // 일반 클릭 (통계 데이터 부모 Fragment로 전송)
                holder.itemView.setOnClickListener(v -> {
                    Bundle bundle = new Bundle();
                    bundle.putInt("budgetFood", item.getFood());
                    bundle.putInt("budgetTransport", item.getTransport());
                    bundle.putInt("budgetAccom", item.getAccom());
                    bundle.putInt("budgetShopping", item.getShopping());
                    bundle.putInt("budgetSightseeing", item.getSightseeing());
                    bundle.putInt("budgetEtc", item.getEtc());

                    getParentFragmentManager().setFragmentResult("budgetResult", bundle);
                    dismiss();
                });

                // 길게 누르면 삭제 (롱클릭 감지 인터페이스)
                holder.itemView.setOnLongClickListener(v -> {
                    new AlertDialog.Builder(requireActivity())
                            .setTitle("가계부 내역 삭제")
                            .setMessage("[" + item.getTitle() + "] 여행의 모든 지출 내역을 삭제하시겠습니까?")
                            .setPositiveButton("삭제", (dialog, which) -> {
                                int matchedTripId = -1;
                                List<Trip> allTrips = dbHelper.getAllTrips();
                                if (allTrips != null) {
                                    for (Trip t : allTrips) {
                                        if (t.getTitle() != null && t.getTitle().equals(item.getTitle())) {
                                            matchedTripId = t.getId();
                                            break;
                                        }
                                    }
                                }

                                if (matchedTripId == -1) {
                                    matchedTripId = position + 1;
                                }

                                // 로컬 DB 및 파이어베이스 삭제 (여기서는 tripId를 기준으로 삭제해야 함)
                                // deleteBudget은 단일 ID 삭제이므로, tripId 기준 삭제 로직 필요
                                // 임시로 반복문 처리 또는 DatabaseHelper 수정 필요
                                List<Budget> tripBudgets = dbHelper.getBudgetsByTrip(matchedTripId);
                                if (tripBudgets != null) {
                                    for (Budget b : tripBudgets) {
                                        dbHelper.deleteBudget(b.getId());
                                    }
                                }

                                // 2. 파이어베이스 삭제
                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                if (user != null) {
                                    FirebaseFirestore.getInstance()
                                            .collection("budgets")
                                            .whereEqualTo("tripId", matchedTripId)
                                            .get()
                                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                                for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                                                    doc.getReference().delete();
                                                }
                                            });
                                }

                                Toast.makeText(getContext(), "삭제되었습니다.", Toast.LENGTH_SHORT).show();

                                // 3. 새로고침
                                loadBudgetSummaries();
                                notifyDataSetChanged();
                            })
                            .setNegativeButton("취소", null)
                            .show();

                    return true;
                });
            }

            @Override
            public int getItemCount() { return summaryList.size(); }
        };

        rv.setAdapter(myAdapter);
        return view;
    }

    private void loadBudgetSummaries() {
        if (summaryList == null) summaryList = new ArrayList<>();
        summaryList.clear();

        DatabaseHelper helper = new DatabaseHelper(requireContext());
        List<Trip> tripList = helper.getAllTrips();
        if (tripList == null) return;

        for (Trip trip : tripList) {
            List<Budget> budgets = helper.getBudgetsByTrip(trip.getId());
            int food = 0, transport = 0, lodge = 0, shopping = 0, activity = 0, etc = 0, total = 0;

            if (budgets != null) {
                for (Budget b : budgets) {
                    if ("expense".equals(b.getType())) {
                        int amount = (int) b.getAmount();
                        total += amount;
                        switch (b.getCategory()) {
                            case "식비": food += amount; break;
                            case "교통": transport += amount; break;
                            case "숙박": lodge += amount; break;
                            case "쇼핑": shopping += amount; break;
                            case "관광": activity += amount; break;
                            default: etc += amount; break;
                        }
                    }
                }
            }
            summaryList.add(new BudgetSummary(
                    trip.getTitle(), total / 10000, food / 10000, transport / 10000,
                    lodge / 10000, shopping / 10000, activity / 10000, etc / 10000
            ));
        }
    }

    private static class BudgetViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSummary;
        public BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvBudgetTitle);
            tvSummary = itemView.findViewById(R.id.tvBudgetSummary);
        }
    }
}