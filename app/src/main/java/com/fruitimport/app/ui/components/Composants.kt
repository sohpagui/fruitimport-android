package com.fruitimport.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertFrais
import com.fruitimport.app.ui.theme.VertClair

// ── Barre d application avec gradient vert/orange
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarreApp(
    titre: String,
    onRetour: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                titre,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
        },
        navigationIcon = {
            if (onRetour != null) {
                IconButton(onClick = onRetour) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = VertFrais,
            actionIconContentColor = Color.White,
            titleContentColor = Color.White
        )
    )
}

// ── Indicateur de chargement centre
@Composable
fun ChargementIndicateur() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = VertFrais, strokeWidth = 4.dp, modifier = Modifier.size(48.dp))
            Text("Chargement...", color = Color.Gray, fontSize = 14.sp)
        }
    }
}

// ── Carte statistique moderne
@Composable
fun CarteStatistique(
    titre: String,
    valeur: String,
    icone: ImageVector,
    couleur: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(couleur.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icone, contentDescription = null, tint = couleur, modifier = Modifier.size(28.dp))
            }
            Text(valeur, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = couleur, textAlign = TextAlign.Center)
            Text(titre, fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

// ── Badge de statut colore
@Composable
fun BadgeStatut(statut: String, libelle: String) {
    val (couleurFond, couleurTexte) = when (statut) {
        "EN_ATTENTE" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        "CONFIRMEE", "EN_REGLE" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "PREPAREE" -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        "EN_LIVRAISON" -> Color(0xFFF3E5F5) to Color(0xFF6A1B9A)
        "LIVREE" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "ANNULEE", "EN_RETARD" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        "A_RELANCER" -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
        else -> Color(0xFFF5F5F5) to Color(0xFF757575)
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = couleurFond
    ) {
        Text(
            libelle,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = couleurTexte,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Carte alerte moderne
@Composable
fun CarteAlerte(message: String, type: String = "warning") {
    val (couleur, icone) = when (type) {
        "warning" -> Color(0xFFFFF3E0) to Icons.Default.Warning
        "error" -> Color(0xFFFFEBEE) to Icons.Default.Error
        else -> Color(0xFFE8F5E9) to Icons.Default.CheckCircle
    }
    val couleurTexte = when (type) {
        "warning" -> Color(0xFFE65100)
        "error" -> Color(0xFFC62828)
        else -> Color(0xFF2E7D32)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = couleur)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icone, contentDescription = null, tint = couleurTexte, modifier = Modifier.size(20.dp))
            Text(message, color = couleurTexte, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ── Section titre avec separateur
@Composable
fun SectionTitre(titre: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(titre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1C1B1F))
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = VertClair.copy(alpha = 0.3f), thickness = 2.dp)
    }
}

// ── Bouton primaire style FruitImport
@Composable
fun BoutonPrimaire(
    texte: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    icone: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = active,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = VertFrais)
    ) {
        if (icone != null) {
            Icon(icone, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(texte, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

// ── Page vide
@Composable
fun PageVide(message: String, icone: ImageVector = Icons.Default.Inbox) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icone, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
            Text(message, color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}
