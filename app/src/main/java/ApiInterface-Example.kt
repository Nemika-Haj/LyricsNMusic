package com.nemika.lyricsnmusic

import com.nemika.lyricsnmusic.data.LyricsData
import com.nemika.lyricsnmusic.data.YoutubeData
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query


// Rename to ApiInterface
interface ApiInterface_Example {
    @GET("lyrics")
    @Headers("Authorization: YourApiKey")
    fun fetchLyrics(@Query("song") songName: String, @Query("author") authorName: String): Call<LyricsData>

    @GET("beta/youtube")
    @Headers("Authorization: YourApiKey")
    fun fetchYoutubeData(@Query("query") query: String): Call<YoutubeData>
}