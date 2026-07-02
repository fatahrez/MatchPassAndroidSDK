package africa.matchpass.sample

import android.app.Application
import africa.matchpass.sdk.MatchPassConfig
import africa.matchpass.sdk.MatchPassSDK

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MatchPassSDK.init(
            context = this,
            config = MatchPassConfig(
                apiKey = "6bb774af7ab3181a7cc7f00e2e020d112934ae3de787beb64355ac1dc4740c83",
                baseUrl = "https://staging.api.matchpass.africa/api/v1/",
                debug = true,
            ),
        )
//        MatchPassSDK.Builder(this)
//            .apiKey("6bb774af7ab3181a7cc7f00e2e020d112934ae3de787beb64355ac1dc4740c83")   // from dashboard.matchpass.africa
//            .debug(true)
//            .initialize()
    }
}
