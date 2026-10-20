package ai.copilotdrive.app

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    private val model: DashboardViewModel by viewModels()
    private val microphone = registerForActivityResult(ActivityResultContracts.RequestPermission()) { model.listen() }
    private val contacts = registerForActivityResult(ActivityResultContracts.RequestPermission()) { model.foreground() }
    private val bluetooth = registerForActivityResult(ActivityResultContracts.RequestPermission()) { model.refreshPairedBluetooth() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CopilotApp(model,
                onMicrophone = { microphone.launch(Manifest.permission.RECORD_AUDIO) },
                onContacts = { contacts.launch(Manifest.permission.READ_CONTACTS) },
                onBluetoothPermission = {
                    if (Build.VERSION.SDK_INT >= 31) bluetooth.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    else model.refreshPairedBluetooth()
                },
                onMediaPermission = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    if (intent.resolveActivity(packageManager) != null) startActivity(intent)
                },
            )
        }
    }
    override fun onStart() { super.onStart(); model.foreground() }
    override fun onStop() { model.background(); super.onStop() }
}
