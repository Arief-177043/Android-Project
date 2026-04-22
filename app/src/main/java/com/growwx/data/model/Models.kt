package com.growwx.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// ─── Network / Domain Models ──────────────────────────────────────────────────

data class Stock(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,        // absolute change
    val changePct: Double,     // % change
    val high: Double = 0.0,
    val low: Double = 0.0,
    val volume: Long = 0L,
    val sector: String = "",
    val isCrypto: Boolean = false
)

data class PricePoint(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long = 0L
)

data class AlphaVantageQuoteResponse(
    val globalQuote: GlobalQuote?
)

data class GlobalQuote(
    val symbol: String = "",
    val open: String = "0",
    val high: String = "0",
    val low: String = "0",
    val price: String = "0",
    val volume: String = "0",
    val previousClose: String = "0",
    val change: String = "0",
    val changePercent: String = "0%"
)

// ─── Room Entities ────────────────────────────────────────────────────────────

@Entity(tableName = "holdings")
data class HoldingEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val qty: Double,
    val avgBuyPrice: Double,
    val isCrypto: Boolean = false
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val name: String,
    val type: String,          // "BUY" or "SELL"
    val qty: Double,
    val price: Double,
    val total: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val isCrypto: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "price_alerts")
data class PriceAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val name: String,
    val targetPrice: Double,
    val condition: String,     // "ABOVE" or "BELOW"
    val isTriggered: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_quotes")
data class CachedQuoteEntity(
    @PrimaryKey val symbol: String,
    val price: Double,
    val change: Double,
    val changePct: Double,
    val high: Double,
    val low: Double,
    val volume: Long,
    val cachedAt: Long = System.currentTimeMillis()
)

// ─── UI State Models ──────────────────────────────────────────────────────────

data class PortfolioSummary(
    val totalValue: Double = 0.0,
    val totalInvested: Double = 0.0,
    val cashBalance: Double = 100_000.0,
    val pnl: Double = 0.0,
    val pnlPercent: Double = 0.0,
    val holdings: List<HoldingWithPrice> = emptyList()
)

data class HoldingWithPrice(
    val holding: HoldingEntity,
    val currentPrice: Double,
    val pnl: Double,
    val pnlPercent: Double,
    val currentValue: Double
)

data class User(
    val uid: String,
    val name: String,
    val email: String
)

// ─── Sealed Result Wrapper ────────────────────────────────────────────────────

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
