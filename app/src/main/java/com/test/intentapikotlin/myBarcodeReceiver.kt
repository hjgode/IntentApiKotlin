package com.test.intentapikotlin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlin.experimental.and

class myBarcodeReceiver:BroadcastReceiver (){
    public val ACTION_BARCODE_DATA = "com.honeywell.sample.action.BARCODE_DATA"
    val TAG = "myBarcodeReceiver"
    var activity : MainActivity? = null
    @Volatile
    var iScanCnt=0
    override fun onReceive(context: Context?, intent: Intent) {
        Log.d(TAG, "onReceive")
        if (ACTION_BARCODE_DATA.equals(intent.action)) {
/*
These extras are available:
"version" (int) = Data Intent Api version
"aimId" (String) = The AIM Identifier
"charset" (String) = The charset used to convert "dataBytes" to "data" string
"codeId" (String) = The Honeywell Symbology Identifier
"data" (String) = The barcode data as a string
"dataBytes" (byte[]) = The barcode data as a byte array
"timestamp" (String) = The barcode timestamp

*/
            val version = intent.getIntExtra("version", 0)
            if (version >= 1) {
                val aimId = intent.getStringExtra("aimId")
                val charset = intent.getStringExtra("charset")
                val codeId = intent.getStringExtra("codeId")
                val data = intent.getStringExtra("data")
                val dataBytes = intent.getByteArrayExtra("dataBytes")
                var dataBytesStr: String? = ""
                if (dataBytes != null && dataBytes.size > 0) dataBytesStr =
                    bytesToHex(dataBytes)
                val timestamp = intent.getStringExtra("timestamp")
                val text = String.format(
                    """
                        Data:%s
                        Charset:%s
                        Bytes:%s
                        AimId:%s
                        CodeId:%s
                        Timestamp:%s
                        ScanCount:%s
                        """.trimIndent(),
                    data, charset, dataBytesStr, aimId, codeId, timestamp, ++iScanCnt
                )
                Log.d(TAG, text)
                try {
                    MainActivity.getInstance()?.setText(text)
                } catch (e: Exception) {

                }
            }
        }
    }
    private val HEX_ARRAY = "0123456789ABCDEF".toCharArray()
    fun bytesToHex(bytes: ByteArray): String? {
        val hexChars = CharArray(bytes.size * 2)
        val sb = StringBuilder()
        sb.append("{")
        for (j in bytes.indices) {
            val v: Int = (bytes[j] and 0xFF.toByte()).toInt()
            hexChars[j * 2] = HEX_ARRAY[v ushr 4]
            hexChars[j * 2 + 1] = HEX_ARRAY[v and 0x0F]
            sb.append("0x" + HEX_ARRAY[v ushr 4])
            sb.append(HEX_ARRAY[v and 0x0F].toString() + ", ")
        }
        sb.append("}")
        return sb.toString()
    }
}