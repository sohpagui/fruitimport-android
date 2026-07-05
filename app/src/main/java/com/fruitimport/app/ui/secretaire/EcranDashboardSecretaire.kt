package com.fruitimport.app.ui.secretaire

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.StatsAgence
import com.fruitimport.app.navigation.Routes
import com.fruitimport.app.ui.components.ChargementIndicateur
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertFrais
import com.fruitimport.app.utils.SessionManager
import com.fruitimport.app.utils.toFCFA
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardSecretaireViewModel : ViewModel() {
    var chargement by mutableStateOf(true)
    var stats by mutableStateOf<StatsAgence?>(null)
    init { charger() }
    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val agenceId = SessionManager.obtenirAgenceId() ?: 1
                val rep = RetrofitClient.instance.dashboardAgence(agenceId)
                if (rep.isSuccessful) stats = rep.body()?.data
            } catch (e: Exception) {}
            chargement = false
        }
    }
}

@Composable
fun BoutonAction(emoji: String, titre: String, couleur: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.shadow(4.dp, RoundedCornerShape(16.dp)).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(couleur.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 24.sp)
            }
            Text(titre, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, color = Color(0xFF1C1B1F))
        }
    }
}

@Composable
fun EcranDashboardSecretaire(navController: NavController, vm: DashboardSecretaireViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.charger() }
    val dateAujourdhui = remember { SimpleDateFormat("EEE d MMM", Locale.FRENCH).format(Date()).replaceFirstChar { it.uppercase() } }
    val agenceNom = if (SessionManager.obtenirAgenceId() == 1) "Douala" else "Yaounde"

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color(0xFFF1F8E9), Color(0xFFF5F5F5)))))
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(Color(0xFF1565C0), Color(0xFF1976D2)))).padding(horizontal = 20.dp, vertical = 14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val photoUrl = SessionManager.utilisateurConnecte?.photoUrl
                        if (photoUrl != null) {
                            AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.size(45.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            Box(modifier = Modifier.size(45.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                Text(SessionManager.utilisateurConnecte?.nom?.take(1) ?: "S", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        Column {
                            Text("Secretaire $agenceNom", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            Text(SessionManager.utilisateurConnecte?.nom ?: "", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Row {
                        IconButton(onClick = { navController.navigate(Routes.PROFIL) }) { Icon(Icons.Default.Person, null, tint = Color.White) }
                        IconButton(onClick = { SessionManager.effacerSession(); navController.navigate(Routes.CONNEXION) { popUpTo(0) { inclusive = true } } }) { Icon(Icons.Default.Logout, null, tint = Color.White) }
                    }
                }
            }
            Box(modifier = Modifier.fillMaxWidth().background(OrangeFruit).padding(horizontal = 20.dp, vertical = 5.dp)) {
                Text("$dateAujourdhui", color = Color.White, fontSize = 12.sp)
            }
            if (vm.chargement) ChargementIndicateur()
            else Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val s = vm.stats
                if (s != null) {
                    Text("Mon bilan du jour", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Card(modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = VertFrais.copy(alpha = 0.1f))) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("💰", fontSize = 32.sp)
                            Column {
                                Text(s.ventesJour.montant.toFCFA(), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = VertFrais)
                                Text("Ventes (${s.ventesJour.nbCommandes} commandes)", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📦", fontSize = 24.sp)
                                Text("${s.stockTotal}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF1565C0))
                                Text("Stock", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🚚", fontSize = 24.sp)
                                Text("${s.livraisonsEnCours}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF6A1B9A))
                                Text("Livraisons", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("👥", fontSize = 24.sp)
                                Text("${s.nbClients}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = VertFrais)
                                Text("Clients", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
                Text("Que voulez-vous faire ?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BoutonAction("🛒", "Nouvelle Vente", VertFrais, { navController.navigate(Routes.NOUVELLE_COMMANDE) }, Modifier.weight(1f))
                    BoutonAction("📋", "Commandes", OrangeFruit, { navController.navigate(Routes.COMMANDES) }, Modifier.weight(1f))
                    BoutonAction("🚚", "Livraisons", Color(0xFF6A1B9A), { navController.navigate(Routes.LIVRAISONS) }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BoutonAction("↩", "Retours", Color(0xFFC62828), { navController.navigate(Routes.RETOURS) }, Modifier.weight(1f))
                    BoutonAction("👤", "Clients", Color(0xFF00695C), { navController.navigate(Routes.CLIENTS_PDG) }, Modifier.weight(1f))
                    BoutonAction("⚙", "Profil", Color.Gray, { navController.navigate(Routes.PROFIL) }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
