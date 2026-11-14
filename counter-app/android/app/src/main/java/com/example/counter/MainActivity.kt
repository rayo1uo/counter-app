package com.example.counter

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.counter.ui.CounterRoot
import com.example.counter.ui.CounterTheme
import com.example.counter.ui.CounterViewModel

class MainActivity : ComponentActivity() {

    private val vm: CounterViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CounterViewModel(application) as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
        setContent {
            val ui by vm.ui.collectAsState()
            CounterTheme(dark = ui.settings.darkTheme) {
                CounterRoot(vm = vm)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP &&
            vm.ui.value.settings.volumeCountEnabled
        ) {
            vm.volumeIncrement()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
