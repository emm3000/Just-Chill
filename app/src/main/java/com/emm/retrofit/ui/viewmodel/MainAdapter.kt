package com.emm.retrofit.ui.viewmodel

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.emm.retrofit.R
import com.emm.retrofit.base.BaseViewHolder
import com.emm.retrofit.data.model.Drink
import com.emm.retrofit.databinding.MainItemBinding

class MainAdapter(
    private val context: Context,
    private val drinks: List<Drink>,
    private  val itemClickListener: OnTragoClickListener
) : RecyclerView.Adapter<BaseViewHolder<*>>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<*> {
        return MainViewHolder(
            LayoutInflater
                .from(context)
                .inflate(R.layout.main_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: BaseViewHolder<*>, position: Int) {
        when(holder) {
            is MainViewHolder -> holder.bind(drinks[position], position)
        }
    }

    override fun getItemCount(): Int {
        return drinks.size
    }

    inner class MainViewHolder(itemView: View) : BaseViewHolder<Drink>(itemView) {

        override fun bind(item: Drink, position: Int) {
            val binding = MainItemBinding.bind(itemView)
            Glide.with(context).load(item.imagen).centerCrop().into(binding.itemImage)
            binding.itemTitle.text = item.nombre
            binding.itemDescription.text = item.description
            itemView.setOnClickListener { itemClickListener.onTragoClick(item) }
        }

    }
}