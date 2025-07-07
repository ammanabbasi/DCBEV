package com.dealerscloud.android

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.dealerscloud.utils.FirebaseInitializer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DealersCloudApplication : Application() {
    companion object {
        private const val TAG = "DealersCloudApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        Log.i(TAG, "DCBEV Application starting...")
        
        // Initialize Firebase with proper credentials
        initializeFirebase()
        
        Log.i(TAG, "DCBEV Application initialized successfully")
    }
    
    private fun initializeFirebase() {
        try {
            // Initialize Firebase App
            FirebaseApp.initializeApp(this)
            Log.i(TAG, "Firebase App initialized successfully")
            
            // Initialize Firebase services
            FirebaseAuth.getInstance()
            Log.i(TAG, "Firebase Auth initialized")
            
            FirebaseFirestore.getInstance()
            Log.i(TAG, "Firebase Firestore initialized")
            
            FirebaseMessaging.getInstance()
            Log.i(TAG, "Firebase Messaging initialized")
            
            FirebaseStorage.getInstance()
            Log.i(TAG, "Firebase Storage initialized")
            
            // Enable Firestore offline persistence
            try {
                FirebaseFirestore.getInstance().enableNetwork()
                Log.i(TAG, "Firestore offline persistence enabled")
            } catch (e: Exception) {
                Log.w(TAG, "Firestore offline persistence already enabled or failed: ${e.message}")
            }
            
            // Check Firebase service status
            val serviceStatus = FirebaseInitializer.checkServices()
            Log.i(TAG, "Firebase services status: $serviceStatus")
            
            // Log Firebase configuration info
            val firebaseInfo = FirebaseInitializer.getFirebaseInfo()
            Log.i(TAG, "Firebase configuration: $firebaseInfo")
            
            Log.i(TAG, "All Firebase services initialized successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase: ${e.message}", e)
            // Don't crash the app if Firebase fails to initialize
            // The app can still function without Firebase for basic features
        }
    }
}