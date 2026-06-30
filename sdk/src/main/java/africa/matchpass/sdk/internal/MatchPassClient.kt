package africa.matchpass.sdk.internal

import africa.matchpass.sdk.MatchPassConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

internal class MatchPassClient(config: MatchPassConfig) {

    private val okHttp = OkHttpClient.Builder()
        .apply {
            if (config.debug) {
                addInterceptor(
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
                )
            }
        }
        .build()

    val service: MatchPassService = Retrofit.Builder()
        .baseUrl(config.baseUrl)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(MatchPassService::class.java)
}
