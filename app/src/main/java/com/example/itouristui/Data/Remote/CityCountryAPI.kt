package com.example.itouristui.Data.Remote

import com.example.itouristui.models.CityDetails
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.net.URLEncoder

private val BASE_URL = "https://spott.p.rapidapi.com/"

private val client = OkHttpClient.Builder().addInterceptor{chain->
    chain.request().newBuilder()
        .addHeader("X-RapidAPI-Key","API KEY")
        .addHeader("X-RapidAPI-Host","spott.p.rapidapi.com")
        .build().run {
            chain.proceed(this)
        }
}.build()

val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .client(client)
    .addConverterFactory(ScalarsConverterFactory.create())
    .build()


interface CityCountryApiInterface {
    @GET("places/autocomplete")
    fun getCities(
        @Query("limit") limit : Int=6,
        @Query("skip") skip : Int=0,
        @Query("type") type : String = "CITY",
        @Query("q") prefixName:String
    ): Call<List<CityDetails>>
}

object CityCountryApiObject{
    val cityCountryApiInterface : CityCountryApiInterface by lazy{
        retrofit.create(CityCountryApiInterface::class.java)
    }
}
