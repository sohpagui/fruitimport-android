package com.fruitimport.app

// ============================================================
// FICHIER : MainActivity.kt
// Rôle : Point d'entrée de l'application Android.
//        Configure le thème et lance la navigation.
// ============================================================

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.fruitimport.app.navigation.NavigationPrincipale
import com.fruitimport.app.ui.theme.FruitImportTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FruitImportTheme {
                // Le NavController gère la navigation entre les écrans
                val navController = rememberNavController()
                NavigationPrincipale(navController = navController)
            }
        }
    }
}
