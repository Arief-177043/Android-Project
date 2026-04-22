package com.growwx.data.local

import androidx.room.*
import com.growwx.data.model.*
import kotlinx.coroutines.flow.Flow

// ─── DAOs ─────────────────────────────────────────────────────────────────────

@Dao
interface HoldingDao {
    @Query("SELECT * FROM holdings")
    fun observeAll(): Flow<List<HoldingEntity>>

    @Query("SELECT * FROM holdings WHERE symbol = :symbol")
    suspend fun getBySymbol(symbol: String): HoldingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(holding: HoldingEntity)

    @Delete
    suspend fun delete(holding: HoldingEntity)

    @Query("DELETE FROM holdings WHERE symbol = :symbol")
    suspend fun deleteBySymbol(symbol: String)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE symbol = :symbol ORDER BY timestamp DESC")
    fun observeBySymbol(symbol: String): Flow<List<TransactionEntity>>

    @Insert
    suspend fun insert(tx: TransactionEntity)

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int
}

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<WatchlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE symbol = :symbol)")
    fun isWatched(symbol: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE symbol = :symbol")
    suspend fun remove(symbol: String)
}

@Dao
interface PriceAlertDao {
    @Query("SELECT * FROM price_alerts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PriceAlertEntity>>

    @Query("SELECT * FROM price_alerts WHERE isTriggered = 0")
    suspend fun getActiveAlerts(): List<PriceAlertEntity>

    @Insert
    suspend fun insert(alert: PriceAlertEntity)

    @Query("UPDATE price_alerts SET isTriggered = 1 WHERE id = :id")
    suspend fun markTriggered(id: Long)

    @Query("DELETE FROM price_alerts WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface QuoteCacheDao {
    @Query("SELECT * FROM cached_quotes WHERE symbol = :symbol")
    suspend fun get(symbol: String): CachedQuoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(quote: CachedQuoteEntity)

    // Cache expires after 60 seconds
    @Query("SELECT * FROM cached_quotes WHERE symbol = :symbol AND cachedAt > :minTime")
    suspend fun getFresh(symbol: String, minTime: Long): CachedQuoteEntity?
}

// ─── Database ─────────────────────────────────────────────────────────────────

@Database(
    entities = [
        HoldingEntity::class,
        TransactionEntity::class,
        WatchlistEntity::class,
        PriceAlertEntity::class,
        CachedQuoteEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GrowwXDatabase : RoomDatabase() {
    abstract fun holdingDao(): HoldingDao
    abstract fun transactionDao(): TransactionDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun priceAlertDao(): PriceAlertDao
    abstract fun quoteCacheDao(): QuoteCacheDao
}

class Converters {
    // Add type converters if needed for complex types
}
