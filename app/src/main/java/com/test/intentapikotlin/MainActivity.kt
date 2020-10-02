package com.test.intentapikotlin

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.experimental.and

class MainActivity : AppCompatActivity() {

    private val TAG = "IntentApiSampleKotlin"

    private val IntentPackageName="com.intermec.datacollectionservice"

    public val ACTION_BARCODE_DATA = "com.honeywell.sample.action.BARCODE_DATA"
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

    companion object {
        var ins: MainActivity? = null
        fun getInstance(): MainActivity? {
            return ins
        }
    }
    var barcodeDataReceiver: myBarcodeReceiver? = null

    @Volatile
    private var receiversRegistered = false // see registerIntent function

    private var textView: TextView? = null
    var button: Button? = null
    var sdkVersion = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ins=this

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
                    Intent(EXTRA_CONTROL)
                        .putExtra(
                        EXTRA_SCAN,
                        false
                        )
                        .setPackage(IntentPackageName)
                )
            }, 3000)
        }
        Log.d(TAG, "onCreate registerIntent")
        registerIntent()
    }

    fun registerIntent(){
        /*
        You may leave your receivers being registered to the same intents in AndroidManifest.xml file
        (as this works for Android before v.7 ...), but note that in this case in logcat you will
        still see "Background execution not allowed" with a reference to your receivers. This only
        means that registration via the AndroidManifest.xml doesn't work (as expected for
        Android 8+) but self-registered receivers should be called anyway!

        For me: I need to have the receiver inside Manifest and using registerReceiver does not make difference

         */
        // Better use Manifest to register Receiver
        if(! receiversRegistered) {
            Log.d(TAG, "registerIntent()...")
            if (barcodeDataReceiver == null) {
                Log.d(TAG, "registerIntent() barcodeDataReceiver is null...")
                var intentFilter: IntentFilter = IntentFilter()
                intentFilter.addAction(ACTION_BARCODE_DATA)
                intentFilter.addCategory("android.intent.category.DEFAULT")
                registerReceiver(barcodeDataReceiver, intentFilter)
            }
            receiversRegistered=true;
        }else{
            Log.d(TAG, "registerIntent() already registered")
        }
    }
    override fun onResume() {
        super.onResume()
        //        IntentFilter intentFilter = new IntentFilter("hsm.RECVRBI");
        Log.d(TAG, "onResume registerIntent()")
        registerIntent()
        claimScanner()
    }

    override fun onPause() {
        super.onPause()
        releaseScanner()

        Log.d(TAG, "onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        if(barcodeDataReceiver!=null) {
            unregisterReceiver(barcodeDataReceiver)
        }
    }
    private fun claimScanner() {
        Log.d(TAG, "claimScanner")
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
                .setPackage(IntentPackageName)
                .putExtra(EXTRA_SCANNER, "dcs.scanner.imager")
                .putExtra(EXTRA_PROFILE, "DEFAULT") // "MyProfile1")
                .putExtra(EXTRA_PROPERTIES, properties)
        )
    }

    private fun releaseScanner() {
        Log.d(TAG, "releaseScanner")
        mysendBroadcast(Intent(ACTION_RELEASE_SCANNER).setPackage(IntentPackageName))
    }

    private fun sendImplicitBroadcast(ctxt: Context, i: Intent) {
        Log.d(TAG, "sendImplicitBroadcast")

        if(i.getPackage()!=null){
            Log.d(TAG, "broadcast as intent has package name")
            //sendBroadcast(i);
        }

        val pm = ctxt.packageManager
        val matches = pm.queryBroadcastReceivers(i, 0)
        if(matches.size>0) {
            for (resolveInfo in matches) {
                val explicit = Intent(i)
                val cn = ComponentName(
                    resolveInfo.activityInfo.applicationInfo.packageName,
                    resolveInfo.activityInfo.name
                )
                explicit.component = cn
                Log.d(
                    TAG,
                    "sendImplicitBroadcast: ctxt.sendBroadcast(implicit): " + ctxt.toString() + ", " + explicit.toString()
                )
                ctxt.sendBroadcast(explicit)
            }
        }else{
            // to be compatible with Android 9 and later version for dynamic receiver
            Log.d(TAG, "Android 9 needed sendBroadcast")
            ctxt.sendBroadcast(i)
        }
    }

    private fun mysendBroadcast(intent: Intent) {
        Log.d(TAG, "mysendBroadcast")
        sendImplicitBroadcast(applicationContext, intent)

 /*       if (sdkVersion < 26) {
            Log.d(TAG, "mysendBroadcast: sendBroadcast")
            sendBroadcast(intent)
        } else { //for Android O above "gives W/BroadcastQueue: Background execution not allowed: receiving Intent"
//either set targetSDKversion to 25 or use implicit broadcast
            Log.d(TAG, "mysendBroadcast: sendImplicitBroadcast")
            sendImplicitBroadcast(applicationContext, intent)
        }
*/
    }

    public fun setText(text: String) {
        if (textView != null) {
            this@MainActivity.runOnUiThread {
                textView!!.text = text
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
