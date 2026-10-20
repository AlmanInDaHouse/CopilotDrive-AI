package ai.copilotdrive.app.platform

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.net.toUri
import ai.copilotdrive.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidNavigation(private val context: Context) : NavigationProvider {
    private fun intent(destination: String, provider: String): Intent {
        require(provider in setOf("maps", "waze"))
        val uri = if (provider == "waze") NavigationUris.waze(destination) else NavigationUris.maps(destination)
        return Intent(Intent.ACTION_VIEW, uri.toUri()).setPackage(if (provider == "waze") "com.waze" else "com.google.android.apps.maps").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    fun available(provider: String) = intent("Madrid", provider).resolveActivity(context.packageManager) != null
    override suspend fun openDestination(destination: String, provider: String): ToolResult = withContext(Dispatchers.Main.immediate) {
        val primary = intent(destination, provider)
        val fallbackName = if (provider == "waze") "maps" else "waze"
        val chosen = if (primary.resolveActivity(context.packageManager) != null) primary else intent(destination, fallbackName)
        if (chosen.resolveActivity(context.packageManager) == null) return@withContext ToolResult.denied("navigation.open_destination", "NO_HANDLER", "Instala Google Maps o Waze para abrir destinos.")
        try {
            context.startActivity(chosen)
            val name = if (chosen.`package` == "com.waze") "Waze" else "Google Maps"
            ToolResult("navigation.open_destination", ExecutionStatus.SUCCESS, "Abierto $name con el destino. No puedo confirmar la ruta ni su tiempo de llegada.", result = mapOf("destination" to destination, "provider" to name))
        } catch (_: android.content.ActivityNotFoundException) {
            ToolResult.denied("navigation.open_destination", "NO_HANDLER", "El navegador ya no está disponible.")
        }
    }
}

class AndroidContacts(private val context: Context) : ContactProvider {
    fun permitted() = context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    fun dialerAvailable() = Intent(Intent.ACTION_DIAL, "tel:123".toUri()).resolveActivity(context.packageManager) != null
    override suspend fun search(name: String): List<Contact> = withContext(Dispatchers.IO) {
        if (!permitted()) throw SecurityException("contacts")
        require(name.isNotBlank() && name.length <= 200)
        val phone = ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI
        val uri = Uri.withAppendedPath(phone, Uri.encode(name))
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone._ID, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER)
        context.contentResolver.query(uri, projection, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")?.use { cursor ->
            buildList {
                while (cursor.moveToNext() && size < 5) {
                    val number = cursor.getString(2).orEmpty()
                    if (NavigationUris.validPhone(number)) add(Contact(cursor.getString(0), cursor.getString(1).orEmpty().take(80), number))
                }
            }
        } ?: emptyList()
    }
    override suspend fun openDialer(number: String): ToolResult = withContext(Dispatchers.Main.immediate) {
        require(NavigationUris.validPhone(number))
        val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) == null) return@withContext ToolResult.denied("phone.open_dialer", "NO_HANDLER", "No hay marcador disponible.")
        try {
            context.startActivity(intent)
            ToolResult("phone.open_dialer", ExecutionStatus.SUCCESS, "Marcador abierto. No se ha realizado ninguna llamada.")
        } catch (_: android.content.ActivityNotFoundException) {
            ToolResult.denied("phone.open_dialer", "NO_HANDLER", "Marcador no disponible.")
        }
    }
}
