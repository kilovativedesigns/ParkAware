package com.kilovativedesigns.parkaware.ui.education

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kilovativedesigns.parkaware.R
import androidx.navigation.fragment.findNavController

class EducationFragment : Fragment() {

    private data class Category(
        val id: String,
        val title: String,
        val icon: Int,
        val gated: Boolean = false
    )

    private val categories = listOf(
        Category("onStreet",        "On-street Parking",   R.drawable.ic_parking_sign_24),
        Category("carParks",        "Car Parks",           R.drawable.ic_car_24),
        Category("schoolZones",     "School Zones",        R.drawable.ic_school_24),
        Category("disabledParking", "Accessible/Disabled", R.drawable.ic_accessible),
        Category("topFines",        "Top Fines",           R.drawable.ic_money_24),
        Category("dealWithFines",   "Dealing with Fines",  R.drawable.ic_help)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_education, container, false)

        // RecyclerView of categories
        val list = root.findViewById<RecyclerView>(R.id.edu_list)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = CategoryAdapter(categories) { category ->
            findNavController().navigate(
                R.id.educationDetailFragment,
                bundleOf(
                    "category" to category.id,
                    "title" to category.title
                )
            )
        }

        return root
    }

    private class CategoryVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.row_icon)
        val title: TextView = itemView.findViewById(R.id.row_title)
        val lock: ImageView = itemView.findViewById(R.id.row_lock)
    }

    private class CategoryAdapter(
        private val items: List<Category>,
        private val onClick: (Category) -> Unit
    ) : RecyclerView.Adapter<CategoryVH>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryVH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_education_row, parent, false)
            return CategoryVH(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: CategoryVH, position: Int) {
            val item = items[position]
            holder.icon.setImageResource(item.icon)
            holder.title.text = item.title
            holder.lock.visibility = if (item.gated) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        const val REQUEST_SET_TITLE = "edu:setTitle"
        const val KEY_TITLE = "title"
    }
}