package com.wifiguard.core.data.local

// РЕЗЕРВНАЯ КОПИЯ: Весь класс DataTransferManager закомментирован, так как функционал импорта/экспорта удален
// Если понадобится восстановить, раскомментируйте весь файл

/*
import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.wifiguard.core.data.local.entity.WifiScanEntity
import com.wifiguard.core.data.local.entity.WifiScanResultEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader

class DataTransferManager(
    private val context: Context,
    private val wifiGuardDatabase: WifiGuardDatabase
) {

    suspend fun exportData(uri: Uri) {
        withContext(Dispatchers.IO) {
            val wifiScanResults = wifiGuardDatabase.wifiScanDao().getAllWifiScansSuspend()
            val gson = Gson()
            val jsonString = gson.toJson(wifiScanResults)

            context.contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { stream ->
                    stream.write(jsonString.toByteArray())
                }
            }
        }
    }

    suspend fun importData(uri: Uri) {
        withContext(Dispatchers.IO) {
            val stringBuilder = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        line = reader.readLine()
                    }
                }
            }
            val jsonString = stringBuilder.toString()
            val gson = Gson()
            val wifiScanResults: List<WifiScanEntity> = gson.fromJson(jsonString, Array<WifiScanEntity>::class.java).toList()
            wifiGuardDatabase.wifiScanDao().insertScans(wifiScanResults)
        }
    }
}
*/