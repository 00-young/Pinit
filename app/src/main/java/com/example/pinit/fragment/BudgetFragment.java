package com.example.pinit.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;
import com.example.pinit.activity.AddBudgetActivity;
import com.example.pinit.activity.ReceiptScanActivity;
import com.example.pinit.adapter.BudgetAdapter;
import com.example.pinit.database.DatabaseHelper;
import com.example.pinit.model.Budget;
import com.example.pinit.model.Trip;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BudgetFragment extends Fragment {

    private static final int REQUEST_SCAN = 100;
    private static final int REQUEST_ADD = 200;

    private DatabaseHelper dbHelper;
    private BudgetAdapter adapter;
    private RecyclerView recyclerView;
    private Spinner spinnerTrip;
    private TextView tvBudgetTotal, tvBudgetUsed, tvBudgetRemain, tvBudgetCount;
    private TextView tvCatFood, tvCatTransport, tvCatLodge, tvCatShopping, tvCatActivity, tvCatEtc;
    private List<Trip> tripList = new ArrayList<>();
    private List<Budget> currentBudgets = new ArrayList<>();
    private int selectedTripId = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);

        dbHelper = new DatabaseHelper(requireContext());
        spinnerTrip = view.findViewById(R.id.spinnerTrip);
        tvBudgetTotal = view.findViewById(R.id.tvBudgetTotal);
        tvBudgetUsed = view.findViewById(R.id.tvBudgetUsed);
        tvBudgetRemain = view.findViewById(R.id.tvBudgetRemain);
        tvBudgetCount = view.findViewById(R.id.tvBudgetCount);
        tvCatFood = view.findViewById(R.id.tvCatFood);
        tvCatTransport = view.findViewById(R.id.tvCatTransport);
        tvCatLodge = view.findViewById(R.id.tvCatLodge);
        tvCatShopping = view.findViewById(R.id.tvCatShopping);
        tvCatActivity = view.findViewById(R.id.tvCatActivity);
        tvCatEtc = view.findViewById(R.id.tvCatEtc);

        view.findViewById(R.id.cardFood).setOnClickListener(v -> showCategoryDetail("식비"));
        view.findViewById(R.id.cardTransport).setOnClickListener(v -> showCategoryDetail("교통"));
        view.findViewById(R.id.cardLodge).setOnClickListener(v -> showCategoryDetail("숙박"));
        view.findViewById(R.id.cardShopping).setOnClickListener(v -> showCategoryDetail("쇼핑"));
        view.findViewById(R.id.cardActivity).setOnClickListener(v -> showCategoryDetail("관광"));
        view.findViewById(R.id.cardEtc).setOnClickListener(v -> showCategoryDetail("기타"));

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new BudgetAdapter(requireContext(), new ArrayList<>(), id -> {
            dbHelper.deleteBudget(id);
            loadBudgets();
        });
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.btnOpenMyPage).setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new MyPageFragment())
                        .addToBackStack(null)
                        .commit());

        // 직접 입력 버튼
        view.findViewById(R.id.btnAddBudget).setOnClickListener(v -> {
            if (selectedTripId == -1) {
                Toast.makeText(requireContext(), "여행을 먼저 선택해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(requireContext(), AddBudgetActivity.class);
            intent.putExtra("trip_id", selectedTripId);
            startActivityForResult(intent, REQUEST_ADD);
        });

        // 영수증 스캔 버튼
        view.findViewById(R.id.btnScanReceipt).setOnClickListener(v -> {
            if (selectedTripId == -1) {
                Toast.makeText(requireContext(), "여행을 먼저 선택해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(requireContext(), ReceiptScanActivity.class);
            intent.putExtra("trip_id", selectedTripId);
            startActivityForResult(intent, REQUEST_SCAN);
        });

        loadTrips();
        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_SCAN && data != null) {
                // 영수증 스캔으로 인식된 금액 → AddBudgetActivity로 전달
                double amount = data.getDoubleExtra("amount", 0);
                Intent intent = new Intent(requireContext(), AddBudgetActivity.class);
                intent.putExtra("trip_id", selectedTripId);
                intent.putExtra("auto_amount", amount);
                startActivityForResult(intent, REQUEST_ADD);
            } else if (requestCode == REQUEST_ADD) {
                loadBudgets();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTrips();
    }

    private void loadTrips() {
        tripList = dbHelper.getAllTrips();
        if (tripList.isEmpty()) return;

        List<String> tripNames = new ArrayList<>();
        for (Trip t : tripList) tripNames.add(t.getTitle());

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, tripNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTrip.setAdapter(spinnerAdapter);

        selectedTripId = tripList.get(0).getId();
        loadBudgets();

        spinnerTrip.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTripId = tripList.get(position).getId();
                loadBudgets();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadBudgets() {
        if (selectedTripId == -1 || recyclerView == null) return;
        Trip trip = dbHelper.getTripById(selectedTripId);
        if (trip == null) return;

        NumberFormat fmt = NumberFormat.getNumberInstance(Locale.KOREA);
        List<Budget> budgets = dbHelper.getBudgetsByTrip(selectedTripId);
        currentBudgets = budgets;

        double used = 0, food = 0, transport = 0, lodge = 0, shopping = 0, activity = 0, etc = 0;
        for (Budget b : budgets) {
            if ("expense".equals(b.getType())) {
                used += b.getAmount();
                switch (b.getCategory()) {
                    case "식비": food += b.getAmount(); break;
                    case "교통": transport += b.getAmount(); break;
                    case "숙박": lodge += b.getAmount(); break;
                    case "쇼핑": shopping += b.getAmount(); break;
                    case "관광": activity += b.getAmount(); break;
                    default: etc += b.getAmount(); break;
                }
            }
        }

        double remain = trip.getBudget() - used;

        tvBudgetTotal.setText(fmt.format(trip.getBudget()) + "원");
        tvBudgetUsed.setText(fmt.format(used) + "원");
        tvBudgetRemain.setText(fmt.format(remain) + "원");
        tvBudgetRemain.setTextColor(remain < 0 ? Color.RED : Color.parseColor("#333333"));
        tvBudgetCount.setText("총 " + budgets.size() + "건");

        tvCatFood.setText(fmt.format(food) + "원");
        tvCatTransport.setText(fmt.format(transport) + "원");
        tvCatLodge.setText(fmt.format(lodge) + "원");
        tvCatShopping.setText(fmt.format(shopping) + "원");
        tvCatActivity.setText(fmt.format(activity) + "원");
        tvCatEtc.setText(fmt.format(etc) + "원");

        adapter = new BudgetAdapter(requireContext(), budgets, id -> {
            dbHelper.deleteBudget(id);
            loadBudgets();
        });
        recyclerView.setAdapter(adapter);
    }

    private void showCategoryDetail(String category) {
        CategoryDetailBottomSheet.newInstance(category, currentBudgets)
                .show(getChildFragmentManager(), "category_detail");
    }
}
