package com.example.pinit.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;

import java.util.List;
import java.util.Map;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.ViewHolder> {

    public interface OnClickListener { void onClick(Map<String, String> place); }

    private Context context;
    private List<Map<String, String>> list;
    private OnClickListener listener;

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
        holder.tvName.setText(place.getOrDefault("name", ""));
        holder.tvAddress.setText("📍 " + place.getOrDefault("address", ""));
        String rating = place.getOrDefault("rating", "");
        String total = place.getOrDefault("user_ratings_total", "0");
        holder.tvRating.setText(rating.isEmpty() ? "" : "⭐ " + rating + " (" + total + "개 리뷰)");
        holder.itemView.setOnClickListener(v -> listener.onClick(place));

    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress, tvRating;

        ViewHolder(View view) {
            super(view);
            tvName = view.findViewById(R.id.tvName);
            tvAddress = view.findViewById(R.id.tvAddress);
            tvRating = view.findViewById(R.id.tvRating);
        }
    }
}
