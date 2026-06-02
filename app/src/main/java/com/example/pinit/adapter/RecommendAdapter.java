package com.example.pinit.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;
import com.example.pinit.model.RecommendedPlace;

import java.util.List;

public class RecommendAdapter extends RecyclerView.Adapter<RecommendAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(RecommendedPlace place);
    }

    private final List<RecommendedPlace> items;
    private final OnItemClickListener listener;

    public RecommendAdapter(List<RecommendedPlace> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recommend_place, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecommendedPlace place = items.get(position);
        holder.tvName.setText(place.getName() != null ? place.getName() : "");
        holder.tvAddress.setText(place.getAddress() != null ? place.getAddress() : "");
        holder.tvRating.setText(place.getRating() > 0
                ? String.format("%.1f", place.getRating()) : "-");
        holder.tvScore.setText("추천점수 " + (int) place.getScore());
        holder.itemView.setOnClickListener(v -> listener.onClick(place));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress, tvRating, tvScore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvRecommendName);
            tvAddress = itemView.findViewById(R.id.tvRecommendAddress);
            tvRating = itemView.findViewById(R.id.tvRecommendRating);
            tvScore = itemView.findViewById(R.id.tvRecommendScore);
        }
    }
}
