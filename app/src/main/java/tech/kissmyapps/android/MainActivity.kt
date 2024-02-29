package tech.kissmyapps.android

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = (applicationContext as Application).packageName
        })
    }
}