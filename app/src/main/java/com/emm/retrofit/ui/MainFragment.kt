package com.emm.retrofit.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.emm.retrofit.R
import com.emm.retrofit.data.DataSource
import com.emm.retrofit.data.model.Drink
import com.emm.retrofit.databinding.FragmentMainBinding
import com.emm.retrofit.domain.DrinkRepositoryImpl
import com.emm.retrofit.ui.viewmodel.MainAdapter
import com.emm.retrofit.ui.viewmodel.MainViewModel
import com.emm.retrofit.ui.viewmodel.OnTragoClickListener
import com.emm.retrofit.ui.viewmodel.VMFactory
import com.emm.retrofit.vo.Result

class MainFragment : Fragment(), OnTragoClickListener {

    private lateinit var binding: FragmentMainBinding

    private val viewModel by viewModels<MainViewModel> {
        VMFactory(DrinkRepositoryImpl(DataSource()))
    }

    private val mainAdapter: MainAdapter by lazy {
        MainAdapter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycleView()
        viewModel.fetchDrinks.observe(viewLifecycleOwner) {
            when (it) {
                is Result.Loading -> {
                    binding.progessBar.visibility = View.VISIBLE
                }

                is Result.Success -> {
                    binding.progessBar.visibility = View.INVISIBLE
                    mainAdapter.submitList(it.data)
                }

                is Result.Failure -> {
                    Toast.makeText(requireContext(), "Error", Toast.LENGTH_LONG).show()
                    AlertDialog.Builder(requireContext())
                        .setMessage(it.exception.message)
                        .show()
                }
            }
        }
    }

    private fun setupRecycleView() {
        binding.rvTragos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTragos.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        binding.rvTragos.adapter = mainAdapter
    }

    override fun onTragoClick(drink: Drink) {
        val bundle = Bundle()
        bundle.putParcelable("drink", drink)
        findNavController().navigate(R.id.action_mainFragment_to_detailFragment, bundle)
    }
}