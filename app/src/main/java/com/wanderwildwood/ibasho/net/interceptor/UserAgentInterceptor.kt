package com.wanderwildwood.ibasho.net.interceptor

import com.wanderwildwood.ibasho.net.FMD_USER_AGENT
import com.wanderwildwood.ibasho.net.HEADER_USER_AGENT
import okhttp3.Interceptor
import okhttp3.Response

class UserAgentInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .header(HEADER_USER_AGENT, FMD_USER_AGENT)
            .build()

        return chain.proceed(request)
    }
}
