package `in`.hiddenindia.app.data

import `in`.hiddenindia.app.network.AiChatHistoryMessage
import `in`.hiddenindia.app.network.AiDayPlan
import `in`.hiddenindia.app.network.AiItineraryItem
import `in`.hiddenindia.app.network.PlanTripResponse

/**
 * Explicit mapping from backend/server.ts's POST /api/ai/plan-trip response
 * shape (PlanTripResponse) into the app's existing GeneratedItinerary UI
 * model, which predates this backend integration and already drives
 * ItineraryResultScreen, ProfileScreen's saved-itinerary list, and the
 * Stage 4 POST /api/auth/itineraries save flow. Field names differ between
 * the two (see AiModels.kt's PlanTripResponse doc comment) — this function
 * is the one place that reconciles them, rather than changing either the
 * backend contract or the existing itinerary UI.
 */
fun PlanTripResponse.toGeneratedItinerary(): GeneratedItinerary {
    return GeneratedItinerary(
        id = id,
        destinationId = destinationId,
        destinationName = destinationName,
        state = state,
        days = days,
        budgetStyle = budgetStyle,
        interests = interests,
        totalCost = totalEstimatedCost,
        summary = summary,
        dayPlans = dayPlans.map { it.toDayItinerary() }
    )
}

private fun AiDayPlan.toDayItinerary(): DayItinerary {
    val resolvedTheme = when {
        !title.isNullOrBlank() -> title
        !theme.isNullOrBlank() -> theme
        else -> "Day $dayNumber"
    }
    return DayItinerary(
        dayNumber = dayNumber,
        theme = resolvedTheme,
        activities = items.map { it.toItineraryActivity() }
    )
}

private fun AiItineraryItem.toItineraryActivity(): ItineraryActivity {
    // The backend doesn't return a separate "insider tip" per item — its
    // closest real equivalent is the duration/travel-distance metadata, so
    // that's what's surfaced here rather than inventing tip text.
    val tip = listOfNotNull(
        duration?.takeIf { it.isNotBlank() },
        travelDistance?.takeIf { it.isNotBlank() }
    ).joinToString(" • ")

    return ItineraryActivity(
        timeSlot = time.orEmpty(),
        placeName = placeName.orEmpty(),
        activity = activity.orEmpty(),
        description = description.orEmpty(),
        cost = estimatedCost.orEmpty(),
        insiderTip = tip
    )
}

/** Maps existing ChatMessage history into the {sender: "user"|"ai", text} shape backend/server.ts expects. */
fun List<ChatMessage>.toAiChatHistory(): List<AiChatHistoryMessage> =
    mapNotNull { msg ->
        when (msg.sender) {
            MessageSender.USER -> AiChatHistoryMessage(sender = "user", text = msg.text)
            MessageSender.AI -> AiChatHistoryMessage(sender = "ai", text = msg.text)
            MessageSender.SYSTEM -> null // backend only recognizes 'user' and 'ai' roles
        }
    }
