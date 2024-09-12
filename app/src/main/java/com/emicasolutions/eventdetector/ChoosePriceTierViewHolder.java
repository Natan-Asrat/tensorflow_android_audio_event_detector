package com.emicasolutions.eventdetector;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ChoosePriceTierViewHolder extends RecyclerView.ViewHolder {
    public ChoosePriceTierViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public void bind(String data) {
        TextView textView = itemView.findViewById(R.id.price_name);
        textView.setText(data);
        itemView.setOnClickListener(view -> {
            Intent intent = new Intent(itemView.getContext(), ActivateActivity.class);
            intent.putExtra("type", data);
            itemView.getContext().startActivity(intent);
        });
    }
}
