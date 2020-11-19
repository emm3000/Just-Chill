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
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.main_item.view.*

class MainAdapter(
    private val context: Context,
    private val tragosList: List<Drink>,
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
            is MainViewHolder -> holder.bind(tragosList[position], position)
        }
    }

    override fun getItemCount(): Int {
        return tragosList.size
    }
    /*Inner para accede al context del Adapter*/
    inner class MainViewHolder(itemView: View) : BaseViewHolder<Drink>(itemView) {
        override fun bind(item: Drink, position: Int) {
            Glide.with(context).load(item.imagen).centerCrop().into(itemView.item_image)
            itemView.item_title.text = item.nombre
            itemView.item_description.text = item.description
            itemView.setOnClickListener { itemClickListener.onTragoClick(item) }
        }

    }
}