package de.froehlichmedia.adaptkey.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.froehlichmedia.adaptkey.R

/**
 * Settings entry point (scaffold stub).
 *
 * Currently only hosts a placeholder layout and serves as both the launcher activity and the
 * IME settings activity referenced from {@code res/xml/method.xml}. The configurable parameters
 * C-01 … C-09 will be implemented in a dedicated later session.
 */
class SettingsActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
    }
}
