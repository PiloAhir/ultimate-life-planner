package com.saltanat45.planner

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.DecimalFormat

class MainActivity : Activity() {

    private val green = Color.rgb(24, 65, 48)
    private val gold = Color.rgb(218, 176, 93)
    private val cream = Color.rgb(246, 245, 238)

    private val goals = mutableListOf<String>()
    private val tasks = mutableListOf<JSONObject>()

    private lateinit var goalCount: TextView
    private lateinit var taskCount: TextView

    private val createBackupRequest = 1001
    private val restoreBackupRequest = 1002
    private val imageRequest = 1003
    private val videoRequest = 1004
    private var pendingMediaTaskIndex = -1

    private var pendingBackup: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadGoals()
        loadTasks()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cream)
        }

        val header = TextView(this).apply {
            text = "👑 SALTANAT 45\nMy Life Blueprint"
            textSize = 26f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setBackgroundColor(green)
            setPadding(20, 32, 20, 32)
        }
        root.addView(header)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 12, 18, 24)
        }

        addText(content, "તમારા સપનાની સફર અહીંથી શરૂ થાય છે", 18f, green)

        goalCount = TextView(this).apply {
            textSize = 16f
            setTextColor(green)
            setPadding(4, 8, 4, 8)
        }
        content.addView(goalCount)

        taskCount = TextView(this).apply {
            textSize = 16f
            setTextColor(green)
            setPadding(4, 8, 4, 8)
        }
        content.addView(taskCount)

        updateCounts()

        addButton(content, "🎯 મારા લક્ષ્યો") {
            showGoals()
        }

        addButton(content, "➕ નવું કામ ઉમેરો") {
            addTaskDialog()
        }

        addButton(content, "✅ મારાં કામ અને પ્રોગ્રેસ") {
            showTasks()
        }

        addButton(content, "💰 કુલ ખર્ચ") {
            showTotalCost()
        }

        addButton(content, "📔 નોંધો અને અનુભવો") {
            showNotes()
        }

        addButton(content, "💾 બેકઅપ ફાઇલ સાચવો") {
            startBackup()
        }

        addButton(content, "♻️ બેકઅપ રિસ્ટોર કરો") {
            confirmRestore()
        }

        addText(
            content,
            "તમારો પ્લાન. તમારી પ્રગતિ. તમારું ભવિષ્ય.",
            14f,
            green
        )

        val scroll = ScrollView(this)
        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
    }

    private fun prefs() = getSharedPreferences(
        "saltanat45_data",
        Context.MODE_PRIVATE
    )

    private fun loadGoals() {
        goals.clear()
        try {
            val array = JSONArray(prefs().getString("goals", "[]"))
            for (i in 0 until array.length()) {
                goals.add(array.getString(i))
            }
        } catch (_: Exception) {
            goals.clear()
        }
    }

    private fun saveGoals() {
        val array = JSONArray()
        goals.forEach { array.put(it) }
        prefs().edit().putString("goals", array.toString()).apply()
    }

    private fun loadTasks() {
        tasks.clear()
        try {
            val array = JSONArray(prefs().getString("tasks", "[]"))
            for (i in 0 until array.length()) {
                tasks.add(array.getJSONObject(i))
            }
        } catch (_: Exception) {
            tasks.clear()
        }
    }

    private fun saveTasks() {
        val array = JSONArray()
        tasks.forEach { array.put(it) }
        prefs().edit().putString("tasks", array.toString()).apply()
    }

    private fun updateCounts() {
        goalCount.text = "🎯 કુલ લક્ષ્યો: ${goals.size}"
        taskCount.text = "✅ કુલ કામ: ${tasks.size}"
    }

    private fun showGoals() {
        val input = EditText(this).apply {
            hint = "નવું લક્ષ્ય લખો"
        }

        val message = if (goals.isEmpty()) {
            "હજુ કોઈ લક્ષ્ય નથી."
        } else {
            goals.mapIndexed { i, g -> "${i + 1}. $g" }
                .joinToString("\n")
        }

        AlertDialog.Builder(this)
            .setTitle("🎯 મારા લક્ષ્યો")
            .setMessage(message)
            .setView(input)
            .setPositiveButton("ઉમેરો") { _, _ ->
                val value = input.text.toString().trim()
                if (value.isNotEmpty()) {
                    goals.add(value)
                    saveGoals()
                    updateCounts()
                    toast("લક્ષ્ય સાચવાયું ✅")
                } else {
                    toast("લક્ષ્યનું નામ લખો")
                }
            }
            .setNegativeButton("બંધ", null)
            .show()
    }

    private fun addTaskDialog() {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 8, 28, 4)
        }

        val name = EditText(this).apply {
            hint = "કામનું નામ"
        }
        form.addView(name)

        val goal = EditText(this).apply {
            hint = "સંબંધિત લક્ષ્ય (વૈકલ્પિક)"
        }
        form.addView(goal)

        val cost = EditText(this).apply {
            hint = "ખર્ચ રૂપિયા"
            inputType = 8194
            setText("0")
        }
        form.addView(cost)

        val notes = EditText(this).apply {
            hint = "નોંધ અથવા અનુભવ"
            minLines = 2
            gravity = Gravity.TOP
        }
        form.addView(notes)

        AlertDialog.Builder(this)
            .setTitle("➕ નવું કામ")
            .setView(form)
            .setPositiveButton("સાચવો") { _, _ ->
                val taskName = name.text.toString().trim()
                val amount = cost.text.toString().toDoubleOrNull()

                if (taskName.isEmpty()) {
                    toast("કામનું નામ લખો")
                } else if (amount == null || amount < 0) {
                    toast("ખર્ચની યોગ્ય રકમ લખો")
                } else {
                    val task = JSONObject().apply {
                        put("name", taskName)
                        put("goal", goal.text.toString().trim())
                        put("cost", amount)
                        put("notes", notes.text.toString().trim())
                        put("progress", 0)
                        put("status", "શરૂ કરવાનું બાકી")
                        put("media", JSONArray())
                    }

                    tasks.add(task)
                    saveTasks()
                    updateCounts()
                    toast("કામ સાચવાઈ ગયું ✅")
                }
            }
            .setNegativeButton("રદ કરો", null)
            .show()
    }

    private fun showTasks() {
        if (tasks.isEmpty()) {
            toast("હજુ કોઈ કામ ઉમેર્યું નથી.")
            return
        }

        val items = tasks.mapIndexed { i, t ->
            "${i + 1}. ${t.optString("name")} — ${t.optInt("progress")}%"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("✅ મારાં કામ")
            .setItems(items) { _, which ->
                editTask(which)
            }
            .setNegativeButton("બંધ", null)
            .show()
    }

    private fun editTask(index: Int) {
        val task = tasks[index]

        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 8, 28, 4)
        }

        val progress = EditText(this).apply {
            hint = "પ્રોગ્રેસ 0 થી 100"
            inputType = 2
            setText(task.optInt("progress").toString())
        }
        form.addView(progress)

        val statuses = arrayOf(
            "શરૂ કરવાનું બાકી",
            "ચાલુ છે",
            "પૂર્ણ થયું",
            "થોભાવ્યું"
        )

        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            statuses
        )

        val oldStatus = task.optString("status", statuses[0])
        spinner.setSelection(statuses.indexOf(oldStatus).coerceAtLeast(0))
        form.addView(spinner)

        val cost = EditText(this).apply {
            hint = "ખર્ચ રૂપિયા"
            inputType = 8194
            setText(task.optDouble("cost", 0.0).toString())
        }
        form.addView(cost)

        val notes = EditText(this).apply {
            hint = "નોંધ અને અનુભવ"
            minLines = 2
            gravity = Gravity.TOP
            setText(task.optString("notes"))
        }
        form.addView(notes)
        val mediaButton = Button(this).apply {
            text = "📸 ફોટા અને વીડિયો મેનેજ કરો"
            isAllCaps = false
        }
        form.addView(mediaButton)
        mediaButton.setOnClickListener {
            manageMedia(index)
        }

        AlertDialog.Builder(this)
            .setTitle(task.optString("name"))
            .setView(form)
            .setPositiveButton("સાચવો") { _, _ ->
                val p = progress.text.toString().toIntOrNull()
                val c = cost.text.toString().toDoubleOrNull()

                if (p == null || p !in 0..100 || c == null || c < 0) {
                    toast("પ્રોગ્રેસ 0-100 અને ખર્ચ યોગ્ય લખો")
                } else {
                    task.put("progress", p)
                    task.put("status", spinner.selectedItem.toString())
                    task.put("cost", c)
                    task.put("notes", notes.text.toString())

                    saveTasks()
                    updateCounts()
                    toast("કામની વિગતો સાચવાઈ ✅")
                }
            }
            .setNeutralButton("કામ કાઢી નાખો") { _, _ ->
                AlertDialog.Builder(this)
                    .setMessage("આ કામ કાઢી નાખવું છે?")
                    .setPositiveButton("હા") { _, _ ->
                        tasks.removeAt(index)
                        saveTasks()
                        updateCounts()
                        toast("કામ કાઢી નાખ્યું")
                    }
                    .setNegativeButton("ના", null)
                    .show()
            }
            .setNegativeButton("રદ કરો", null)
            .show()
    }

    private fun showTotalCost() {
        val total = tasks.sumOf {
            it.optDouble("cost", 0.0)
        }
        val format = DecimalFormat("#,##0.00")
        toast("બધાં કામનો કુલ ખર્ચ: ₹${format.format(total)}")
    }

    private fun showNotes() {
        val notes = tasks.mapNotNull {
            val note = it.optString("notes").trim()
            if (note.isNotEmpty()) {
                "${it.optString("name")}\n$note"
            } else {
                null
            }
        }

        AlertDialog.Builder(this)
            .setTitle("📔 નોંધો અને અનુભવો")
            .setMessage(
                if (notes.isEmpty()) "હજુ કોઈ નોંધ નથી."
                else notes.joinToString("\n\n──────────\n\n")
            )
            .setPositiveButton("બરાબર", null)
            .show()
    }

    private fun startBackup() {
        pendingBackup = buildBackupJson()

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(
                Intent.EXTRA_TITLE,
                "SALTANAT45-backup.json"
            )
        }

        startActivityForResult(intent, createBackupRequest)
    }

    private fun buildBackupJson(): String {
        val backup = JSONObject()
        backup.put("app", "SALTANAT 45")
        backup.put("version", 1)
        backup.put("goals", JSONArray(prefs().getString("goals", "[]")))
        backup.put("tasks", JSONArray(prefs().getString("tasks", "[]")))
        return backup.toString(2)
    }

    private fun confirmRestore() {
        AlertDialog.Builder(this)
            .setTitle("♻️ બેકઅપ રિસ્ટોર")
            .setMessage(
                "બેકઅપમાંથી ડેટા પાછો મેળવશો તો હાલનાં " +
                "લક્ષ્યો અને કામ બદલાઈ જશે. આગળ વધતાં પહેલાં " +
                "હાલનો બેકઅપ બનાવી રાખો."
            )
            .setPositiveButton("ફાઇલ પસંદ કરો") { _, _ ->
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
                startActivityForResult(intent, restoreBackupRequest)
            }
            .setNegativeButton("રદ કરો", null)
            .show()
    }

    @Deprecated("Uses Android document picker result")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != RESULT_OK || data?.data == null) {
            pendingBackup = null
            return
        }

        val uri = data.data!!

        when (requestCode) {
            createBackupRequest -> writeBackup(uri)
            restoreBackupRequest -> readBackup(uri)
        }
        
   imageRequest -> {
        val index = pendingMediaTaskIndex
        if (index >= 0) {
            val clipData = data?.clipData

            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    saveMediaReference(
                        index,
                        clipData.getItemAt(i).uri,
                        "image"
                    )
                }
            } else {
                saveMediaReference(index, uri, "image")
            }
        }
    }

    videoRequest -> {
        val index = pendingMediaTaskIndex
        if (index >= 0) {
            saveMediaReference(index, uri, "video")
        }
    }
        }
    }

    private fun writeBackup(uri: Uri) {
        try {
            val json = pendingBackup ?: buildBackupJson()
            contentResolver.openOutputStream(uri)?.use { output ->
                output.write(json.toByteArray(Charsets.UTF_8))
            } ?: throw Exception("ફાઇલ ખોલી શકાઈ નથી.")

            pendingBackup = null
            toast("બેકઅપ ફાઇલ સફળતાપૂર્વક સાચવાઈ ગઈ ✅")
        } catch (e: Exception) {
            pendingBackup = null
            toast("બેકઅપ નિષ્ફળ: ${e.message}")
        }
    }

    private fun readBackup(uri: Uri) {
        try {
            val json = contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8))
                    .readText()
            } ?: throw Exception("ફાઇલ વાંચી શકાઈ નથી.")

            val backup = JSONObject(json)

            require(backup.optString("app") == "SALTANAT 45") {
                "આ SALTANAT 45 ની બેકઅપ ફાઇલ નથી."
            }

            val goalsArray = backup.getJSONArray("goals")
            val tasksArray = backup.getJSONArray("tasks")

            for (i in 0 until goalsArray.length()) {
                require(goalsArray.get(i) is String) {
                    "લક્ષ્યોની માહિતી ખોટી છે."
                }
            }

            for (i in 0 until tasksArray.length()) {
                val task = tasksArray.getJSONObject(i)
                require(task.has("name")) {
                    "કામની માહિતી ખોટી છે."
                }

                val progress = task.optInt("progress", 0)
                require(progress in 0..100) {
                    "પ્રોગ્રેસની માહિતી ખોટી છે."
                }

                val cost = task.optDouble("cost", 0.0)
                require(cost >= 0) {
                    "ખર્ચની માહિતી ખોટી છે."
                }
            }

            AlertDialog.Builder(this)
                .setTitle("બેકઅપ રિસ્ટોર કરવો?")
                .setMessage(
                    "${goalsArray.length()} લક્ષ્યો અને " +
                    "${tasksArray.length()} કામ લોડ થશે. " +
                    "હાલનો ડેટા બદલાઈ જશે."
                )
                .setPositiveButton("રિસ્ટોર કરો") { _, _ ->
                    try {
                        val success = prefs().edit()
                            .putString("goals", goalsArray.toString())
                            .putString("tasks", tasksArray.toString())
                            .commit()

                        if (!success) {
                            throw Exception("ડેટા સાચવી શકાયો નથી.")
                        }

                        loadGoals()
                        loadTasks()
                        updateCounts()
                        toast("બેકઅપ સફળતાપૂર્વક રિસ્ટોર થયો ✅")
                    } catch (e: Exception) {
                        toast("રિસ્ટોર નિષ્ફળ: ${e.message}")
                    }
                }
                .setNegativeButton("રદ કરો", null)
                .show()

        } catch (e: Exception) {
            toast("બેકઅપ ફાઇલ ખોટી છે: ${e.message}")
        }
    }

    private fun addText(
        parent: LinearLayout,
        value: String,
        size: Float,
        color: Int
    ) {
        val text = TextView(this).apply {
            this.text = value
            textSize = size
            setTextColor(color)
            setPadding(4, 12, 4, 12)
        }
        parent.addView(text)
    }

    private fun addButton(
        parent: LinearLayout,
        label: String,
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(green)
            setOnClickListener { action() }
        }

        parent.addView(
            button,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    private fun manageMedia(index: Int) {
    val options = arrayOf(
        "📸 ફોટા પસંદ કરો",
        "🎥 વીડિયો પસંદ કરો",
        "📂 જોડેલી ફાઇલો જુઓ"
    )

    AlertDialog.Builder(this)
        .setTitle("મીડિયા મેનેજ કરો")
        .setItems(options) { _, choice ->
            pendingMediaTaskIndex = index

            when (choice) {
                0 -> {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "image/*"
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                    startActivityForResult(intent, imageRequest)
                }

                1 -> {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "video/*"
                    }
                    startActivityForResult(intent, videoRequest)
                }

                2 -> showTaskMedia(index)
            }
        }
        .setNegativeButton("બંધ", null)
        .show()
}
    private fun saveMediaReference(index: Int, uri: Uri, type: String) {
    try {
        val task = tasks[index]
        val media = task.optJSONArray("media") ?: JSONArray()

        val item = JSONObject().apply {
            put("uri", uri.toString())
            put("type", type)
            put("name", uri.lastPathSegment ?: "Media file")
        }

        media.put(item)
        task.put("media", media)

        saveTasks()
        toast("મીડિયા ઉમેરાયું ✅")
    } catch (e: Exception) {
        toast("મીડિયા સાચવી શકાયું નથી: ${e.message}")
    }
}
private fun showTaskMedia(index: Int) {
    val media = tasks[index].optJSONArray("media") ?: JSONArray()

    if (media.length() == 0) {
        toast("આ કામ સાથે કોઈ ફોટો કે વીડિયો જોડાયેલો નથી.")
        return
    }

    val items = Array(media.length()) { i ->
        val item = media.getJSONObject(i)
        val icon = if (item.optString("type") == "video") "🎥" else "📸"
        "$icon ${item.optString("name")}"
    }
   AlertDialog.Builder(this)
        .setTitle("જોડેલી ફાઇલો")
        .setItems(items) { _, which ->
            try {
                val item = media.getJSONObject(which)
                val uri = Uri.parse(item.getString("uri"))
                val mime = if (item.optString("type") == "video") "video/*" else "image/*"

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mime)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(intent)
            } catch (e: Exception) {
                toast("ફાઇલ ખોલી શકાયી નથી: ${e.message}")
            }
        }
        .setNegativeButton("બંધ", null)
        .show()
}
}
