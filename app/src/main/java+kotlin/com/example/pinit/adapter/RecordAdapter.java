package com.example.pinit.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;
import com.example.pinit.model.Record;

import java.util.List;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.ViewHolder> {

    public interface OnClickListener { void onClick(Record record); }
    public interface OnDeleteListener { void onDelete(int id); }

    private Context context;
    private List<Record> list;
    private OnClickListener clickListener;
    private OnDeleteListener listener;

    public RecordAdapter(Context context, List<Record> list,
                         OnClickListener clickListener, OnDeleteListener listener) {
        this.context = context;
        this.list = list;
        this.clickListener = clickListener;
        this.listener = listener;
    }

    public void updateList(List<Record> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Record r = list.get(position);
        holder.tvTitle.setText(r.getTitle());
        holder.tvDate.setText("📅 " + r.getDate());
        holder.tvContent.setText(r.getContent() != null ? r.getContent() : "");
        holder.tvPlace.setText(r.getPlaceName() != null ? "📍 " + r.getPlaceName() : "");
        holder.itemView.setOnClickListener(v -> { if (clickListener != null) clickListener.onClick(r); });
        holder.itemView.setOnLongClickListener(v -> { listener.onDelete(r.getId()); return true; });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvContent, tvPlace;
        ViewHolder(View view) {
            super(view);
            tvTitle = view.findViewById(R.id.tvTitle);
            tvDate = view.findViewById(R.id.tvDate);
            tvContent = view.findViewById(R.id.tvContent);
            tvPlace = view.findViewById(R.id.tvPlace);
        }
    }
}
