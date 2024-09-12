package com.emicasolutions.eventdetector;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.google.common.collect.ImmutableList;

import java.time.Period;


public class ChooseSubscriptionViewholder extends RecyclerView.ViewHolder {
    private TextView textView;

    public ChooseSubscriptionViewholder(@NonNull View itemView) {
        super(itemView);
        textView = itemView.findViewById(R.id.price_name);
    }


    public void bind(ProductDetails productDetails, ProductDetails.SubscriptionOfferDetails offer, BillingClient billingClient) {
        TextView textView = itemView.findViewById(R.id.price_name);
        String basePlanPrice = offer.getPricingPhases().getPricingPhaseList().get(0).getFormattedPrice();
        String basePlanDuration = offer.getPricingPhases().getPricingPhaseList().get(0).getBillingPeriod();
        String name = offer.getOfferTags().get(0); // dont forget to make the first name of
        // Display the base plan (for example, using a dialog or UI list)
        String days = String.valueOf(Period.parse(basePlanDuration).getDays());
        String display =  name + " for " + basePlanPrice;
        textView.setText(display);


        itemView.setOnClickListener(view -> {
            // Launch purchase flow
            launchPurchaseFlow(productDetails,offer, billingClient);
        });
    }

    private void launchPurchaseFlow(ProductDetails productDetails, ProductDetails.SubscriptionOfferDetails selectedOffer, BillingClient billingClient) {
        if (billingClient != null) {
            // Use ProductDetails directly
            BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                            ImmutableList.of(
                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                            .setProductDetails(productDetails)
                                            .setOfferToken(selectedOffer.getOfferToken())
                                            .build()
                            ))

                    .build();

            // Launch the billing flow
            BillingResult billingResult = billingClient.launchBillingFlow(
                    (Activity) itemView.getContext(), billingFlowParams);

            // Check the result
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                // Handle error
                Toast.makeText(itemView.getContext(), "Error initiating purchase flow: " + billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            // Handle case where BillingClient is null
            Toast.makeText(itemView.getContext(), "BillingClient is not initialized", Toast.LENGTH_SHORT).show();
        }
    }

}
