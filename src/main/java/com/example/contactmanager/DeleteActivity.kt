package com.example.contactmanager

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class DeleteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.delete_view)

        val editText = findViewById<EditText>(R.id.searchContactEdit)
        val searchButton = findViewById<Button>(R.id.searchContactButton)
        val checkBoxContainer = findViewById<LinearLayout>(R.id.checkBoxContainer)
        val deleteButton = findViewById<Button>(R.id.deleteContactsButton)
        val contactList = mutableListOf<Contact>()
        var boxes = mutableListOf<CheckBox>()

        deleteButton.visibility = View.INVISIBLE

        searchButton.setOnClickListener {
            contactList.clear()
            val pattern = editText.text.toString().lowercase(Locale.ROOT)
            val nameMap = applicationContext.getSharedPreferences("nameMap", Context.MODE_PRIVATE).all
            for (name in nameMap.keys) {
                if (name.toString().lowercase(Locale.ROOT).contains(pattern)) {
                    val value = nameMap[name].toString().split(";")
                    contactList.add(Contact(value[0], name, value[1]))
                }
            }
            Log.d("CONTACTOS","${contactList.size} contactos encontrados.")
            boxes = createCheckBoxes(checkBoxContainer, contactList)
            deleteButton.visibility = View.VISIBLE
        }

        deleteButton.setOnClickListener {
            for (i in 0 until contactList.size){
                if (boxes[i].isChecked) {
                    val contact = contactList[i]
                    Log.d("Contact","Eliminar a ${contact.name}")
                    val contactUri: Uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contact.id.toLong())
                    contentResolver.delete(contactUri, null, null)
                    // TODO: DELETE CONTACT FROM PERSISTENT MAPS
                }
            }
            checkBoxContainer.removeAllViews()
            deleteButton.visibility = View.INVISIBLE
        }
    }

    private fun createCheckBoxes(container: LinearLayout, contactList: MutableList<Contact>) : MutableList<CheckBox> {
        container.removeAllViews()
        val selectAllCheckBox = CheckBox(this)
        val boxes = mutableListOf<CheckBox>()
        selectAllCheckBox.text = "Seleccionar todos"
        container.addView(selectAllCheckBox)

        // Crear CheckBoxes dinámicamente
        for (contact in contactList) {
            val checkBox = CheckBox(this)
            checkBox.text = contact.name
            boxes.add(checkBox)
            container.addView(checkBox)
        }
        selectAllCheckBox.setOnCheckedChangeListener { _, isChecked ->
            for (cb in boxes) {
                cb.isChecked = isChecked
            }
        }
        return boxes
    }

    data class Contact(
        val id: String,
        val name: String,
        val numbers: String
    )
}
