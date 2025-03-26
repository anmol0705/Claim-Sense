package android.saswat.dashcamapplication

import android.app.Application
import com.google.firebase.FirebaseApp

class DashCamApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase at application startup
        FirebaseApp.initializeApp(this)
    }
}