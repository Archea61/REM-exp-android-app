package com.example.firsttest

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject

class AddDocumentActivity : AppCompatActivity() {

    private lateinit var etTitle: TextInputEditText
    private lateinit var etRegNumber: TextInputEditText
    private lateinit var etStatus: TextInputEditText
    private lateinit var etType: TextInputEditText
    private lateinit var btnSave: MaterialButton

    private var editingDocId: Int = 0
    private var authorId: Int = 1

    private val BASE = "http://10.0.2.2/doc_api/"
    private val URL_ADD = BASE + "add_document.php"
    private val URL_UPDATE = BASE + "update_document.php"
    private val URL_GET_BY_ID = BASE + "get_documents_by_id.php"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_document)

        etTitle = findViewById(R.id.etTitle)
        etRegNumber = findViewById(R.id.etRegNumber)
        etStatus = findViewById(R.id.etStatus)
        etType = findViewById(R.id.etType)
        btnSave = findViewById(R.id.btnSave)

        editingDocId = intent.getIntExtra("EXTRA_DOC_ID", 0)
        authorId = intent.getIntExtra("EXTRA_AUTHOR_ID", 1)

        if (editingDocId > 0) {
            btnSave.text = "Сохранить изменения"
            loadDocumentForEdit(editingDocId)
        } else {
            btnSave.text = "Создать документ"
        }

        btnSave.setOnClickListener { onSaveClicked() }
    }

    private fun loadDocumentForEdit(id: Int) {
        val url = "$URL_GET_BY_ID?id=$id"
        btnSave.isEnabled = false
        val queue = Volley.newRequestQueue(this)
        val req = StringRequest(Request.Method.GET, url,
            { resp ->
                btnSave.isEnabled = true
                val r = resp.trim()
                try {
                    val obj = JSONObject(r)
                    // если сервер вернул объект
                    etTitle.setText(obj.optString("title", ""))
                    etRegNumber.setText(obj.optString("reg_number", ""))
                    etStatus.setText(obj.optString("status", ""))
                    etType.setText(obj.optString("type_name", ""))
                } catch (e: Exception) {
                    // Возможно сервер вернул массив или пустой ответ
                    try {
                        val arr = org.json.JSONArray(r)
                        if (arr.length() > 0) {
                            val o = arr.getJSONObject(0)
                            etTitle.setText(o.optString("title", ""))
                            etRegNumber.setText(o.optString("reg_number", ""))
                            etStatus.setText(o.optString("status", ""))
                            etType.setText(o.optString("type_name", ""))
                        } else {
                            Toast.makeText(this, "Документ не найден", Toast.LENGTH_SHORT).show()
                        }
                    } catch (ex: Exception) {
                        Toast.makeText(this, "Ошибка парсинга ответа сервера", Toast.LENGTH_LONG).show()
                    }
                }
            },
            { err ->
                btnSave.isEnabled = true
                Toast.makeText(this, "Ошибка загрузки документа: ${err.message}", Toast.LENGTH_LONG).show()
            })
        req.setShouldCache(false)
        queue.add(req)
    }

    private fun onSaveClicked() {
        val title = etTitle.text?.toString()?.trim() ?: ""
        val reg = etRegNumber.text?.toString()?.trim() ?: ""
        val status = etStatus.text?.toString()?.trim() ?: ""
        val typeText = etType.text?.toString()?.trim() ?: ""

        if (title.isEmpty()) {
            Toast.makeText(this, "Введите название документа", Toast.LENGTH_SHORT).show()
            return
        }

        if (editingDocId == 0 && reg.isEmpty()) {
            Toast.makeText(this, "Введите регистрационный номер", Toast.LENGTH_SHORT).show()
            return
        }

        if (authorId <= 0) {
            Toast.makeText(this, "Не определён автор документа (author_id). Передайте EXTRA_AUTHOR_ID.", Toast.LENGTH_LONG).show()
            return
        }

        if (editingDocId == 0) {
            createDocument(title, reg, typeText, status, authorId)
        } else {
            updateDocument(editingDocId, title, typeText, status)
        }
    }

    private fun createDocument(
        title: String,
        regNumber: String,
        typeName: String,
        status: String,
        authorId: Int
    ) {
        btnSave.isEnabled = false
        val queue = Volley.newRequestQueue(this)
        val req = object : StringRequest(Method.POST, URL_ADD,
            { resp ->
                btnSave.isEnabled = true
                val r = resp.trim()
                try {
                    val obj = JSONObject(r)
                    if (obj.optBoolean("success", false)) {
                        Toast.makeText(this, "Документ создан (id=${obj.optInt("id", 0)})", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this, "Ошибка создания: ${obj.optString("error")}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    // для отладки покажем сырой ответ сервера
                    Toast.makeText(this, "Неправильный ответ сервера: $r", Toast.LENGTH_LONG).show()
                }
            },
            { err ->
                btnSave.isEnabled = true
                Toast.makeText(this, "Ошибка соединения: ${err.message}", Toast.LENGTH_LONG).show()
            }) {
            override fun getParams(): MutableMap<String, String> {
                val m = HashMap<String, String>()
                m["title"] = title
                m["reg_number"] = regNumber
                m["type_id"] = "0"                 // учебный вариант: 0 (если используете справочник типов — передавайте id)
                m["receiver"] = typeName           // временно передаём текст типа в receiver
                m["status"] = if (status.isEmpty()) "черновик" else status
                m["author_id"] = authorId.toString()
                m["due_date"] = ""                 // можно передать дату если есть
                return m
            }
        }
        req.setShouldCache(false)
        queue.add(req)
    }

    private fun updateDocument(
        docId: Int,
        title: String,
        typeName: String,
        status: String
    ) {
        btnSave.isEnabled = false
        val queue = Volley.newRequestQueue(this)
        val req = object : StringRequest(Method.POST, URL_UPDATE,
            { resp ->
                btnSave.isEnabled = true
                val r = resp.trim()
                try {
                    val obj = JSONObject(r)
                    if (obj.optBoolean("success", false)) {
                        Toast.makeText(this, "Изменения сохранены", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this, "Ошибка обновления: ${obj.optString("error")}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Неправильный ответ сервера: $r", Toast.LENGTH_LONG).show()
                }
            },
            { err ->
                btnSave.isEnabled = true
                Toast.makeText(this, "Ошибка соединения: ${err.message}", Toast.LENGTH_LONG).show()
            }) {
            override fun getParams(): MutableMap<String, String> {
                val m = HashMap<String, String>()
                m["id"] = docId.toString()
                m["title"] = title
                m["receiver"] = typeName
                if (status.isNotEmpty()) m["status"] = status
                return m
            }
        }
        req.setShouldCache(false)
        queue.add(req)
    }
}
