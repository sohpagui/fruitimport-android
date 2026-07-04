package com.fruitimport.app.ui.pdg

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.DashboardPDG
import com.fruitimport.app.navigation.Routes
import com.fruitimport.app.ui.components.*
import com.fruitimport.app.utils.SessionManager
import com.fruitimport.app.utils.toFCFA
import kotlinx.coroutines.launch

class DashboardPDGViewModel : ViewModel() {
    var chargement by mutableStateOf(true)
    var dashboard by mutableStateOf<DashboardPDG?>(null)
    var erreur by mutableStateOf<String?>(null)

    init { charger() }

    fun charger() {
        viewModelScope.launch {
            chargement = true
            erreur = null
            try {
                val rep = RetrofitClient.instance.dashboardPDG()
                if (rep.isSuccessful) dashboard = rep.body()?.data
                else erreur = "Erreur de chargement"
            } catch (e: Exception) {
                erreur = "Impossible de contacter le serveur."
            }
            chargement = false
        }
    }
}

@Composable
fun EcranDashboardPDG(navController: NavController, vm: DashboardPDGViewModel = viewModel()) {
    Scaffold(
        topBar = {
            BarreApp(
                titre = "Dashboard PDG",
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.GRAPHIQUES) }) {
                        Icon(Icons.Default.BarChart, contentDescription = "Graphiques")
                    }
                    IconButton(onClick = { navController.navigate(Routes.PROFIL) }) {
                        Icon(Icons.Default.Person, contentDescription = "Profil")
                    }
                    IconButton(onClick = {
                        SessionManager.effacerSession()
                        navController.navigate(Routes.CONNEXION) { popUpTo(0) { inclusive = true } }
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Deconnexion")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = true, onClick = {},
                    icon = { Icon(Icons.Default.Dashboard, null) }, label = { Text("Dashboard") })
                NavigationBarItem(selected = false, onClick = { navController.navigate(Routes.EMPLOYES) },
                    icon = { Icon(Icons.Default.People, null) }, label = { Text("Employés") })
                NavigationBarItem(selected = false, onClick = { navController.navigate(Routes.CLIENTS_PDG) },
                    icon = { Icon(Icons.Default.Store, null) }, label = { Text("Clients") })
                NavigationBarItem(selected = false, onClick = { navController.navigate(Routes.TRANSFERTS_PDG) },
                    icon = { Icon(Icons.Default.SwapHoriz, null) }, label = { Text("Transferts") })
            }
        }
    ) { padding ->
        when {
            vm.chargement -> ChargementIndicateur()
            vm.erreur != null -> MessageErreur(vm.erreur!!, onReessayer = { vm.charger() })
            else -> {
                val data = vm.dashboard!!
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Bonjour, ${SessionManager.utilisateurConnecte?.nom} 👋",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    // KPIs
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CarteStatistique(
                            titre = "Ventes du jour",
                            valeur = data.kpis.ventesTotalesJour.toFCFA(),
                            icone = Icons.Default.TrendingUp,
                            couleur = Color(0xFF2E7D32),
                            modifier = Modifier.weight(1f)
                        )
                        CarteStatistique(
                            titre = "Commandes",
                            valeur = data.kpis.nbCommandesJour.toString(),
                            icone = Icons.Default.ShoppingCart,
                            couleur = Color(0xFF1565C0),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CarteStatistique(
                            titre = "Créances",
                            valeur = data.kpis.creancesTotales.toFCFA(),
                            icone = Icons.Default.AccountBalance,
                            couleur = Color(0xFFE65100),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Comparaison agences
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Comparaison Agences", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("DOUALA", fontWeight = FontWeight.Bold)
                                    Text(data.comparaison.douala.ventesJour.montant.toFCFA(), fontSize = 13.sp)
                                    Text("Stock: ${data.comparaison.douala.stockTotal} cartons", fontSize = 12.sp, color = Color.Gray)
                                    if (data.comparaison.meilleureAgence == "DOUALA") {
                                        Badge { Text("🏆 Meilleure") }
                                    }
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("YAOUNDÉ", fontWeight = FontWeight.Bold)
                                    Text(data.comparaison.yaounde.ventesJour.montant.toFCFA(), fontSize = 13.sp)
                                    Text("Stock: ${data.comparaison.yaounde.stockTotal} cartons", fontSize = 12.sp, color = Color.Gray)
                                    if (data.comparaison.meilleureAgence == "YAOUNDE") {
                                        Badge { Text("🏆 Meilleure") }
                                    }
                                }
                            }
                        }
                    }

                    // Alertes
                    val alertes = data.alertes
                    val totalAlertes = alertes.clientsEnRetard + alertes.stockBas +
                            alertes.transfertsEnAttente + alertes.commandesEnAttente
                    if (totalAlertes > 0) {
                        Card(modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4))) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("⚠️ Alertes ($totalAlertes)", fontWeight = FontWeight.Bold)
                                if (alertes.stockBas > 0) Text("• ${alertes.stockBas} stock(s) bas", fontSize = 13.sp)
                                if (alertes.clientsEnRetard > 0) Text("• ${alertes.clientsEnRetard} client(s) en retard", fontSize = 13.sp)
                                if (alertes.transfertsEnAttente > 0) Text("• ${alertes.transfertsEnAttente} transfert(s) en attente", fontSize = 13.sp)
                                if (alertes.commandesEnAttente > 0) Text("• ${alertes.commandesEnAttente} commande(s) en attente", fontSize = 13.sp)
                            }
                        }
                    }

                    // Synthèse automatique
                    if (data.synthese.isNotEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("📊 Synthèse", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                data.synthese.forEach { phrase ->
                                    Text("• $phrase", fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
