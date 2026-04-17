package com.example.alohi.data.repository

import com.example.alohi.data.model.StoriesFeedResponse
import com.example.alohi.data.model.StoryItem
import com.example.alohi.data.remote.ApiClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class StoryRepository {
    private val api = ApiClient.storyApi

    suspend fun getFeed(): Result<StoriesFeedResponse> {
        return try {
            val response = api.getFeed()
            if (response.isSuccessful) {
                Result.success(response.body()?.data ?: StoriesFeedResponse())
            } else {
                Result.failure(Exception("Failed to get feed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyStories(): Result<List<StoryItem>> {
        return try {
            val response = api.getMyStories()
            if (response.isSuccessful) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception("Failed to get my stories: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createStory(
        type: String,
        file: File?,
        privacy: String = "friends",
        caption: String? = null,
        musicFile: File? = null
    ): Result<StoryItem> {
        return try {
            val typePart = type.toRequestBody("text/plain".toMediaTypeOrNull())
            val privacyPart = privacy.toRequestBody("text/plain".toMediaTypeOrNull())
            val captionPart = caption?.toRequestBody("text/plain".toMediaTypeOrNull())

            var filePart: MultipartBody.Part? = null
            if (file != null) {
                val mediaType = if (type == "video") "video/*".toMediaTypeOrNull() else "image/*".toMediaTypeOrNull()
                val requestBody = file.asRequestBody(mediaType)
                filePart = MultipartBody.Part.createFormData("media", file.name, requestBody)
            }

            var musicPart: MultipartBody.Part? = null
            if (musicFile != null) {
                val requestBody = musicFile.asRequestBody("audio/*".toMediaTypeOrNull())
                musicPart = MultipartBody.Part.createFormData("music", musicFile.name, requestBody)
            }

            val response = api.createStory(typePart, privacyPart, captionPart, filePart, musicPart)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to create story: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteStory(id: String): Result<Any> {
        return try {
            val response = api.deleteStory(id)
            if (response.isSuccessful) {
                Result.success(Any())
            } else {
                Result.failure(Exception("Failed to delete story: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun viewStory(id: String): Result<Any> {
        return try {
            val response = api.viewStory(id)
            if (response.isSuccessful) {
                Result.success(Any())
            } else {
                Result.failure(Exception("Failed to mark story as viewed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
