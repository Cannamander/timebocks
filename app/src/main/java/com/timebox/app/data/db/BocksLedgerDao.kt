package com.timebox.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.timebox.app.data.model.BocksLedger
import kotlinx.coroutines.flow.Flow

@Dao
interface BocksLedgerDao {

    @Query("SELECT * FROM bocks_ledger WHERE id = 1 LIMIT 1")
    fun getBalance(): Flow<BocksLedger?>

    @Query("SELECT * FROM bocks_ledger WHERE id = 1 LIMIT 1")
    suspend fun getBalanceOnce(): BocksLedger?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun initIfNeeded(row: BocksLedger = BocksLedger())

    @Query(
        "UPDATE bocks_ledger SET balance = balance + :amount, lifetimeEarned = lifetimeEarned + :amount WHERE id = 1"
    )
    suspend fun addBocks(amount: Int)

    @Query("UPDATE bocks_ledger SET balance = balance - :amount WHERE id = 1")
    suspend fun spendBocksInternal(amount: Int)

    @Transaction
    suspend fun spendBocks(amount: Int): Boolean {
        initIfNeeded()
        val current = getBalanceOnce() ?: BocksLedger()
        if (current.balance < amount) return false
        spendBocksInternal(amount)
        return true
    }
}

