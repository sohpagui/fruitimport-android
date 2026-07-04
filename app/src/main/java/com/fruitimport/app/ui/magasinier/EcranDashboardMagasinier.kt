package com.fruitimport.app.ui.magasinier

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
import kotlinx.coroutines.launch

class DashboardMagasinierViewModel : ViewModel() {
    var chargement by mutableStateOf(true)
    var stats by mutableStateOf<StatsAgence?>(null)
    init { charger() }
    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val rep = RetrofitClient.instance.dashboardAgence(SessionManager.obtenirAgenceId() ?: 1)
                if (rep.isSuccessful) stats = rep.body()?.data
            } catch (e: Exception) {}
            chargement = false
        }
    }
}

@Composable
fun EcranDashboardMagasinier(navController: NavController, vm: DashboardMagasinierViewModel = viewModel()) {
    Scaffold(
        topBar = { BarreApp("Dashboard Magasinier", actions = {
            IconButton(onClick = { navController.navigate(Routes.PROFIL) }) {
                    Icon(Icons.Default.Person, contentDescription = null)
                }
                    IconButton(onClick = { SessionManager.effacerSession(); navController.navigate(Routes.CONNEXION) { popUpTo(0) { inclusive = true } } }) {
                Icon(Icons.Default.Logout, null)
            }
        }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Dashboard, null) }, label = { Text("Accueil") })
                NavigationBarItem(selected = false, onClick = { navController.navigate(Routes.STOCK) }, icon = { Icon(Icons.Default.Inventory, null) }, label = { Text("Stock") })
                NavigationBarItem(selected = false, onClick = { navController.navigate(Routes.RECEPTION) }, icon = { Icon(Icons.Default.MoveToInbox, null) }, label = { Text("Réception") })
                NavigationBarItem(selected = false, onClick = { navController.navigate(Routes.PERTES) }, icon = { Icon(Icons.Default.Delete, null) }, label = { Text("Pertes") })
            }
        }
    ) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Bonjour, ${SessionManager.utilisateurConnecte?.nom} 👋", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            val s = vm.stats
            if (s != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CarteStatistique("Stock total", "${s.stockTotal} cartons", Icons.Default.Inventory, Color(0xFF2E7D32), Modifier.weight(1f))
                    CarteStatistique("Livraisons", "${s.livraisonsEnCours}", Icons.Default.LocalShipping, Color(0xFF1565C0), Modifier.weight(1f))
                }
            }
            Button(onClick = { navController.navigate(Routes.TRANSFERTS) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.SwapHoriz, null)
                Spacer(Modifier.width(8.dp))
                Text("Demander un transfert vers Yaoundé")
            }
        }
    }
}
