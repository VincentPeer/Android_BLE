package ch.heigvd.iict.dma.labo4.ui

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import ch.heigvd.iict.dma.labo4.R
import ch.heigvd.iict.dma.labo4.databinding.FragmentConnectedBinding
import ch.heigvd.iict.dma.labo4.viewmodels.BleViewModel

class BleConnectedFragment : Fragment(), MenuProvider {

    private val bleViewModel : BleViewModel by activityViewModels()

    private var _binding : FragmentConnectedBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConnectedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe LiveData
        bleViewModel.temperature.observe(viewLifecycleOwner) {
            binding.temperatureValue.text = bleViewModel.temperature.value.toString()
        }
        bleViewModel.currentTime.observe(viewLifecycleOwner) {
            binding.deviceCurrentTime.text = bleViewModel.currentTime.value?.time.toString()
        }
        bleViewModel.buttonClick.observe(viewLifecycleOwner) {
            binding.btnClickCounterValue.text = bleViewModel.buttonClick.value.toString()
        }

        // Set up buttons click listeners
        binding.temperatureRequestButton.setOnClickListener {
            bleViewModel.readTemperature()
        }

        binding.setCurrentTimeButton.setOnClickListener {
            bleViewModel.setTime()
        }

        binding.sendIntegerButton.setOnClickListener {
            if(binding.integerInput.text.isNotEmpty())
                bleViewModel.sendValue(Integer.parseInt(binding.integerInput.text.toString()))
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as AppCompatActivity).addMenuProvider(this)
    }

    override fun onPause() {
        super.onPause()
        (requireActivity() as AppCompatActivity).removeMenuProvider(this)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.connected_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when(menuItem.itemId) {
            R.id.menu_ble_connected_disconnect -> {
                bleViewModel.disconnect()
                true
            }
            else -> false
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = BleConnectedFragment()
    }
}
