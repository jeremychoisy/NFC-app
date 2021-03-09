package com.mbds.android.nfc.code

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.liveData
import com.mbds.android.nfc.code.api.repositories.MeetingRepository
import com.mbds.android.nfc.code.databinding.ActivityMainBinding
import com.mbds.android.nfc.code.models.Resource
import com.mbds.android.nfc.code.models.Status.*
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType
import okhttp3.RequestBody
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val repository = MeetingRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        handleActions()
        // TODO: Remove, only for example purpose
        createMeeting().observe(this, Observer { it ->
            it?.let { resource ->
                when (resource.status) {
                    SUCCESS -> {
                        resource.data?.let { it -> Log.v("RESPONSE", it.meeting._id)}
                        // TODO: Hide spinner
                    }
                    ERROR -> {
                        resource.message?.let { errorMessage -> Log.v("RESPONSE", errorMessage)}
                        // TODO: Hide spinner
                    }
                    LOADING -> {
                        // TODO: Display spinner
                    }
                }
            }
        })
        // TODO: Remove, only for example purpose
        /* fetchMeeting().observe(this, Observer { it ->
            it?.let { resource ->
                when (resource.status) {
                    SUCCESS -> {
                        resource.data?.let { it -> Log.v("RESPONSE", it.meeting._id)}
                        // TODO: Hide spinner
                    }
                    ERROR -> {
                        resource.message?.let { errorMessage -> Log.v("RESPONSE", errorMessage)}
                        // TODO: Hide spinner
                    }
                    LOADING -> {
                        // TODO: Display spinner
                    }
                }
            }
        }) */
    }

    private fun handleActions() {
        binding.btnReadTag.setOnClickListener { startActivity(Intent(this@MainActivity, NFCReaderActivity::class.java)) }
        binding.btnWriteTag.setOnClickListener { startActivity(Intent(this@MainActivity, NFCWriterActivity::class.java)) }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // TODO: Remove, only for example purpose
    private fun fetchMeeting() = liveData(Dispatchers.IO) {
        emit(Resource.loading(data = null))
        try {
            emit(Resource.success(data = repository.get("60473513acaf1d0015dde0d9")))
        } catch (exception: Exception) {
            emit(Resource.error(data = null, message = exception.message ?: "Error Occurred!"))
        }
    }

    // TODO: Remove, only for example purpose
    private fun createMeeting() = liveData(Dispatchers.IO) {
        emit(Resource.loading(data = null))
        try {
            val name = RequestBody.create(MediaType.parse("text/plain"), "Depuis");
            val site = RequestBody.create(MediaType.parse("text/plain"), "Nice");
            val date = RequestBody.create(MediaType.parse("text/plain"), Date().toString());
            emit(Resource.success(data = repository.create(name, site, date)))
        } catch (exception: Exception) {
            emit(Resource.error(data = null, message = exception.message ?: "Error Occurred!"))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)
    }
}