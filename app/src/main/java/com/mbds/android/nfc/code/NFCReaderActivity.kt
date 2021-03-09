package com.mbds.android.nfc.code

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.liveData
import com.mbds.android.nfc.code.api.repositories.MeetingRepository
import com.mbds.android.nfc.code.models.Resource
import com.mbds.android.nfc.code.models.Status
import kotlinx.coroutines.Dispatchers
import java.io.UnsupportedEncodingException
import kotlin.experimental.and

class NFCReaderActivity : AppCompatActivity() {
    private val repository = MeetingRepository()

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    lateinit var labelTextRead: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.read_tag_layout)

        labelTextRead = findViewById<TextView>(R.id.readNfcInfo)

        // Get default NfcAdapter and PendingIntent instances
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        // check NFC feature:
        if (nfcAdapter == null) {
            needNfc()
        }

        // single top flag avoids activity multiple instances launching
        pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
    }

    override fun onResume() {
        super.onResume()
        val adapter = nfcAdapter ?: return run {
            needNfc()
        }

        if (!adapter.isEnabled) {
            // process error NFC not activated…
            Toast.makeText(this, "Votre capteur NFC est désactivé", Toast.LENGTH_LONG).show()
            finish()
        }
        // Activer la découverte de tag en --> Android va nous envoyer directement les tags détéctés
        adapter.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    private fun needNfc() {
        Toast.makeText(this, "Ce service est disponible uniquement sur un téléphone NFC", Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onPause() {
        super.onPause()

        // Soyons sympa en désactivant le NFC quand l'activité n'est plus visible
        nfcAdapter?.disableForegroundDispatch(this)
    }

    private fun fetchMeeting(id: String) = liveData(Dispatchers.IO) {
        emit(Resource.loading(data = null))
        try {
            emit(Resource.success(data = repository.get(id)))
        } catch (exception: Exception) {
            emit(Resource.error(data = null, message = exception.message ?: "Error Occurred!"))
        }
    }

    private fun deleteMeeting(id: String) = liveData(Dispatchers.IO) {
        try {
            emit(Resource.success(data = repository.delete(id)))
        } catch (exception: Exception) {
            emit(Resource.error(data = null, message = exception.message ?: "Error Occurred!"))
        }
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Get the Tag object:
        // ===================
        // retrieve the action from the received intent
        val action = intent.action
        // check the event was triggered by the tag discovery
        if (NfcAdapter.ACTION_TAG_DISCOVERED == action || NfcAdapter.ACTION_TECH_DISCOVERED == action || NfcAdapter.ACTION_NDEF_DISCOVERED == action) {
            var message = "AUCUNE INFORMATION TROUVEE SUR LE TAG !!!"

            // get the tag object from the received intent
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)

            // Get the Tag object information:
            // ===============================
            // get the UTD from the tag
            val uid = tag.id
            message = "Tag détecté UID : $uid"

            // get the technology list from the tag
            val technologies = tag.techList
            // bit reserved to an optional file content descriptor
            val content = tag.describeContents()
            // get NDEF content
            val ndef = Ndef.get(tag)
            // is the tag writable?
            val isWritable = ndef.isWritable
            message = if (isWritable) "$message réinscriptible" else "$message non inscriptible"

            // can the tag be locked in writing?
            val canMakeReadOnly = ndef.canMakeReadOnly()
            message = if (canMakeReadOnly) "$message, verrouillable en écriture" else "$message, non verrouillable en écriture"

            // : get NDEF records:
            // ===================
            val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            // check if the tag contains an NDEF message
            if (rawMsgs != null && rawMsgs.size != 0) {
                // instantiate a NDEF message array to get NDEF records
                val ndefMessage = arrayOfNulls<NdefMessage>(rawMsgs.size)
                // loop to get the NDEF records
                for (i in rawMsgs.indices) {
                    ndefMessage[i] = rawMsgs[i] as NdefMessage
                    for (j in ndefMessage[i]!!.records.indices) {
                        val ndefRecord = ndefMessage[i]!!.records[j]

                        // parse NDEF record as String:
                        // ============================
                        val payload = ndefRecord.payload
                        val encoding = if (payload[0] and 128.toByte() == 0.toByte()) "UTF-8" else "UTf-8"
                        val languageSize: Int = (payload[0] and 51.toByte()).toInt()
                        try {
                            val recordTxt = String(
                                payload, languageSize + 1,
                                payload.size - languageSize - 1, charset(encoding)
                            )
                            val fetchMeeting = fetchMeeting(recordTxt)
                            fetchMeeting.observe(this, Observer { it ->
                                it?.let { resource ->
                                    when (resource.status) {
                                        Status.SUCCESS -> {
                                            labelTextRead.text = " Rendez-vous valide, dirigez vous vers l'acceuil."
                                            print("MESSSSSAGGGE")
                                            deleteMeeting(recordTxt).observe(this, Observer {
                                                if(resource.status == Status.SUCCESS) {
                                                    Toast.makeText(this, "Votre carte ne contient plus de rendez-vous.", Toast.LENGTH_SHORT).show()
                                                    // TODO: Hide spinner
                                                } else {
                                                    Toast.makeText(this, "Échec de la suppression du rendez-vous", Toast.LENGTH_SHORT).show()
                                                    // TODO: Hide spinner
                                                }
                                            })
                                        }
                                        Status.ERROR -> {
                                            Toast.makeText(this, "Rendez-vous non valide", Toast.LENGTH_SHORT).show()
                                            labelTextRead.text = "Le rendez-vous associé à cette carte n'est pas valide."
                                        }
                                        Status.LOADING -> {
                                            // TODO: Show spinner
                                        }
                                    }
                                }
                            })
                        } catch (e: UnsupportedEncodingException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
}
