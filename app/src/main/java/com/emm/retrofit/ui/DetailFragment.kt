package com.emm.retrofit.ui

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.emm.retrofit.R
import com.emm.retrofit.data.model.Drink

class DetailFragment : Fragment(R.layout.fragment_detail) {

    private var drink: Drink? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                drink = it?.getParcelable("drink", Drink::class.java)
            } else {
                @Suppress("DEPRECATION")
                it?.getParcelable<Drink>("drink")
            }
        }
    }

}