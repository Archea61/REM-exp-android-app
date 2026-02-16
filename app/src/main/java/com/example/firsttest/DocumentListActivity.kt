package com.example.firsttest

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.firsttest.MainMenuActivity.Companion
import com.example.firsttest.MainMenuActivity.Companion.URL_STATS
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray

data class DocItem(
    val id: Int,
    val regNumber: String,
    val title: String,
    val status: String,
    val typeName: String?
)

class DocumentListActivity : AppCompatActivity() {

    private val TAG = "DocList"
    private val URL_GET = "http://10.0.2.2/doc_api/get_documents.php"

    private lateinit var recycler: RecyclerView
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var searchView: SearchView
    private lateinit var emptyView: TextView
    private lateinit var fabAdd: FloatingActionButton

    private val data = ArrayList<DocItem>()
    private val filtered = ArrayList<DocItem>()
    private lateinit var adapter: DocAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_list)

        recycler = findViewById(R.id.docRecycler)
        swipe = findViewById(R.id.swipeRefresh)
        searchView = findViewById(R.id.searchView)
        emptyView = findViewById(R.id.emptyView)
        fabAdd = findViewById(R.id.fabAdd)

        recycler.layoutManager = LinearLayoutManager(this)
        adapter = DocAdapter(filtered) { item ->
            showDocumentOptions(item.id, item)
        }
        recycler.adapter = adapter

        swipe.setOnRefreshListener { loadDocuments() }

        fabAdd.setOnClickListener {
            startActivity(Intent(this, AddDocumentActivity::class.java))
        }

        setupSearch()

        swipe.isRefreshing = true
        loadDocuments()
    }


    private fun showDocumentOptions(docId: Int, item: DocItem) {
        val options = arrayOf("Редактировать", "Удалить", "Отмена")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(item.title)
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> { // Редактировать
                        val i = android.content.Intent(this, AddDocumentActivity::class.java)
                        i.putExtra("EXTRA_DOC_ID", docId)
                        startActivity(i)
                    }
                    1 -> { // Удалить
                        confirmAndDelete(docId)
                    }
                    2 -> dialog.dismiss()
                }
            }.show()
    }

    private fun confirmAndDelete(docId: Int) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Удалить документ")
            .setMessage("Вы уверены, что хотите удалить документ №$docId?")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Удалить") { _, _ ->
                performDelete(docId)
            }.show()
    }

    private fun performDelete(docId: Int) {
        val url = "http://10.0.2.2/doc_api/delete_document.php"
        val queue = Volley.newRequestQueue(this)
        val req = object : StringRequest(Method.POST, url,
            { resp ->
                try {
                    val obj = org.json.JSONObject(resp)
                    if (obj.optBoolean("success", false)) {
                        // удалить из списка локально и обновить адаптер
                        val idx = data.indexOfFirst { it.id == docId }
                        if (idx >= 0) {
                            data.removeAt(idx)
                            filtered.removeAll { it.id == docId }
                            adapter.notifyItemRemoved(idx)
                        } else {
                            // если не нашли — просто перезагрузить
                            loadDocuments()
                        }
                        android.widget.Toast.makeText(this, "Документ удалён", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(this, "Не удалось удалить: ${obj.optString("error")}", android.widget.Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(this, "Неправильный ответ сервера", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            { err ->
                android.widget.Toast.makeText(this, "Ошибка соединения при удалении", android.widget.Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf("id" to docId.toString())
            }
        }
        req.setShouldCache(false)
        queue.add(req)
    }


    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterList(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(newText ?: "")
                return true
            }
        })
    }


    private fun filterList(q: String) {
        val term = q.trim().lowercase()
        filtered.clear()
        if (term.isEmpty()) filtered.addAll(data)
        else data.forEach {
            if (it.title.lowercase().contains(term) ||
                it.regNumber.lowercase().contains(term)
            ) filtered.add(it)
        }
        adapter.notifyDataSetChanged()
        emptyView.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadDocuments() {
        swipe.isRefreshing = true
        val queue = Volley.newRequestQueue(this)
        val req = StringRequest(Request.Method.GET, URL_GET,
            { resp ->
                data.clear()
                filtered.clear()
                try {
                    val arr = JSONArray(resp)
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val item = DocItem(
                            id = o.optInt("id", 0),
                            regNumber = o.optString("reg_number", ""),
                            title = o.optString("title", "Без названия"),
                            status = o.optString("status", ""),
                            typeName = o.optString("type_name", null)
                        )
                        data.add(item)
                    }
                    filtered.addAll(data)
                    adapter.notifyDataSetChanged()
                    emptyView.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
                } catch (e: Exception) {
                    emptyView.visibility = View.VISIBLE
                } finally {
                    swipe.isRefreshing = false
                }
            },
            {
                swipe.isRefreshing = false
                emptyView.visibility = View.VISIBLE
            })
        req.setShouldCache(false)
        queue.add(req)
    }

    class DocAdapter(private val list: List<DocItem>, private val onClick: (DocItem) -> Unit) :
        RecyclerView.Adapter<DocAdapter.Holder>() {

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            private val title = view.findViewById<TextView>(R.id.docTitle)
            private val reg = view.findViewById<TextView>(R.id.docReg)
            private val status = view.findViewById<TextView>(R.id.docStatus)
            private val type = view.findViewById<TextView>(R.id.docType)
            private val card = view.findViewById<androidx.cardview.widget.CardView>(R.id.cardContainer)

            fun bind(item: DocItem) {
                title.text = item.title
                reg.text = "№ ${item.regNumber}"
                status.text = "Статус: ${item.status}"
                type.text = "Тип: ${item.typeName ?: "-"}"
                card.setOnClickListener { onClick(item) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.document_item, parent, false)
            return Holder(v)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(list[position])
        }

        override fun getItemCount(): Int = list.size
    }
}
