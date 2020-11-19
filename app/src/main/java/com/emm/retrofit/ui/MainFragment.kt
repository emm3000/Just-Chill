package com.emm.retrofit.ui

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.emm.retrofit.R
import com.emm.retrofit.data.DataSource
import com.emm.retrofit.data.model.Drink
import com.emm.retrofit.domain.RepoImpl
import com.emm.retrofit.ui.viewmodel.MainAdapter
import com.emm.retrofit.ui.viewmodel.MainViewModel
import com.emm.retrofit.ui.viewmodel.OnTragoClickListener
import com.emm.retrofit.ui.viewmodel.VMFactory
import com.emm.retrofit.vo.Resource
import kotlinx.android.synthetic.main.fragment_main.*

class MainFragment : Fragment(), OnTragoClickListener {

    private val viewModel by viewModels<MainViewModel> {
        VMFactory(RepoImpl(DataSource()))
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setuRecycleView()
        viewModel.fetchTragosList.observe(viewLifecycleOwner, Observer {
            when(it) {
                is Resource.Loading -> {
                    progessBar.visibility = View.VISIBLE
                    Log.d("LOADING", "LOADING")
                }
                is Resource.Success -> {
                    Log.d("LOADING", "sUCESS")
                    progessBar.visibility = View.INVISIBLE
                    rv_tragos.adapter = MainAdapter(requireContext(), it.data, this)
                }
                is Resource.Failure -> {
                    Log.d("LOADING", "ERROR")
                    //progessBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error", Toast.LENGTH_LONG)
                }
            }
        })
    }

    private fun setuRecycleView() {
        rv_tragos.layoutManager = LinearLayoutManager(requireContext())
        rv_tragos.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
    }

    override fun onTragoClick(drink: Drink) {
        val bundle = Bundle()
        bundle.putParcelable("drink", drink)
        findNavController().navigate(R.id.action_mainFragment_to_detailFragment, bundle)
    }
}