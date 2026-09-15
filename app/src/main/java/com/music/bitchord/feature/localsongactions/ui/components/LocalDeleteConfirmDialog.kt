package com.music.bitchord.feature.localsongactions.ui.components

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.music.bitchord.data.model.Song
import java.io.File

/**
 * Confirmation dialog and MediaStore delete request launcher for deleting a song from device.
 */
@Composable
fun LocalDeleteConfirmDialog(
    song: Song,
    onDismissRequest: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var launchedRRequest by remember { mutableStateOf(false) }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, "Deleted ${song.title}", Toast.LENGTH_SHORT).show()
            onDeleted()
        }
        onDismissRequest()
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        LaunchedEffect(song) {
            if (!launchedRRequest) {
                launchedRRequest = true
                val uri = song.localUri?.let { Uri.parse(it) }
                if (uri != null) {
                    try {
                        val pendingIntent = MediaStore.createDeleteRequest(
                            context.contentResolver,
                            listOf(uri),
                        )
                        deleteLauncher.launch(
                            IntentSenderRequest.Builder(pendingIntent.intentSender).build(),
                        )
                    } catch (t: Throwable) {
                        Toast.makeText(context, "Error deleting song: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                        onDismissRequest()
                    }
                } else {
                    onDismissRequest()
                }
            }
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = {
                Text(text = "Delete from device")
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete \"${song.title}\" from your device? This action cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        var success = false
                        try {
                            if (!song.localUri.isNullOrBlank()) {
                                val deletedRows = context.contentResolver.delete(
                                    Uri.parse(song.localUri),
                                    null,
                                    null,
                                )
                                success = deletedRows > 0
                            }
                            if (!success && !song.localPath.isNullOrBlank()) {
                                val file = File(song.localPath)
                                if (file.exists()) {
                                    success = file.delete()
                                }
                            }
                        } catch (t: Throwable) {
                            success = false
                        }

                        if (success) {
                            Toast.makeText(context, "Deleted ${song.title}", Toast.LENGTH_SHORT).show()
                            onDeleted()
                        } else {
                            Toast.makeText(context, "Could not delete file", Toast.LENGTH_SHORT).show()
                        }
                        onDismissRequest()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }
            },
            modifier = modifier,
        )
    }
}
