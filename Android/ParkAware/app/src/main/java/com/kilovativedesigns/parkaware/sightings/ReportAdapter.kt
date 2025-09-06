package com.kilovativedesigns.parkaware.ui.sightings

import android.location.Location
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kilovativedesigns.parkaware.R
import com.kilovativedesigns.parkaware.data.model.Report
import com.kilovativedesigns.parkaware.databinding.ItemReportBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportAdapter(
    private var items: List<Report> = emptyList(),
    private var userLocation: Location? = null,
    private val onClick: (Report) -> Unit
) : RecyclerView.Adapter<ReportAdapter.VH>() {

    fun submit(list: List<Report>, loc: Location?) {
        // 👇 Debug: see what we’re binding with
        Log.d(
            "ReportAdapter",
            "submit: items=${list.size}, userLoc=${loc?.latitude},${loc?.longitude}"
        )

        items = list
        userLocation = loc
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], userLocation)
    }

    override fun getItemCount(): Int = items.size

    class VH(
        private val b: ItemReportBinding,
        private val onClick: (Report) -> Unit
    ) : RecyclerView.ViewHolder(b.root) {

        private val timeFmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

        fun bind(r: Report, userLoc: Location?) {
            val iconRes = when (r.rangerType?.lowercase(Locale.getDefault())) {
                "officer", "ranger", "inspector" -> R.drawable.ic_pin_officer
                "chalk", "chalking", "tyre", "tire" -> R.drawable.ic_pin_chalk
                "fine", "ticket" -> R.drawable.ic_pin_fine
                else -> R.drawable.ic_pin_officer
            }
            b.icon.setImageResource(iconRes)
            b.icon.imageTintList = null
            b.title.text = r.rangerType?.ifBlank { "Sighting" } ?: "Sighting"

            val subtitle = if (r.timeReported > 0L) timeFmt.format(Date(r.timeReported)) else "—"
            b.subtitle.text = subtitle

            b.distance.text = formatDistance(userLoc, r.lat, r.longitude)

            b.root.setOnClickListener { onClick(r) }
        }

        private fun formatDistance(userLoc: Location?, lat: Double?, lon: Double?): String {
            if (userLoc == null) {
                Log.d("ReportAdapter", "formatDistance: userLoc is null → distance '—'")
                return "—"
            }
            if (lat == null || lon == null) {
                Log.d("ReportAdapter", "formatDistance: missing coords lat=$lat lon=$lon → distance '—'")
                return "—"
            }
            val results = FloatArray(1)
            Location.distanceBetween(userLoc.latitude, userLoc.longitude, lat, lon, results)
            val meters = results[0].toDouble()
            return if (meters >= 1000) {
                String.format(Locale.getDefault(), "%.1f km", meters / 1000.0)
            } else {
                String.format(Locale.getDefault(), "%.0f m", meters)
            }
        }
    }
}