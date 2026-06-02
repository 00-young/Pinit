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
import com.example.pinit.adapter.BudgetAdapter;
import com.example.pinit.model.Budget;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class CategoryDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_CATEGORY = "category";
    private static final String ARG_BUDGET_IDS = "budget_ids";

    private List<Budget> allBudgets;

    public static CategoryDetailBottomSheet newInstance(String category, List<Budget> budgets) {
        CategoryDetailBottomSheet sheet = new CategoryDetailBottomSheet();
        sheet.allBudgets = budgets;
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY, category);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_category_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        String category = getArguments() != null ? getArguments().getString(ARG_CATEGORY, "") : "";

        List<Budget> filtered = new ArrayList<>();
        if (allBudgets != null) {
            for (Budget b : allBudgets) {
                if ("expense".equals(b.getType())) {
                    boolean match = category.equals("기타")
                            ? isEtcCategory(b.getCategory())
                            : category.equals(b.getCategory());
                    if (match) filtered.add(b);
                }
            }
        }

        TextView tvTitle = view.findViewById(R.id.tvCategoryTitle);
        TextView tvCount = view.findViewById(R.id.tvCategoryCount);
        RecyclerView rv = view.findViewById(R.id.rvCategoryDetail);
        TextView tvEmpty = view.findViewById(R.id.tvEmpty);

        tvTitle.setText(category + " 지출 내역");
        tvCount.setText("총 " + filtered.size() + "건");

        if (filtered.isEmpty()) {
            rv.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            rv.setLayoutManager(new LinearLayoutManager(requireContext()));
            rv.setAdapter(new BudgetAdapter(requireContext(), filtered, id -> {}));
        }
    }

    private boolean isEtcCategory(String cat) {
        if (cat == null) return true;
        switch (cat) {
            case "식비": case "교통": case "숙박": case "쇼핑": case "관광": return false;
            default: return true;
        }
    }
}
