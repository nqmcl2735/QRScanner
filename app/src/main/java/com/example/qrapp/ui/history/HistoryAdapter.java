package com.example.qrapp.ui.history;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.qrapp.R;
import com.example.qrapp.data.model.HistorySource;
import com.example.qrapp.data.model.QRHistoryItem;
import com.example.qrapp.util.QRTypeStyle;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    public interface Listener {
        void onItemClick(QRHistoryItem item);
        void onCopyText(QRHistoryItem item);
        void onCopyImage(QRHistoryItem item);
        void onDelete(QRHistoryItem item);
    }

    private final List<QRHistoryItem> items = new ArrayList<>();
    private final Listener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public HistoryAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<QRHistoryItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QRHistoryItem item = items.get(position);
        Context context = holder.itemView.getContext();
        holder.content.setText(item.getContent());
        String sourceLabel = item.getSource() == HistorySource.GENERATED
                ? context.getString(R.string.history_source_generated)
                : context.getString(R.string.history_source_scanned);
        holder.time.setText(dateFormat.format(item.getTimestamp()) + " • " + sourceLabel);

        int color = QRTypeStyle.color(context, item.getType());
        int softColor = QRTypeStyle.softColor(context, item.getType());
        holder.typeIconBg.setBackgroundTintList(ColorStateList.valueOf(softColor));
        holder.typeIcon.setImageResource(QRTypeStyle.iconRes(item.getType()));
        holder.typeIcon.setImageTintList(ColorStateList.valueOf(color));
        holder.typeChip.setText(QRTypeStyle.labelRes(item.getType()));
        holder.typeChip.setBackgroundTintList(ColorStateList.valueOf(softColor));
        holder.typeChip.setTextColor(color);

        holder.itemView.setOnClickListener(view -> listener.onItemClick(item));
        holder.btnCopyText.setOnClickListener(view -> listener.onCopyText(item));
        holder.btnCopyImage.setOnClickListener(view -> listener.onCopyImage(item));
        holder.btnDelete.setOnClickListener(view -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView content;
        final TextView time;
        final TextView typeChip;
        final FrameLayout typeIconBg;
        final ImageView typeIcon;
        final View btnCopyText;
        final View btnCopyImage;
        final View btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            content = itemView.findViewById(R.id.text_content);
            time = itemView.findViewById(R.id.text_time);
            typeChip = itemView.findViewById(R.id.text_type_chip);
            typeIconBg = itemView.findViewById(R.id.image_type_icon_bg);
            typeIcon = itemView.findViewById(R.id.image_type_icon);
            btnCopyText = itemView.findViewById(R.id.btn_copy_text);
            btnCopyImage = itemView.findViewById(R.id.btn_copy_image);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
