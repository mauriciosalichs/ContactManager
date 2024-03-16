package com.example.contactmanager

//import android.Manifest

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


// MainActivity.kt
class MainActivity : AppCompatActivity() {

    private lateinit var createNewContactB: Button
    private lateinit var modifyContactB: Button
    private lateinit var deleteContactB: Button
    private lateinit var updateContactsB: Button
    private lateinit var configB: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_view)
        createNewContactB = findViewById(R.id.createNewContactButton)
        modifyContactB = findViewById(R.id.modifyContactButton)
        deleteContactB = findViewById(R.id.deleteContactButton)
        updateContactsB = findViewById(R.id.updateContactsButton)
        configB = findViewById(R.id.configButton)

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_PHONE_STATE), 1)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_CONTACTS), 1)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_CONTACTS), 1)
        }

        val tm = this.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        Log.d("COUNTRY CODE",tm.networkCountryIso)

        createNewContactB.setOnClickListener {
            startActivity(Intent(this, CreateActivity::class.java))
        }
        modifyContactB.setOnClickListener {
            //startActivity(Intent(this, ModifyActivity::class.java))
        }
        deleteContactB.setOnClickListener {
            startActivity(Intent(this, DeleteActivity::class.java))
        }
        updateContactsB.setOnClickListener {
            startActivity(Intent(this, UpdateActivity::class.java))
        }
        configB.setOnClickListener {
            startActivity(Intent(this, ConfigActivity::class.java))
        }
    }
}


