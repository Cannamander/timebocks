package com.timebox.app.percy

sealed class PercyDialogue {
    // Onboarding
    data object Welcome : PercyDialogue()
    data object PermissionsExplainer : PercyDialogue()
    data object PermissionsGranted : PercyDialogue()

    // Limit reached (first time today for this app)
    data class LimitReached(val appName: String) : PercyDialogue()

    // Extension responses
    data class FirstExtension(val appName: String) : PercyDialogue()
    data class SecondExtensionChallenge(val appName: String) : PercyDialogue()
    data object ThirdExtensionRefusal : PercyDialogue()
    data class ThirdExtensionCurrencyPrompt(val bocksBalance: Int) : PercyDialogue()
    data object CurrencySpent : PercyDialogue()
    data object InsufficientBocks : PercyDialogue()

    // User chose to leave without extending
    data object UserWalkedAway : PercyDialogue()

    // Next-day feedback
    data class NextDayClean(val streak: Int) : PercyDialogue()
    data class NextDayHadExtensions(val streak: Int) : PercyDialogue()
    data class StreakMilestone(val days: Int) : PercyDialogue()

    // Achievements
    data class AchievementUnlocked(val title: String, val bocksAwarded: Int) : PercyDialogue()

    // Generic
    data object Idle : PercyDialogue()
}

