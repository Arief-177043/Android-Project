package com.growwx.di

import android.content.Context
import androidx.room.Room
import com.growwx.BuildConfig
import com.growwx.data.api.StockApiService
import com.growwx.data.local.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ─── Network ──────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG)
                    HttpLoggingInterceptor.Level.BODY
                else
                    HttpLoggingInterceptor.Level.NONE
            })
            // Debounce: cache responses for 60s to avoid hammering free API
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                response.newBuilder()
                    .header("Cache-Control", "public, max-age=60")
                    .build()
            }
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideStockApiService(retrofit: Retrofit): StockApiService =
        retrofit.create(StockApiService::class.java)

    // ─── Database ─────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GrowwXDatabase =
        Room.databaseBuilder(context, GrowwXDatabase::class.java, "growwx.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideHoldingDao(db: GrowwXDatabase): HoldingDao = db.holdingDao()
    @Provides fun provideTransactionDao(db: GrowwXDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideWatchlistDao(db: GrowwXDatabase): WatchlistDao = db.watchlistDao()
    @Provides fun providePriceAlertDao(db: GrowwXDatabase): PriceAlertDao = db.priceAlertDao()
    @Provides fun provideQuoteCacheDao(db: GrowwXDatabase): QuoteCacheDao = db.quoteCacheDao()
}
