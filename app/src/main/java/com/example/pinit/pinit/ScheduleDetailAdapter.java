package com.example.pinit.pinit;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;
import com.example.pinit.model.Schedule;

import java.util.List;

public class ScheduleDetailAdapter extends RecyclerView.Adapter<ScheduleDetailAdapter.ViewHolder> {

    public interface OnMapClickListener { void onMapClick(Schedule schedule); }
    public interface OnDeleteListener { void onDelete(int id); }

    private Context context;
    private List<Schedule> list;
    private OnMapClickListener mapClickListener;
    private OnDeleteListener deleteListener;

    public ScheduleDetailAdapter(Context context, List<Schedule> list,
                                 OnMapClickListener mapClickListener,
                                 OnDeleteListener deleteListener) {
        this.context = context;
        this.list = list;
        this.mapClickListener = mapClickListener;
        this.deleteListener = deleteListener;
    }

    public void updateList(List<Schedule> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_schedule_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Schedule s = list.get(position);

        holder.tvTime.setText(s.getTime() != null && !s.getTime().isEmpty() ? s.getTime() : "--:--");
        holder.tvTitle.setText(s.getTitle());
        holder.tvPlace.setText(s.getPlaceName() != null && !s.getPlaceName().isEmpty()
                ? "📍 " + s.getPlaceName() : "");

        holder.tvOpenMap.setOnClickListener(v -> mapClickListener.onMapClick(s));

        holder.itemView.setOnLongClickListener(v -> {
            deleteListener.onDelete(s.getId());
            return true;
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvTitle, tvPlace, tvOpenMap;

        ViewHolder(View view) {
            super(view);
            tvTime = view.findViewById(R.id.tvTime);
            tvTitle = view.findViewById(R.id.tvTitle);
            tvPlace = view.findViewById(R.id.tvPlace);
            tvOpenMap = view.findViewById(R.id.tvOpenMap);
        }
    }
}
