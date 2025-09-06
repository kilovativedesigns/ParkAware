package com.kilovativedesigns.parkaware.ui.education

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kilovativedesigns.parkaware.R
import com.kilovativedesigns.parkaware.models.EducationTips
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

class EducationDetailFragment : Fragment() {

    private val tips = mutableListOf<String>()
    private lateinit var adapter: TipsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        s: Bundle?
    ): View {
        // Independent Education layout (now with MaterialToolbar)
        return inflater.inflate(R.layout.fragment_education_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // ---- Toolbar ----
        val toolbar = view.findViewById<MaterialToolbar>(R.id.edu_detail_toolbar)
        val title = arguments?.getString("title") ?: getString(R.string.education_header)
        toolbar.title = title
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24)
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        activity?.title = title

        // ---- RecyclerView (with divider) ----
        val rv = view.findViewById<RecyclerView>(R.id.edu_detail_list)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.addItemDecoration(
            androidx.recyclerview.widget.DividerItemDecoration(
                requireContext(),
                androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
            )
        )
        adapter = TipsAdapter(tips)
        rv.adapter = adapter

        // Load tips for the selected category
        val category = arguments?.getString("category") ?: "onStreet"
        lifecycleScope.launch {
            val data = loadTipsJson()

            // TODO: Replace "NSW" with detected/saved user state when available
            val state = data.NSW

            val selected: List<String> = when (category) {
                "onStreet"        -> state?.onStreet
                "carParks"        -> state?.carParks
                "schoolZones"     -> state?.schoolZones
                "disabledParking" -> state?.disabledParking
                "topFines"        -> state?.topFines
                "dealWithFines"   -> state?.dealWithFines
                else              -> state?.onStreet
            } ?: emptyList()

            tips.clear()
            tips.addAll(selected)
            adapter.notifyDataSetChanged()
        }
    }

    // --- JSON Loader --------------------------------------------------------

    private suspend fun loadTipsJson(): EducationTips = withContext(Dispatchers.IO) {
        val gson = Gson()
        val type = object : TypeToken<EducationTips>() {}.type

        // 1) Try hosted JSON (Raw GitHub URL)
        try {
            val url = URL("https://raw.githubusercontent.com/kilovativedesigns/ParkAware/main/AllStatesParkingRulesEducation1.json")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5_000
            conn.readTimeout = 5_000
            conn.requestMethod = "GET"
            conn.inputStream.bufferedReader().use { reader ->
                return@withContext gson.fromJson(reader, type)
            }
        } catch (_: Exception) { /* ignore and fall back */ }

        // 2) Fallback to bundled raw resource (rename if your file differs)
        val raw = resources.openRawResource(R.raw.tips)
        val reader = BufferedReader(raw.reader())
        gson.fromJson(reader, type)
    }

    // --- Adapter (bullet-style text rows) -----------------------------------

    private class TipsAdapter(private val items: List<String>) :
        RecyclerView.Adapter<TipsAdapter.TipVH>() {

        class TipVH(view: View) : RecyclerView.ViewHolder(view) {
            val text: TextView = view.findViewById(R.id.tip_text)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TipVH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_education_tip, parent, false)
            return TipVH(v)
        }

        override fun onBindViewHolder(holder: TipVH, position: Int) {
            holder.text.text = items[position]
        }

        override fun getItemCount() = items.size
    }
}