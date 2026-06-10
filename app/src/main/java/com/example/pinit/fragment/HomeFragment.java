package com.example.pinit.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;
import com.example.pinit.activity.AddTripActivity;
import com.example.pinit.activity.TripDetailActivity;
import com.example.pinit.adapter.TripAdapter;
import com.example.pinit.model.Trip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private TripAdapter adapter;
    private RecyclerView recyclerView;
    private View layoutEmpty;

    // 💡 생성된 랜덤 문서 ID를 정확히 추적하기 위한 매핑 테이블
    private Map<Integer, String> tripDocIdMap = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new TripAdapter(requireContext(), new ArrayList<>(),
                trip -> {
                    Intent intent = new Intent(requireContext(), TripDetailActivity.class);
                    intent.putExtra("trip_id", trip.getId());

                    // 상세 페이지로 갈 때 진짜 문서 ID 챙겨주기
                    String firestoreDocId = tripDocIdMap.get(trip.getId());
                    intent.putExtra("firestore_doc_id", firestoreDocId);

                    startActivity(intent);
                },
                trip -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("여행 삭제")
                            .setMessage("'" + trip.getTitle() + "' 여행을 삭제하시겠습니까?")
                            .setPositiveButton("삭제", (d, w) -> {

                                // 💡 1. 가짜 ID 조합 대신 진짜 파이어베이스 문서 ID를 꺼내서 정확히 사살(!)
                                String firestoreDocId = tripDocIdMap.get(trip.getId());
                                if (firestoreDocId != null) {
                                    FirebaseFirestore.getInstance().collection("schedules")
                                            .document(firestoreDocId).delete()
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(requireContext(), "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                                                loadData(); // 삭제 직후 리스트 새로고침
                                            });
                                }
                            })
                            .setNegativeButton("취소", null).show();
                });
        recyclerView.setAdapter(adapter);

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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }

        FirebaseFirestore.getInstance().collection("schedules")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Trip> trips = new ArrayList<>();
                    tripDocIdMap.clear(); // 맵 초기화

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Trip trip = new Trip();

                        String title = doc.getString("title");
                        String country = doc.getString("country");
                        String start = doc.getString("startDate");
                        String end = doc.getString("endDate");

                        trip.setTitle(title != null && !title.isEmpty() ? title : "이름 없는 일정");
                        trip.setDestination(country != null && !country.isEmpty() ? country : "미정");
                        trip.setStartDate(start != null && !start.isEmpty() ? start : "2026-06-10");
                        trip.setEndDate(end != null && !end.isEmpty() ? end : "2026-06-10");

                        String docId = doc.getId();
                        int tripId = docId.hashCode();
                        if (docId.contains("_")) {
                            String[] parts = docId.split("_");
                            if (parts.length > 1) {
                                try { tripId = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
                            }
                        }
                        trip.setId(tripId);
                        trips.add(trip);

                        // 문서 ID 보관
                        tripDocIdMap.put(tripId, docId);
                    }

                    adapter.updateList(trips);
                    boolean isEmpty = trips.isEmpty();
                    layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                });
    }
}