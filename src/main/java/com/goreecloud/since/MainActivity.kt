package com.goreecloud.since

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.goreecloud.since.ui.SinceApp
import com.goreecloud.since.ui.theme.SinceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SinceTheme {
                SinceApp()
            }
        }
    }
}
