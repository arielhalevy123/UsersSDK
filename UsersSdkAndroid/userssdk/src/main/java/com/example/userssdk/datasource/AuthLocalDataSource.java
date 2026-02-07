package com.example.userssdk.datasource;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class AuthLocalDataSource {

    private static final String PREF_NAME = "users_sdk_pref";
    private static final String KEY_TOKEN = "jwt_token";

    private final SharedPreferences prefs;

    public AuthLocalDataSource(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    public void clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply();
    }

    public void saveToken(String token) {
        Log.d("AuthLocalDS", "Saving token → " + token);
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }
    public String getToken() {
        String t = prefs.getString(KEY_TOKEN, null);
        Log.d("AuthLocalDS", "Retrieving token → " + t);
        return t;
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}