package com.emm.retrofit.experiences.drinks.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.emm.retrofit.R
import com.emm.retrofit.experiences.drinks.data.DrinkApiModel
import com.emm.retrofit.databinding.MainItemBinding

class MainAdapter : ListAdapter<DrinkApiModel, MainAdapter.MainViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val itemView: View = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.main_item, parent, false)

        return MainViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MainViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(item: DrinkApiModel) {
            val binding = MainItemBinding.bind(itemView)
            Glide.with(binding.root).load(item.image).centerCrop().into(binding.itemImage)
            binding.itemTitle.text = item.name
            binding.itemDescription.text = item.description
        }
    }

    companion object {

        val DIFF_CALLBACK: DiffUtil.ItemCallback<DrinkApiModel> = object : DiffUtil.ItemCallback<DrinkApiModel>() {

            override fun areItemsTheSame(oldItem: DrinkApiModel, newItem: DrinkApiModel): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: DrinkApiModel, newItem: DrinkApiModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}