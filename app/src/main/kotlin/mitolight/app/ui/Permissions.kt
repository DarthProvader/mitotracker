package mitolight.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mitolight.app.R

/**
 * Scanning needs different permissions either side of Android 12: the newer Bluetooth ones, or
 * fine location before that, because a scan used to be able to reveal where you are.
 */
private fun requiredPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

/**
 * Asked for at the same time as the rest, because the notification is where the remaining time is
 * read from once the phone is in a pocket. Refusing it costs the notification, not the app, so it
 * must not be allowed to hold the whole screen hostage.
 */
private fun optionalPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyArray()
    }

@Composable
fun BluetoothPermissions(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val required = remember { requiredPermissions() }
    val requested = remember { required + optionalPermissions() }

    var granted by remember {
        mutableStateOf(
            required.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED },
        )
    }

    val request = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { outcome ->
        granted = required.all { outcome[it] == true }
    }

    LaunchedEffect(Unit) {
        // Asked whenever anything is missing, not only when the app is unusable: the Bluetooth
        // permissions are often already granted from an earlier run, and that must not be the reason
        // the notification is never offered.
        val missing = requested.any { context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing) request.launch(requested)
    }

    if (granted) {
        content()
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.permission_bluetooth_needed),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.permission_simulator_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = { request.launch(requested) }) {
            Text(stringResource(R.string.action_allow))
        }
    }
}
