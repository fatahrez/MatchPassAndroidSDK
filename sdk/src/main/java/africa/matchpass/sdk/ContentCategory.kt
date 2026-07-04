package africa.matchpass.sdk

/**
 * Broad category of content — maps to the platform pricing policy taxonomy.
 *
 * The platform enforces pricing rules per category:
 *  - [SPORTS]        Fixed price (KES 50). Set your content price to this value.
 *  - [ENTERTAINMENT] Price range (KES 200–300). Set within these bounds.
 *  - [EDUCATION]     Open pricing. Consult with MatchPass for guidance.
 *  - [BROADCASTING]  Open pricing. Consult with MatchPass for guidance.
 *
 * Pass [null] if the category is not applicable or not yet known.
 */
enum class ContentCategory {
    /** Live sports, matches, tournaments, boxing, MMA. */
    SPORTS,

    /** Films, comedy specials, creator PPV, music concerts, live entertainment. */
    ENTERTAINMENT,

    /** Graduation ceremonies, lectures, campus events, university streams. */
    EDUCATION,

    /** News channels, documentaries, broadcaster live streams. */
    BROADCASTING,
}
