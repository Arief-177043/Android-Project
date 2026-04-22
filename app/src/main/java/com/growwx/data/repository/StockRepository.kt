package com.growwx.data.repository

import com.growwx.data.api.StockApiService
import com.growwx.data.local.QuoteCacheDao
import com.growwx.data.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.random.Random

@Singleton
class StockRepository @Inject constructor(
    private val api: StockApiService,
    private val cacheDao: QuoteCacheDao
) {
    companion object {
        private const val CACHE_TTL_MS = 60_000L // 60 seconds

        // Fallback dummy data — used when API rate-limited (demo key = 5 req/min)
        val DUMMY_STOCKS = listOf(
            Stock("RELIANCE", "Reliance Industries", 2847.50, 34.80, 1.24, 2860.0, 2810.0, 8_432_100, "Energy"),
            Stock("TCS", "Tata Consultancy Services", 3654.20, -32.10, -0.87, 3690.0, 3640.0, 1_234_500, "IT"),
            Stock("HDFCBANK", "HDFC Bank", 1723.80, 7.70, 0.45, 1730.0, 1710.0, 5_678_200, "Finance"),
            Stock("INFOSYS", "Infosys Ltd", 1876.40, 39.20, 2.13, 1890.0, 1850.0, 3_456_700, "IT"),
            Stock("WIPRO", "Wipro Ltd", 487.60, -6.52, -1.32, 492.0, 483.0, 4_321_000, "IT"),
            Stock("TATAMOTORS", "Tata Motors", 934.20, 31.10, 3.45, 945.0, 905.0, 7_654_300, "Auto"),
            Stock("BAJFINANCE", "Bajaj Finance", 7234.50, -47.40, -0.65, 7290.0, 7200.0, 987_600, "Finance"),
            Stock("ADANIPORTS", "Adani Ports", 1243.80, 22.90, 1.87, 1255.0, 1230.0, 2_345_600, "Infra"),
            Stock("SUNPHARMA", "Sun Pharmaceutical", 1567.30, 14.30, 0.92, 1575.0, 1555.0, 1_876_500, "Pharma"),
            Stock("ITC", "ITC Ltd", 456.70, -1.06, -0.23, 459.0, 452.0, 9_876_500, "FMCG"),
            Stock("MARUTI", "Maruti Suzuki", 10234.60, 157.60, 1.56, 10280.0, 10080.0, 456_700, "Auto"),
            Stock("LTIM", "LTIMindtree", 5678.90, -123.90, -2.14, 5810.0, 5660.0, 345_600, "IT"),
        )

        val DUMMY_CRYPTO = listOf(
            Stock("BTC", "Bitcoin", 5_843_200.0, 133_580.0, 2.34, 5_900_000.0, 5_700_000.0, 0, "", isCrypto = true),
            Stock("ETH", "Ethereum", 312_840.0, -4_591.0, -1.45, 318_000.0, 308_000.0, 0, "", isCrypto = true),
            Stock("SOL", "Solana", 14_230.0, 763.0, 5.67, 14_500.0, 13_500.0, 0, "", isCrypto = true),
            Stock("BNB", "BNB", 46_780.0, 413.0, 0.89, 47_200.0, 46_400.0, 0, "", isCrypto = true),
        )
    }

    /**
     * Fetch quote with cache-first strategy.
     * Falls back to simulated dummy data if API fails (rate limit, network).
     */
    suspend fun getQuote(symbol: String): Result<Stock> {
        // 1. Check fresh cache
        val cached = cacheDao.getFresh(symbol, System.currentTimeMillis() - CACHE_TTL_MS)
        if (cached != null) {
            return Result.Success(cached.toStock())
        }

        // 2. Try network
        return try {
            val response = api.getGlobalQuote(symbol = symbol)
            val dto = response.globalQuote
            if (dto != null && dto.price.toDoubleOrNull() != null && dto.price.toDouble() > 0) {
                val stock = dto.toStock(symbol)
                // Save to cache
                cacheDao.upsert(stock.toCacheEntity())
                Result.Success(stock)
            } else {
                // API returned empty (rate limited with demo key)
                Result.Success(simulatePrice(symbol))
            }
        } catch (e: Exception) {
            // Network error — return simulated price
            Result.Success(simulatePrice(symbol))
        }
    }

    /**
     * Polling flow — emits updated quotes every [intervalMs]
     * Uses debounce-friendly polling with simulated live fluctuations
     */
    fun observeQuotes(symbols: List<String>, intervalMs: Long = 5000L): Flow<Map<String, Stock>> = flow {
        val current = mutableMapOf<String, Stock>()

        // Initial load from dummy data
        (DUMMY_STOCKS + DUMMY_CRYPTO).filter { it.symbol in symbols }.forEach {
            current[it.symbol] = it
        }
        emit(current.toMap())

        // Simulate live updates
        while (true) {
            delay(intervalMs)
            symbols.forEach { symbol ->
                val prev = current[symbol] ?: return@forEach
                val delta = prev.price * (Random.nextDouble(-0.003, 0.003))
                val newPrice = maxOf(prev.price + delta, 0.01)
                val changePct = ((newPrice - (prev.price - prev.change)) / (prev.price - prev.change)) * 100
                current[symbol] = prev.copy(
                    price = String.format("%.2f", newPrice).toDouble(),
                    change = newPrice - (prev.price - prev.change),
                    changePct = changePct
                )
            }
            emit(current.toMap())
        }
    }

    /**
     * Generate plausible chart data for a symbol over N days
     */
    fun generateChartData(symbol: String, days: Int = 30): List<PricePoint> {
        val base = (DUMMY_STOCKS + DUMMY_CRYPTO).find { it.symbol == symbol }?.price ?: 1000.0
        var price = base * 0.92
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L

        return (days downTo 0).map { i ->
            val delta = price * Random.nextDouble(-0.025, 0.027)
            price = maxOf(price + delta, 1.0)
            PricePoint(
                timestamp = now - i * dayMs,
                open = price * (1 + Random.nextDouble(-0.005, 0.005)),
                high = price * (1 + Random.nextDouble(0.001, 0.015)),
                low = price * (1 - Random.nextDouble(0.001, 0.015)),
                close = if (i == 0) base else price
            )
        }
    }

    private fun simulatePrice(symbol: String): Stock {
        val base = (DUMMY_STOCKS + DUMMY_CRYPTO).find { it.symbol == symbol }
            ?: DUMMY_STOCKS.first()
        val fluctuation = base.price * Random.nextDouble(-0.005, 0.005)
        return base.copy(price = base.price + fluctuation)
    }
}

// ─── Extension mappers ────────────────────────────────────────────────────────

private fun com.growwx.data.api.GlobalQuoteDto.toStock(symbolFallback: String): Stock {
    val p = price.toDoubleOrNull() ?: 0.0
    val c = change.toDoubleOrNull() ?: 0.0
    val pct = changePercent.removeSuffix("%").toDoubleOrNull() ?: 0.0
    return Stock(
        symbol = symbol.ifEmpty { symbolFallback },
        name = symbol.ifEmpty { symbolFallback },
        price = p,
        change = c,
        changePct = pct,
        high = high.toDoubleOrNull() ?: 0.0,
        low = low.toDoubleOrNull() ?: 0.0,
        volume = volume.toLongOrNull() ?: 0L
    )
}

private fun CachedQuoteEntity.toStock() = Stock(
    symbol = symbol, name = symbol, price = price,
    change = change, changePct = changePct, high = high, low = low, volume = volume
)

private fun Stock.toCacheEntity() = CachedQuoteEntity(
    symbol = symbol, price = price, change = change, changePct = changePct,
    high = high, low = low, volume = volume
)
