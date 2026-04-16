package com.timebox.app.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timebox.app.data.db.BocksLedgerDao
import com.timebox.app.data.db.AchievementRecordDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AchievementsState(
    val lifetimeEarned: Int = 0,
    val unlockedById: Map<String, String> = emptyMap()
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val bocksLedgerDao: BocksLedgerDao,
    private val achievementRecordDao: AchievementRecordDao
) : ViewModel() {

    private val _state = MutableStateFlow(AchievementsState())
    val state: StateFlow<AchievementsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            bocksLedgerDao.initIfNeeded()
            val ledger = bocksLedgerDao.getBalanceOnce()
            val lifetime = ledger?.lifetimeEarned ?: 0
            _state.update { it.copy(lifetimeEarned = lifetime) }
        }
        viewModelScope.launch {
            achievementRecordDao.getAll().collect { list ->
                _state.update {
                    it.copy(
                        unlockedById = list.associate { r -> r.achievementId to r.unlockedDate }
                    )
                }
            }
        }
    }
}

