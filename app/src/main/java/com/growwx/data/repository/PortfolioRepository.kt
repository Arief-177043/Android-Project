package com.growwx.data.repository

import com.growwx.data.local.HoldingDao
import com.growwx.data.local.TransactionDao
import com.growwx.data.local.UserPreferences
import com.growwx.data.model.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortfolioRepository @Inject constructor(
    private val holdingDao: HoldingDao,
    private val transactionDao: TransactionDao,
    private val prefs: UserPreferences
) {
    val holdings: Flow<List<HoldingEntity>> = holdingDao.observeAll()
    val transactions: Flow<List<TransactionEntity>> = transactionDao.observeAll()
    val cashBalance: Flow<Double> = prefs.cashBalance

    /**
     * Execute a BUY order.
     * Validates cash, updates holding (weighted avg), records transaction.
     */
    suspend fun buy(symbol: String, name: String, qty: Double, price: Double, isCrypto: Boolean): Result<Unit> {
        val total = qty * price
        val currentCash = prefs.cashBalance.first()

        if (total > currentCash) {
            return Result.Error("Insufficient balance. Available: ₹${currentCash.format()}")
        }

        // Update or create holding (weighted average buy price)
        val existing = holdingDao.getBySymbol(symbol)
        val updatedHolding = if (existing != null) {
            val newQty = existing.qty + qty
            val newAvg = (existing.avgBuyPrice * existing.qty + price * qty) / newQty
            existing.copy(qty = newQty, avgBuyPrice = newAvg)
        } else {
            HoldingEntity(symbol, name, qty, price, isCrypto)
        }

        holdingDao.upsert(updatedHolding)
        prefs.updateCashBalance(currentCash - total)
        transactionDao.insert(
            TransactionEntity(symbol = symbol, name = name, type = "BUY", qty = qty, price = price, total = total)
        )

        return Result.Success(Unit)
    }

    /**
     * Execute a SELL order.
     * Validates holdings qty, removes or reduces holding, credits cash.
     */
    suspend fun sell(symbol: String, name: String, qty: Double, price: Double): Result<Unit> {
        val holding = holdingDao.getBySymbol(symbol)
            ?: return Result.Error("You don't own any $symbol")

        if (qty > holding.qty) {
            return Result.Error("You only have ${holding.qty} shares of $symbol")
        }

        val total = qty * price
        val remaining = holding.qty - qty

        if (remaining <= 0.0001) {
            holdingDao.deleteBySymbol(symbol)
        } else {
            holdingDao.upsert(holding.copy(qty = remaining))
        }

        prefs.updateCashBalance(prefs.cashBalance.first() + total)
        transactionDao.insert(
            TransactionEntity(symbol = symbol, name = name, type = "SELL", qty = qty, price = price, total = total)
        )

        return Result.Success(Unit)
    }

    /**
     * Compute portfolio summary given a map of live prices.
     */
    fun computeSummary(
        holdings: List<HoldingEntity>,
        livePrices: Map<String, Stock>,
        cash: Double
    ): PortfolioSummary {
        val withPrices = holdings.map { h ->
            val currentPrice = livePrices[h.symbol]?.price ?: h.avgBuyPrice
            val currentValue = currentPrice * h.qty
            val invested = h.avgBuyPrice * h.qty
            val pnl = currentValue - invested
            val pnlPct = if (invested > 0) (pnl / invested) * 100 else 0.0
            HoldingWithPrice(h, currentPrice, pnl, pnlPct, currentValue)
        }

        val totalValue = withPrices.sumOf { it.currentValue } + cash
        val totalInvested = withPrices.sumOf { it.holding.avgBuyPrice * it.holding.qty }
        val pnl = totalValue - (totalInvested + cash)
        val pnlPct = if (totalInvested > 0) (pnl / totalInvested) * 100 else 0.0

        return PortfolioSummary(
            totalValue = totalValue,
            totalInvested = totalInvested,
            cashBalance = cash,
            pnl = pnl,
            pnlPercent = pnlPct,
            holdings = withPrices
        )
    }
}

private fun Double.format() = String.format("%,.2f", this)
