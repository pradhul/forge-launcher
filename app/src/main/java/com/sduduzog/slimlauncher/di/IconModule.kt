package com.sduduzog.slimlauncher.di

import android.content.Context
import com.sduduzog.slimlauncher.R
import com.sduduzog.slimlauncher.icons.ApiKeyStore
import com.sduduzog.slimlauncher.icons.FileIconPackStore
import com.sduduzog.slimlauncher.icons.GeminiIconClient
import com.sduduzog.slimlauncher.icons.IconPackStore
import com.sduduzog.slimlauncher.icons.IconPreferences
import com.sduduzog.slimlauncher.ui.home.LaunchStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IconModule {

    @Provides
    @Singleton
    fun provideIconPackStore(@ApplicationContext context: Context): IconPackStore {
        return FileIconPackStore(File(context.filesDir, "icon_packs"))
    }

    @Provides
    @Singleton
    fun provideIconPreferences(@ApplicationContext context: Context): IconPreferences {
        val prefs = context.getSharedPreferences(
            context.getString(R.string.prefs_settings),
            Context.MODE_PRIVATE
        )
        return IconPreferences(prefs)
    }

    @Provides
    @Singleton
    fun provideLaunchStore(@ApplicationContext context: Context): LaunchStore {
        return LaunchStore(
            context.getSharedPreferences(LaunchStore.PREFS_NAME, Context.MODE_PRIVATE)
        )
    }

    @Provides
    @Singleton
    fun provideApiKeyStore(@ApplicationContext context: Context): ApiKeyStore {
        return ApiKeyStore(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideGeminiIconClient(
        httpClient: OkHttpClient,
        apiKeyStore: ApiKeyStore
    ): GeminiIconClient {
        return GeminiIconClient(httpClient, apiKeyStore)
    }
}
