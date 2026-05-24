package com.example.pinit.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;
import com.example.pinit.service.CongestionCallback;
import com.example.pinit.service.CongestionData;
import com.example.pinit.service.CongestionService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// [어댑터] 장소 검색/주변 찾기 결과 목록
// 각 카드: 장소명, 주소, 별점/리뷰 수, 혼잡도 배지(서울 주요 지역만)
// 혼잡도 배지: 서울 열린데이터 API 조회, 결과는 congestionCache에 캐싱 (중복 요청 방지)
// 배지 클릭 → 혼잡도 상세 다이얼로그 (현재 수준·메시지·완화 예상 시각)
public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.ViewHolder> {

    public interface OnClickListener { void onClick(Map<String, String> place); }

    // 서울 열린데이터 API가 지원하는 주요 지역 키워드 목록
    // 장소명+주소에 이 키워드가 포함될 때만 혼잡도 배지를 표시
    private static final String[] SEOUL_AREAS = {
        "강남역", "건대입구역", "경복궁", "고속터미널역", "광화문", "구로디지털단지역",
        "국립중앙박물관", "남산공원", "노량진역", "대학로", "덕수궁", "동대문",
        "동대문디자인플라자", "DDP", "동묘앞역", "망원한강공원", "명동", "반포한강공원",
        "보라매공원", "북촌한옥마을", "삼청동", "상암", "서울대입구역", "서울역",
        "서울어린이대공원", "서울식물원", "성수역", "수서역", "수유역", "신도림역",
        "신림역", "신촌역", "아이파크몰", "압구정로데오역", "압구정", "여의도",
        "연남동", "영등포", "왕십리역", "용산", "이태원", "인사동", "잠실역",
        "잠실", "종로", "청계", "청담역", "코엑스", "홍대입구역", "홍대", "혜화역"
    };

    private Context context;
    private List<Map<String, String>> list;
    private OnClickListener listener;

    private final CongestionService congestionService = new CongestionService();
    private final Map<String, CongestionData> congestionCache = new HashMap<>(); // area → 결과 캐시
    private final Set<String> pendingRequests = new HashSet<>();                 // 중복 요청 방지

    public PlaceAdapter(Context context, List<Map<String, String>> list, OnClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    public void updateList(List<Map<String, String>> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_place, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> place = list.get(position);
        String name = place.getOrDefault("name", "");

        holder.tvName.setText(name);
        holder.tvAddress.setText("📍 " + place.getOrDefault("address", ""));
        String rating = place.getOrDefault("rating", "");
        String total = place.getOrDefault("user_ratings_total", "0");
        holder.tvRating.setText(rating.isEmpty() ? "" : "⭐ " + rating + " (" + total + "개 리뷰)");
        holder.itemView.setOnClickListener(v -> listener.onClick(place));

        // 혼잡도 배지 초기화 (스크롤 재사용 시 이전 상태 제거)
        holder.tvStatus.setVisibility(View.GONE);
        holder.tvStatus.setOnClickListener(null);

        // 장소명+주소에서 서울 주요 지역 키워드 매칭
        String address = place.getOrDefault("address", "");
        String area = findSeoulArea(name, address);
        if (area == null) return; // 매칭 지역 없으면 배지 미표시

        if (congestionCache.containsKey(area)) {
            // 캐시에 결과 있으면 즉시 배지 표시
            CongestionData data = congestionCache.get(area);
            if (data != null) bindCongestionBadge(holder.tvStatus, data);
        } else if (!pendingRequests.contains(area)) {
            // 처음 요청: API 호출 후 캐시 저장 → 해당 위치 아이템 갱신
            pendingRequests.add(area);
            congestionService.getCongestionData(area, new CongestionCallback() {
                @Override
                public void onSuccess(CongestionData data) {
                    congestionCache.put(area, data);
                    pendingRequests.remove(area);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        int idx = findPosition(name);
                        if (idx >= 0) notifyItemChanged(idx);
                    });
                }

                @Override
                public void onFailure(String error) {
                    congestionCache.put(area, null); // null 캐싱으로 재요청 방지
                    pendingRequests.remove(area);
                }
            });
        }
    }

    // 혼잡도 수준에 따라 배지 색상 설정 후 표시 (여유:초록, 보통:파랑, 붐빔:주황, 매우붐빔:빨강)
    private void bindCongestionBadge(TextView tvStatus, CongestionData data) {
        String level = data.getCongestionLevel();
        int bgColor, textColor;
        switch (level) {
            case "여유":
                bgColor = Color.parseColor("#E8F5E9");
                textColor = Color.parseColor("#2E7D32");
                break;
            case "보통":
                bgColor = Color.parseColor("#E3F2FD");
                textColor = Color.parseColor("#1565C0");
                break;
            case "붐빔":
                bgColor = Color.parseColor("#FFF3E0");
                textColor = Color.parseColor("#E65100");
                break;
            case "매우붐빔":
                bgColor = Color.parseColor("#FFEBEE");
                textColor = Color.parseColor("#C62828");
                break;
            default:
                bgColor = Color.parseColor("#F5F5F5");
                textColor = Color.parseColor("#757575");
        }

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(24f);
        bg.setColor(bgColor);
        tvStatus.setBackground(bg);
        tvStatus.setTextColor(textColor);
        tvStatus.setText(level);
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setOnClickListener(v -> showCongestionDialog(data));
    }

    // 혼잡도 상세 다이얼로그: 현재 수준, 안내 메시지, 완화 예상 시각
    private void showCongestionDialog(CongestionData data) {
        boolean isForecastNow = data.getForecastTime().equals("현재 쾌적");
        String forecastLine = isForecastNow
                ? "현재 쾌적한 상태입니다"
                : data.getForecastTime() + " 이후 '" + data.getForecastLevel() + "' 예상";

        String message = "현재 혼잡도:  " + data.getCongestionLevel() + "\n\n"
                + data.getCongestionMessage() + "\n\n"
                + "📊 혼잡도 예보\n" + forecastLine;

        new AlertDialog.Builder(context)
                .setTitle("📍 " + data.getAreaName() + " 혼잡도")
                .setMessage(message)
                .setPositiveButton("확인", null)
                .show();
    }

    // 장소명+주소 합쳐서 SEOUL_AREAS 키워드와 매칭, 첫 번째 일치 키워드 반환
    private String findSeoulArea(String name, String address) {
        String combined = name + " " + address;
        for (String area : SEOUL_AREAS) {
            if (combined.contains(area)) return area;
        }
        return null;
    }

    // 특정 장소명의 RecyclerView 내 위치 검색 (API 응답 후 해당 아이템만 갱신할 때 사용)
    private int findPosition(String name) {
        for (int i = 0; i < list.size(); i++) {
            if (name.equals(list.get(i).getOrDefault("name", ""))) return i;
        }
        return -1;
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress, tvRating, tvStatus;

        ViewHolder(View view) {
            super(view);
            tvName = view.findViewById(R.id.tvName);
            tvAddress = view.findViewById(R.id.tvAddress);
            tvRating = view.findViewById(R.id.tvRating);
            tvStatus = view.findViewById(R.id.tvStatus); // 혼잡도 배지
        }
    }
}
