package com.example.userssdk.datasource;

import com.example.userssdk.model.*;
import com.example.userssdk.network.AuthApi;
import retrofit2.Call;

import java.util.List;

public class AuthRemoteDataSource {

    private final AuthApi api;

    public AuthRemoteDataSource(AuthApi api) {
        this.api = api;
    }

    public Call<AuthResponse> register(RegisterRequest req) { return api.register(req); }
    public Call<AuthResponse> login(LoginRequest req)       { return api.login(req); }
    public Call<UserDTO> me()                               { return api.me(); }
    public Call<List<UserDTO>> myUsers()                    { return api.myUsers(); }
    public Call<UserDTO> updateUser(UserDTO u)              { return api.updateUser(u.getId(), u); }
}
