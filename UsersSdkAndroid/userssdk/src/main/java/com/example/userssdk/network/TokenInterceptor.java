package com.example.userssdk.network;

import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class TokenInterceptor implements Interceptor {

    public interface TokenProvider {
        String getToken();
    }

    private final TokenProvider provider;

    public TokenInterceptor(TokenProvider provider) {
        this.provider = provider;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        String url = original.url().toString();
        String token = provider.getToken();

        Log.d("TokenInterceptor", "📡 Request to: " + url);

        // אל תוסיף Authorization אם מדובר ב-login או register
        if (url.contains("/login") || url.contains("/register")) {
            Log.d("TokenInterceptor", "⛔ Skipping token for auth route");
            return chain.proceed(original);
        }

        if (token == null || token.isEmpty()) {
            Log.d("TokenInterceptor", "⚠️ No token available – sending request without Authorization");
            return chain.proceed(original);
        }

        Log.d("TokenInterceptor", "✅ Adding token to request");

        Request newReq = original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();

        return chain.proceed(newReq);
    }
}