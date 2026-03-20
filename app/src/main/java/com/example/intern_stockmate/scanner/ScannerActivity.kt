package com.example.intern_stockmate.scanner

import android.content.pm.ActivityInfo
import com.journeyapps.barcodescanner.CaptureActivity

class ScannerActivity : CaptureActivity() {
    override fun onResume() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onResume()
    }
}