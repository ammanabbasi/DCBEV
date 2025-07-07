package com.dealerscloud.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.dealerscloud.integration.JWTAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AuthNavigationViewModel @Inject constructor(
    private val authManager: JWTAuthManager
) : ViewModel() {
    
    val authState: StateFlow<JWTAuthManager.AuthState> = authManager.authState
    
    fun logout() {
        authManager.logout()
    }
}