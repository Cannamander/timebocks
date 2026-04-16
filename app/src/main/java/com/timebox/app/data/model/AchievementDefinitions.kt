package com.timebox.app.data.model

data class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
    val bocksReward: Int
)

object AchievementDefinitions {
    val NO_FUNNY_BOCKSINESS = AchievementDef(
        id = "NO_FUNNY_BOCKSINESS",
        title = "No Funny Bocksiness",
        description = "Completed a full day with zero extensions.",
        bocksReward = 15
    )
    val IRON_BEAK = AchievementDef(
        id = "IRON_BEAK",
        title = "Iron Beak",
        description = "Maintained a 7-day clean streak.",
        bocksReward = 50
    )
    val SOCIALLY_STABLE = AchievementDef(
        id = "SOCIALLY_STABLE",
        title = "Socially Stable",
        description = "Stayed within limits on all social apps for a full day.",
        bocksReward = 20
    )
    val YOU_MEANT_IT = AchievementDef(
        id = "YOU_MEANT_IT",
        title = "You Meant It",
        description = "Closed the block screen without extending, three times.",
        bocksReward = 10
    )
    val FIRST_REFUSAL = AchievementDef(
        id = "FIRST_REFUSAL",
        title = "Actually No",
        description = "Walked away from an extension on your first try.",
        bocksReward = 5
    )

    val ALL = listOf(
        NO_FUNNY_BOCKSINESS,
        IRON_BEAK,
        SOCIALLY_STABLE,
        YOU_MEANT_IT,
        FIRST_REFUSAL
    )

    fun getById(id: String): AchievementDef? = ALL.find { it.id == id }
}

