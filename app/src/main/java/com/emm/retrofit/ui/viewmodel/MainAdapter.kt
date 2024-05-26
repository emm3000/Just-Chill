package com.emm.retrofit.ui.viewmodel

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.bumptech.glide.Glide
import com.emm.retrofit.R
import com.emm.retrofit.base.BaseViewHolder
import com.emm.retrofit.data.model.Drink
import com.emm.retrofit.databinding.MainItemBinding

class MainAdapter(
    private val itemClickListener: OnTragoClickListener
) : ListAdapter<Drink, MainAdapter.MainViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val itemView: View = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.main_item, parent, false)

        return MainViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class MainViewHolder(itemView: View) : BaseViewHolder<Drink>(itemView) {

        override fun bind(item: Drink, position: Int) {
            val binding = MainItemBinding.bind(itemView)
            Glide.with(binding.root).load(item.image).centerCrop().into(binding.itemImage)
            binding.itemTitle.text = item.name
            binding.itemDescription.text = item.description
            itemView.setOnClickListener { itemClickListener.onTragoClick(item) }
        }
    }

    companion object {

        val DIFF_CALLBACK: DiffUtil.ItemCallback<Drink> = object : DiffUtil.ItemCallback<Drink>() {

            override fun areItemsTheSame(oldItem: Drink, newItem: Drink): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Drink, newItem: Drink): Boolean {
                return oldItem == newItem
            }
        }
    }
}