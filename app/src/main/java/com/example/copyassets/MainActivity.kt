package com.example.copyassets

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File

class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }
        val btn = Button(this).apply { text = "Fayllarni joylash" }
        status = TextView(this).apply { text = "Tayyor" }
        layout.addView(btn)
        layout.addView(status)
        setContentView(layout)

        if (!Environment.isExternalStorageManager()) {
            startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        }

        btn.setOnClickListener {
            Thread { copyAssets() }.start()
        }
    }

    private fun copyAssets() {
        val outRoot = File(Environment.getExternalStorageDirectory(), "Download/Generals")
        outRoot.mkdirs()
        var count = 0
        try {
            val folders = assets.list("") ?: arrayOf()
            for (folder in folders) {
                if (folder.startsWith("Command and Conquer")) {
                    count = copyFolder(folder, File(outRoot, folder), count)
                }
            }
            runOnUiThread { status.text = "Tayyor! $count fayl ko'chirildi" }
        } catch (ex: Exception) {
            runOnUiThread { status.text = "Xato: ${ex.message}" }
        }
    }

    private fun copyFolder(assetPath: String, outDir: File, startCount: Int): Int {
        var count = startCount
        outDir.mkdirs()
        val items = assets.list(assetPath) ?: arrayOf()
        if (items.isEmpty()) {
            val f = File(outDir.parentFile, outDir.name)
            assets.open(assetPath).use { input ->
                f.outputStream().use { output -> input.copyTo(output) }
            }
            count++
            return count
        }
        for (item in items) {
            val childAssetPath = "$assetPath/$item"
            val childOut = File(outDir, item)
            val subItems = assets.list(childAssetPath) ?: arrayOf()
            if (subItems.isEmpty()) {
                assets.open(childAssetPath).use { input ->
                    childOut.outputStream().use { output -> input.copyTo(output) }
                }
                count++
                if (count % 10 == 0) {
                    runOnUiThread { status.text = "Ko'chirilmoqda: $count fayl" }
                }
            } else {
                count = copyFolder(childAssetPath, childOut, count)
            }
        }
        return count
    }
}
