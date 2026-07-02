package com.fruitimport.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fruitimport.app.utils.toFCFA

// ── Carte statistique (utilisée dans les dashboards)
@Composable
fun CarteStatistique(
    titre: String,
    valeur: String,
    icone: ImageVector,
    couleur: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(couleur.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icone, contentDescription = null, tint = couleur, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(valeur, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(titre, fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}

// ── Badge de statut coloré
@Composable
fun BadgeStatut(statut: String, texte: String) {
    val (couleurFond, couleurTexte) = when (statut) {
        "EN_ATTENTE" -> Pair(Color(0xFFFFF9C4), Color(0xFFF57F17))
        "CONFIRMEE", "APPROUVE", "EN_REGLE" -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        "LIVREE", "LIVRE" -> Pair(Color(0xFFE3F2FD), Color(0xFF1565C0))
        "ANNULEE", "REJETE", "EN_RETARD" -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
        "EN_LIVRAISON", "EN_ROUTE" -> Pair(Color(0xFFE8EAF6), Color(0xFF3949AB))
        "A_RELANCER" -> Pair(Color(0xFFFFF3E0), Color(0xFFE65100))
        else -> Pair(Color(0xFFF5F5F5), Color.Gray)
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = couleurFond
    ) {
        Text(
            text = texte,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = couleurTexte
        )
    }
}

// ── Barre d'application personnalisée
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarreApp(
    titre: String,
    onRetour: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(titre, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            if (onRetour != null) {
                IconButton(onClick = onRetour) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

// ── Indicateur de chargement
@Composable
fun ChargementIndicateur() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

// ── Message d'erreur
@Composable
fun MessageErreur(message: String, onReessayer: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null,
            tint = Color.Red, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, color = Color.Gray)
        if (onReessayer != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onReessayer) { Text("Réessayer") }
        }
    }
}

// ── Carte alerte
@Composable
fun CarteAlerte(message: String, type: String = "warning") {
    val couleur = when (type) {
        "error" -> Color(0xFFFFEBEE)
        "success" -> Color(0xFFE8F5E9)
        else -> Color(0xFFFFF9C4)
    }
    val icone = when (type) {
        "error" -> Icons.Default.Error
        "success" -> Icons.Default.CheckCircle
        else -> Icons.Default.Warning
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = couleur),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icone, contentDescription = null, tint = Color(0xFFF57F17))
            Spacer(modifier = Modifier.width(8.dp))
            Text(message, fontSize = 13.sp)
        }
    }
}
