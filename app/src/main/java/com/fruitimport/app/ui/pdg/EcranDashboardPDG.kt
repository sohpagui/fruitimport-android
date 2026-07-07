package com.fruitimport.app.ui.pdg

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.DashboardPDG
import com.fruitimport.app.navigation.Routes
import com.fruitimport.app.ui.components.ChargementIndicateur
import com.fruitimport.app.ui.components.BoutonMessages
import com.fruitimport.app.ui.components.PhotoViewer
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertClair
import com.fruitimport.app.ui.theme.VertFrais
import com.fruitimport.app.utils.SessionManager
import com.fruitimport.app.utils.toFCFA
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardPDGViewModel : ViewModel() {
    var chargement by mutableStateOf(true)
    var stats by mutableStateOf<DashboardPDG?>(null)
    var erreur by mutableStateOf<String?>(null)
    var photoUrl by mutableStateOf(SessionManager.utilisateurConnecte?.photoUrl)

    init { charger() }

    fun charger() {
        viewModelScope.launch {
            chargement = true
            erreur = null
            try {
                val rep = RetrofitClient.instance.dashboardPDG()
                if (rep.isSuccessful) {
                    stats = rep.body()?.data
                    photoUrl = SessionManager.utilisateurConnecte?.photoUrl
                }
                else erreur = "Erreur de chargement"
            } catch (e: Exception) { erreur = e.message }
            chargement = false
        }
    }
}

@Composable
fun CarteAction(
    emoji: String,
    titre: String,
    sousTitre: String,
    couleur: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(couleur.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 28.sp)
            }
            Text(titre, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center, color = Color(0xFF1C1B1F))
            Text(sousTitre, fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

@Composable
fun CarteKPI(titre: String, valeur: String, emoji: String, couleur: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = couleur.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(emoji, fontSize = 32.sp)
            Column {
                Text(valeur, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = couleur)
                Text(titre, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun EcranDashboardPDG(navController: NavController, vm: DashboardPDGViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.charger() }
    var afficherPhoto by remember { mutableStateOf(false) }

    val dateAujourdhui = remember {
        SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRENCH).format(Date()).replaceFirstChar { it.uppercase() }
    }

    if (afficherPhoto && SessionManager.utilisateurConnecte?.photoUrl != null) {
        PhotoViewer(url = SessionManager.utilisateurConnecte?.photoUrl!!, onFermer = { afficherPhoto = false })
    }
    Box(modifier = Modifier.fillMaxSize()) {
        // Fond degrade
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(colors = listOf(Color(0xFFF1F8E9), Color(0xFFE8F5E9), Color(0xFFF5F5F5)))
            )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // En-tete avec fond vert
            Box(
                modifier = Modifier.fillMaxWidth().background(
                    Brush.horizontalGradient(colors = listOf(VertFrais, Color(0xFF388E3C)))
                ).padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Avatar initiales
                        if (SessionManager.utilisateurConnecte?.photoUrl != null) {
                            AsyncImage(
                                model = SessionManager.utilisateurConnecte?.photoUrl,
                                contentDescription = null,
                                modifier = Modifier.size(50.dp).clip(CircleShape).clickable { afficherPhoto = true },
                                contentScale = ContentScale.Crop
                            )
                        } else {
                        Box(
                            modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                SessionManager.utilisateurConnecte?.nom?.take(1) ?: "P",
                                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White
                            )
                        }
                        }
                        Column {
                            Text("Bonjour 👋", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                            Text(
                                SessionManager.utilisateurConnecte?.nom ?: "",
                                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White
                            )
                        }
                    }
                    Row {
                        IconButton(onClick = { navController.navigate(Routes.GRAPHIQUES) }) {
                            Icon(Icons.Default.BarChart, null, tint = Color.White)
                        }
                        BoutonMessages(navController)
                        IconButton(onClick = { navController.navigate(Routes.PROFIL) }) {
                            Icon(Icons.Default.Person, null, tint = Color.White)
                        }
                        IconButton(onClick = {
                            SessionManager.effacerSession()
                            navController.navigate(Routes.CONNEXION) { popUpTo(0) { inclusive = true } }
                        }) {
                            Icon(Icons.Default.Logout, null, tint = Color.White)
                        }
                    }
                }
            }

            // Date
            Box(
                modifier = Modifier.fillMaxWidth().background(OrangeFruit).padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Text(dateAujourdhui, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            if (vm.chargement) {
                ChargementIndicateur()
            } else if (vm.erreur != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("😔", fontSize = 48.sp)
                        Text(vm.erreur!!, color = Color.Gray, textAlign = TextAlign.Center)
                        Button(onClick = { vm.charger() }, colors = ButtonDefaults.buttonColors(containerColor = VertFrais)) {
                            Text("Reessayer")
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val s = vm.stats

                    // KPIs
                    if (s != null) {
                        Text("Aujourd'hui", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1C1B1F))
                        CarteKPI("Ventes du jour", s.kpis.ventesTotalesJour.toFCFA(), "💰", VertFrais, Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CarteKPI("Commandes", "${s.kpis.nbCommandesJour}", "📦", OrangeFruit, Modifier.weight(1f))
                            CarteKPI("Creances", s.kpis.creancesTotales.toFCFA(), "⚠️", Color(0xFFC62828), Modifier.weight(1f))
                        }

                        // Alertes
                        if (s.alertes.stockBas > 0 || s.alertes.clientsEnRetard > 0 || s.alertes.commandesEnAttente > 0) {
                            Text("⚠️ Alertes", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFC62828))
                            if (s.alertes.stockBas > 0) {
                                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("📉", fontSize = 20.sp)
                                        Text("${s.alertes.stockBas} produit(s) en stock bas", color = Color(0xFFC62828), fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                            if (s.alertes.clientsEnRetard > 0) {
                                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("💳", fontSize = 20.sp)
                                        Text("${s.alertes.clientsEnRetard} client(s) en retard de paiement", color = OrangeFruit, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        // Comparaison agences
                        Text("Mes agences", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1C1B1F))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Card(modifier = Modifier.weight(1f).shadow(4.dp, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp)) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🏢 DOUALA", fontWeight = FontWeight.Bold, color = VertFrais)
                                    Text("Stock: ${s.comparaison.douala.stockTotal} cartons", fontSize = 12.sp)
                                    Text("Clients: ${s.comparaison.douala.nbClients}", fontSize = 12.sp)
                                    Text("Employes: ${s.comparaison.douala.nbEmployes}", fontSize = 12.sp)
                                }
                            }
                            Card(modifier = Modifier.weight(1f).shadow(4.dp, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp)) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🏢 YAOUNDE", fontWeight = FontWeight.Bold, color = OrangeFruit)
                                    Text("Stock: ${s.comparaison.yaounde.stockTotal} cartons", fontSize = 12.sp)
                                    Text("Clients: ${s.comparaison.yaounde.nbClients}", fontSize = 12.sp)
                                    Text("Employes: ${s.comparaison.yaounde.nbEmployes}", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Menu actions
                    Text("Que voulez-vous faire ?", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1C1B1F))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CarteAction("👥", "Mes Clients", "Gerer les credits", VertFrais, { navController.navigate(Routes.CLIENTS_PDG) }, Modifier.weight(1f))
                        CarteAction("👨‍💼", "Mon Equipe", "Voir les employes", OrangeFruit, { navController.navigate(Routes.EMPLOYES) }, Modifier.weight(1f))
                        CarteAction("🔄", "Transferts", "Douala ↔ Yaounde", Color(0xFF1565C0), { navController.navigate(Routes.TRANSFERTS_PDG) }, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CarteAction("📊", "Graphiques", "Voir les stats", Color(0xFF6A1B9A), { navController.navigate(Routes.GRAPHIQUES) }, Modifier.weight(1f))
                        CarteAction("⚙️", "Parametres", "Logo et config", Color(0xFF00695C), { navController.navigate(Routes.PARAMETRES_PDG) }, Modifier.weight(1f))
                        CarteAction("📋", "Commandes", "Toutes les agences", OrangeFruit, { navController.navigate(Routes.COMMANDES_PDG) }, Modifier.weight(1f))
                        CarteAction("🚪", "Deconnexion", "Quitter l'app", Color(0xFFC62828), {
                            SessionManager.effacerSession()
                            navController.navigate(Routes.CONNEXION) { popUpTo(0) { inclusive = true } }
                        }, Modifier.weight(1f))
                    }

                    // Synthese
                    if (s != null && s.synthese.isNotEmpty()) {
                        Text("Informations", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1C1B1F))
                        s.synthese.forEach { msg ->
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("ℹ️", fontSize = 16.sp)
                                    Text(msg, fontSize = 13.sp, color = Color(0xFF1B5E20))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
