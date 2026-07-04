package africa.matchpass.sample

import africa.matchpass.sdk.ContentCategory
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
    val channelNumber: String? = null,
    val onNow: String? = null,
    val kickoffLabel: String? = null,  // e.g. "TODAY · 15:00", "Thu 3 Jul · 15:30"
)

// Produces IDs like: arsenalmancity01jul26
// home/away = slug-friendly names, day/mon/yr = kickoff date parts
fun matchId(home: String, away: String, day: Int, mon: String, yr: String): String {
    val h = home.lowercase().filter { it.isLetter() }
    val a = away.lowercase().filter { it.isLetter() }
    val d = day.toString().padStart(2, '0')
    return "$h$a$d${mon.lowercase()}$yr"
}

// ── Channels (Broadcasting — open pricing) ────────────────────────────────────
// Channels repeat daily — no date stamp needed, just a stable channel ID.

val CHANNELS = listOf(
    SampleContent(
        passContent = MatchPassContent(
            id = "ch-supersport1",
            title = "SuperSport 1",
            price = "150",
            currency = "KSh",
            durationHours = 24,
            contentType = ContentType.CHANNEL,
            category = ContentCategory.BROADCASTING,
        ),
        subtitle = "Live Sport",
        isLive = true,
        bgStart = Color(0xFF001E62),
        bgEnd = Color(0xFF003A9E),
        emoji = "🏆",
        channelNumber = "201",
        onNow = "UFC Fight Night",
    ),
    SampleContent(
        passContent = MatchPassContent(
            id = "ch-supersport2",
            title = "SuperSport 2",
            price = "150",
            currency = "KSh",
            durationHours = 24,
            contentType = ContentType.CHANNEL,
            category = ContentCategory.BROADCASTING,
        ),
        subtitle = "Football",
        isLive = true,
        bgStart = Color(0xFF002244),
        bgEnd = Color(0xFF004488),
        emoji = "⚽",
        channelNumber = "202",
        onNow = "Premier League Review",
    ),
    SampleContent(
        passContent = MatchPassContent(
            id = "ch-mnet",
            title = "M-Net",
            price = "200",
            currency = "KSh",
            durationHours = 24,
            contentType = ContentType.CHANNEL,
            category = ContentCategory.ENTERTAINMENT,
        ),
        subtitle = "Entertainment",
        isLive = true,
        bgStart = Color(0xFF1A0A2E),
        bgEnd = Color(0xFF3D1A6E),
        emoji = "🎬",
        channelNumber = "101",
        onNow = "The Crown S4 E6",
    ),
    SampleContent(
        passContent = MatchPassContent(
            id = "ch-channel-o",
            title = "Channel O",
            price = "80",
            currency = "KSh",
            durationHours = 24,
            contentType = ContentType.CHANNEL,
            category = ContentCategory.ENTERTAINMENT,
        ),
        subtitle = "Music",
        isLive = true,
        bgStart = Color(0xFF2D0A3D),
        bgEnd = Color(0xFF5A1A7A),
        emoji = "🎵",
        channelNumber = "320",
        onNow = "Top 10 Afrobeats",
    ),
    SampleContent(
        passContent = MatchPassContent(
            id = "ch-cnn",
            title = "CNN International",
            price = "120",
            currency = "KSh",
            durationHours = 24,
            contentType = ContentType.CHANNEL,
            category = ContentCategory.BROADCASTING,
        ),
        subtitle = "News",
        isLive = true,
        bgStart = Color(0xFF9B1C1C),
        bgEnd = Color(0xFF5C0000),
        emoji = "📺",
        channelNumber = "401",
        onNow = "World Business Today",
    ),
)

// ── Live Sport (Sports — fixed KES 50) ───────────────────────────────────────
// Platform policy: all sports content is fixed at KES 50 to maximise viewership.
// IDs encode the fixture + date so a 2027 replay is a distinct content item.

val LIVE_SPORT = listOf(
    SampleContent(
        passContent = MatchPassContent(
            id = matchId("arsenal", "mancity", 1, "jul", "26"),
            title = "Arsenal vs Manchester City",
            price = "50",
            currency = "KSh",
            durationHours = 4,
            contentType = ContentType.MATCH,
            category = ContentCategory.SPORTS,
        ),
        subtitle = "English Premier League",
        isLive = true,
        bgStart = Color(0xFF0D1B4B),
        bgEnd = Color(0xFF9B1C1C),
        emoji = "⚽",
        kickoffLabel = "TODAY · LIVE NOW",
    ),
    SampleContent(
        passContent = MatchPassContent(
            id = matchId("realmadrid", "bayernmunich", 2, "jul", "26"),
            title = "Real Madrid vs Bayern Munich",
            price = "50",
            currency = "KSh",
            durationHours = 4,
            contentType = ContentType.MATCH,
            category = ContentCategory.SPORTS,
        ),
        subtitle = "UEFA Champions League",
        isLive = false,
        bgStart = Color(0xFF1A0533),
        bgEnd = Color(0xFF153370),
        emoji = "🏆",
        kickoffLabel = "Thu 2 Jul · 21:00",
    ),
    SampleContent(
        passContent = MatchPassContent(
            id = matchId("chiefs", "pirates", 3, "jul", "26"),
            title = "Kaizer Chiefs vs Orlando Pirates",
            price = "50",
            currency = "KSh",
            durationHours = 3,
            contentType = ContentType.MATCH,
            category = ContentCategory.SPORTS,
        ),
        subtitle = "PSL · Soweto Derby",
        isLive = false,
        bgStart = Color(0xFF1A1000),
        bgEnd = Color(0xFF2D1800),
        emoji = "⚽",
        kickoffLabel = "Fri 3 Jul · 15:30",
    ),
    SampleContent(
        passContent = MatchPassContent(
            id = matchId("southafrica", "nigeria", 4, "jul", "26"),
            title = "South Africa vs Nigeria",
            price = "50",
            currency = "KSh",
            durationHours = 3,
            contentType = ContentType.MATCH,
            category = ContentCategory.SPORTS,
        ),
        subtitle = "AFCON Qualifier",
        isLive = false,
        bgStart = Color(0xFF003300),
        bgEnd = Color(0xFF006600),
        emoji = "🌍",
        kickoffLabel = "Sat 4 Jul · 18:00",
    ),
    SampleContent(
        passContent = MatchPassContent(
            id = matchId("boxing", "riyadhseason", 5, "jul", "26"),
            title = "Riyadh Season Boxing",
            price = "50",
            currency = "KSh",
            durationHours = 6,
            contentType = ContentType.MATCH,
            category = ContentCategory.SPORTS,
        ),
        subtitle = "Main Event · PPV",
        isLive = false,
        bgStart = Color(0xFF3B0A0A),
        bgEnd = Color(0xFF0D0D0D),
        emoji = "🥊",
        kickoffLabel = "Sun 5 Jul · 22:00",
    ),
)

// ── Movies (Entertainment — KES 200–300) ─────────────────────────────────────

val MOVIES = listOf(
    SampleContent(
        passContent = MatchPassContent(
            id = "movie-the-woman-king",
            title = "The Woman King",
            price = "250",
            currency = "KSh",
            durationHours = 72,
            contentType = ContentType.MOVIE,
            category = ContentCategory.ENTERTAINMENT,
        ),
        subtitle = "2022 · Action / History · 2h 15m",
        isLive = false,
        bgStart = Color(0xFF2D0A3D),
        bgEnd = Color(0xFF0A1A0A),
        emoji = "🎬",
    ),
    SampleContent(
        passContent = MatchPassContent(
            id = "movie-black-panther",
            title = "Black Panther: Wakanda Forever",
            price = "299",
            currency = "KSh",
            durationHours = 72,
            contentType = ContentType.MOVIE,
            category = ContentCategory.ENTERTAINMENT,
        ),
        subtitle = "2022 · Action / Adventure · 2h 41m",
        isLive = false,
        bgStart = Color(0xFF0A0A2D),
        bgEnd = Color(0xFF1A0A3D),
        emoji = "🦅",
    ),
)

// ── Series (Entertainment — KES 200–300 per season) ──────────────────────────

val SERIES = listOf(
    SampleContent(
        passContent = MatchPassContent(
            id = "series-shaka-ilembe-s1",
            title = "Shaka Ilembe",
            price = "250",
            currency = "KSh",
            durationHours = 720,
            contentType = ContentType.SEASON,
            category = ContentCategory.ENTERTAINMENT,
        ),
        subtitle = "Season 1 · 10 Episodes · Historical Drama",
        isLive = false,
        bgStart = Color(0xFF0A2D0A),
        bgEnd = Color(0xFF1A3010),
        emoji = "📺",
    ),
    SampleContent(
        passContent = MatchPassContent(
            id = "series-those-who-remain-s1",
            title = "Those Who Remain",
            price = "200",
            currency = "KSh",
            durationHours = 720,
            contentType = ContentType.SEASON,
            category = ContentCategory.ENTERTAINMENT,
        ),
        subtitle = "Season 1 · 8 Episodes · Crime Thriller",
        isLive = false,
        bgStart = Color(0xFF1A0A0A),
        bgEnd = Color(0xFF2D1010),
        emoji = "🔍",
    ),
)

// ── Special Events (Entertainment — KES 200–300) ──────────────────────────────
// Comedy nights, concerts, live shows — use ContentType.EVENT

val EVENTS = listOf(
    SampleContent(
        passContent = MatchPassContent(
            id = "event-eric-omondi-live-jul26",
            title = "Eric Omondi: Live & Unfiltered",
            price = "200",
            currency = "KSh",
            durationHours = 4,
            contentType = ContentType.EVENT,
            category = ContentCategory.ENTERTAINMENT,
        ),
        subtitle = "Live Comedy · Nairobi",
        isLive = false,
        bgStart = Color(0xFF1A0A2E),
        bgEnd = Color(0xFF3D1A6E),
        emoji = "😂",
        kickoffLabel = "Sat 5 Jul · 20:00",
    ),
    SampleContent(
        passContent = MatchPassContent(
            id = "event-churchill-show-jul26",
            title = "Churchill Show Live",
            price = "250",
            currency = "KSh",
            durationHours = 5,
            contentType = ContentType.EVENT,
            category = ContentCategory.ENTERTAINMENT,
        ),
        subtitle = "Live Comedy · KICC Nairobi",
        isLive = false,
        bgStart = Color(0xFF0A1A2E),
        bgEnd = Color(0xFF1A3D6E),
        emoji = "🎤",
        kickoffLabel = "Fri 10 Jul · 19:30",
    ),
)

val ALL_CONTENT = CHANNELS + LIVE_SPORT + MOVIES + SERIES + EVENTS
