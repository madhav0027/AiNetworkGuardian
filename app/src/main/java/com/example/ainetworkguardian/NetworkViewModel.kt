package com.example.ainetworkguardian

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NetworkViewModel : ViewModel() {

    private val _isLivingScanning = MutableStateFlow(false)
    val isLivingScanning : StateFlow<Boolean> = _isLivingScanning.asStateFlow()

    private var monitoringJob: Job? = null

    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return

        _isLivingScanning.value = true

        monitoringJob = viewModelScope.launch {
            try {
                while (isActive) {
                    monitorNetwork()

                    delay(1000)
                }
            } finally {
                _isLivingScanning.value = false
                stopScanning()
            }
        }
    }

    fun stopScanning() {
        monitoringJob?.cancel()
        monitoringJob = null
        _isLivingScanning.value = false
    }

    private suspend fun monitorNetwork() {
        // Replace with your actual monitoring operation.
    }

    private fun stopMonitoring() {
        // Release resources or stop packet capture here.
    }

    override fun onCleared() {
        stopScanning()
        super.onCleared()
    }
}