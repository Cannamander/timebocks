package com.timebox.app.percy

object PercyLines {
    fun get(dialogue: PercyDialogue): String = when (dialogue) {
        PercyDialogue.Welcome -> listOf(
            "Bwok. I'm Percy. You clearly need help. Let's begin.",
            "Percival Cluckworth at your service. I've seen your screen time. It's not pretty.",
            "Hello. I'm Percy. You downloaded this app, which means part of you knows. Good."
        ).random()

        PercyDialogue.PermissionsExplainer -> listOf(
            "I need a few permissions to do my job. Don't overthink it — I'm a chicken, not spyware.",
            "Before we begin: I need to see what you're doing. No, I'm not judging. (I'm judging.)"
        ).random()

        PercyDialogue.PermissionsGranted -> listOf(
            "Excellent. I'm watching now. Behave accordingly.",
            "Access granted. Let the supervision begin."
        ).random()

        is PercyDialogue.LimitReached -> listOf(
            "That's your ${dialogue.appName} time for today. Percy has spoken.",
            "Time's up on ${dialogue.appName}. I don't make the rules. Wait, I do. Goodbye.",
            "You've used all your ${dialogue.appName} time. Revolutionary, I know."
        ).random()

        is PercyDialogue.FirstExtension -> listOf(
            "Fine. Five more minutes. But I'm noting this.",
            "One extension. Clocked. Don't make it a habit — I have a very long memory.",
            "I'll allow it. This once. The clock is watching."
        ).random()

        is PercyDialogue.SecondExtensionChallenge -> listOf(
            "Again? Tell me why you actually need more time. And don't say 'just a sec'.",
            "Second extension. This is where we have a conversation.",
            "You're back. Interesting. Before I unlock anything, you're going to earn it."
        ).random()

        PercyDialogue.ThirdExtensionRefusal -> listOf(
            "No. Absolutely not. We're done here.",
            "Three times? The answer is no. That's it. That's the whole sentence.",
            "I admire the persistence. The answer remains no."
        ).random()

        is PercyDialogue.ThirdExtensionCurrencyPrompt -> listOf(
            "...You could spend ${dialogue.bocksBalance} Bocks. But you earned those. Think about that.",
            "You have ${dialogue.bocksBalance} Bocks saved up. Spending them here is your choice. A bad one, but yours.",
            "There's a Bocks option. You have ${dialogue.bocksBalance}. I'll just be over here, disappointed."
        ).random()

        PercyDialogue.CurrencySpent -> listOf(
            "Bocks spent. Five minutes purchased. I hope it was worth it.",
            "Fine. You bought your way out. Very on brand.",
            "Transaction complete. Please use this time wisely. Please."
        ).random()

        PercyDialogue.InsufficientBocks -> listOf(
            "You don't have enough Bocks. Maybe try having more self-control yesterday.",
            "Insufficient Bocks. The irony writes itself.",
            "Not enough Bocks. You'll need to earn those. By, you know, not doing this."
        ).random()

        PercyDialogue.UserWalkedAway -> listOf(
            "Good. That was the right call.",
            "Look at you. Walking away. Genuinely proud.",
            "Discipline. It's attractive. Well done."
        ).random()

        is PercyDialogue.NextDayClean -> listOf(
            "Yesterday: clean. Streak: ${dialogue.streak} day${if (dialogue.streak != 1) "s" else ""}. Percy approves.",
            "Not a single extension yesterday. ${dialogue.streak} days running. Keep it going.",
            "You did it yesterday. ${dialogue.streak}-day streak. I'll note that in your file."
        ).random()

        is PercyDialogue.NextDayHadExtensions -> when (dialogue.streak) {
            0 -> listOf(
                "Yesterday had some extensions. Streak reset. Today is a new opportunity to not do that.",
                "Streak broken. It happens. Start again today. I'll be watching.",
                "Extensions happened. No streak. Fresh start. You know what to do."
            ).random()
            else -> listOf(
                "Extensions yesterday, but your streak was ${dialogue.streak} days. Not bad. Reset. Try again.",
                "Streak of ${dialogue.streak} ended. Don't let that stop you from starting a new one."
            ).random()
        }

        is PercyDialogue.StreakMilestone -> listOf(
            "${dialogue.days} days clean. That's not nothing. That's actually something.",
            "${dialogue.days}-day streak. Percy is begrudgingly impressed.",
            "You've been clean for ${dialogue.days} days. I've updated your file. Favorably."
        ).random()

        is PercyDialogue.AchievementUnlocked -> listOf(
            "Achievement unlocked: ${dialogue.title}. +${dialogue.bocksAwarded} Bocks. You've earned that.",
            "New achievement: ${dialogue.title}. ${dialogue.bocksAwarded} Bocks added to your account.",
            "Percy notes: ${dialogue.title} achieved. ${dialogue.bocksAwarded} Bocks. Well done."
        ).random()

        PercyDialogue.Idle -> ""
    }
}

