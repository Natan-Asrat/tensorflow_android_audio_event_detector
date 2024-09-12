package com.emicasolutions.eventdetector;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.paypal.android.corepayments.CoreConfig;
import com.paypal.android.corepayments.Environment;
import com.paypal.android.corepayments.PayPalSDKError;
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient;
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutListener;
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutRequest;
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutResult;

import org.json.JSONObject;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

class RetrofitClient {

    private static final String BASE_URL = "https://api-m.paypal.com/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}

public class PaypalActivity extends AppCompatActivity {
    static final String RETURN_URL = "event-detector://paypal-redirect";
    static final String CANCEL_URL = "event-detector://paypal-cancel";
    private static final String PAYPAL_CLIENT_ID = "";
    private static final String PAYPAL_SECRET = "";
    private static final String PAYPAL_API_BASE_URL = "https://api-m.paypal.com/";
    private static final String LOG_TAG = "paypal";
    private PayPalWebCheckoutClient client;
    private PayPalApi payPalApi;

    private final OkHttpClient okHttpClient = new OkHttpClient();
    public void getAccessToken() {
        PayPalApi apiService = RetrofitClient.getClient().create(PayPalApi.class);

        // Basic Auth header
        String credentials = PAYPAL_CLIENT_ID + ":" + PAYPAL_SECRET ;
        String authHeader = "Basic " + java.util.Base64.getEncoder().encodeToString(credentials.getBytes());

        Call<AccessTokenResponse> call = apiService.getAccessToken(authHeader, "client_credentials");

        call.enqueue(new Callback<AccessTokenResponse>() {
            @Override
            public void onResponse(Call<AccessTokenResponse> call, Response<AccessTokenResponse> response) {
                if (response.isSuccessful()) {
                    AccessTokenResponse accessTokenResponse = response.body();
                    if (accessTokenResponse != null) {
                        String accessToken = accessTokenResponse.getAccessToken();
                        createOrder(accessToken);
                        Log.d("paypal", "Access Token: " + accessToken);
                    }
                } else {
                    Log.e("paypal", "Request failed: " + response.message());
                    Toast.makeText(PaypalActivity.this, "Failed to get access token", Toast.LENGTH_LONG).show();

                }
            }

            @Override
            public void onFailure(Call<AccessTokenResponse> call, Throwable t) {
                Log.e("paypal", "Request error: ", t);
                Toast.makeText(PaypalActivity.this, "Error getting access token", Toast.LENGTH_LONG).show();

            }
        });
    }
    //    public void makePostRequest() {
//        String credentials = "AVoOR2ygqH-D1wTg9q015orv6vWPvg50W9wQRle3DzO_MW2f-plYEhq5IHUpkCLUeJnhKhfN_bltr2is:EJsQr6u5tfsE-QonLyDR3KfYiF4SHzvuqXbL-ZDHGYWQlKF4AQ1XKWgMQvrirADg3NFbtvS3d-LLDnkT";
//        String base64Credentials = "Basic " + okhttp3.Credentials.basic("AVoOR2ygqH-D1wTg9q015orv6vWPvg50W9wQRle3DzO_MW2f-plYEhq5IHUpkCLUeJnhKhfN_bltr2is", "EJsQr6u5tfsE-QonLyDR3KfYiF4SHzvuqXbL-ZDHGYWQlKF4AQ1XKWgMQvrirADg3NFbtvS3d-LLDnkT");
//
//        RequestBody body = RequestBody.create("grant_type=client_credentials", MediaType.parse("application/x-www-form-urlencoded"));
//        Request request = new Request.Builder()
//                .url("https://api-m.sandbox.paypal.com/v1/oauth2/token")
//                .post(body)
//                .addHeader("Authorization", base64Credentials)
//                .addHeader("Content-Type", "application/x-www-form-urlencoded")
//                .build();
//
//        okHttpClient.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                e.printStackTrace();
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                if (!response.isSuccessful()) {
//                    throw new IOException("Unexpected code " + response);
//                }
//
//                // Process the response
//                String responseBody = response.body().string();
//
//                // Parse the JSON response
//                try {
//                    JSONObject jsonResponse = new JSONObject(responseBody);
//                    String accessToken = jsonResponse.getString("access_token");
//
//                    // Use the access token as needed
//                    System.out.println("Access Token: " + accessToken);
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "onCreate: Starting PayPal Activity");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_paypal);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(PAYPAL_API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        payPalApi = retrofit.create(PayPalApi.class);

        Log.d(LOG_TAG, "onCreate: Calling getAccessToken");
        getAccessToken();
    }

//    private void getAccessToken() {
//        String authHeader = Credentials.basic(PAYPAL_CLIENT_ID, PAYPAL_SECRET);
//        Call<AccessTokenResponse> call = payPalApi.getAccessToken(authHeader, "client_credentials");
//
//        call.enqueue(new Callback<AccessTokenResponse>() {
//            @Override
//            public void onResponse(Call<AccessTokenResponse> call, Response<AccessTokenResponse> response) {
//                if (response.isSuccessful()) {
//                    String accessToken = response.body().access_token;
//                    Log.d(LOG_TAG, "onResponse: Access Token received");
//                    createOrder(accessToken);
//                } else {
//                    Log.e(LOG_TAG, "onResponse: Failed to get access token. Response code: " + response.code());
//                    try {
//                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
//                        Log.e(LOG_TAG, "Error Body: " + errorBody);
//                    } catch (IOException e) {
//                        Log.e(LOG_TAG, "Error reading error body", e);
//                    }
//                }
//            }
//
//            @Override
//            public void onFailure(Call<AccessTokenResponse> call, Throwable t) {
//                Log.e(LOG_TAG, "onFailure: Error getting access token", t);
//            }
//        });
//    }


    private void createOrder(String accessToken) {
        OrderRequest orderRequest = new OrderRequest("0.00");  // Set the amount

        Call<CreateOrderResponse> call = payPalApi.createOrder("Bearer " + accessToken, orderRequest);

        call.enqueue(new Callback<CreateOrderResponse>() {
            @Override
            public void onResponse(Call<CreateOrderResponse> call, Response<CreateOrderResponse> response) {
                if (response.isSuccessful()) {
                    String orderId = response.body().id;
                    Log.d(LOG_TAG, "onResponse: Order created successfully. Order ID: " + orderId);
                    startPayPalCheckout(orderId);
                } else {
                    Log.e(LOG_TAG, "onResponse: Failed to create order. Response code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<CreateOrderResponse> call, Throwable t) {
                Log.e(LOG_TAG, "onFailure: Error creating order", t);
                Toast.makeText(PaypalActivity.this, "Error creating order", Toast.LENGTH_LONG).show();

            }
        });
    }

    private void startPayPalCheckout(String orderId) {
        CoreConfig config = new CoreConfig(PAYPAL_CLIENT_ID, Environment.LIVE);
        client = new PayPalWebCheckoutClient(this, config, RETURN_URL);

        PayPalWebCheckoutListener listener = new PayPalWebCheckoutListener() {
            @Override
            public void onPayPalWebSuccess(@NonNull PayPalWebCheckoutResult result) {
                Log.d(LOG_TAG, "onPayPalWebSuccess: Payment successful");
                getSharedPreferences(ProductVersion.PREF_KEY_ACTIVATED_BY_PAYPAL, MODE_PRIVATE)
                        .edit()
                        .putBoolean("isPremium", true)
                        .apply();
                Toast.makeText(PaypalActivity.this, "Payment successful", Toast.LENGTH_LONG).show();

                Intent i = new Intent(PaypalActivity.this, SettingsActivity.class);
                startActivity(i);
            }

            @Override
            public void onPayPalWebFailure(@NonNull PayPalSDKError error) {
                Log.e(LOG_TAG, "onPayPalWebFailure: Payment failed", error);
                Toast.makeText(PaypalActivity.this, "Payment failed: " + error.getMessage(), Toast.LENGTH_LONG).show();

            }

            @Override
            public void onPayPalWebCanceled() {
                Log.d(LOG_TAG, "onPayPalWebCanceled: Payment canceled");
                Toast.makeText(PaypalActivity.this, "Payment canceled", Toast.LENGTH_LONG).show();

            }
        };

        client.setListener(listener);
        PayPalWebCheckoutRequest paymentRequest = new PayPalWebCheckoutRequest(orderId);
        client.start(paymentRequest);
        Log.d(LOG_TAG, "startPayPalCheckout: Checkout started with Order ID: " + orderId);
    }

    @Override
    protected void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);  // Call the superclass implementation with the new intent
        setIntent(newIntent);          // Update the activity's intent with the new intent
        Log.d(LOG_TAG, "onNewIntent: New intent received");
        client.handleBrowserSwitchResult$PayPalWebPayments_release();
    }
}


interface PayPalApi {
    @FormUrlEncoded
    @POST("v1/oauth2/token")
    Call<AccessTokenResponse> getAccessToken(
            @Header("Authorization") String authHeader,
            @Field("grant_type") String grantType
    );

    @POST("v2/checkout/orders")
    @Headers("Content-Type: application/json")
    Call<CreateOrderResponse> createOrder(
            @Header("Authorization") String authHeader,
            @Body OrderRequest orderRequest
    );
}

class AccessTokenResponse {
    private String scope;
    private String access_token;
    private String token_type;
    private String app_id;
    private int expires_in;
    private String nonce;

    // Getters and Setters
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getAccessToken() { return access_token; }
    public void setAccessToken(String access_token) { this.access_token = access_token; }

    public String getTokenType() { return token_type; }
    public void setTokenType(String token_type) { this.token_type = token_type; }

    public String getAppId() { return app_id; }
    public void setAppId(String app_id) { this.app_id = app_id; }

    public int getExpiresIn() { return expires_in; }
    public void setExpiresIn(int expires_in) { this.expires_in = expires_in; }

    public String getNonce() { return nonce; }
    public void setNonce(String nonce) { this.nonce = nonce; }
}


class CreateOrderResponse {
    String id; // PayPal order ID
    // Other fields can be added as needed
}

class OrderRequest {
    public String intent = "CAPTURE";
    public PurchaseUnit[] purchase_units;
    public AppContext application_context;

    public OrderRequest(String amount) {
        PurchaseUnit purchaseUnit = new PurchaseUnit();
        purchaseUnit.amount = new Amount();
        purchaseUnit.amount.currency_code = "USD";
        purchaseUnit.amount.value = amount;
        this.application_context = new AppContext(PaypalActivity.RETURN_URL, PaypalActivity.CANCEL_URL);

        this.purchase_units = new PurchaseUnit[] {purchaseUnit};
    }
}

class AppContext {
    public String return_url;
    public String cancel_url;

    public String getReturn_url() {
        return return_url;
    }

    public String getCancel_url() {
        return cancel_url;
    }

    public AppContext(String return_url, String cancel_url) {
        this.return_url = return_url;
        this.cancel_url = cancel_url;
    }
}
class PurchaseUnit {
    public Amount amount;
}

class Amount {
    public String currency_code;
    public String value;
}
