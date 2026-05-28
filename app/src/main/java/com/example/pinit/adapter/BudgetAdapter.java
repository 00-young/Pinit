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
import com.example.pinit.model.Budget;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.ViewHolder> {

    public interface OnDeleteListener { void onDelete(int id); }

    private Context context;
    private List<Budget> list;
    private OnDeleteListener listener;

    public BudgetAdapter(Context context, List<Budget> list, OnDeleteListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_budget, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Budget b = list.get(position);
        NumberFormat fmt = NumberFormat.getNumberInstance(Locale.KOREA);
        holder.tvTitle.setText(b.getTitle());
        holder.tvCategory.setText(b.getCategory() + " · " + b.getDate());
        if ("income".equals(b.getType())) {
            holder.tvAmount.setText("+" + fmt.format(b.getAmount()) + "원");
            holder.tvAmount.setTextColor(Color.parseColor("#4CAF50"));
            holder.tvIcon.setText("💰");
        } else {
            holder.tvAmount.setText("-" + fmt.format(b.getAmount()) + "원");
            holder.tvAmount.setTextColor(Color.parseColor("#F44336"));
            holder.tvIcon.setText(getCategoryEmoji(b.getCategory()));
        }
        holder.itemView.setOnLongClickListener(v -> { listener.onDelete(b.getId()); return true; });
    }

    private String getCategoryEmoji(String cat) {
        if (cat == null) return "💸";
        switch (cat) {
            case "식비": return "🍽️";
            case "교통": return "🚌";
            case "숙박": return "🏨";
            case "쇼핑": return "🛍️";
            case "관광": return "🏛️";
            default: return "💸";
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvTitle, tvCategory, tvAmount;
        ViewHolder(View view) {
            super(view);
            tvIcon = view.findViewById(R.id.tvIcon);
            tvTitle = view.findViewById(R.id.tvTitle);
            tvCategory = view.findViewById(R.id.tvCategory);
            tvAmount = view.findViewById(R.id.tvAmount);
        }
    }
}
