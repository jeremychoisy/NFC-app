package com.mbds.android.nfc.code

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.DrawableContainer
import android.nfc.*
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.*

class NFCWriterActivity : Activity() {
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    lateinit var btnConfirm: Button
    lateinit var inputName: EditText
    lateinit var inputLastName: EditText
    lateinit var labelText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.write_tag_layout)

        // Get default NfcAdapter and PendingIntent instances
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        // check NFC feature:
        if (nfcAdapter == null) {
            // process error device not NFC-capable…
        }
        pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
        // single top flag avoids activity multiple instances launching

        btnConfirm = findViewById<Button>(R.id.btn_confirm_rdv)
        inputName = findViewById<EditText>(R.id.input_name)
        inputLastName = findViewById<EditText>(R.id.input_lastname)
        labelText = findViewById<TextView>(R.id.txtView1)


        btnConfirm.isEnabled = false
        labelText.visibility = View.INVISIBLE


        addCheckInputEvent(inputName)
        addCheckInputEvent(inputLastName)

        btnConfirm.setOnClickListener {
            Log.d("btn", "btn debug")
            labelText.visibility = View.VISIBLE
        }

    }



    fun addCheckInputEvent(edittext: EditText) {
        edittext.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(p0: Editable?) {
                labelText.visibility = View.INVISIBLE
                checkForm()
            }
        })
    }

    override fun onResume() {
        super.onResume()

        // Enable NFC foreground detection
        if (nfcAdapter != null) {
            if (!nfcAdapter!!.isEnabled) {
                // process error NFC not activated…
            }
            nfcAdapter!!.enableForegroundDispatch(this, pendingIntent, null, null)
        }
    }

    override fun onPause() {
        super.onPause()

        // Disable NFC foreground detection
        if (nfcAdapter != null) {
            nfcAdapter!!.disableForegroundDispatch(this)
        }
    }

    fun checkForm(): Boolean {
        if (inputLastName.text.toString() != "" && inputName.text.toString() != "") {
            btnConfirm.isEnabled = true
            return true;
        }
        btnConfirm.isEnabled = false
        //TODO Update message
        return false;
    }


    public override fun onNewIntent(intent: Intent) {

        // Get the Tag object:
        // ===================
        // retrieve the action from the received intent
        val action = intent.action
        // check the event was triggered by the tag discovery
        if (NfcAdapter.ACTION_TAG_DISCOVERED == action || NfcAdapter.ACTION_TECH_DISCOVERED == action || NfcAdapter.ACTION_NDEF_DISCOVERED == action) {

            // get the tag object from the received intent
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)

            // create the NDEF mesage:
            // ========================
            // dimension is the int number of entries of ndefRecords:
            val dimension = 1
            val ndefRecords = arrayOfNulls<NdefRecord>(dimension)
            // Example with an URI NDEF record:
            val uriTxt = "http://www.mbds-fr.org" // your URI in String format
            var ndefRecord = NdefRecord.createUri(uriTxt)
            // Add the record to the NDEF message:
            ndefRecords[0] = ndefRecord
            val ndefMessage = NdefMessage(ndefRecords)

            // Create NDEF message record type MIME:
            // =====================================
            val msgTxt = "Hello world!"
            val mimeType = "application/mbds.android.nfc" // your MIME type
            ndefRecord = NdefRecord.createMime(
                    mimeType,
                    msgTxt.toByteArray(Charset.forName("US-ASCII"))
            )
            // NDEF record URI type
            ndefRecord = NdefRecord.createUri(uriTxt)

            // NDEF record WELL KNOWN type (NFC Forum): TEXT
            // =============================================
            var lang = ByteArray(0)
            var data = ByteArray(0)
            var langeSize = 0
            try {
                lang = Locale.getDefault().language.toByteArray(charset("UTF-8"))
                langeSize = lang.size
                data = ByteArray(0)
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
            try {
                data = msgTxt.toByteArray(charset("UTF-8"))
                val dataLength = data.size
                val payload = ByteArrayOutputStream(1 + langeSize + dataLength)
                payload.write((langeSize and 0x1F))
                payload.write(lang, 0, langeSize)
                ndefRecord = NdefRecord(
                        NdefRecord.TNF_WELL_KNOWN,
                        NdefRecord.RTD_TEXT, ByteArray(0),
                        payload.toByteArray()
                )
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }

            // check and write the tag received:
            // =================================
            // check the targeted tag the memory size and is the tag writable
            val ndef = Ndef.get(tag)
            val size = ndefMessage.toByteArray().size
            if (ndef != null) {
                try {
                    ndef.connect()
                    if (!ndef.isWritable) {
                        // tag is locked in writing!
                    }
                    if (ndef.maxSize < size) {
                        // manage oversize!
                    }
                    // write the NDEF message on the tag
                    ndef.writeNdefMessage(ndefMessage)
                    ndef.close()
                    Toast.makeText(this, "Message écrit avec succès", Toast.LENGTH_SHORT).show()
                } catch (e1: IOException) {
                    e1.printStackTrace()
                } catch (e2: FormatException) {
                    e2.printStackTrace()
                }
            }

            // check and write the tag received at activity:
            // =============================================
            // is the tag formatted?
            if (ndef == null) {
                val format = NdefFormatable.get(tag)
                if (format != null) {
                    // can you format the tag?
                    try {
                        format.connect()
                        // Format and write the NDEF message on the tag
                        format.format(ndefMessage)
                        // Example of tag locked in writing:
                        // formatable.formatReadOnly(message);
                        format.close()
                        Toast.makeText(this, "Message écrit avec succès", Toast.LENGTH_SHORT).show()
                    } catch (e1: IOException) {
                        e1.printStackTrace()
                    } catch (e2: FormatException) {
                        e2.printStackTrace()
                    }
                }
            }
        }
    }
}
