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
                apiKey = "01e2d4474870b3604973b4fd022df6a3aadde5b6b82dfadcd186b25f19694a6a",
                baseUrl = "https://staging.api.matchpass.africa/api/v1/",
                debug = true,
            ),
        )
    }
}
