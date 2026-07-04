package africa.matchpass.sdk

/**
 * Semantic type of content being sold. The SDK selects an appropriate [PassPolicy]
 * for each type — you rarely need to configure the policy manually.
 */
enum class ContentType {

    /**
     * A single live match or sporting event.
     * Pass is valid for the event window only (typically 4–6 hours).
     * Validation is checked frequently because the content is live.
     */
    MATCH,

    /**
     * Continuous access to a broadcast channel for a fixed time window.
     * Validation is checked on every open because the content is streaming live.
     */
    CHANNEL,

    /**
     * Ownership of a full TV season. User can rewatch any episode freely.
     * Validation is cached for 24 hours — one server round-trip per day.
     */
    SEASON,

    /**
     * Perpetual movie ownership. Once purchased, the user can rewatch anytime.
     * Validation is cached for 30 days — effectively permanent once purchased.
     */
    MOVIE,

    /**
     * A ticketed special event — comedy night, concert, graduation, etc.
     * Treated like a MATCH for validation purposes (live window, frequent checks).
     */
    EVENT,
}
