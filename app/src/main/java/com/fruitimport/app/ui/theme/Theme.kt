package com.fruitimport.app.ui.theme

// ============================================================
// FICHIER : ui/theme/Theme.kt
// Rôle : Définit les couleurs et le thème visuel de l'app.
//        Toutes les couleurs de l'interface sont ici.
//        Pour changer le thème de l'entreprise, modifier ce fichier.
// ============================================================

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Couleurs principales de FruitImport
val VertFrais = Color(0xFF2E7D32)          // Vert foncé — couleur principale
val VertClair = Color(0xFF4CAF50)          // Vert clair — accents
val OrangeFruit = Color(0xFFFF6F00)        // Orange — couleur secondaire
val BleuInfo = Color(0xFF1565C0)           // Bleu — informations
val RougeAlerte = Color(0xFFC62828)        // Rouge — erreurs/alertes
val JauneAvertissement = Color(0xFFF9A825) // Jaune — avertissements
val GrisFond = Color(0xFFF5F5F5)           // Gris clair — fond

// ── Thème clair (mode jour)
private val ThemeClair = lightColorScheme(
    primary = VertFrais,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA5D6A7),
    secondary = OrangeFruit,
    onSecondary = Color.White,
    background = GrisFond,
    surface = Color.White,
    error = RougeAlerte,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

@Composable
fun FruitImportTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ThemeClair,
        content = content
    )
}
