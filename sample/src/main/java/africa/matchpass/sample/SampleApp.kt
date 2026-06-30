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
                apiKey = "your-operator-api-key",
                debug = true,
            ),
        )
    }
}
