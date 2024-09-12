package com.emicasolutions.eventdetector;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.common.collect.ImmutableList;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class ProductVersion {

    private static final String PREF_KEY_HASH = "productVersionHash";
    private static final String PREF_KEY_DATA = "productVersionData";
    private static final String PREF_KEY_EXPIRATION = "productVersionExpiration";
    private static final int LICENSE_DURATION_DAYS_YEAR = 365; // License duration in days (1 year)
    private static final String TAG = "ProductVersion";
    private static final String PREF_KEY_ACTIVATED_BY_GOOGLE_PLAY = "activatedByGooglePlay";
    public static final String PREF_KEY_ACTIVATED_BY_PAYPAL = "activatedByPaypal";
    private static final int LICENSE_DURATION_DAYS_MONTH = 30; // License duration in days (1 month)
    static final String PAYPAL_CLIENT_ID = "";
    private static final int LICENSE_DURATION_DAYS_3_MONTH = 90; // License duration in days (3 months)
    private EmptyCallback productLoadedCallback;
    private Context context;
    private SharedPreferences prefs;
    BillingClient billingClient;
    List<ProductDetails> productDetails;
    private EmptyCallback showLoadingCallback;
    private boolean hasClickedToGoToSubsctiptions;
    public GooglePlayOperations googlePlayOps;
    public ProductVersion(Context context, EmptyCallback productsLoadedCallback, EmptyCallback showLoadingCallback){
        this(context);
        this.productLoadedCallback = productsLoadedCallback;
        this.showLoadingCallback = showLoadingCallback;
    }

    public ProductVersion(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences("ProductPrefs", Context.MODE_PRIVATE);
        this.googlePlayOps = new GooglePlayOperations();

        googlePlayOps.initializeBillingClient();

    }

    public boolean isTrialVersion() {
        try {
            boolean activatedByGooglePlay = prefs.getBoolean(PREF_KEY_ACTIVATED_BY_GOOGLE_PLAY, false);
            Log.d(TAG, "activate pref is : "+ activatedByGooglePlay);
            if (activatedByGooglePlay) {
                // If activated by Google Play, no need to verify other details
                return false;

            }
            boolean activatedByPaypal = prefs.getBoolean(PREF_KEY_ACTIVATED_BY_PAYPAL, false);

            if (activatedByPaypal) {
                // If activated by Google Play, no need to verify other details
                return false;

            }
            String uniqueId = getDeviceId();
            String paidString = "paid:true";
            String storedHash = getFromPreferences(PREF_KEY_HASH);
            String expirationDateStr = getFromPreferences(PREF_KEY_EXPIRATION);

            if (storedHash == null || expirationDateStr == null) {
                // No data found, assume trial version
                return true;
            }

            // Check if the hash is correct
            if (!verifyHash(paidString, uniqueId, storedHash)) {
                return true;
            }

            // Check if the license has expired
            long expirationDate = Long.parseLong(expirationDateStr);
            if (new Date().getTime() > expirationDate) {
                return true; // License has expired
            }

            return false; // License is valid and not expired
        } catch (Exception e) {
            e.printStackTrace();
            return true; // In case of error, assume trial version
        }
    }
    public class GooglePlayOperations  implements PurchasesUpdatedListener{
        private void initializeBillingClient() {
            Log.d(TAG, "Initializing Billing Client...");

            billingClient = BillingClient.newBuilder(context)
                    .setListener(this)  // Set the PurchasesUpdatedListener
                    .enablePendingPurchases(PendingPurchasesParams.newBuilder()
                            .enableOneTimeProducts()  // Enable support for one-time products
                            .build())
                    .build();

            // Connect to the Google Play Billing service
            billingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(BillingResult billingResult) {
                    Log.d(TAG, "Billing setup finished with response code: " + billingResult.getResponseCode());

                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        // Billing client is ready. You can query purchases here.
                        Log.d(TAG, "Billing client is ready, querying product details and purchases.");

                        queryProductDetails();
                        queryPurchases(null);
                    } else {
                        Log.e(TAG, "Error connecting to Google Play Billing: " + billingResult.getDebugMessage());

                        Toast.makeText(context, "Error connecting to Google Play Billing... Did you add a payment method to your Play store Google Account?", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onBillingServiceDisconnected() {
                    // Try reconnecting if the service is disconnected
                    Log.w(TAG, "Billing service disconnected, trying to reconnect.");

                    initializeBillingClient();
                }
            });
        }

        public void queryPurchases(EmptyCallback popup) {
            if (billingClient != null && billingClient.isReady()) {
                // BillingClient is already connected, proceed with query
                Log.d(TAG, "Querying purchases...");

                billingClient.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder()
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build(),
                        (billingResult, purchases) -> {
                            Log.d("queryPurchases", "Billing Result: Code = " + billingResult.getResponseCode()
                                    + ", Message = " + billingResult.getDebugMessage());
                            // check billingResult
                            // process returned purchase list, e.g. display the plans user owns
                            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                                if (!purchases.isEmpty()) {
                                    handlePurchases(purchases);
                                } else {
                                    // No subscription purchases found, update preferences and show activation screen
                                    prefs.edit().putBoolean("activatedByGooglePlay", false).apply();
                                    if(popup!=null){popup.execute();}
                                }
                            }else {
                                Toast.makeText(context, "There was error while processing your purchases!", Toast.LENGTH_SHORT).show();
                                String errorMessage = "Error while processing purchases. Response Code: "
                                        + billingResult.getResponseCode() + ", Message: " + billingResult.getDebugMessage();
                                Log.e("queryPurchases", errorMessage);
                            }
                        }
                );            }

        }
        private void queryProductDetails() {
            Log.d(TAG, "Querying product details...");

            QueryProductDetailsParams queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
                    .setProductList(
                            ImmutableList.of(
//                                    QueryProductDetailsParams.Product.newBuilder()
//                                            .setProductId("subscription")
//                                            .setProductType(BillingClient.ProductType.SUBS)
//                                            .build()
                                    QueryProductDetailsParams.Product.newBuilder()
                                            .setProductId(ChoosePriceTierAdapter.SUBSCRIPTION_PRODUCT_ID)
                                            .setProductType(BillingClient.ProductType.SUBS)
                                            .build()
                            ))
                    .build();

            billingClient.queryProductDetailsAsync(queryProductDetailsParams, new ProductDetailsResponseListener() {
                @Override
                public void onProductDetailsResponse(BillingResult billingResult, List<ProductDetails> productDetailsList) {
                    Log.d(TAG, "Product details response received with response code: " + billingResult.getResponseCode());

                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        // Process the returned productDetailsList
                        Log.d(TAG, "Product details loaded successfully.");

                        productDetails = productDetailsList;
                        if(productDetails==null){
                            Toast.makeText(context, "Couldn't find products on Google play!", Toast.LENGTH_SHORT).show();
                            
                        }else{
                            Log.d(TAG, "Loaded Products");
                            for (ProductDetails product: productDetails){
                                Log.d(TAG, "Loaded product: " + product.getName());

                            }
                        }
                        if(productLoadedCallback!=null && ProductVersion.this.hasClickedToGoToSubsctiptions){
                            productLoadedCallback.execute();
                        }
                    } else {
                        Log.e(TAG, "Error querying product details: " + billingResult.getDebugMessage());

                        Toast.makeText(context, "Error querying product details", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        @Override
        public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
            Log.d(TAG, "Purchases updated with response code: " + billingResult.getResponseCode());

            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                // Handle the purchase here (e.g., save purchase data)
                Log.d(TAG, "Handling purchase: " + purchases.toString());

                handlePurchases(purchases);
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                // Handle the user cancelling the purchase flow
                Log.w(TAG, "User canceled the purchase.");

                Toast.makeText(context, "Purchase canceled", Toast.LENGTH_SHORT).show();
            } else {
                // Handle any other error codes
                Log.e(TAG, "Error during purchase: " + billingResult.getDebugMessage());

                Toast.makeText(context, "Error during purchase", Toast.LENGTH_SHORT).show();
            }
        }

        private void handlePurchases(List<Purchase> purchases) {

            for (Purchase purchase : purchases) {
                // Check if the purchase is valid
                Log.d(TAG, "Handling purchase with product IDs: " + purchase.getProducts());

                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged()) {
                    // Get the list of product IDs (SKUs) associated with the purchase

                    List<String> productIds = purchase.getProducts();


                    // Ensure there is at least one product ID
                    if (productIds != null && !productIds.isEmpty()) {
                        // Assume only one product per purchase
                        for(String productId: productIds){
                            if(productId.equals(ChoosePriceTierAdapter.SUBSCRIPTION_PRODUCT_ID)){
                                prefs.edit().putBoolean("activatedByGooglePlay", true).apply();

                            }
                        }

                        // Acknowledge the purchase if it has not been acknowledged
                        if (!purchase.isAcknowledged()) {
                            AcknowledgePurchaseParams acknowledgePurchaseParams =
                                    AcknowledgePurchaseParams.newBuilder()
                                            .setPurchaseToken(purchase.getPurchaseToken())
                                            .build();

                            billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
                                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                    // Purchase acknowledged
                                    // Optionally, you can add code here to handle successful acknowledgment
                                    Toast.makeText(context, "Purchase was successful!", Toast.LENGTH_SHORT).show();
                                } else {
                                    // Handle error during acknowledgment
                                    Toast.makeText(context, "Failed to acknowledge purchase", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        // Handle case where product IDs are missing
                        Toast.makeText(context, "No products found in purchase", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Handle case where purchase state is not PURCHASED
                    Toast.makeText(context, "Purchase state is not valid", Toast.LENGTH_SHORT).show();
                }
            }
        }
        public void gotoPlay(){
            Log.d(TAG, "User clicked to go to subscriptions.");

            hasClickedToGoToSubsctiptions = true;
            if(productDetails !=null){
                Log.d(TAG, "Product details already loaded, executing callback.");

                productLoadedCallback.execute();
            }else{
                Log.d(TAG, "Showing loading screen, waiting for product details.");

                showLoadingCallback.execute();
            }
        }
    }


    public void setProductVersion(boolean isPaid, String type) {
        try {
            String uniqueId = getDeviceId();
            String data = "paid:" + isPaid;
            String hash = generateHash(data, uniqueId);

            saveToPreferences(PREF_KEY_DATA, data);
            saveToPreferences(PREF_KEY_HASH, hash);
            long expirationDate = 0 ;
            // Set the expiration date to 1 year (365 days) from now
            if(type.equals(ChoosePriceTierAdapter.MONTHLY_CODE)){
                expirationDate  = getAMonthFromNow();
            } else if(type.equals(ChoosePriceTierAdapter.THREE_MONTHS_CODE)){
                expirationDate  = get3MonthsFromNow();

            } else if(type.equals(ChoosePriceTierAdapter.YEARLY_CODE)){
                expirationDate  = getOneYearFromNow();

            }

            saveToPreferences(PREF_KEY_EXPIRATION, String.valueOf(expirationDate));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getDeviceId() {
        // Generates a unique device ID based on the Android ID
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private String generateHash(String data, String uniqueId) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String input = data + uniqueId + MainActivity.SECRET_KEY;
        byte[] hashBytes = digest.digest(input.getBytes());
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP);
    }

    private boolean verifyHash(String data, String uniqueId, String expectedHash) throws NoSuchAlgorithmException {
        String calculatedHash = generateHash(data, uniqueId);
        return calculatedHash.equals(expectedHash);
    }

    private void saveToPreferences(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    private String getFromPreferences(String key) {
        return prefs.getString(key, null);
    }

    private long getAMonthFromNow() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, LICENSE_DURATION_DAYS_MONTH);
        return calendar.getTimeInMillis();
    }
    private long get3MonthsFromNow() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, LICENSE_DURATION_DAYS_3_MONTH);
        return calendar.getTimeInMillis();
    }
    private long getOneYearFromNow() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, LICENSE_DURATION_DAYS_YEAR);
        return calendar.getTimeInMillis();
    }
}
