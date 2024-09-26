package com.example.devs.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.devs.databinding.RowSearchAddressBinding
import com.google.android.libraries.places.api.model.AutocompletePrediction

class SearchAddressAdapter(
    var itemClickCallback: (placeId: String?, primaryAddress: String?, mainAddress: String?) -> Unit
) : RecyclerView.Adapter<SearchAddressAdapter.ViewHolder>() {

     var mList: List<AutocompletePrediction> = emptyList()

    inner class ViewHolder(var binding: RowSearchAddressBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AutocompletePrediction) = with(binding) {
            textViewAddressType.text = item.getPrimaryText(null).toString()
            textViewAddress.text = item.getFullText(null).toString()

            //CLicks
            root.setOnClickListener {
                itemClickCallback.invoke(
                    item.placeId,
                    item.getPrimaryText(null).toString(),
                    item.getFullText(null).toString()
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            RowSearchAddressBinding.inflate(
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

    fun setPredictions(predictions: List<AutocompletePrediction>) {
        this.mList = predictions
        notifyDataSetChanged()
    }

}