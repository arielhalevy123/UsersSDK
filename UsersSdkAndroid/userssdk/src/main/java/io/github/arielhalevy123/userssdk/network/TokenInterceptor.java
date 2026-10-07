package io.github.arielhalevy123.userssdk.network;

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

        // אל תוסיף Authorization אם מדובר ב-login או register
        if (url.contains("/login") || url.contains("/register")) {
            return chain.proceed(original);
        }

        if (token == null || token.isEmpty()) {
            return chain.proceed(original);
        }

        Request newReq = original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();

        return chain.proceed(newReq);
    }
}