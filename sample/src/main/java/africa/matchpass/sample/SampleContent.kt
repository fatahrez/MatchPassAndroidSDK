package africa.matchpass.sample

import africa.matchpass.sdk.ContentType
import africa.matchpass.sdk.MatchPassContent
import androidx.compose.ui.graphics.Color

data class SampleContent(
    val passContent: MatchPassContent,
    val subtitle: String,
    val isLive: Boolean = false,
    val bgStart: Color,
    val bgEnd: Color,
    val emoji: String,
)

// Seeded content — IDs match external_id values in MatchPassAPI/seed_demo.py
val LIVE_SPORT = listOf(
    SampleContent(
        passContent = MatchPassContent(
            id = "epl-match-001",
            title = "Arsenal vs Manchester City",
            price = "29.00",
            currency = "ZAR",
            durationHours = 4,
            contentType = ContentType.MATCH,
        ),
        subtitle = "English Premier League",
        isLive = true,
        bgStart = Color(0xFF0D1B4B),
        bgEnd = Color(0xFF9B1C1C),
        emoji = "⚽",
    ),
    SampleContent(
        passContent = MatchPassContent(
            id = "ucl-match-001",
            title = "Real Madrid vs Bayern Munich",
            price = "35.00",
            currency = "ZAR",
            durationHours = 4,
            contentType = ContentType.MATCH,
        ),
        subtitle = "UEFA Champions League · 21:00",
        isLive = false,
        bgStart = Color(0xFF1A0533),
        bgEnd = Color(0xFF153370),
        emoji = "🏆",
    ),
    SampleContent(
        passContent = MatchPassContent(
            id = "psl-match-001",
            title = "Kaizer Chiefs vs Orlando Pirates",
            price = "25.00",
            currency = "ZAR",
            durationHours = 3,
            contentType = ContentType.MATCH,
        ),
        subtitle = "PSL · Soweto Derby · 15:30",
        isLive = false,
        bgStart = Color(0xFF1A1000),
        bgEnd = Color(0xFF2D1800),
        emoji = "⚽",
    ),
    SampleContent(
        passContent = MatchPassContent(
            id = "boxing-001",
            title = "Riyadh Season Boxing",
            price = "49.00",
            currency = "ZAR",
            durationHours = 6,
            contentType = ContentType.MATCH,
        ),
        subtitle = "Main Event · Tonight 22:00",
        isLive = false,
        bgStart = Color(0xFF3B0A0A),
        bgEnd = Color(0xFF0D0D0D),
        emoji = "🥊",
    ),
)

val MOVIES = listOf(
    SampleContent(
        passContent = MatchPassContent(
            id = "movie-001",
            title = "The Woman King",
            price = "35.00",
            currency = "ZAR",
            durationHours = 2,
            contentType = ContentType.MOVIE,
        ),
        subtitle = "2022 · Action / History · 2h 15m",
        isLive = false,
        bgStart = Color(0xFF2D0A3D),
        bgEnd = Color(0xFF0A1A0A),
        emoji = "🎬",
    ),
)

val SERIES = listOf(
    SampleContent(
        passContent = MatchPassContent(
            id = "series-s1-001",
            title = "Shaka Ilembe",
            price = "59.00",
            currency = "ZAR",
            durationHours = 720,
            contentType = ContentType.SEASON,
        ),
        subtitle = "Season 1 · 10 Episodes · Historical Drama",
        isLive = false,
        bgStart = Color(0xFF0A2D0A),
        bgEnd = Color(0xFF1A3010),
        emoji = "📺",
    ),
)

val ALL_CONTENT = LIVE_SPORT + MOVIES + SERIES
