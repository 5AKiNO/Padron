package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthRepository
import com.example.data.auth.LogoutResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SessionViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _timeLeft = MutableStateFlow(repository.getTrialMinutes() * 60L)
    val timeLeft = _timeLeft.asStateFlow()

    private val _isLimitedUser = MutableStateFlow(!repository.isUnlimited())
    val isLimitedUser = _isLimitedUser.asStateFlow()

    private val _forceLogout = MutableStateFlow(false)
    val forceLogout = _forceLogout.asStateFlow()

    private val _showNoInternetDialog = MutableStateFlow(false)
    val showNoInternetDialog = _showNoInternetDialog.asStateFlow()

    private var timerJob: Job? = null

    fun startTimer() {
        timerJob?.cancel()
        val isLimited = !repository.isUnlimited()
        _isLimitedUser.value = isLimited

        if (!isLimited) return

        val loginTime = repository.getLoginTime()
        val totalSeconds = repository.getTrialMinutes() * 60L
        _timeLeft.value = totalSeconds

        timerJob = viewModelScope.launch {
            while (isActive) {
                val currentTime = System.currentTimeMillis()
                val elapsedSeconds = (currentTime - loginTime) / 1000
                val remaining = (totalSeconds - elapsedSeconds).coerceAtLeast(0L)

                if (remaining <= 0L) {
                    _timeLeft.value = 0L
                    attemptAutoLogout()
                    break
                }
                _timeLeft.value = remaining
                delay(1000)
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private suspend fun attemptAutoLogout() {
        val result = repository.logout()
        if (result is LogoutResult.Success) {
            _showNoInternetDialog.value = false
            _forceLogout.value = true
        } else if (result is LogoutResult.NoInternet) {
            _showNoInternetDialog.value = true
        } else {
            _showNoInternetDialog.value = true
        }
    }

    fun retryLogout() {
        viewModelScope.launch {
            attemptAutoLogout()
        }
    }
}
