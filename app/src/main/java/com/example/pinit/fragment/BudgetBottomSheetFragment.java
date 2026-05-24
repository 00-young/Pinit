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
import com.example.pinit.model.Budget;
import com.example.pinit.model.BudgetSummary;
import com.example.pinit.model.Trip;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BudgetBottomSheetFragment extends BottomSheetDialogFragment {

    private DatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_budget_bottom_sheet, container, false);

        RecyclerView rv = view.findViewById(R.id.rvMyBudgetList);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        dbHelper = new DatabaseHelper(requireContext());
        List<Trip> tripList = dbHelper.getAllTrips();
        List<BudgetSummary> summaryList = new ArrayList<>();

        for (Trip trip : tripList) {
            List<Budget> budgets = dbHelper.getBudgetsByTrip(trip.getId());
            int food = 0, transport = 0, lodge = 0, shopping = 0, activity = 0, etc = 0, total = 0;

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
            // 금액을 만원 단위로 변환하여 모델에 담기
            summaryList.add(new BudgetSummary(
                    trip.getTitle(), total / 10000, food / 10000, transport / 10000,
                    lodge / 10000, shopping / 10000, activity / 10000, etc / 10000
            ));
        }

        rv.setAdapter(new RecyclerView.Adapter<BudgetViewHolder>() {
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
            }
            @Override
            public int getItemCount() { return summaryList.size(); }
        });

        return view;
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