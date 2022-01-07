package jp.co.testopencv.detect

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import jp.co.testopencv.R

/**
 * COPYRIGHT ZYYX. ALL RIGHTS RESERVED,2020
 */
class EnterFragment: DialogFragment() {
    var onSizeInputClicked: ((Int, Int) -> Unit)? = null

    companion object {
        fun getInstance(): EnterFragment {
            return EnterFragment()
        }
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.fragment_enter_input)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.findViewById<EditText>(R.id.editTextNumber)?.setText("10409")
        dialog.findViewById<EditText>(R.id.editTextScale)?.setText("1")
        dialog.findViewById<Button>(R.id.btnOk)?.setOnClickListener {
            val input = dialog.findViewById<EditText>(R.id.editTextNumber).text.toString()
            val scale = dialog.findViewById<EditText>(R.id.editTextScale).text.toString()
            if (input.isNotEmpty() && scale.isNotEmpty()) {
                val size = input.toInt()
                onSizeInputClicked?.invoke(size, scale.toInt())
            }
            dismiss()

        }
        return dialog
    }
}