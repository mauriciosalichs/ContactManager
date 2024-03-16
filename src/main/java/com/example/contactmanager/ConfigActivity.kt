package com.example.contactmanager

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.CheckBox
import android.widget.EditText

class ConfigActivity : AppCompatActivity() {

    private lateinit var defaultCodeCB: CheckBox
    private lateinit var foreingClipboardCB: CheckBox
    private lateinit var autoUpdateCB: CheckBox
    private lateinit var defaultPrefix: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.config_view)
        val configMap = applicationContext.getSharedPreferences("configMap", Context.MODE_PRIVATE)
        val configValues = applicationContext.getSharedPreferences("configValues", Context.MODE_PRIVATE)

        // Referencias a los componentes
        defaultCodeCB = findViewById(R.id.defaultCodeCB)
        foreingClipboardCB = findViewById(R.id.foreingClipboardCB)
        autoUpdateCB = findViewById(R.id.autoUpdateCB)
        defaultPrefix = findViewById(R.id.defaultPrefix)

        // Habilitar/deshabilitar EditText basado en el estado del CheckBox
        defaultCodeCB.setOnCheckedChangeListener { _, isChecked ->
            defaultPrefix.isEnabled = isChecked
        }

        // Retrieve state of boxes from persisted config
        retrieveState(configMap, configValues)
    }

    override fun onPause() {
        super.onPause()
        val configMap = applicationContext.getSharedPreferences("configMap", Context.MODE_PRIVATE)
        val configValues = applicationContext.getSharedPreferences("configValues", Context.MODE_PRIVATE)
        saveConfigState(configMap, configValues)
    }

    private fun saveConfigState(configMap: SharedPreferences, configValues: SharedPreferences) {
        val mapEditor = configMap.edit()
        val valueEditor = configValues.edit()
        mapEditor.putBoolean("defaultCode", defaultCodeCB.isChecked)
        mapEditor.putBoolean("foreingClipboard", foreingClipboardCB.isChecked)
        mapEditor.putBoolean("autoUpdate", autoUpdateCB.isChecked)
        if (defaultCodeCB.isChecked){
            valueEditor.putString("defaultPrefix", defaultPrefix.text.toString())
        } else {
            valueEditor.putString("defaultPrefix", "")
        }
        mapEditor.apply()
        valueEditor.apply()
    }

    private fun retrieveState(configMap: SharedPreferences, configValues: SharedPreferences) {
        configMap.all.forEach { (key, value) ->
            if (value is Boolean) {
                when (key) {
                    "defaultCode" -> defaultCodeCB.isChecked = value
                    "foreingClipboard" -> foreingClipboardCB.isChecked = value
                    "autoUpdate" -> autoUpdateCB.isChecked = value
                }
            }
        }
        configValues.all.forEach { (key, value) ->
            if (value is String) {
                when (key) {
                    "defaultPrefix" -> defaultPrefix.setText(value)
                }
            }
        }
    }
}
