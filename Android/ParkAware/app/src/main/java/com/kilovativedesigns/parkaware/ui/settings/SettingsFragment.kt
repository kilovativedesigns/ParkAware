package com.kilovativedesigns.parkaware.ui.settings

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.kilovativedesigns.parkaware.R
import com.kilovativedesigns.parkaware.databinding.FragmentSettingsBinding
import com.kilovativedesigns.parkaware.databinding.ItemSettingRowBinding
import com.kilovativedesigns.parkaware.subscription.SubscriptionManager

class SettingsFragment : Fragment() {
    private var _b: FragmentSettingsBinding? = null
    private val b get() = _b!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentSettingsBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rows = listOf(
            SettingRow("User Profile", R.drawable.ic_person, R.id.userProfileFragment),
            SettingRow("Sighting Settings", R.drawable.ic_visibility, R.id.notificationLocationFragment, gated = true),
            SettingRow("Parking Reminder Settings", R.drawable.ic_directions_car, R.id.remindersFragment),
            SettingRow("Community Settings", R.drawable.ic_groups, R.id.socialSettingsFragment),
            SettingRow("About ParkAware", R.drawable.ic_info, R.id.aboutFragment),
            SettingRow("Help and Feedback", R.drawable.ic_help, R.id.feedbackFragment)
        )

        b.list.layoutManager = LinearLayoutManager(requireContext())
        b.list.adapter = SettingsAdapter(rows) { row ->
            if (row.gated && !SubscriptionManager.isSubscribed) {
                UpgradeBottomSheet().show(childFragmentManager, "upgrade")
            } else {
                row.destinationId?.let { findNavController().navigate(it) }
            }
        }
    }

    override fun onDestroyView() { _b = null; super.onDestroyView() }
}

private class SettingsAdapter(
    private val data: List<SettingRow>,
    private val onClick: (SettingRow) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<SettingsVH>() {

    override fun onCreateViewHolder(p: ViewGroup, t: Int): SettingsVH {
        val binding = ItemSettingRowBinding.inflate(LayoutInflater.from(p.context), p, false)
        return SettingsVH(binding, onClick)
    }
    override fun onBindViewHolder(h: SettingsVH, pos: Int) = h.bind(data[pos])
    override fun getItemCount() = data.size
}

private class SettingsVH(
    private val b: ItemSettingRowBinding,
    private val onClick: (SettingRow) -> Unit
) : androidx.recyclerview.widget.RecyclerView.ViewHolder(b.root) {
    fun bind(row: SettingRow) {
        b.icon.setImageResource(row.icon)
        b.title.text = row.title
        b.lock.visibility = if (row.gated && !SubscriptionManager.isSubscribed) View.VISIBLE else View.GONE
        b.root.setOnClickListener { onClick(row) }
    }
}