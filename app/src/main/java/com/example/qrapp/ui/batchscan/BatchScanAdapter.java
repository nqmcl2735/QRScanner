package com.example.qrapp.ui.batchscan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.qrapp.R;
import com.example.qrapp.data.model.QRHistoryItem;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BatchScanAdapter extends RecyclerView.Adapter<BatchScanAdapter.ViewHolder> {

    private final List<QRHistoryItem> entries = new ArrayList<>();
    private final OnItemClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    public interface OnItemClickListener {
        void onItemClick(QRHistoryItem entry);
    }

    public BatchScanAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void addEntry(QRHistoryItem entry) {
        entries.add(0, entry);
        notifyItemInserted(0);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_batch_scan_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QRHistoryItem entry = entries.get(position);
        holder.bind(entry);
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textPreview;
        private final TextView textTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textPreview = itemView.findViewById(R.id.text_preview);
            textTime = itemView.findViewById(R.id.text_time);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(entries.get(position));
                }
            });
        }

        void bind(QRHistoryItem entry) {
            textPreview.setText(entry.getContent());
            String timeStr = dateFormat.format(new Date(entry.getTimestamp()));
            textTime.setText(timeStr);
        }
    }
}
