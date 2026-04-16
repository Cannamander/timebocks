package com.timebox.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bocks_ledger")
data class BocksLedger(
    @PrimaryKey val id: Int = 1,
    val balance: Int = 0,
    val lifetimeEarned: Int = 0
)

