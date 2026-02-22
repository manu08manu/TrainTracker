package com.example.traintracker.data.api

import android.content.Context
import com.example.traintracker.data.Constants
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

object RetrofitInstance {

    private val gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    private fun createHttpClient(context: Context): OkHttpClient {
        val cacheSize = 5 * 1024 * 1024L // 5 MB
        val cacheDir = File(context.cacheDir, "http_cache")
        val cache = Cache(cacheDir, cacheSize)

        val interceptor = Interceptor { chain ->
            val original = chain.request()
            val originalUrl = original.url

            val url = originalUrl.newBuilder()
                .addQueryParameter("app_id", Constants.APP_ID)
                .addQueryParameter("app_key", Constants.APP_KEY)
                .build()

            val request = original.newBuilder()
                .url(url)
                .build()
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(interceptor)
            .build()
    }

    private var retrofit: Retrofit? = null

    fun getApi(context: Context): ApiService {
        return retrofit?.create(ApiService::class.java) ?: synchronized(this) {
            val r = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(createHttpClient(context))
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
            retrofit = r
            r.create(ApiService::class.java)
        }
    }
}
