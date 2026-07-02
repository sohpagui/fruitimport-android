package com.fruitimport.app.ui.client

import androidx.compose.foundation.layout.*
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
import com.fruitimport.app.data.models.ClientDetail
import com.fruitimport.app.navigation.Routes
import com.fruitimport.app.ui.components.BarreApp
import com.fruitimport.app.ui.components.ChargementIndicateur
import com.fruitimport.app.utils.SessionManager
import com.fruitimport.app.utils.toFCFA
import kotlinx.coroutines.launch

class DashboardClientViewModel : ViewModel() {
    var chargement by mutableStateOf(true)
    var clientDetail by mutableStateOf<ClientDetail?>(null)
    init { charger() }
    fun charger() {
        viewModelScope.launch {
            try {
                val id = SessionManager.utilisateurConnecte?.id ?: return@launch
                val rep = RetrofitClient.instance.obtenirDetailClient(id)
                if (rep.isSuccessful) clientDetail = rep.body()?.data
            } catch (e: Exception) {}
            chargement = false
        }
    }
}

@Composable
fun EcranDashboardClient(navController: NavController, vm: DashboardClientViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.charger() }
    Scaffold(
        topBar = { BarreApp("Mon Espace", actions = {
            IconButton(onClick = { SessionManager.effacerSession(); navController.navigate(Routes.CONNEXION) { popUpTo(0) { inclusive = true } } }) {
                Icon(Icons.Default.Logout, null)
            }
        }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Accueil") })
                NavigationBarItem(selected = false, onClick = { navController.navigate(Routes.CATALOGUE) }, icon = { Icon(Icons.Default.ShoppingBag, null) }, label = { Text("Catalogue") })
                NavigationBarItem(selected = false, onClick = { navController.navigate(Routes.MES_COMMANDES) }, icon = { Icon(Icons.Default.Receipt, null) }, label = { Text("Commandes") })
            }
        }
    ) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Default.Person, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
            Text(SessionManager.utilisateurConnecte?.nom ?: "", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            val detail = vm.clientDetail
            if (detail != null && detail.limiteCredit.toDouble() > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (detail.statutCredit == "EN_RETARD") Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Mon Credit", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Limite accordee:")
                            Text(detail.limiteCredit.toDouble().toFCFA(), fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Dette actuelle:")
                            Text(detail.creditUtilise.toDouble().toFCFA(),
                                color = if (detail.creditUtilise.toDouble() > 0) Color.Red else Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold)
                        }
                        if (detail.dateEcheance != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Echeance:")
                                Text(detail.dateEcheance.take(10), color = Color.Red)
                            }
                        }
                        val statut = when (detail.statutCredit) {
                            "EN_REGLE" -> Pair("En regle", Color(0xFF2E7D32))
                            "A_RELANCER" -> Pair("A relancer", Color(0xFFE65100))
                            "EN_RETARD" -> Pair("En retard", Color.Red)
                            else -> Pair(detail.statutCredit, Color.Gray)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Statut:")
                            Text(statut.first, color = statut.second, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Button(onClick = { navController.navigate(Routes.CATALOGUE) }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Icon(Icons.Default.ShoppingBag, null); Spacer(Modifier.width(8.dp)); Text("Voir le catalogue")
            }
            OutlinedButton(onClick = { navController.navigate(Routes.MES_COMMANDES) }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Icon(Icons.Default.Receipt, null); Spacer(Modifier.width(8.dp)); Text("Mes commandes")
            }
        }
    }
}
