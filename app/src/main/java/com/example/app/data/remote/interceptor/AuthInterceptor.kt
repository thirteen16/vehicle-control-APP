package com.example.app.data.remote.interceptor

import com.example.app.common.Constants
import com.example.app.data.local.TokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenStore: TokenStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val token = runBlocking {
            tokenStore.getToken()
        }

        if (token.isNullOrBlank()) {
            return chain.proceed(originalRequest)
        }

        val newRequest = originalRequest.newBuilder()
            .addHeader(Constants.AUTH_HEADER, Constants.TOKEN_PREFIX + token)
            .build()

        return chain.proceed(newRequest)
    }
}