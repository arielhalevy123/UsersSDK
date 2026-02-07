package com.example.userssdk.network;

import com.example.userssdk.model.*;
import retrofit2.Call;
import retrofit2.http.*;
import java.util.List;

public interface AuthApi {
    @POST("api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest body);
    @GET("api/auth/all")
    Call<List<UserDTO>> allUsers();
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest body);

    @GET("api/auth/me")
    Call<UserDTO> me();

    @GET("api/auth/my-users")
    Call<List<UserDTO>> myUsers();

    @PUT("api/auth/users/{id}")
    Call<UserDTO> updateUser(@Path("id") long id, @Body UserDTO body);
}
