package com.fruitimport.app.ui.client

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fruitimport.app.navigation.Routes
import com.fruitimport.app.ui.components.BarreApp
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertFrais
import com.fruitimport.app.utils.SessionManager
import com.fruitimport.app.utils.toFCFA

@Composable
fun CarteMenuClient(emoji: String, titre: String, sousTitre: String, couleur: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.shadow(4.dp, RoundedCornerShape(20.dp)).clickable { onClick() }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(couleur.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 26.sp)
            }
            Text(titre, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)
            Text(sousTitre, fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

@Composable
fun EcranDashboardClient(navController: NavController) {
    val user = SessionManager.utilisateurConnecte
    val limite = user?.limiteCredit?.toDoubleOrNull() ?: 0.0
    val dette = user?.creditUtilise?.toDoubleOrNull() ?: 0.0
    val statut = user?.statutCredit ?: "EN_REGLE"

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color(0xFFE8F5E9), Color(0xFFF1F8E9), Color(0xFFF5F5F5)))))

        Column(modifier = Modifier.fillMaxSize()) {
            // En-tete avec fond vert
            Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(VertFrais, Color(0xFF2E7D32)))).padding(horizontal = 20.dp, vertical = 20.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.size(70.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Text(user?.nom?.take(1) ?: "C", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(user?.nom ?: "", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(user?.telephone ?: "", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                    Spacer(Modifier.height(4.dp))
                    Surface(shape = RoundedCornerShape(20.dp), color = OrangeFruit.copy(alpha = 0.9f)) {
                        Text("Client", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    Row {
                        IconButton(onClick = { navController.navigate(Routes.PROFIL) }) { Icon(Icons.Default.Person, null, tint = Color.White) }
                        IconButton(onClick = { SessionManager.effacerSession(); navController.navigate(Routes.CONNEXION) { popUpTo(0) { inclusive = true } } }) { Icon(Icons.Default.Logout, null, tint = Color.White) }
                    }
                }
            }

            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // Carte credit si disponible
                if (limite > 0) {
                    val couleurCard = when (statut) { "EN_RETARD" -> Color(0xFFFFEBEE); "A_RELANCER" -> Color(0xFFFFF3E0); else -> Color(0xFFE8F5E9) }
                    val couleurTexte = when (statut) { "EN_RETARD" -> Color(0xFFC62828); "A_RELANCER" -> OrangeFruit; else -> VertFrais }
                    val emojiStatut = when (statut) { "EN_RETARD" -> "🔴"; "A_RELANCER" -> "🟡"; else -> "🟢" }

                    Card(modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(20.dp)), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = couleurCard)) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Mon Credit", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(emojiStatut, fontSize = 20.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Limite accordee", fontSize = 11.sp, color = Color.Gray)
                                    Text(limite.toFCFA(), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = VertFrais)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Dette actuelle", fontSize = 11.sp, color = Color.Gray)
                                    Text(dette.toFCFA(), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = couleurTexte)
                                }
                            }
                            // Barre de progression
                            val progression = if (limite > 0) (dette / limite).coerceIn(0.0, 1.0).toFloat() else 0f
                            LinearProgressIndicator(progress = { progression }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = couleurTexte, trackColor = Color.White)
                            Text("${(progression * 100).toInt()}% utilise", fontSize = 11.sp, color = Color.Gray)
                            user?.dateEcheance?.let {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("📅", fontSize = 14.sp)
                                    Text("Echeance: ${it.take(10)}", fontSize = 12.sp, color = couleurTexte, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                Text("Que voulez-vous faire ?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CarteMenuClient("📝", "Commander", "Passer une commande", VertFrais, { navController.navigate(Routes.NOUVELLE_COMMANDE) }, Modifier.weight(1f))
                    CarteMenuClient("🛒", "Voir le Catalogue", "Nos fruits disponibles", VertFrais, { navController.navigate(Routes.CATALOGUE) }, Modifier.weight(1f))
                    CarteMenuClient("📋", "Mes Commandes", "Historique des achats", OrangeFruit, { navController.navigate(Routes.MES_COMMANDES) }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CarteMenuClient("⚙", "Mon Profil", "Changer mot de passe", Color.Gray, { navController.navigate(Routes.PROFIL) }, Modifier.weight(1f))
                    CarteMenuClient("🚪", "Deconnexion", "Quitter l application", Color(0xFFC62828), { SessionManager.effacerSession(); navController.navigate(Routes.CONNEXION) { popUpTo(0) { inclusive = true } } }, Modifier.weight(1f))
                }

                // Message de bienvenue
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = VertFrais.copy(alpha = 0.08f))) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("🍎", fontSize = 32.sp)
                        Column {
                            Text("Bienvenue chez FruitImport !", fontWeight = FontWeight.Bold, color = VertFrais)
                            Text("Des fruits frais importes pour vous, livres a domicile.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
