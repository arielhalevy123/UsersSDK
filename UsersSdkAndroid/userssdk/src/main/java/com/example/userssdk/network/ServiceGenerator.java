package com.example.userssdk.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;


import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServiceGenerator {

    private static Retrofit retrofit;

    public static Retrofit create(String baseUrl, TokenInterceptor.TokenProvider provider) {
        if (retrofit == null) {
            synchronized (ServiceGenerator.class) {
                if (retrofit == null) {
                    // 1. צור את ה-logging interceptor
                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    logging.setLevel(HttpLoggingInterceptor.Level.BODY);

                    // 2. בנה את ה-OkHttpClient עם שני ה-interceptors
                    OkHttpClient client = new OkHttpClient.Builder()
                            .addInterceptor(logging)                    // <-- logging
                            .addInterceptor(new TokenInterceptor(provider)) // <-- token
                            .build();

                    // 3. צרף ל-Retrofit
                    Gson gson = new GsonBuilder().create();
                    retrofit = new Retrofit.Builder()
                            .baseUrl(baseUrl)
                            .addConverterFactory(GsonConverterFactory.create(gson))
                            .client(client)
                            .build();
                }
            }
        }
        return retrofit;
    }
}