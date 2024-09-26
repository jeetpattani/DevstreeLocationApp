package com.example.devs.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.devs.databinding.RowLocationBinding
import com.example.devs.db.entity.LocationEntity
import com.example.devs.utils.formatToOneDecimalPlaces
import com.example.devs.utils.gone
import com.example.devs.utils.visible

class LocationAdapter(
    var onEventListener: (view: View, adapterPos: Int) -> Unit,
) : RecyclerView.Adapter<LocationAdapter.ViewHolder>() {

    var mList = ArrayList<LocationEntity>()

    inner class ViewHolder(var binding: RowLocationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LocationEntity) = with(binding) {
            textViewCity.text = "${item.primaryAddress}"
            textViewAddresses.text = "${item.city}"
            textViewDistance.text = formatToOneDecimalPlaces(item.distance ?: "").let {
                if (it == "0.00") "" else "Distance : $it KM"
            }

            if (item.isPrimary == true) {
                textViewPrimary.visible()
            } else {
                textViewPrimary.gone()
            }

            // Clicks
            imageViewDelete.setOnClickListener {
                onEventListener.invoke(it, adapterPosition)
            }
            imageViewEdit.setOnClickListener {
                onEventListener.invoke(it, adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            RowLocationBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(mList[position])
    }

}