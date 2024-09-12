
package com.emicasolutions.eventdetector;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.billingclient.api.ProductDetails;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ChooseSubscriptionAdapter extends RecyclerView.Adapter<ChooseSubscriptionViewholder> {


    @NonNull
    @Override
    public ChooseSubscriptionViewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.price_item, parent, false);
        return new ChooseSubscriptionViewholder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChooseSubscriptionViewholder holder, int position) {
        if (SettingsActivity.version.productDetails != null && !SettingsActivity.version.productDetails.isEmpty()) {
            ProductDetails productDetails = SettingsActivity.version.productDetails.get(0);
            ProductDetails.SubscriptionOfferDetails offers = getSortedOffersByPrice(productDetails.getSubscriptionOfferDetails()).get(position) ;
            holder.bind(productDetails, offers, SettingsActivity.version.billingClient);
        } else {
            // Handle case where product list is not available
            Toast.makeText(holder.itemView.getContext(), "Product Details not loaded", Toast.LENGTH_SHORT).show();
        }
    }
    public static List<ProductDetails.SubscriptionOfferDetails> getSortedOffersByPrice(List<ProductDetails.SubscriptionOfferDetails> offers) {
        // Sort the offers by price in ascending order
        Collections.sort(offers, new Comparator<ProductDetails.SubscriptionOfferDetails>() {
            @Override
            public int compare(ProductDetails.SubscriptionOfferDetails offer1, ProductDetails.SubscriptionOfferDetails offer2) {
                // Convert price from micros to dollars (or your currency) and compare
                double price1 = convertMicrosToDollars(offer1.getPricingPhases().getPricingPhaseList().get(0).getPriceAmountMicros());
                double price2 = convertMicrosToDollars(offer2.getPricingPhases().getPricingPhaseList().get(0).getPriceAmountMicros());
                return Double.compare(price1, price2);
            }
        });
        return offers;
    }

    private static double convertMicrosToDollars(long micros) {
        return micros / 1_000_000.0;
    }

    @Override
    public int getItemCount() {
        return SettingsActivity.version.productDetails != null && !SettingsActivity.version.productDetails.isEmpty() ? SettingsActivity.version.productDetails.get(0).getSubscriptionOfferDetails().size() : 0;
    }
}

