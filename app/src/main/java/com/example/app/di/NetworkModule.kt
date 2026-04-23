package com.example.app.di

import android.content.Context
import com.example.app.common.Constants
import com.example.app.data.local.TokenStore
import com.example.app.data.remote.api.AuthApi
import com.example.app.data.remote.api.CommandApi
import com.example.app.data.remote.api.VehicleApi
import com.example.app.data.remote.interceptor.AuthInterceptor
import com.example.app.data.remote.ws.AppWebSocketClient
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    fun provideTokenStore(context: Context): TokenStore {
        return TokenStore(context)
    }

    fun provideAuthInterceptor(tokenStore: TokenStore): AuthInterceptor {
        return AuthInterceptor(tokenStore)
    }

    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    fun provideVehicleApi(retrofit: Retrofit): VehicleApi {
        return retrofit.create(VehicleApi::class.java)
    }

    fun provideCommandApi(retrofit: Retrofit): CommandApi {
        return retrofit.create(CommandApi::class.java)
    }

    fun provideAppWebSocketClient(okHttpClient: OkHttpClient): AppWebSocketClient {
        return AppWebSocketClient(okHttpClient)
    }
}