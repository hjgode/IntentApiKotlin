package com.test.intentapikotlin

import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val TAG = "IntentApiSampleKotlin"
    private val ACTION_BARCODE_DATA = "com.honeywell.sample.action.BARCODE_DATA"
    /**
     * Honeywell DataCollection Intent API
     * Claim scanner
     * Package Permissions:
     * "com.honeywell.decode.permission.DECODE"
     */
    private val ACTION_CLAIM_SCANNER =
        "com.honeywell.aidc.action.ACTION_CLAIM_SCANNER"
    /**
     * Honeywell DataCollection Intent API
     * Release scanner claim
     * Permissions:
     * "com.honeywell.decode.permission.DECODE"
     */
    private val ACTION_RELEASE_SCANNER =
        "com.honeywell.aidc.action.ACTION_RELEASE_SCANNER"
    /**
     * Honeywell DataCollection Intent API
     * Optional. Sets the scanner to claim. If scanner is not available or if extra is not used,
     * DataCollection will choose an available scanner.
     * Values : String
     * "dcs.scanner.imager" : Uses the internal scanner
     * "dcs.scanner.ring" : Uses the external ring scanner
     */
    private val EXTRA_SCANNER = "com.honeywell.aidc.extra.EXTRA_SCANNER"
    /**
     * Honeywell DataCollection Intent API
     * Optional. Sets the profile to use. If profile is not available or if extra is not used,
     * the scanner will use factory default properties (not "DEFAULT" profile properties).
     * Values : String
     */
    private val EXTRA_PROFILE = "com.honeywell.aidc.extra.EXTRA_PROFILE"
    /**
     * Honeywell DataCollection Intent API
     * Optional. Overrides the profile properties (non-persistent) until the next scanner claim.
     * Values : Bundle
     */
    private val EXTRA_PROPERTIES = "com.honeywell.aidc.extra.EXTRA_PROPERTIES"

    private val EXTRA_CONTROL = "com.honeywell.aidc.action.ACTION_CONTROL_SCANNER"
    /*
        Extras
        "com.honeywell.aidc.extra.EXTRA_SCAN" (boolean): Set to true to start or continue scanning. Set to false to stop scanning. Most scenarios only need this extra, however the scanner can be put into other states by adding from the following extras.
        "com.honeywell.aidc.extra.EXTRA_AIM" (boolean): Specify whether to turn the scanner aimer on or off. This is optional; the default value is the value of EXTRA_SCAN.
        "com.honeywell.aidc.extra.EXTRA_LIGHT" (boolean): Specify whether to turn the scanner illumination on or off. This is optional; the default value is the value of EXTRA_SCAN.
        "com.honeywell.aidc.extra.EXTRA_DECODE" (boolean): Specify whether to turn the decoding operation on or off. This is optional; the default value is the value of EXTRA_SCAN
    */
    private val EXTRA_SCAN = "com.honeywell.aidc.extra.EXTRA_SCAN"

    private var textView: TextView? = null
    var button: Button? = null
    var sdkVersion = 0



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sdkVersion = Build.VERSION.SDK_INT
        Log.d(TAG, "sdkVersion=$sdkVersion\n")

        textView = findViewById(R.id.textView) as TextView
        button = findViewById(R.id.button) as Button
        button!!.text = "Start Scan"

        button!!.setOnClickListener {
            mysendBroadcast(
                Intent(EXTRA_CONTROL).putExtra(
                    EXTRA_SCAN,
                    true
                )
            )
            //software defined decode timeout!
            val handler = Handler()
            handler.postDelayed({
                mysendBroadcast(
                    Intent(EXTRA_CONTROL).putExtra(
                        EXTRA_SCAN,
                        false
                    )
                )
            }, 3000)
        }
        Log.d("IntentApiSample: ", "onCreate")

    }

    override fun onResume() {
        super.onResume()
        //        IntentFilter intentFilter = new IntentFilter("hsm.RECVRBI");
        registerReceiver(barcodeDataReceiver, IntentFilter(ACTION_BARCODE_DATA))
        claimScanner()
        Log.d("IntentApiSample: ", "onResume")
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(barcodeDataReceiver)
        releaseScanner()
        Log.d("IntentApiSample: ", "onPause")
    }

    private fun claimScanner() {
        Log.d("IntentApiSample: ", "claimScanner")
        val properties = Bundle()
        properties.putBoolean("DPR_DATA_INTENT", true)
        properties.putString("DPR_DATA_INTENT_ACTION", ACTION_BARCODE_DATA)
        properties.putInt("TRIG_AUTO_MODE_TIMEOUT", 2)
        properties.putString(
            "TRIG_SCAN_MODE",
            "readOnRelease"
        ) //This works for Hardware Trigger only! If scan is started from code, the code is responsible for a switching off the scanner before a decode
        mysendBroadcast(
            Intent(ACTION_CLAIM_SCANNER)
                .putExtra(EXTRA_SCANNER, "dcs.scanner.imager")
                .putExtra(EXTRA_PROFILE, "DEFAULT") // "MyProfile1")
                .putExtra(EXTRA_PROPERTIES, properties)
        )
    }

    private fun releaseScanner() {
        Log.d("IntentApiSample: ", "releaseScanner")
        mysendBroadcast(Intent(ACTION_RELEASE_SCANNER))
    }

    private val barcodeDataReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("IntentApiSample: ", "onReceive")
            if (ACTION_BARCODE_DATA == intent.action) { /*
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
                        bytesToHexString(dataBytes)
                    val timestamp = intent.getStringExtra("timestamp")
                    val text = String.format(
                        "Data:%s\n" +
                                "Charset:%s\n" +
                                "Bytes:%s\n" +
                                "AimId:%s\n" +
                                "CodeId:%s\n" +
                                "Timestamp:%s\n",
                        data, charset, dataBytesStr, aimId, codeId, timestamp
                    )
                    setText(text)
                }
            }
        }
    }

    private fun sendImplicitBroadcast(ctxt: Context, i: Intent) {
        Log.d(TAG, "sendImplicitBroadcast")
        val pm = ctxt.packageManager
        val matches = pm.queryBroadcastReceivers(i, 0)
        for (resolveInfo in matches) {
            val explicit = Intent(i)
            val cn = ComponentName(
                resolveInfo.activityInfo.applicationInfo.packageName,
                resolveInfo.activityInfo.name
            )
            explicit.component = cn
            Log.d(TAG, "sendImplicitBroadcast: ctxt.sendBroadcast(explicit): " + ctxt.toString()+", "+explicit.toString())
            ctxt.sendBroadcast(explicit)
        }
    }

    private fun mysendBroadcast(intent: Intent) {
        Log.d(TAG, "mysendBroadcast")
        if (sdkVersion < 26) {
            Log.d(TAG, "mysendBroadcast: sendBroadcast")
            sendBroadcast(intent)
        } else { //for Android O above "gives W/BroadcastQueue: Background execution not allowed: receiving Intent"
//either set targetSDKversion to 25 or use implicit broadcast
            Log.d(TAG, "mysendBroadcast: sendImplicitBroadcast")
            sendImplicitBroadcast(applicationContext, intent)
        }
    }
    private fun setText(text: String) {
        if (textView != null) {
            runOnUiThread { textView!!.text = text }
        }
    }

    private fun bytesToHexString(arr: ByteArray?): String? {
        var s = "[]"
        if (arr != null) {
            s = "["
            for (i in arr.indices) {
                s += "0x" + Integer.toHexString(arr[i].toInt()) + ", "
            }
            s = s.substring(0, s.length - 2) + "]"
        }
        return s
    }
}
