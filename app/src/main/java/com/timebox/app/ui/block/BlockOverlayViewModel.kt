package com.timebox.app.ui.block

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timebox.app.data.repository.GamificationRepository
import com.timebox.app.percy.PercyDialogue
import com.timebox.app.percy.PercyLines
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BlockPhase {
    data object LimitReached : BlockPhase()
    data object FirstExtension : BlockPhase()
    data object SecondChallenge : BlockPhase()
    data object ThirdRefusal : BlockPhase()
    data object BocksPrompt : BlockPhase()
    data object ExtensionGranted : BlockPhase()
    data object WalkedAway : BlockPhase()
}

data class BlockOverlayState(
    val extensionCount: Int = 0,
    val bocksBalance: Int = 0,
    val percyDialogue: String = "",
    val blockPhase: BlockPhase = BlockPhase.LimitReached,
    val challengeQuestion: String = "",
    val challengeAnswered: Boolean = false,
    val showBocksOption: Boolean = false
)

sealed class BlockOverlayEvent {
    data object GrantExtension : BlockOverlayEvent()
    data object NavigateHome : BlockOverlayEvent()
}

@HiltViewModel
class BlockOverlayViewModel @Inject constructor(
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BlockOverlayState())
    val state: StateFlow<BlockOverlayState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<BlockOverlayEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<BlockOverlayEvent> = _events.asSharedFlow()

    fun onScreenOpened(packageName: String, appName: String) {
        viewModelScope.launch {
            val count = gamificationRepository.getExtensionCount(packageName)
            val bocks = gamificationRepository.getBocksBalanceOnce()
            val phase = when {
                count <= 0 -> BlockPhase.LimitReached
                count == 1 -> BlockPhase.FirstExtension
                else -> BlockPhase.ThirdRefusal
            }
            val percy = when {
                count <= 0 -> PercyLines.get(PercyDialogue.LimitReached(appName))
                count == 1 -> PercyLines.get(PercyDialogue.FirstExtension(appName))
                else -> PercyLines.get(PercyDialogue.ThirdExtensionRefusal)
            }
            _state.update {
                it.copy(
                    extensionCount = count,
                    bocksBalance = bocks,
                    blockPhase = phase,
                    percyDialogue = percy,
                    challengeQuestion = "Why do you actually need more time?",
                    challengeAnswered = false,
                    showBocksOption = bocks >= 20
                )
            }
        }
    }

    fun onExtendTapped(packageName: String, appName: String) {
        viewModelScope.launch {
            val count = _state.value.extensionCount
            val bocks = gamificationRepository.getBocksBalanceOnce()
            when {
                count <= 0 -> {
                    val newCount = gamificationRepository.incrementExtension(packageName)
                    _state.update {
                        it.copy(
                            extensionCount = newCount,
                            bocksBalance = bocks,
                            blockPhase = BlockPhase.ExtensionGranted,
                            percyDialogue = PercyLines.get(PercyDialogue.FirstExtension(appName)),
                            showBocksOption = bocks >= 20
                        )
                    }
                    _events.tryEmit(BlockOverlayEvent.GrantExtension)
                }
                count == 1 -> {
                    _state.update {
                        it.copy(
                            bocksBalance = bocks,
                            blockPhase = BlockPhase.SecondChallenge,
                            percyDialogue = PercyLines.get(PercyDialogue.SecondExtensionChallenge(appName)),
                            challengeQuestion = "Why do you actually need more time?",
                            challengeAnswered = false,
                            showBocksOption = bocks >= 20
                        )
                    }
                }
                else -> {
                    val canSpend = bocks >= 20
                    _state.update {
                        it.copy(
                            bocksBalance = bocks,
                            blockPhase = BlockPhase.ThirdRefusal,
                            percyDialogue = if (canSpend) {
                                PercyLines.get(PercyDialogue.ThirdExtensionCurrencyPrompt(bocks))
                            } else {
                                PercyLines.get(PercyDialogue.InsufficientBocks)
                            },
                            showBocksOption = canSpend
                        )
                    }
                }
            }
        }
    }

    fun onChallengeAnswered(packageName: String, appName: String, answer: String) {
        viewModelScope.launch {
            gamificationRepository.awardBocks(2, "EXTENSION_CHALLENGE")
            val ok = answer.trim().length >= 10
            val bocks = gamificationRepository.getBocksBalanceOnce()
            if (!ok) {
                _state.update {
                    it.copy(
                        bocksBalance = bocks,
                        challengeAnswered = true,
                        percyDialogue = PercyLines.get(PercyDialogue.SecondExtensionChallenge(appName))
                    )
                }
                return@launch
            }

            val newCount = gamificationRepository.incrementExtension(packageName)
            _state.update {
                it.copy(
                    extensionCount = newCount,
                    bocksBalance = bocks,
                    blockPhase = BlockPhase.ExtensionGranted,
                    percyDialogue = PercyLines.get(PercyDialogue.FirstExtension(appName)),
                    challengeAnswered = true,
                    showBocksOption = bocks >= 20
                )
            }
            _events.tryEmit(BlockOverlayEvent.GrantExtension)
        }
    }

    fun onSpendBocks(packageName: String, appName: String) {
        viewModelScope.launch {
            val ok = gamificationRepository.spendBocks(20)
            val bocks = gamificationRepository.getBocksBalanceOnce()
            if (!ok) {
                _state.update {
                    it.copy(
                        bocksBalance = bocks,
                        blockPhase = BlockPhase.ThirdRefusal,
                        percyDialogue = PercyLines.get(PercyDialogue.InsufficientBocks),
                        showBocksOption = false
                    )
                }
                return@launch
            }

            val newCount = gamificationRepository.incrementExtension(packageName)
            _state.update {
                it.copy(
                    extensionCount = newCount,
                    bocksBalance = bocks,
                    blockPhase = BlockPhase.ExtensionGranted,
                    percyDialogue = PercyLines.get(PercyDialogue.CurrencySpent),
                    showBocksOption = bocks >= 20
                )
            }
            _events.tryEmit(BlockOverlayEvent.GrantExtension)
        }
    }

    fun onGoHomeTapped(appName: String) {
        _state.update {
            it.copy(
                blockPhase = BlockPhase.WalkedAway,
                percyDialogue = PercyLines.get(PercyDialogue.UserWalkedAway)
            )
        }
        _events.tryEmit(BlockOverlayEvent.NavigateHome)
    }
}

