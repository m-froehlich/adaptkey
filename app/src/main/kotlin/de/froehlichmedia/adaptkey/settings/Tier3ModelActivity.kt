package de.froehlichmedia.adaptkey.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import de.froehlichmedia.adaptkey.R
import de.froehlichmedia.adaptkey.prediction.onnx.Tier3ModelInstaller
import de.froehlichmedia.adaptkey.prediction.onnx.Tier3ModelStorage

/**
 * Model-import screen for the optional tier-3 mini-LLM (§9 / C-06).
 *
 * The app has no internet permission, so the network step is delegated to the browser: "download" opens
 * the model URL, the user's browser fetches the file, and "import" then picks it with the system file
 * picker (SAF — no storage permission) and copies it into app-private storage via [Tier3ModelInstaller].
 * The large copy runs off the UI thread. Like the other Android-facing layers it is covered by
 * instrumented rather than unit tests; the copy/validation logic it delegates to is unit-tested.
 */
class Tier3ModelActivity : AppCompatActivity() {
    
    private lateinit var statusView: TextView
    private lateinit var downloadButton: Button
    private lateinit var importButton: Button
    private lateinit var removeButton: Button
    private var busy = false
    
    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importModel(uri)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tier3_model)
        title = getString(R.string.c06_model_title)
        
        statusView = findViewById(R.id.tier3_status)
        downloadButton = findViewById(R.id.tier3_download)
        importButton = findViewById(R.id.tier3_import)
        removeButton = findViewById(R.id.tier3_remove)
        
        downloadButton.setOnClickListener { openDownloadPage() }
        importButton.setOnClickListener { openDocument.launch(arrayOf("*/*")) }
        removeButton.setOnClickListener { removeModel() }
        
        refresh()
    }
    
    private fun openDownloadPage() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(MODEL_URL))
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.c06_model_no_browser, Toast.LENGTH_LONG).show()
        }
    }
    
    private fun importModel(uri: Uri) {
        if (busy) {
            return
        }
        setBusy(true)
        statusView.setText(R.string.c06_model_importing)
        Thread {
            val result = runCatching {
                contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "cannot open $uri" }
                    Tier3ModelInstaller.install(input, Tier3ModelStorage.modelDir(this))
                }
            }
            runOnUiThread {
                setBusy(false)
                val message = if (result.isSuccess) R.string.c06_model_imported else R.string.c06_model_import_failed
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                refresh()
            }
        }.start()
    }
    
    private fun removeModel() {
        Tier3ModelInstaller.clear(Tier3ModelStorage.modelDir(this))
        Toast.makeText(this, R.string.c06_model_removed, Toast.LENGTH_SHORT).show()
        refresh()
    }
    
    private fun setBusy(value: Boolean) {
        busy = value
        downloadButton.isEnabled = !value
        importButton.isEnabled = !value
        removeButton.isEnabled = !value
    }
    
    private fun refresh() {
        val installed = Tier3ModelStorage.isModelInstalled(this)
        statusView.text = getString(if (installed) R.string.c06_model_present else R.string.c06_model_absent)
        removeButton.isEnabled = installed && !busy
    }
    
    companion object {
        
        // Public, stable Hugging Face URL of the quantised SmolLM2-360M model graph (Apache-2.0).
        private const val MODEL_URL =
            "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct/resolve/main/onnx/model_q4f16.onnx?download=true"
    }
}
