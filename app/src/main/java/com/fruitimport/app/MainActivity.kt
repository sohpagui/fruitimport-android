package com.fruitimport.app

// ============================================================
// FICHIER : MainActivity.kt
// Rôle : Point d'entrée de l'application Android.
//        Configure le thème et lance la navigation.
// ============================================================

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.messaging.FirebaseMessaging
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.fruitimport.app.navigation.NavigationPrincipale
import com.fruitimport.app.ui.theme.FruitImportTheme

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Demander permission notifications Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Recuperer le token FCM
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("FCM_TOKEN", "Token: ${task.result}")
            }
        }
        setContent {
            FruitImportTheme {
                // Le NavController gère la navigation entre les écrans
                val navController = rememberNavController()
                NavigationPrincipale(navController = navController)
            }
        }
    }
}
