package com.example.pinit.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;
import com.example.pinit.model.Schedule;

import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    public interface OnDeleteListener { void onDelete(int id); }

    private Context context;
    private List<Schedule> list;
    private OnDeleteListener listener;

    public ScheduleAdapter(Context context, List<Schedule> list, OnDeleteListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Schedule s = list.get(position);
        holder.tvTitle.setText(s.getTitle());
        holder.tvTime.setText(s.getTime() != null ? s.getTime() : "");
        holder.tvPlace.setText(s.getPlaceName() != null ? "📍 " + s.getPlaceName() : "");
        holder.tvDate.setText(s.getDate());
        try { holder.viewColor.setBackgroundColor(Color.parseColor(s.getColor())); }
        catch (Exception e) { holder.viewColor.setBackgroundColor(Color.parseColor("#1976D2")); }
        holder.itemView.setOnLongClickListener(v -> { listener.onDelete(s.getId()); return true; });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime, tvPlace, tvDate;
        View viewColor;
        ViewHolder(View view) {
            super(view);
            tvTitle = view.findViewById(R.id.tvTitle);
            tvTime = view.findViewById(R.id.tvTime);
            tvPlace = view.findViewById(R.id.tvMemo);
            tvDate = view.findViewById(R.id.tvDate) != null ? view.findViewById(R.id.tvDate) : view.findViewById(R.id.tvTitle);
            viewColor = view.findViewById(R.id.viewColor);
        }
    }
}
