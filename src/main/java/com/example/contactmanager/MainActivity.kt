package com.example.contactmanager

import android.annotation.SuppressLint
import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TableRow
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import java.lang.ref.WeakReference

class MainActivity : Activity() {

    private lateinit var currentContactBeingShownTextView: TextView
    private lateinit var dupCountTextView: TextView
    private lateinit var contactOptionsField: TableRow
    private lateinit var startButton: Button
    private val outerClass = WeakReference<MainActivity>(this)

    var dupCounter = 0
    var index = 0
    var checkOffset = 1
    var searchDone = false
    val dupContacts = mutableListOf<SelectionPage>()
    companion object {
        private const val PERMISSION_REQUEST_READ_CONTACTS = 1
        private const val PERMISSION_REQUEST_WRITE_CONTACTS = 1
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (index < dupCounter-1) showPage(++index)
            return true
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (index > 0) showPage(--index)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.contacts_list_view)
        currentContactBeingShownTextView = findViewById(R.id.currentContactBeingShown)
        dupCountTextView = findViewById(R.id.dupCount)
        startButton = findViewById(R.id.findContacts)
        contactOptionsField = findViewById(R.id.contactOptionsField)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), PERMISSION_REQUEST_READ_CONTACTS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_CONTACTS), PERMISSION_REQUEST_WRITE_CONTACTS)
        }

        val handler = object: Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    0 -> {
                        val dups = msg.obj as? List<Contact>
                        dups?.let {
                            if (dups.isEmpty()) {
                                outerClass.get()?.searchHasFinished()
                            } else {
                                outerClass.get()?.addDupContact(it)
                            }
                        }
                    }
                }
            }
        }

        startButton.setOnClickListener {
            val contentResolver: ContentResolver = contentResolver
            if (!searchDone) {
                startButton.isEnabled = false
                startButton.text = "Buscando contactos duplicados..."
                val calcDupThread = Thread(CalcDupRunnable(contentResolver, handler))
                calcDupThread.start()
            } else {
                deleteAllUnselectedContacts(contentResolver)
                startButton.isEnabled = false
                contactOptionsField.isVisible = false
                currentContactBeingShownTextView.isVisible = false
            }
        }
    }

    fun addDupContact(dups: List<Contact>) {
        dupCounter++
        dupCountTextView.text = "Contactos duplicados: " + dupCounter.toString()
        val selPage = SelectionPage(dups, -1)
        dupContacts.add(selPage)
        if (dupCounter == 1) {
            showPage(0)
        }
    }

    fun searchHasFinished() {
        dupCountTextView.text = "Se han encontrado $dupCounter contactos\n duplicados en total."
        searchDone = true
        startButton.text = "Eliminar contactos duplicados"
        startButton.isEnabled = true
    }

    fun deleteAllUnselectedContacts(contentResolver: ContentResolver) {
        var count = 0
        for (page in dupContacts) {
            if (page.selected >= 0) {
                val listContacts = page.options
                for (i in 0 until listContacts.size) {
                    if (i != page.selected) {
                        count++
                        val contactUri: Uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, listContacts[i].id.toLong())
                        contentResolver.delete(contactUri, null, null)
                    }
                }
            }
        }
        dupCountTextView.text = "Se han eliminado $count\n contactos duplicados."
    }

    fun showPage(page: Int) {
        currentContactBeingShownTextView.text = "Mostrando conflicto ${index+1}"
        contactOptionsField.removeAllViews()
        val currentPage: SelectionPage = dupContacts[page]
        var count = 0

        val contactOptionsGroup = RadioGroup(this)
        contactOptionsGroup.setOnCheckedChangeListener(null)
        for (opts in currentPage.options) {
            checkOffset++
            val rb = RadioButton(this)
            rb.textSize = 18f
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 20)
            rb.layoutParams = params
            var numbersTxt = ""
            for (n in opts.numbers) numbersTxt += n+"\n"
            rb.text = opts.name+'\n'+"Numeros:\n"+numbersTxt
            rb.isChecked = (currentPage.selected == count)
            contactOptionsGroup.addView(rb)
            count++
        }
        contactOptionsGroup.setOnCheckedChangeListener { _, checkedId ->
            var realCheckedId = checkedId - checkOffset + currentPage.options.size
            currentPage.selected = realCheckedId
        }
        contactOptionsField.addView(contactOptionsGroup)
    }

    class CalcDupRunnable(private val contentResolver: ContentResolver, private val handler: Handler) : Runnable {
        private fun normalize(number: String): String {
            val regex = Regex("[^0-9+]")
            val numberClean = regex.replace(number, "")
            if (numberClean.startsWith("+")) {
                numberClean.removeRange(0,3)
                if (numberClean.startsWith("9")) {
                    numberClean.removeRange(0,1)
                }
            }
            return numberClean
        }

        @SuppressLint("Range")
        private fun calcContacts(): ArrayList<Contact> {
            val contacts = ArrayList<Contact>()
            val cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                null,
                null,
                null
            )
            val count = cursor?.count ?: 0
            while (cursor?.moveToNext() == true) {
                val name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                val id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
                val numbers = getNumbers(id)
                if (name == null) {break}
                val contact = Contact(id, name, numbers, true)
                contacts.add(contact)
            }
            return contacts
        }

        @SuppressLint("Range")
        private fun getNumbers(contactId: String): List<String> {
            val numerosList = mutableListOf<String>()
            val contentResolver: ContentResolver = contentResolver

            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                arrayOf(contactId),
                null
            )
            while (cursor?.moveToNext() == true) {
                val numero =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                numerosList.add(numero)
            }
            cursor?.close()
            return numerosList
        }

        override fun run() {
            val contactList = calcContacts()
            Log.d("MENSAJE","Se ha terminado la buscqueda de contactos")
            for (i in contactList.indices) {
                val contact1 = contactList[i]
                if (!contact1.isUnique) continue
                val repeatedSet = mutableListOf<Contact>()
                for (number1 in contact1.numbers) {
                    var normNumber1 = normalize(number1)
                    for (j in i + 1 until contactList.size) {
                        val contact2 = contactList[j]
                        for (number2 in contact2.numbers) {
                            if (normNumber1 == normalize(number2)) {
                                if (contact1.isUnique) {
                                    repeatedSet.add(contact1)
                                    contact1.isUnique = false
                                }
                                repeatedSet.add(contact2)
                                contact2.isUnique = false
                                val messageObj = handler.obtainMessage(0, repeatedSet)
                                handler.sendMessage(messageObj)
                            }
                        }
                    }
                }
            }
            val messageObj = handler.obtainMessage(0, mutableListOf<Contact>())
            handler.sendMessage(messageObj)
        }
    }

    data class Contact(
        val id: String,
        val name: String,
        val numbers: List<String>,
        var isUnique: Boolean
    )

    data class SelectionPage(
        val options: List<Contact>,
        var selected: Int
    )
}