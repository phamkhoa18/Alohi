package com.example.alohi.data.remote

import com.example.alohi.data.model.ApiResponse
import com.example.alohi.data.model.StoriesFeedResponse
import com.example.alohi.data.model.StoryItem
import com.example.alohi.data.model.StoryViewer
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface StoryApiService {
    @GET("stories/feed")
    suspend fun getFeed(): Response<ApiResponse<StoriesFeedResponse>>

    @GET("stories/me")
    suspend fun getMyStories(): Response<ApiResponse<List<StoryItem>>>

    @Multipart
    @POST("stories")
    suspend fun createStory(
        @Part("type") type: RequestBody,
        @Part("privacy") privacy: RequestBody,
        @Part("caption") caption: RequestBody?,
        @Part media: MultipartBody.Part?,
        @Part music: MultipartBody.Part?
    ): Response<ApiResponse<StoryItem>>

    @DELETE("stories/{id}")
    suspend fun deleteStory(@Path("id") id: String): Response<ApiResponse<Any>>

    @GET("stories/{id}/viewers")
    suspend fun getViewers(@Path("id") id: String): Response<ApiResponse<List<StoryViewer>>>

    @POST("stories/{id}/view")
    suspend fun viewStory(@Path("id") id: String): Response<ApiResponse<Any>>
    
    @FormUrlEncoded
    @POST("stories/{id}/react")
    suspend fun reactStory(
        @Path("id") id: String,
        @Field("reaction") reaction: String
    ): Response<ApiResponse<Any>>
}
