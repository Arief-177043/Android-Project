package com.growwx.data.api

import com.google.gson.annotations.SerializedName
import com.growwx.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Query

// ─── Alpha Vantage API ────────────────────────────────────────────────────────
// Free key: get yours at https://www.alphavantage.co/support/#api-key
// Demo key has rate limits — for internship, use your own key.

interface StockApiService {

    /**
     * Global Quote — live price snapshot for a symbol
     * e.g. GET /query?function=GLOBAL_QUOTE&symbol=RELIANCE.BSE&apikey=KEY
     */
    @GET("query")
    suspend fun getGlobalQuote(
        @Query("function") function: String = "GLOBAL_QUOTE",
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String = BuildConfig.ALPHA_VANTAGE_KEY
    ): GlobalQuoteResponse

    /**
     * Intraday time series — for sparkline / chart data
     */
    @GET("query")
    suspend fun getIntraday(
        @Query("function") function: String = "TIME_SERIES_INTRADAY",
        @Query("symbol") symbol: String,
        @Query("interval") interval: String = "60min",
        @Query("outputsize") outputSize: String = "compact",
        @Query("apikey") apiKey: String = BuildConfig.ALPHA_VANTAGE_KEY
    ): IntradayResponse

    /**
     * Daily time series — for 30-day chart
     */
    @GET("query")
    suspend fun getDaily(
        @Query("function") function: String = "TIME_SERIES_DAILY",
        @Query("symbol") symbol: String,
        @Query("outputsize") outputSize: String = "compact",
        @Query("apikey") apiKey: String = BuildConfig.ALPHA_VANTAGE_KEY
    ): DailyResponse
}

// ─── Response DTOs ────────────────────────────────────────────────────────────

data class GlobalQuoteResponse(
    @SerializedName("Global Quote") val globalQuote: GlobalQuoteDto?
)

data class GlobalQuoteDto(
    @SerializedName("01. symbol") val symbol: String = "",
    @SerializedName("02. open") val open: String = "0",
    @SerializedName("03. high") val high: String = "0",
    @SerializedName("04. low") val low: String = "0",
    @SerializedName("05. price") val price: String = "0",
    @SerializedName("06. volume") val volume: String = "0",
    @SerializedName("09. change") val change: String = "0",
    @SerializedName("10. change percent") val changePercent: String = "0%"
)

data class IntradayResponse(
    @SerializedName("Time Series (60min)") val timeSeries: Map<String, OhlcDto>?
)

data class DailyResponse(
    @SerializedName("Time Series (Daily)") val timeSeries: Map<String, OhlcDto>?
)

data class OhlcDto(
    @SerializedName("1. open") val open: String = "0",
    @SerializedName("2. high") val high: String = "0",
    @SerializedName("3. low") val low: String = "0",
    @SerializedName("4. close") val close: String = "0",
    @SerializedName("5. volume") val volume: String = "0"
)
