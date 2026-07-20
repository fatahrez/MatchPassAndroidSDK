package africa.matchpass.sdk.internal

import africa.matchpass.sdk.MatchPassOwnedItem

/** Pages through `GET passes/` until `next` is null, returning every active pass for [userRef]. */
internal suspend fun fetchAllOwnedPasses(
    service: MatchPassService,
    apiKey: String,
    userRef: String,
): List<MatchPassOwnedItem> {
    val auth = "ApiKey $apiKey"
    val items = mutableListOf<MatchPassOwnedItem>()
    var page = 1
    while (true) {
        val resp = service.listPasses(auth, userRef, status = "active", page = page)
        items += resp.results.map { it.toOwnedItem() }
        if (resp.next == null) break
        page++
    }
    return items
}
