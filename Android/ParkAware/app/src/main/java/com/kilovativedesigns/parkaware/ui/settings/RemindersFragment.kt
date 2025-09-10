package com.kilovativedesigns.parkaware.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.kilovativedesigns.parkaware.R
import com.kilovativedesigns.parkaware.databinding.FragmentRemindersBinding
import com.kilovativedesigns.parkaware.reminders.ReminderManager

class RemindersFragment : Fragment() {

    private var _b: FragmentRemindersBinding? = null
    private val b get() = _b!!

    // Local fallback prefs so this compiles today
    private object LocalPrefs {
        private const val FILE = "reminders_prefs"
        private const val K_ENABLED = "enabled"
        private const val K_MINUTES = "minutes"
        private const val K_STYLE = "style"

        fun loadEnabled(ctx: Context) =
            ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(K_ENABLED, false)

        fun saveEnabled(ctx: Context, v: Boolean) =
            ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(K_ENABLED, v).apply()

        fun loadMinutes(ctx: Context) =
            ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getInt(K_MINUTES, 30)

        fun saveMinutes(ctx: Context, v: Int) =
            ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putInt(K_MINUTES, v).apply()

        fun loadStyle(ctx: Context) =
            ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(K_STYLE, "banner") ?: "banner"

        fun saveStyle(ctx: Context, v: String) =
            ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(K_STYLE, v).apply()
    }

    private val styles = listOf("banner", "alert", "silent")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _b = FragmentRemindersBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // --- load initial values ---
        val enabled = LocalPrefs.loadEnabled(requireContext())
        val minutes = LocalPrefs.loadMinutes(requireContext())
        val style   = LocalPrefs.loadStyle(requireContext())

        b.switchEnable.isChecked = enabled
        b.etMinutes.setText(minutes.toString())

        b.spinnerStyle.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            styles
        )
        b.spinnerStyle.setSelection(styles.indexOf(style).coerceAtLeast(0))

        // --- listeners that persist changes ---
        b.switchEnable.setOnCheckedChangeListener { _, on ->
            LocalPrefs.saveEnabled(requireContext(), on)
        }

        b.etMinutes.doAfterTextChanged {
            val m = it?.toString()?.toIntOrNull() ?: return@doAfterTextChanged
            LocalPrefs.saveMinutes(requireContext(), m)
        }

        b.spinnerStyle.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) {
                LocalPrefs.saveStyle(requireContext(), styles[pos])
            }
            override fun onNothingSelected(p: AdapterView<*>) {}
        }

        // --- actions ---
        b.btnSetNow.setOnClickListener {
            val m = LocalPrefs.loadMinutes(requireContext())
            val seconds = (m * 60 - 300).coerceAtLeast(60)
            ReminderManager.schedule(requireContext(), seconds.toLong())
            Snackbar.make(b.root, getString(R.string.rem_scheduled_fmt, m), Snackbar.LENGTH_SHORT).show()
        }

        b.btnClearAll.setOnClickListener {
            ReminderManager.clearAll(requireContext())
            Snackbar.make(b.root, R.string.rem_cleared, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }
}