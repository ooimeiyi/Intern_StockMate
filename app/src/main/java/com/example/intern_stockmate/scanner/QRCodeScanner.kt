package com.example.intern_stockmate.scanner

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@Composable
fun QRCodeScanner(
    scanning: Boolean,
    onResult: (String) -> Unit,
    onScanFinished: () -> Unit
) {

    val launcher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            onResult(result.contents)
        }
        onScanFinished()
    }

    LaunchedEffect(scanning) {
        if (scanning) {
            val options = ScanOptions().apply {
                setPrompt("Scan QR code")
                setBeepEnabled(true)
                setOrientationLocked(true) // 🔹 force portrait
                setCaptureActivity(ScannerActivity::class.java) // optional
            }
            launcher.launch(options)
        }
    }
}


