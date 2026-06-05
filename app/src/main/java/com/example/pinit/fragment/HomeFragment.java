package com.example.pinit.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;
import com.example.pinit.activity.AddTripActivity;
import com.example.pinit.activity.TripDetailActivity;
import com.example.pinit.adapter.TripAdapter;
import com.example.pinit.database.DatabaseHelper;
import com.example.pinit.database.FirestoreRepository;
import com.example.pinit.model.Trip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class HomeFragment extends Fragment {

    private DatabaseHelper dbHelper;
    private TripAdapter adapter;
    private RecyclerView recyclerView;
    private View layoutEmpty;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        dbHelper = new DatabaseHelper(requireContext());
        recyclerView = view.findViewById(R.id.recyclerView);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TripAdapter(requireContext(), new java.util.ArrayList<>(),
                trip -> {
                    Intent intent = new Intent(requireContext(), TripDetailActivity.class);
                    intent.putExtra("trip_id", trip.getId());
                    startActivity(intent);
                },
                trip -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("여행 삭제")
                            .setMessage("'" + trip.getTitle() + "' 여행을 삭제하시겠습니까?")
                            .setPositiveButton("삭제", (d, w) -> {
                                // =========================
                                // SQLite 삭제
                                // =========================
                                dbHelper.deleteTrip(trip.getId());
                                // =========================
                                // Firestore 삭제
                                // =========================

                                FirebaseUser user =
                                        FirebaseAuth.getInstance()
                                                .getCurrentUser();

                                if (user != null) {

                                    FirestoreRepository repository =
                                            new FirestoreRepository();

                                    String scheduleId =
                                            user.getUid() + "_"
                                                    + trip.getId();

                                    repository.deleteSchedule(
                                            scheduleId
                                    );
                                }
                                loadData();
                            })
                            .setNegativeButton("취소", null).show();
                });
        recyclerView.setAdapter(adapter);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private final ColorDrawable background = new ColorDrawable(Color.parseColor("#F44336"));
            private final Paint textPaint = new Paint();

            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Trip trip = adapter.getItem(position);
                adapter.notifyItemChanged(position); // 스와이프 복원 후 다이얼로그 표시
                new AlertDialog.Builder(requireContext())
                        .setTitle("여행 삭제")
                        .setMessage("'" + trip.getTitle() + "' 여행을 삭제하시겠습니까?")
                        .setPositiveButton("삭제", (d, w) -> {
                            dbHelper.deleteTrip(trip.getId());
                            com.google.firebase.auth.FirebaseUser user =
                                    com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                new FirestoreRepository().deleteSchedule(user.getUid() + "_" + trip.getId());
                            }
                            loadData();
                        })
                        .setNegativeButton("취소", null).show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv,
                    @NonNull RecyclerView.ViewHolder vh, float dX, float dY, int actionState, boolean isActive) {
                View itemView = vh.itemView;
                background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                background.draw(c);
                textPaint.setColor(Color.WHITE);
                textPaint.setTextSize(40f);
                textPaint.setAntiAlias(true);
                String text = "삭제";
                float textX = itemView.getRight() - 150;
                float textY = itemView.getTop() + (itemView.getHeight() / 2f) + 14;
                if (dX < -100) c.drawText(text, textX, textY, textPaint);
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isActive);
            }
        }).attachToRecyclerView(recyclerView);

        view.findViewById(R.id.btnNewTrip).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddTripActivity.class)));

        view.findViewById(R.id.btnMakePlan).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddTripActivity.class)));

        view.findViewById(R.id.btnOpenMyPage).setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new MyPageFragment())
                        .addToBackStack(null)
                        .commit());

        loadData();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        List<Trip> trips = dbHelper.getAllTrips();
        adapter.updateList(trips);
        boolean isEmpty = trips.isEmpty();
        layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}
