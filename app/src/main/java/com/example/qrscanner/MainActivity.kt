package com.example.qrscanner

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.zxing.integration.android.IntentIntegrator
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    private var scannedContent by mutableStateOf<String?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CenteredButton { initScanner() }
            ScanResultScreen(scannedContent=scannedContent)
            SendExcelButton(onConfirm = { context ->
                sendFileToGmail(
                    context = context,
                    filePath = "/storage/emulated/0/Documents/Reportes/Visitas.csv",
                    email = "yossef3221@gmail.com",
                    subject = "Reporte",
                    body = "Reporte de tiendas visitadas",
                )
                 //restartApp(context = this)
            })
        }
    }

    private fun restartApp(context: Context) {
        val filePath = "/storage/emulated/0/Documents/Reportes/Visitas.csv" // Ruta fija
        val file = File(filePath)
        val message: String
        if (file.exists()) {
            val wasDeleted = file.delete() // Elimina el archivo
            message = if (wasDeleted) {
                "Archivo eliminado exitosamente"
            } else {
                "No se pudo eliminar el archivo"
            }
        } else {
            message = "No se encontró el archivo"
        }

        // Muestra el mensaje de estado
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

        // Reinicia la aplicación
        val restartIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        restartIntent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(restartIntent)
    }

    private fun initScanner() {
        val integrator =IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Escanea un código QR")
        integrator.setBeepEnabled(true)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Cancelado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "El valor escaneado es: ${result.contents}",
                    Toast.LENGTH_SHORT
                ).show()
                guardarInfo(info = result.contents)
                scannedContent = result.contents
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    @Composable
    fun ScanResultScreen(scannedContent: String?) {
        val scanList = remember { mutableStateListOf<String>() }
        LaunchedEffect(scannedContent) {
            scannedContent?.let {
                if (!scanList.contains(it)) { // Evita duplicados si es necesario
                    scanList.add(it)

                }
            }
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn {
                items(scanList) { item ->
                    Text(
                        text = item,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

    }

    @Preview(showBackground = true)
    @Composable
    fun ScanResultScreenPreview() {
        ScanResultScreen(scannedContent = null)
    }
}
@Composable
fun CenteredButton(onScanClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Button(onClick = {
            onScanClick()
        }) {
            Text("Escanear código")
        }
    }
}


@Composable
fun SendExcelButton(onConfirm: (Context) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var showMessage by remember { mutableStateOf(false) } // Estado para controlar el mensaje
    val context = LocalContext.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter // Ubica el botón abajo en el centro
    ) {
        // Botón principal
        Button(onClick = { showDialog = true }) {
            Text("Enviar información")
        }

        // Mostrar mensaje después de la confirmación
        if (showMessage) {
            Text(
                text = "Los datos fueron enviados correctamente",
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }

    // Diálogo de confirmación
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(text = "Confirmación")
            },
            text = {
                Text("¿Desea enviar la información?")
            },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    showMessage = true // Mostrar el mensaje
                    onConfirm(context) // Ejecutar el comando que se pase como parámetro
                }) {
                    Text("Sí")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                }) {
                    Text("No")
                }
            }
        )
    }

    // Ocultar el mensaje automáticamente después de 3 segundos
    if (showMessage) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(3000) // Espera 3 segundos
            showMessage = false // Oculta el mensaje
        }
    }
}

fun sendFileToGmail(context: Context, filePath: String, email: String, subject: String, body: String) {
    val file = File(filePath)
    if (file.exists() && file.length() > 0) {
        try {
            // Crear la URI del archivo
            val uri = FileProvider.getUriForFile(
                context,
                "com.example.qrscanner.provider", // Autoridad definida en el Manifest
                file
            )

            // Crear el Intent para Gmail
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822" // Tipo MIME para correos electrónicos
                putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooserIntent = Intent.createChooser(intent, "Selecciona un cliente de correo")

            // Verificar si Gmail está disponible y lanzar el Intent
            try {
                context.startActivity(chooserIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Gmail no está instalado o no disponible", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error al enviar el archivo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "Archivo no disponible para enviar", Toast.LENGTH_SHORT).show()
    }
}

fun guardarInfo(
    info: String
){

    val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

    val folder = File(path, Constantes.NOM_FOLDER)

    if(!folder.exists()){
        folder.mkdirs()
    }

    val csvFile = File(folder, Constantes.NOM_FILE)
    if (!csvFile.exists()) {
        csvFile.createNewFile()

        val header = "Código de Cliente, Razón Social\n"
        FileOutputStream(csvFile, true).use { fos ->
            fos.bufferedWriter().use {
                it.append(header)
            }
        }
    }

    val existingData = mutableSetOf<String>()
    csvFile.forEachLine { line ->
        existingData.add(line.split(",")[0]) // Assuming "Código de Cliente" is the first column
    }

    FileOutputStream(csvFile, true).use { fos ->
        fos.bufferedWriter().use { writer ->
            val parts = info.trim().split(" ", limit = 3) // Split info into 3 parts
            if (parts.size == 3) {
                val codigoCliente = parts[0]
                if (codigoCliente !in existingData) {
                    val remainingData = info.substring(codigoCliente.length + 1).trim() // Remove codigoCliente and leading/trailing spaces
                    val razonSocialParts = remainingData.split("        ", limit = 2) // Split by 8 spaces

                    val razonSocial = if (razonSocialParts.isNotEmpty()) razonSocialParts[0].trim() else ""

                    // Write only codigoCliente and razonSocial to the CSV file
                    val formattedData = "$codigoCliente,$razonSocial\n"
                    writer.append(formattedData)
                    existingData.add(codigoCliente)
                }
            } else {
                // Handle cases where the data is not in the expected format
                println("Invalid data format: $info")
            }
        }
    }
}
