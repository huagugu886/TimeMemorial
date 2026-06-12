package com.timememorial.app.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.timememorial.app.R
import com.timememorial.app.data.model.Anniversary
import com.timememorial.app.data.model.Memory

class HomeFragment : Fragment() {

    private lateinit var rvMemories: RecyclerView
    private lateinit var rvAnniversaries: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var catTagsContainer: LinearLayout
    private lateinit var tvStatUpcoming: TextView
    private lateinit var tvStatCompleted: TextView

    private var allAnniversaries = listOf<Anniversary>()
    private var allMemories = listOf<Memory>()
    private var selectedCategory = "all"

    private val categories = listOf(
        "all" to "全部",
        "love" to "爱情",
        "birthday" to "生日",
        "travel" to "旅行",
        "work" to "工作",
        "favorite" to "收藏",
        "other" to "其他"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvMemories = view.findViewById(R.id.rvMemories)
        rvAnniversaries = view.findViewById(R.id.rvAnniversaries)
        etSearch = view.findViewById(R.id.etSearch)
        catTagsContainer = view.findViewById(R.id.catTagsContainer)
        tvStatUpcoming = view.findViewById(R.id.tvStatUpcoming)
        tvStatCompleted = view.findViewById(R.id.tvStatCompleted)

        loadData()
        setupCategoryTags()
        setupRecyclerViews()
        setupSearch()
        updateStats()
    }

    private fun loadData() {
        allMemories = listOf(
            Memory(1, "第一次约会", "2024-03-14", "love", R.drawable.bg_memory_cover_love),
            Memory(2, "毕业旅行", "2024-06-20", "travel", R.drawable.bg_memory_cover_travel),
            Memory(3, "生日派对", "2024-09-15", "birthday", R.drawable.bg_memory_cover_birthday),
            Memory(4, "项目上线", "2024-11-01", "work", R.drawable.bg_memory_cover_work)
        )
        allAnniversaries = listOf(
            Anniversary(1, "结婚纪念日", "携手走过的每一天都是最好的礼物", "2024-06-15", "love", 128, 365),
            Anniversary(2, "妈妈的生日", "记得买蛋糕和花", "2026-07-20", "birthday", 45, 365),
            Anniversary(3, "东京之旅", "去看樱花和霓虹灯下的城市", "2026-12-22", "travel", 200, 365),
            Anniversary(4, "项目里程碑", "已完成", "2025-12-01", "work", 0, 180, true),
            Anniversary(5, "读书笔记100篇", "每天进步一点点", "2026-09-10", "favorite", 89, 200)
        )
    }

    private fun setupCategoryTags() {
        catTagsContainer.removeAllViews()
        val ctx = requireContext()
        for ((key, label) in categories) {
            val tag = TextView(ctx).apply {
                text = label
                textSize = 13f
                setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                isClickable = true
                isFocusable = true
                tag = key
                setBackgroundResource(R.drawable.bg_tag_pill_selector)
                if (key == selectedCategory) {
                    isSelected = true
                    setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
                    elevation = dpToPx(4).toFloat()
                } else {
                    isSelected = false
                    setTextColor(ContextCompat.getColor(ctx, R.color.miui_text_secondary))
                    elevation = 0f
                }
            }
            tag.setOnClickListener { v ->
                selectedCategory = v.tag as String
                setupCategoryTags()
                filterData()
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dpToPx(8) }
            catTagsContainer.addView(tag, params)
        }
    }

    private fun setupRecyclerViews() {
        rvMemories.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvMemories.adapter = MemoryAdapter(allMemories)
        rvAnniversaries.layoutManager = LinearLayoutManager(requireContext())
        rvAnniversaries.adapter = AnniversaryAdapter(allAnniversaries)
        rvAnniversaries.isNestedScrollingEnabled = false
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { filterData() }
        })
    }

    private fun filterData() {
        val query = etSearch.text.toString().trim().lowercase()
        val filteredA = allAnniversaries.filter { item ->
            (selectedCategory == "all" || item.tag == selectedCategory) &&
            (query.isEmpty() || item.title.lowercase().contains(query) || item.desc.lowercase().contains(query))
        }
        val filteredM = allMemories.filter { item ->
            (selectedCategory == "all" || item.tag == selectedCategory) &&
            (query.isEmpty() || item.title.lowercase().contains(query))
        }
        rvAnniversaries.adapter = AnniversaryAdapter(filteredA)
        rvMemories.adapter = MemoryAdapter(filteredM)
    }

    private fun updateStats() {
        tvStatUpcoming.text = allAnniversaries.count { !it.isExpired }.toString()
        tvStatCompleted.text = allAnniversaries.count { it.isExpired }.toString()
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
