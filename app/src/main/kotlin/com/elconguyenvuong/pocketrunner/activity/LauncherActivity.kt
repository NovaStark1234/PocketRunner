package com.elconguyenvuong.pocketrunner.activity

import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AppCompatActivity
import com.elconguyenvuong.pocketrunner.R
import kotlinx.android.synthetic.main.activity_launcher.*
import android.content.Intent
import com.elconguyenvuong.pocketrunner.activity.SplashScreenActivity

class LauncherActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        text.movementMethod = LinkMovementMethod.getInstance()

        val arch = System.getProperty("os.arch") ?: "7"
        if (!arch.contains("aarch64") && !arch.contains("8")) {
            text.text = getString(R.string.error_32bit)
            return
        }

        val isInitialized = getSharedPreferences("launcher", 0).getBoolean("initialized", false)
        replaceActivity(
                if (isInitialized)
                    SplashScreenActivity::class.java
                else
                    IntroActivity::class.java
        )
    }

    private fun <T> replaceActivity(activity: Class<T>) {
        val intent = Intent(this, activity)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME
        startActivity(intent)
        this.finish()
    }
}
