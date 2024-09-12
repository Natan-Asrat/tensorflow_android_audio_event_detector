package com.emicasolutions.eventdetector;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ChoosePriceTierAdapter extends RecyclerView.Adapter<ChoosePriceTierViewHolder> {
    static String SUBSCRIPTION_PRODUCT_ID = "subscription";
    static String MONTHLY = "Monthly (Br 500)";
    static String THREE_MONTHS = "3 Months (Br 1000)";
    static String YEARLY = "Yearly (Br 3000)";
    static String MONTHLY_CODE = "A";
    static String THREE_MONTHS_CODE = "B";
    static String YEARLY_CODE = "C";

    static String PLAY_MONTHLY_CODE = "monthly_location_collector";
    static String PLAY_THREE_MONTHS_CODE = "three-months";
    static String PLAY_YEARLY_CODE = "yearly";
    String[] data = new String[]{
            MONTHLY,
            THREE_MONTHS,
            YEARLY
    };

    @NonNull
    @Override
    public ChoosePriceTierViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.price_item, parent, false);
        ChoosePriceTierViewHolder holder = new ChoosePriceTierViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ChoosePriceTierViewHolder holder, int position) {
        holder.bind(data[position]);
    }

    @Override
    public int getItemCount() {
        return data.length;
    }
}
