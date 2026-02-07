package com.example.userssdk.repository;

import com.example.userssdk.datasource.*;
import com.example.userssdk.model.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

public class UserRepository {

    private final AuthRemoteDataSource remote;
    private final AuthLocalDataSource local;

    public UserRepository(AuthRemoteDataSource remote, AuthLocalDataSource local) {
        this.remote = remote;
        this.local = local;
    }

    public interface ResultCallback<T> {
        void onSuccess(T data);
        void onError(Throwable t);
    }

    public void register(RegisterRequest req, ResultCallback<AuthResponse> cb) {
        remote.register(req).enqueue(new Callback<AuthResponse>() {
            @Override public void onResponse(Call<AuthResponse> call, Response<AuthResponse> resp) {
                if (resp.isSuccessful() && resp.body() != null) {
                    local.saveToken(resp.body().getToken());
                    cb.onSuccess(resp.body());
                } else {
                    cb.onError(new Exception("Register failed: " + resp.code()));
                }
            }
            @Override public void onFailure(Call<AuthResponse> call, Throwable t) {
                cb.onError(t);
            }
        });
    }

    public void login(LoginRequest req, ResultCallback<AuthResponse> cb) {
        remote.login(req).enqueue(new Callback<AuthResponse>() {
            @Override public void onResponse(Call<AuthResponse> call, Response<AuthResponse> resp) {
                if (resp.isSuccessful() && resp.body() != null) {
                    local.saveToken(resp.body().getToken());
                    cb.onSuccess(resp.body());
                } else {
                    cb.onError(new Exception("Login failed: " + resp.code()));
                }
            }
            @Override public void onFailure(Call<AuthResponse> call, Throwable t) {
                cb.onError(t);
            }
        });
    }

    public void currentUser(ResultCallback<UserDTO> cb) {
        remote.me().enqueue(new StdCallback<>(cb));
    }

    public void myUsers(ResultCallback<List<UserDTO>> cb) {
        remote.myUsers().enqueue(new StdCallback<>(cb));
    }

    public void updateUser(UserDTO u, ResultCallback<UserDTO> cb) {
        remote.updateUser(u).enqueue(new StdCallback<>(cb));
    }

    public String getToken() { return local.getToken(); }

    // Helper callback class
    private static class StdCallback<T> implements Callback<T> {
        private final ResultCallback<T> cb;
        StdCallback(ResultCallback<T> cb) { this.cb = cb; }
        @Override public void onResponse(Call<T> call, Response<T> resp) {
            if (resp.isSuccessful() && resp.body() != null) {
                cb.onSuccess(resp.body());
            } else {
                cb.onError(new Exception("Request failed: " + resp.code()));
            }
        }
        @Override public void onFailure(Call<T> call, Throwable t) { cb.onError(t); }
    }
    public void clearToken() {
        local.clearToken();
    }
}
