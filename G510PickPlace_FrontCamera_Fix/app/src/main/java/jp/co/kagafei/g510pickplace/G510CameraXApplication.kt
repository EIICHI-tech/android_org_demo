package jp.co.kagafei.g510pickplace

import android.app.Application
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig

class G510CameraXApplication : Application(), CameraXConfig.Provider {
    override fun getCameraXConfig(): CameraXConfig {
        Log.i("G510PickPlaceCamera", "Limit CameraX to G510 FRONT camera")
        return CameraXConfig.Builder
            .fromConfig(Camera2Config.defaultConfig())
            .setAvailableCamerasLimiter(CameraSelector.DEFAULT_FRONT_CAMERA)
            .build()
    }
}
