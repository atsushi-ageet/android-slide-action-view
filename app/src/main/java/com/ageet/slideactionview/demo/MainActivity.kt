package com.ageet.slideactionview.demo

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ageet.slideactionview.SlideActionView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private fun getView(): View? {
        return findViewById(android.R.id.content)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SlideActionView.setDebugMode(true)
        setContentView(R.layout.activity_main)
        slideActionView.setLeftSliderListener {
            Toast.makeText(this, "left slider action", Toast.LENGTH_SHORT).show()
        }
        slideActionView.setRightSliderListener {
            Toast.makeText(this, "right slider action", Toast.LENGTH_SHORT).show()
        }
        resetButton.setOnClickListener {
            Toast.makeText(this, "reset slider", Toast.LENGTH_SHORT).show()
            slideActionView.reset()
        }
    }

    companion object {
        private const val LOG_TAG: String = "MainActivity"
    }
}
