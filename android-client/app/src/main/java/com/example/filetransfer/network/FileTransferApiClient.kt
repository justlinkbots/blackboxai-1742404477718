package com.example.filetransfer.network

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

interface FileTransferApi {
    @GET("status")
    fun checkStatus(): Call<StatusResponse>

    @Multipart
    @POST("upload")
    fun uploadFile(@Part file: MultipartBody.Part): Call<UploadResponse>
}

data class StatusResponse(
    val status: String,
    val serverName: String,
    val timestamp: String
)

data class UploadResponse(
    val status: String,
    val message: String,
    val file: FileInfo
)

data class FileInfo(
    val originalName: String,
    val size: Long,
    val path: String
)

class FileTransferApiClient private constructor(baseUrl: String) {
    private val api: FileTransferApi

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(FileTransferApi::class.java)
    }

    fun checkStatus(onSuccess: () -> Unit, onError: (String) -> Unit) {
        api.checkStatus().enqueue(object : Callback<StatusResponse> {
            override fun onResponse(call: Call<StatusResponse>, response: Response<StatusResponse>) {
                if (response.isSuccessful && response.body()?.status == "ok") {
                    onSuccess()
                } else {
                    onError("Server returned error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<StatusResponse>, t: Throwable) {
                Timber.e(t, "Failed to check server status")
                onError(t.message ?: "Network error")
            }
        })
    }

    fun uploadFile(
        file: File,
        onProgress: (Int) -> Unit,
        onSuccess: (UploadResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        api.uploadFile(body).enqueue(object : Callback<UploadResponse> {
            override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { onSuccess(it) }
                } else {
                    onError("Upload failed: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                Timber.e(t, "Failed to upload file")
                onError(t.message ?: "Network error")
            }
        })
    }

    companion object {
        fun create(baseUrl: String): FileTransferApiClient {
            return FileTransferApiClient(baseUrl)
        }
    }
}