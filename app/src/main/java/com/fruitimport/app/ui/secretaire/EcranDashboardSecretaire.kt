package com.fruitimport.app.ui.secretaire

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.fruitimport.app.data.models.StatsAgence
import com.fruitimport.app.navigation.Routes
import com.fruitimport.app.ui.components.*
import com.fruitimport.app.utils.SessionManager
import com.fruitimport.app.utils.toFCFA
import kotlinx.coroutines.launch

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
fun EcranDashboardSecretaire(navController: NavController, vm: DashboardSecretaireViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.charger() }

    Scaffold(
        topBar = {
            BarreApp("Dashboard Secrétaire", actions = {
                IconButton(onClick = { SessionManager.effacerSession(); navController.navigate(Routes.CONNEXION) { popUpTo(0) { inclusive = true } } }) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                }
            })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Dashboard, null) }, label = { Text("Accueil") })
                NavigationBarItem(selected = false, onClick = { navController.navigate(Routes.COMMANDES) }, icon = { Icon(Icons.Default.ShoppingCart, null) }, label = { Text("Commandes") })
                NavigationBarItem(selected = false, onClick = { navController.navigate(Routes.LIVRAISONS) }, icon = { Icon(Icons.Default.LocalShipping, null) }, label = { Text("Livraisons") })
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Routes.NOUVELLE_COMMANDE) }) {
                Icon(Icons.Default.Add, contentDescription = "Nouvelle commande")
            }
        }
    ) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else {
            val s = vm.stats
            Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Bonjour, ${SessionManager.utilisateurConnecte?.nom} 👋", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (s != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CarteStatistique("Ventes du jour", s.ventesJour.montant.toFCFA(), Icons.Default.TrendingUp, Color(0xFF2E7D32), Modifier.weight(1f))
                        CarteStatistique("Commandes", s.ventesJour.nbCommandes.toString(), Icons.Default.ShoppingCart, Color(0xFF1565C0), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CarteStatistique("Livraisons en cours", s.livraisonsEnCours.toString(), Icons.Default.LocalShipping, Color(0xFFE65100), Modifier.weight(1f))
                        CarteStatistique("Clients", s.nbClients.toString(), Icons.Default.People, Color(0xFF6A1B9A), Modifier.weight(1f))
                    }
                }
                Button(onClick = { navController.navigate(Routes.NOUVELLE_COMMANDE) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Nouvelle Vente")
                }
            }
        }
    }
}
