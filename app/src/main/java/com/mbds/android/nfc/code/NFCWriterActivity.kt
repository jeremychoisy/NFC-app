package com.mbds.android.nfc.code

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.*
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.liveData
import com.mbds.android.nfc.code.api.repositories.MeetingRepository
import com.mbds.android.nfc.code.models.Resource
import com.mbds.android.nfc.code.models.Status.*
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType
import okhttp3.RequestBody
import java.io.IOException
import java.util.Date

class NFCWriterActivity : AppCompatActivity() {
    private val repository = MeetingRepository()

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var meetingId: String? = null

    lateinit var btnConfirm: Button
    lateinit var inputName: EditText
    lateinit var inputLastName: EditText
    lateinit var labelText: TextView
    lateinit var spinnerCentre: Spinner

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
        spinnerCentre = findViewById<Spinner>(R.id.spinner_centre)

        var list = arrayOf("Service de vaccination, Nice",
                "Maison des associations, Antibes",
                "Centre de santé, Marseille")

        var adapter = ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, list)
        spinnerCentre.adapter = adapter

//        spinnerCentre.onItemClickListener()

//        spinnerCentre.setOnItemSelectedListener(object : OnItemSelectedListener {
//            override fun onItemSelected(arg0: AdapterView<*>?, arg1: View, arg2: Int, arg3: Long) {
//                val items: String = spinnerCentre.getSelectedItem().toString()
//                Log.i("Selected item : ", items)
//            }
//
//            override fun onNothingSelected(arg0: AdapterView<*>?) {}
//        })

        btnConfirm.isEnabled = false
        labelText.visibility = View.INVISIBLE


        addCheckInputEvent(inputName)
        addCheckInputEvent(inputLastName)

        btnConfirm.setOnClickListener {
            val name = "" + inputName.text + " " + inputLastName.text
            val site = spinnerCentre.selectedItem.toString()
            //TODO: update when datepicker is setup
            val date = Date()
            createMeeting(name, site, date).observe(this, Observer { it ->
                it?.let { resource ->
                    when (resource.status) {
                        SUCCESS -> {
                            meetingId = resource.data?.meeting?._id
                            labelText.text = "Rendez-vous créé avec succès"
                            labelText.visibility = View.VISIBLE
                            // TODO: Hide spinner
                        }
                        ERROR -> {
                            labelText.text = resource.message
                            // TODO: Hide spinner
                        }
                        LOADING -> {
                            // TODO: Display spinner
                        }
                    }
                }
            })
        }
    }

    private fun createMeeting(name: String, site: String, date: Date) = liveData(Dispatchers.IO) {
        emit(Resource.loading(data = null))
        try {
            val name = RequestBody.create(MediaType.parse("text/plain"), name);
            val site = RequestBody.create(MediaType.parse("text/plain"), site);
            val date = RequestBody.create(MediaType.parse("text/plain"), date.toString());
            emit(Resource.success(data = repository.create(name, site, date)))
        } catch (exception: Exception) {
            emit(Resource.error(data = null, message = exception.message ?: "Error Occurred!"))
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
        super.onNewIntent(intent)

        if (meetingId != null) {

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
                var ndefRecord = NdefRecord.createTextRecord("FR", meetingId.toString())
                // Add the record to the NDEF message:
                ndefRecords[0] = ndefRecord
                val ndefMessage = NdefMessage(ndefRecords)


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
                        Toast.makeText(this, "Rendez-vous écrit avec succès", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(this, "Rendez-vous écrit avec succès", Toast.LENGTH_SHORT).show()
                        } catch (e1: IOException) {
                            e1.printStackTrace()
                        } catch (e2: FormatException) {
                            e2.printStackTrace()
                        }
                    }
                }
            }
        } else {
            Toast.makeText(this, "Vous devez créer un rendez-vous avant de l'écrire sur la carte.", Toast.LENGTH_SHORT).show()
        }
    }
}
