package com.elconguyenvuong.pocketrunner.activity

import com.elconguyenvuong.pocketrunner.activity.home.MainActivity
import com.elconguyenvuong.pocketrunner.R
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.view.WindowManager
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
 
@Suppress("DEPRECATION")
class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        val backgroundImage : ImageView = findViewById(R.id.splash_logo_icon)
        val bottomUpAnimation = AnimationUtils.loadAnimation(this, R.anim.bottom_up)
        backgroundImage.startAnimation(bottomUpAnimation)

        Handler(Looper.getMainLooper()).postDelayed({
            val splashAppName : TextView = findViewById(R.id.splash_app_name)
            splashAppName.text = "Pocket Runner"
        }, 1000)

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }
}
