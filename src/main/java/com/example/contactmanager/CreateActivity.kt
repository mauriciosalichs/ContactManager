package com.example.contactmanager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentProviderOperation
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class CreateActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var copyFromClipboard: Runnable
    private lateinit var nameText: TextView
    private lateinit var prefixText: TextView
    private lateinit var numberText: TextView
    private lateinit var msgTextView: TextView
    private lateinit var createButton: Button
    private lateinit var wspButton: Button
    private lateinit var msgButton: Button
    private lateinit var callButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_view)
        nameText = findViewById(R.id.nameText)
        prefixText = findViewById(R.id.prefixText)
        numberText = findViewById(R.id.numberText)
        msgTextView = findViewById(R.id.msgLog)
        createButton = findViewById(R.id.createButton)
        wspButton = findViewById(R.id.wspButton)
        msgButton = findViewById(R.id.msgButton)
        callButton = findViewById(R.id.callButton)
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val nameMap = applicationContext.getSharedPreferences("nameMap", Context.MODE_PRIVATE)
        val nameMapEditor: SharedPreferences.Editor = nameMap.edit()
        val numberMap = applicationContext.getSharedPreferences("numberMap", Context.MODE_PRIVATE)
        val numberMapEditor: SharedPreferences.Editor = numberMap.edit()
        var completeNumber: String = ""

        prefixText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(s: Editable?) {
                //TODO: RESOLVE THIS
                var code = ""
                val codes = resources.getStringArray(R.array.CountryCodes)
                for (i in codes.indices) {
                    val g: List<String> = codes[i].split(",")
                    if (g[1].trim { it <= ' ' } == s.toString()) {
                        code = g[0]
                        break
                    }
                }
                val locale = Locale("es-ES", code)
                Log.d("PAIS",s.toString()+" "+locale.displayCountry)
            }
        })

        prefixText.text = retrieveDefaultCountryPrefix()
        copyFromClipboard = Runnable {
            checkClipboardContent(clipboardManager)
        }

        createButton.setOnClickListener {
            msgTextView.visibility = View.VISIBLE
            val name = nameText.text.toString()
            if (nameMap.all.containsKey(name)) {
                val numbersTxt = nameMap.all[name].toString().trim().substringAfter(";")
                msgTextView.text =
                    "El contacto con nombre $name ya existe.\nSus números son $numbersTxt"
                nameText.text = ""
            } else {
                val incNumber = numberText.text.toString()
                if (numberMap.all.containsKey(incNumber)) {
                    val nameTxt = numberMap.all[incNumber].toString().trim().substringAfter(";")
                    msgTextView.text =
                        "El contacto con numero $incNumber ya existe.\nSu nombre es $nameTxt"
                    numberText.text = ""
                } else {
                    val prefix = prefixText.text.toString()
                    val emptyPrefix = prefix.isEmpty()
                    completeNumber =
                        (if (emptyPrefix) "" else "+") + prefix + incNumber
                    val id = addContactToPhone(name, completeNumber)
                    nameMapEditor.putString(name, "$id;$incNumber")
                    numberMapEditor.putString(incNumber, "$id;$name")
                    nameMapEditor.apply()
                    numberMapEditor.apply()
                    msgTextView.text = "El contacto con nombre $name ha sido creado."
                    wspButton.visibility = View.VISIBLE
                    msgButton.visibility = View.VISIBLE
                    callButton.visibility = View.VISIBLE
                }
            }
            Log.d("CONTACT_DICT",nameMap.all.toString())
        }

        wspButton.setOnClickListener {
            if (!completeNumber.startsWith("+")) {
                msgTextView.text =
                    "El contacto no tiene un código de país asociado."
            } else {
                val i = Intent(Intent.ACTION_VIEW, Uri.parse("http://wa.me/$completeNumber"))
                startActivity(i)
            }
        }

        handler.postDelayed(copyFromClipboard, 100)
    }

    private fun checkClipboardContent(clipboardManager: ClipboardManager) {
        val clip: ClipData? = clipboardManager.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val clipboardText = clip.getItemAt(0).text.toString()
            val clipboardNumber = Regex("\\+?\\d{6,}").find(clipboardText)?.value ?: ""
            if (clipboardNumber.startsWith("+")) {
                val pairPrefixNumber = retrievePrefixAndNumber(clipboardNumber)
                prefixText.text = pairPrefixNumber.first
                numberText.text = pairPrefixNumber.second
            } else if (clipboardNumber.isNotEmpty()) {
                numberText.text = clipboardNumber
            }
        }
    }

    private fun retrievePrefixAndNumber(clipboardNumber: String): Pair<String, String> {
        // TODO: Create a valid prefix retrieval for any country code, not just of length two
        return Pair(clipboardNumber.substring(1, 3),clipboardNumber.substring(3))
    }

    private fun retrieveDefaultCountryPrefix(): String {
        val configValues = applicationContext.getSharedPreferences("configValues", Context.MODE_PRIVATE).all
        return if (configValues.containsKey("defaultPrefix")) configValues["defaultPrefix"] as String else ""
    }

    private fun addContactToPhone(name: String, phone: String): String {
        val operations = ArrayList<ContentProviderOperation>()
        var phoneId: Long? = null
        // Adding a raw contact to Data.CONTENT_URI
        operations.add(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()
        )

        // Adding contact's name
        operations.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build()
        )

        // Adding contact's phone number
        operations.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build()
        )

        // Applying the batch
        try {
            val result = contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
            if (result != null && result.isNotEmpty()) {
                val uri = result[0].uri
                val segments = uri?.pathSegments
                if (segments != null && segments.size >= 2) {
                    val rawContactId = segments[1].toLongOrNull()
                    if (rawContactId != null) {
                        val cursor = contentResolver.query(
                            ContactsContract.Data.CONTENT_URI,
                            arrayOf(ContactsContract.Data._ID),
                            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                            arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE),
                            null
                        )
                        cursor?.use {
                            if (it.moveToFirst()) {
                                val phoneColumnIndex = it.getColumnIndex(ContactsContract.Data._ID)
                                phoneId = it.getLong(phoneColumnIndex)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return phoneId.toString()
    }
}