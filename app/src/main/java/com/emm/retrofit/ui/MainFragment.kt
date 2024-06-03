package com.emm.retrofit.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.emm.retrofit.R
import com.emm.retrofit.data.DataSource
import com.emm.retrofit.data.model.Drink
import com.emm.retrofit.databinding.FragmentMainBinding
import com.emm.retrofit.domain.DrinkRepositoryImpl
import com.emm.retrofit.ui.viewmodel.MainAdapter
import com.emm.retrofit.ui.viewmodel.MainViewModel
import com.emm.retrofit.ui.viewmodel.OnDrinkClickListener
import com.emm.retrofit.ui.viewmodel.ViewModelFactory
import com.emm.retrofit.vo.Result
import com.emm.retrofit.vo.safeCollect

class MainFragment : Fragment(), OnDrinkClickListener {

    private lateinit var binding: FragmentMainBinding

    private val viewModel by viewModels<MainViewModel> {
        ViewModelFactory(DrinkRepositoryImpl(DataSource()))
    }

    private val mainAdapter: MainAdapter by lazy {
        MainAdapter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycleView()
        safeCollect(viewModel.fetchDrinks, ::resolveResult)
    }

    private fun resolveResult(result: Result<List<Drink>>) {
        when (result) {
            is Result.Loading -> {
                binding.progessBar.visibility = View.VISIBLE
            }

            is Result.Success -> {
                binding.progessBar.visibility = View.INVISIBLE
                mainAdapter.submitList(result.data)
            }

            is Result.Failure -> {
                AlertDialog.Builder(requireContext())
                    .setMessage(result.exception.message)
                    .show()
            }
        }
    }

    private fun setupRecycleView() {
        binding.rvTragos.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        binding.rvTragos.adapter = mainAdapter
    }

    override fun onDrinkClick(drink: Drink) {
        val bundle = Bundle()
        bundle.putParcelable("drink", drink)
        findNavController().navigate(R.id.action_mainFragment_to_detailFragment, bundle)
    }
}