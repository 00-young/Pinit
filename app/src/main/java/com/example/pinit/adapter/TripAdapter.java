package com.example.pinit.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;
import com.example.pinit.activity.TripDetailActivity;
import com.example.pinit.activity.TripSummaryActivity;
import com.example.pinit.model.Trip;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.ViewHolder> {

    public interface OnClickListener { void onClick(Trip trip); }

    private Context context;
    private List<Trip> list;
    private OnClickListener clickListener, longClickListener;

    public TripAdapter(Context context, List<Trip> list,
                       OnClickListener clickListener, OnClickListener longClickListener) {
        this.context = context;
        this.list = list;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    public void updateList(List<Trip> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_trip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Trip t = list.get(position);
        NumberFormat fmt = NumberFormat.getNumberInstance(Locale.KOREA);

        holder.tvTitle.setText(t.getTitle());
        holder.tvDestination.setText("📍 " + t.getDestination()
                + "  📅 " + t.getStartDate() + " - " + t.getEndDate());
        holder.tvDate.setText("");
        holder.tvBudget.setText("예산: " + fmt.format(t.getBudget()) + "원");

        // D-day
        String dday = calcDday(t.getStartDate());
        if (dday != null) {
            holder.tvDday.setVisibility(View.VISIBLE);
            holder.tvDday.setText(dday);
        } else {
            holder.tvDday.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> clickListener.onClick(t));
        holder.itemView.setOnLongClickListener(v -> { longClickListener.onClick(t); return true; });

        // 상세보기 버튼을 눌렀을 때도 카드 전체를 누른 것과 똑같이 프래그먼트의 리스너가 작동하도록 토스해 줍니다.
        holder.btnDetail.setOnClickListener(v -> {
            // 기존에 정의된 변수 이름인 clickListener와 t를 그대로 사용합니다!
            clickListener.onClick(t);
        });

        // 여행요약 → TripSummaryActivity (★ 변경)
        holder.btnSummary.setOnClickListener(v -> {
            Intent intent = new Intent(context, TripSummaryActivity.class);
            intent.putExtra("trip_id", t.getId());
            context.startActivity(intent);
        });
    }

    private String calcDday(String startDateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            Date startDate = sdf.parse(startDateStr);
            Date today = new Date();
            long diff = startDate.getTime() - today.getTime();
            long days = diff / (1000 * 60 * 60 * 24);
            if (days > 0) return "D-" + days;
            else if (days == 0) return "D-Day";
            else return "D+" + Math.abs(days);
        } catch (ParseException e) {
            return null;
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDestination, tvDate, tvBudget, tvDday;
        Button btnDetail, btnSummary;

        ViewHolder(View view) {
            super(view);
            tvTitle = view.findViewById(R.id.tvTitle);
            tvDestination = view.findViewById(R.id.tvDestination);
            tvDate = view.findViewById(R.id.tvDate);
            tvBudget = view.findViewById(R.id.tvBudget);
            tvDday = view.findViewById(R.id.tvDday);
            btnDetail = view.findViewById(R.id.btnDetail);
            btnSummary = view.findViewById(R.id.btnSummary);
        }
    }
}