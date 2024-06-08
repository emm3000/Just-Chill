package com.emm.retrofit.ui

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.emm.retrofit.data.model.Drink

@Suppress("DEPRECATION")
class DetailFragment : Fragment() {

    private var drink: Drink? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments.let {
            drink = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it?.getParcelable("drink", Drink::class.java)
            } else {
                it?.getParcelable("drink")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                Surface {
                    Text(drink?.name.orEmpty())
                }
            }
        }
    }
}