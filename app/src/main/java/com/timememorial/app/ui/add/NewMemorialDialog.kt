package com.timememorial.app.ui.add

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.timememorial.app.R
import java.util.Calendar

class NewMemorialDialog : BottomSheetDialogFragment() {

    private var selectedDate: String = ""
    private var selectedCategory: String = "love"

    private val categories = listOf(
        "love" to "爱情",
        "birthday" to "生日",
        "travel" to "旅行",
        "work" to "工作",
        "favorite" to "收藏",
        "other" to "其他"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_new_memorial, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etDate = view.findViewById<EditText>(R.id.etDate)
        val formCats = view.findViewById<LinearLayout>(R.id.formCats)
        val btnCreate = view.findViewById<MaterialButton>(R.id.btnCreate)

        // Date picker
        etDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                selectedDate = "%04d-%02d-%02d".format(year, month + 1, day)
                etDate.setText(selectedDate)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Category chips
        setupFormCategories(formCats)

        // Create button
        btnCreate.setOnClickListener {
            val name = view.findViewById<EditText>(R.id.etName).text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(context, "请输入名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Toast.makeText(context, "已创建: $name", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun setupFormCategories(container: LinearLayout) {
        container.removeAllViews()
        val ctx = requireContext()

        for ((key, label) in categories) {
            val chip = TextView(ctx).apply {
                text = label
                textSize = 13f
                setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                isClickable = true
                isFocusable = true
                tag = key

                if (key == selectedCategory) {
                    setBackgroundResource(R.drawable.bg_tag_pill_selector)
                    isSelected = true
                    setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
                    elevation = dpToPx(4).toFloat()
                } else {
                    setBackgroundResource(R.drawable.bg_tag_pill_selector)
                    isSelected = false
                    setTextColor(ContextCompat.getColor(ctx, R.color.miui_text_secondary))
                    elevation = 0f
                }
            }

            chip.setOnClickListener { v ->
                selectedCategory = v.tag as String
                setupFormCategories(container)
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(8)
            }
            container.addView(chip, params)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    companion object {
        const val TAG = "NewMemorialDialog"
    }
}