package com.wanderwildwood.ibasho.net

import com.wanderwildwood.ibasho.net.model.DataRequestResponse
import com.wanderwildwood.ibasho.net.model.LoginRequest
import com.wanderwildwood.ibasho.net.model.LoginResponse
import com.wanderwildwood.ibasho.net.model.MessagesResponse
import com.wanderwildwood.ibasho.net.model.PasswordRequest
import com.wanderwildwood.ibasho.net.model.PasswordResponse
import com.wanderwildwood.ibasho.net.model.PushUrlRequestResponse
import com.wanderwildwood.ibasho.net.model.RegisterRequest
import com.wanderwildwood.ibasho.net.model.RegisterResponse
import com.wanderwildwood.ibasho.net.model.SaltResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FmdServerApiV2Service {

    /* ----- Account management ----- */

    @GET("account/{name}/salt")
    fun getSalt(@Path("name") username: String): Call<SaltResponse>

    @POST("account/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("account/register")
    fun register(@Body request: RegisterRequest): Call<RegisterResponse>

    @POST("account/logout")
    fun logout(): Call<Unit>

    @DELETE("account")
    fun deleteAccount(): Call<Unit>

    /* ----- Account settings ----- */

    @GET("account/push_url")
    fun getPushUrl(): Call<PushUrlRequestResponse>

    @POST("account/push_url")
    fun postPushUrl(@Body request: PushUrlRequestResponse): Call<Unit>

    @POST("account/password")
    fun postPassword(@Body request: PasswordRequest): Call<PasswordResponse>

    /* ----- Data ----- */

    @GET("data/{type}")
    fun getData(@Path("type") dataType: String): Call<DataRequestResponse>

    @POST("data/{type}")
    fun postData(
        @Path("type") dataType: String,
        @Body request: DataRequestResponse,
    ): Call<Unit>

    @DELETE("data/{type}/{id}")
    fun deleteData(
        @Path("type") dataType: String,
        @Path("id") id: String,
    ): Call<Unit>

    /* ----- Server messages ----- */

    @GET("messages")
    fun getMessages(): Call<MessagesResponse>

    @DELETE("messages/{id}")
    fun deleteSingleMessage(
        @Path("id") uuid: String,
    ): Call<Unit>
}
