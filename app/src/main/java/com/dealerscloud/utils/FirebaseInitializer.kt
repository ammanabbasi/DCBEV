package com.dealerscloud.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.functions.FirebaseFunctions

object FirebaseInitializer {
    private const val TAG = "FirebaseInitializer"
    
    /**
     * Check status of all Firebase services
     * @return Map of service name to initialization status
     */
    fun checkServices(): Map<String, Boolean> {
        val serviceStatus = mutableMapOf<String, Boolean>()
        
        try {
            // Check Firebase Auth
            serviceStatus["Auth"] = try {
                FirebaseAuth.getInstance() != null
            } catch (e: Exception) {
                Log.w(TAG, "Firebase Auth not available: ${e.message}")
                false
            }
            
            // Check Firebase Firestore
            serviceStatus["Firestore"] = try {
                FirebaseFirestore.getInstance() != null
            } catch (e: Exception) {
                Log.w(TAG, "Firebase Firestore not available: ${e.message}")
                false
            }
            
            // Check Firebase Storage
            serviceStatus["Storage"] = try {
                FirebaseStorage.getInstance() != null
            } catch (e: Exception) {
                Log.w(TAG, "Firebase Storage not available: ${e.message}")
                false
            }
            
            // Check Firebase Messaging
            serviceStatus["Messaging"] = try {
                FirebaseMessaging.getInstance() != null
            } catch (e: Exception) {
                Log.w(TAG, "Firebase Messaging not available: ${e.message}")
                false
            }
            
            // Check Firebase Functions
            serviceStatus["Functions"] = try {
                FirebaseFunctions.getInstance() != null
            } catch (e: Exception) {
                Log.w(TAG, "Firebase Functions not available: ${e.message}")
                false
            }
            
            Log.i(TAG, "Firebase service check completed: $serviceStatus")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Firebase services: ${e.message}", e)
        }
        
        return serviceStatus
    }
    
    /**
     * Get detailed Firebase configuration info
     */
    fun getFirebaseInfo(): Map<String, String> {
        val info = mutableMapOf<String, String>()
        
        try {
            val auth = FirebaseAuth.getInstance()
            info["Auth User"] = auth.currentUser?.email ?: "Not signed in"
            info["Auth Provider"] = auth.currentUser?.providerId ?: "None"
            
            val firestore = FirebaseFirestore.getInstance()
            info["Firestore Settings"] = firestore.firestoreSettings.toString()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Firebase info: ${e.message}", e)
            info["Error"] = e.message ?: "Unknown error"
        }
        
        return info
    }
    
    /**
     * Test Firebase connectivity
     */
    suspend fun testConnectivity(): Map<String, String> {
        val results = mutableMapOf<String, String>()
        
        try {
            // Test Firestore connectivity
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("connectivity_test").limit(1).get()
                .addOnSuccessListener {
                    results["Firestore"] = "Connected"
                }
                .addOnFailureListener { e ->
                    results["Firestore"] = "Failed: ${e.message}"
                }
            
            // Test Auth connectivity
            val auth = FirebaseAuth.getInstance()
            results["Auth"] = if (auth.currentUser != null) "User signed in" else "No user signed in"
            
        } catch (e: Exception) {
            Log.e(TAG, "Error testing Firebase connectivity: ${e.message}", e)
            results["Error"] = e.message ?: "Unknown error"
        }
        
        return results
    }
}