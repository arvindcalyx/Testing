package com.test.safetyconnect.network

import android.content.Context
import com.test.safetyconnect.service.ApiService
import com.test.safetyconnect.BuildConfig
import com.facebook.stetho.okhttp3.StethoInterceptor
/*
import com.readystatesoftware.chuck.ChuckInterceptor
*/
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class NetworkModule {
    var context: Context? = null

    //Dev
    // var BASE_URL: String? = "http://127.0.0.1:8080/"
    //Prod
    var BASE_URL: String? = "https://api.example.com/safetyconnect/"

    constructor(context_: Context?) {
        context = context_
    }

    val cacheSize = 10 * 1024 * 1024 // 10 MB

    fun getApiService(): ApiService {
        return Retrofit.Builder().baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(
                provideOkHttpClient(
                    providesOkHttpClient(context!!),
                    provideHttpLoggingInterceptor(),
                )
            )
            .build()
            .create(ApiService::class.java)
    }


    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    internal fun provideOkHttpClient(
        builder: OkHttpClient.Builder,
        interceptor: HttpLoggingInterceptor,
    ): OkHttpClient {

        builder.addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
            // TODO: replace with real auth header — runtime config injection recommended.
            // Dummy default credentials below are base64("test:test").
            requestBuilder.header("Authorization", "Basic dGVzdDp0ZXN0")

            val request = requestBuilder.build()
            chain.proceed(request)
        }


        if (BuildConfig.DEBUG && !builder.interceptors().contains(interceptor))
            builder.addInterceptor(interceptor)

        return builder.build()
    }

    fun providesOkHttpClient(context: Context): OkHttpClient.Builder {
        val client = OkHttpClient.Builder()
            .cache(Cache(context.cacheDir, cacheSize.toLong()))
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
/*
            .addInterceptor(ChuckInterceptor(context))
*/
            .retryOnConnectionFailure(true)

        if (BuildConfig.DEBUG)
            client.addNetworkInterceptor(StethoInterceptor())

        return client
    }


    internal fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor()
            .setLevel(HttpLoggingInterceptor.Level.BODY)
    }

}