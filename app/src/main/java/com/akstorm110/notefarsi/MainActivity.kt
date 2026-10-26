package com.akstorm110.notefarsi

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

private data class Note(val id: Long, var title: String, var body: String, var updated: Long)

class MainActivity : Activity() {
    private val notes = mutableListOf<Note>()
    private lateinit var list: LinearLayout
    private lateinit var search: EditText
    private val prefs by lazy { getSharedPreferences("notes_store", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(20, 20, 20)
        window.navigationBarColor = android.graphics.Color.rgb(20, 20, 20)
        loadNotes()
        render()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(10))
            setBackgroundColor(android.graphics.Color.rgb(247, 246, 242))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val header = TextView(this).apply {
            text = "یادداشت‌های من"
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(android.graphics.Color.rgb(25, 25, 25))
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(12))
        }
        root.addView(header, LinearLayout.LayoutParams(-1, dp(52)))

        search = EditText(this).apply {
            hint = "جستجو در یادداشت‌ها"
            textSize = 15f
            setSingleLine(true)
            setPadding(dp(12), 0, dp(12), 0)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(android.graphics.Color.WHITE)
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(48)).apply { bottomMargin = dp(10) })

        val add = Button(this).apply {
            text = "+ یادداشت جدید"
            textSize = 16f
            setOnClickListener { editNote(null) }
        }
        root.addView(add, LinearLayout.LayoutParams(-1, dp(50)).apply { bottomMargin = dp(10) })

        val scroll = ScrollView(this)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        search.addTextChangedListener(SimpleTextWatcher { populate() })
        populate()
    }

    private fun populate() {
        list.removeAllViews()
        val q = search.text.toString().trim()
        val shown = notes.filter { it.title.contains(q, true) || it.body.contains(q, true) }
            .sortedByDescending { it.updated }
        if (shown.isEmpty()) {
            list.addView(TextView(this).apply {
                text = if (q.isEmpty()) "هنوز یادداشتی نداری. از دکمه بالا شروع کن." else "یادداشتی پیدا نشد."
                textSize = 15f
                gravity = Gravity.CENTER
                setTextColor(android.graphics.Color.DKGRAY)
                setPadding(dp(12), dp(28), dp(12), dp(28))
            })
        }
        shown.forEach { note ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                setBackgroundColor(android.graphics.Color.WHITE)
            }
            card.addView(TextView(this).apply {
                text = note.title.ifBlank { "بدون عنوان" }
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(android.graphics.Color.rgb(30, 30, 30))
                gravity = Gravity.RIGHT
            })
            if (note.body.isNotBlank()) card.addView(TextView(this).apply {
                text = note.body
                textSize = 14f
                maxLines = 3
                setTextColor(android.graphics.Color.DKGRAY)
                gravity = Gravity.RIGHT
                setPadding(0, dp(6), 0, 0)
            })
            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.LEFT
            }
            actions.addView(Button(this).apply {
                text = "ویرایش"
                setOnClickListener { editNote(note) }
            })
            actions.addView(Button(this).apply {
                text = "حذف"
                setOnClickListener { confirmDelete(note) }
            })
            card.addView(actions)
            card.setOnClickListener { editNote(note) }
            list.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        }
    }

    private fun editNote(existing: Note?) {
        val title = EditText(this).apply {
            hint = "عنوان (اختیاری)"
            setText(existing?.title ?: "")
            textSize = 16f
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        val body = EditText(this).apply {
            hint = "متن یادداشت"
            setText(existing?.body ?: "")
            textSize = 16f
            gravity = Gravity.TOP or Gravity.RIGHT
            minLines = 6
            maxLines = 12
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(title, LinearLayout.LayoutParams(-1, dp(52)))
            addView(body, LinearLayout.LayoutParams(-1, dp(180)))
        }
        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "یادداشت جدید" else "ویرایش یادداشت")
            .setView(box)
            .setPositiveButton("ذخیره") { _, _ ->
                val t = title.text.toString().trim()
                val b = body.text.toString().trim()
                if (t.isNotEmpty() || b.isNotEmpty()) {
                    if (existing == null) notes.add(Note(System.currentTimeMillis(), t, b, System.currentTimeMillis()))
                    else {
                        existing.title = t
                        existing.body = b
                        existing.updated = System.currentTimeMillis()
                    }
                    saveNotes()
                    populate()
                }
            }
            .setNegativeButton("انصراف", null)
            .show()
    }

    private fun confirmDelete(note: Note) {
        AlertDialog.Builder(this)
            .setTitle("حذف یادداشت")
            .setMessage("این یادداشت حذف شود؟")
            .setNegativeButton("انصراف", null)
            .setPositiveButton("حذف") { _, _ ->
                notes.removeAll { it.id == note.id }
                saveNotes()
                populate()
            }.show()
    }

    private fun loadNotes() {
        try {
            val array = JSONArray(prefs.getString("notes", "[]"))
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                notes.add(Note(o.optLong("id"), o.optString("title"), o.optString("body"), o.optLong("updated")))
            }
        } catch (_: Exception) { notes.clear() }
    }

    private fun saveNotes() {
        val array = JSONArray()
        notes.forEach {
            array.put(JSONObject().put("id", it.id).put("title", it.title).put("body", it.body).put("updated", it.updated))
        }
        prefs.edit().putString("notes", array.toString()).apply()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

private class SimpleTextWatcher(val changed: () -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { changed() }
    override fun afterTextChanged(s: android.text.Editable?) {}
}
