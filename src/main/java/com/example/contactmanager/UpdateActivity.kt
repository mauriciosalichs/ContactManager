package com.example.contactmanager

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.ContactsContract
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TableRow
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import java.lang.ref.WeakReference


class UpdateActivity : Activity() {

    private lateinit var currentContactBeingShownTextView: TextView
    private lateinit var infoLabel: TextView
    private lateinit var contactOptionsField: TableRow
    private lateinit var startButton: Button
    private val outerClass = WeakReference<UpdateActivity>(this)

    var dupCounter = 0
    var index = 0
    var checkOffset = 1
    var searchDone = false
    val dupContacts = mutableListOf<SelectionPage>()

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
        setContentView(R.layout.update_view)
        val nameMap = applicationContext.getSharedPreferences("nameMap", Context.MODE_PRIVATE)
        val numberMap = applicationContext.getSharedPreferences("numberMap", Context.MODE_PRIVATE)
        currentContactBeingShownTextView = findViewById(R.id.currentContactBeingShown)
        infoLabel = findViewById(R.id.infoLabel)
        startButton = findViewById(R.id.updateButton)
        contactOptionsField = findViewById(R.id.contactOptionsField)
        var firstTime = false

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_CONTACTS), 1)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_CONTACTS), 1)
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
                    1 -> {
                        val count = msg.obj as? String
                        count?.let {
                            outerClass.get()?.updateHasFinished(count)
                        }
                    }
                }
            }
        }
        val updateRunnable = UpdateRunnable(contentResolver, nameMap, numberMap, handler)

        startButton.setOnClickListener {
            val contentResolver: ContentResolver = contentResolver
            if (!searchDone) {
                if (firstTime) {
                    firstTime = false
                    Thread(updateRunnable).start()
                    startButton.text = "Actualizando base de datos..."
                } else {
                    val calcDupRunnable = CalcDupRunnable(contentResolver, handler)
                    Thread(calcDupRunnable).start()
                    startButton.text = "Buscando contactos duplicados..."
                }
                startButton.isEnabled = false
            } else {
                deleteAllUnselectedContacts(contentResolver)
                startButton.isEnabled = false
                contactOptionsField.isVisible = false
                currentContactBeingShownTextView.isVisible = false
            }
        }

        val extras = intent.extras
        if (extras == null) {
            Log.d("FIRST TIME","blabla")
            //startButton.performClick()
            firstTime = true
            infoLabel.text = "Se aconseja actualizar la base de datos en la primera ejecución."
        }
    }

    fun addDupContact(dups: List<Contact>) {
        dupCounter++
        infoLabel.text = "Contactos duplicados: " + dupCounter.toString()
        val selPage = SelectionPage(dups, -1)
        dupContacts.add(selPage)
        if (dupCounter == 1) {
            showPage(0)
        }
    }

    fun updateHasFinished(count: String) {
        infoLabel.text = "Se han agregado $count contactos\n a la base de datos interna."
        startButton.text = "Actualizar base de datos"
        startButton.isEnabled = true
    }

    fun searchHasFinished() {
        infoLabel.text = "Se han encontrado $dupCounter contactos\n duplicados en total."
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
        infoLabel.text = "Se han eliminado $count\n contactos duplicados."
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

    class UpdateRunnable(private val contentResolver: ContentResolver,
                         private val nameMap: SharedPreferences,
                         private val numberMap: SharedPreferences,
                         private val handler: Handler) : Runnable {

        private fun normalize(number: String): String {
            val regex = Regex("[^0-9+]")
            var numberClean = regex.replace(number, "")
            if (numberClean[0] == '+') {
                numberClean = numberClean.removeRange(0,3)
                if (numberClean[0] == '9') numberClean = numberClean.removeRange(0, 1)
            }
            return numberClean
        }

        @SuppressLint("Range")
        private fun getNumbers(contactId: String): List<String> {
            val numbersList = mutableListOf<String>()
            val contentResolver: ContentResolver = contentResolver

            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                arrayOf(contactId),
                null
            )
            while (cursor?.moveToNext() == true) {
                val number =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                numbersList.add(number)
            }
            cursor?.close()
            return numbersList
        }

        @SuppressLint("Range")
        override fun run() {
            val cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                null,
                null,
                null
            )
            val count = cursor?.count ?: 0
            var addedCount = 0
            val nameMapEditor : SharedPreferences.Editor = nameMap.edit()
            val numberMapEditor : SharedPreferences.Editor = numberMap.edit()
            nameMapEditor.clear().apply()
            numberMapEditor.clear().apply()

            while (cursor?.moveToNext() == true) {
                val name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                if (name == null) {continue}
                if (nameMap.all.containsKey(name)) {
                    Log.d("CONTACT","Nombre repetido: $name")
                    continue
                }
                val id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
                val numbers = getNumbers(id)
                val contactNumbers = "$id;${numbers.toString()}"
                nameMapEditor.putString(name, contactNumbers)
                for (number in numbers) {
                    val incNumber = normalize(number)
                    if (numberMap.all.containsKey(incNumber)) {
                        Log.d("CONTACT","Numero repetido: $incNumber")
                        continue
                    }
                    val contactName = "$id;$name"
                    numberMapEditor.putString(incNumber, contactName)
                }
                addedCount++
            }
            nameMapEditor.apply()
            numberMapEditor.apply()
            Log.d("CONTACT","TERMINADO")
            val messageObj = handler.obtainMessage(1, addedCount.toString())
            handler.sendMessage(messageObj)
        }
    }

    class CalcDupRunnable(private val contentResolver: ContentResolver, private val handler: Handler) : Runnable {
        private fun normalize(number: String): String {
            val regex = Regex("[^0-9+]")
            var numberClean = regex.replace(number, "")
            if (numberClean[0] == '+') {
                numberClean = numberClean.removeRange(0,3)
                if (numberClean[0] == '9') numberClean = numberClean.removeRange(0, 1)
            }
            return numberClean
        }

        @SuppressLint("Range")
        private fun getNumbers(contactId: String): List<String> {
            val numbersList = mutableListOf<String>()
            val contentResolver: ContentResolver = contentResolver

            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                arrayOf(contactId),
                null
            )
            while (cursor?.moveToNext() == true) {
                val number =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                numbersList.add(number)
            }
            cursor?.close()
            return numbersList
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

        override fun run() {
            val contactList = calcContacts()
            val numberHash = HashMap<String, String>()
            val idsHash = HashMap<String, List<String>>()
            Log.d("MENSAJE","Se ha terminado la busqueda de contactos")

            // We separate ids with same numbers in idsHash map
            for (contact in contactList) {
                idsHash[contact.id] = mutableListOf<String>()
                val unrepeatedNumbersSet = mutableSetOf<String>()
                for (number in contact.numbers) unrepeatedNumbersSet.add(normalize(number))
                Log.d("MENSAJE","A ${contact.name}: $unrepeatedNumbersSet")
                for (number in unrepeatedNumbersSet) {
                    if (numberHash.containsKey(number)) {
                        val id = numberHash[number]!!
                        val existingList = idsHash[id]!!.toMutableList()
                        existingList.add(contact.id)
                        idsHash[id] = existingList
                    } else {
                        numberHash[number] = contact.id
                    }
                }
            }
            // For every non-empty value of idsHash, we send a List to the main activity
            for (id in idsHash) {
                if (id.value.isNotEmpty()) {
                    val repeatedSet = mutableListOf<Contact>()
                    val ids = id.value.toMutableList()
                    ids.add(id.key)
                    for (i in ids) repeatedSet.add(contactList.find { it.id == i }!!)
                    val messageObj = handler.obtainMessage(0, repeatedSet)
                    handler.sendMessage(messageObj)
                }
            }
            // We send a final message to tell the thread is over
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