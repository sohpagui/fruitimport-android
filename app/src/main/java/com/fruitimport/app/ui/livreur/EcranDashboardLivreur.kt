package com.fruitimport.app.ui.livreur

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.fruitimport.app.data.models.Livraison
import com.fruitimport.app.data.models.MettreAJourLivraisonRequest
import com.fruitimport.app.navigation.Routes
import com.fruitimport.app.ui.components.*
import com.fruitimport.app.utils.SessionManager
import com.fruitimport.app.utils.traduireStatut
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class LivreurViewModel : ViewModel() {
    var chargement by mutableStateOf(true)
    var livraisons by mutableStateOf<List<Livraison>>(emptyList())
    init { charger() }
    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val livreurId = SessionManager.utilisateurConnecte?.id
                val rep = RetrofitClient.instance.obtenirLivraisons(livreurId = livreurId)
                if (rep.isSuccessful) {
                    val json = Gson().toJson((rep.body()?.data as? Map<*,*>)?.get("livraisons"))
                    livraisons = Gson().fromJson(json, object : TypeToken<List<Livraison>>() {}.type) ?: emptyList()
                }
            } catch (e: Exception) {}
            chargement = false
        }
    }
    fun mettreAJour(id: Int, statut: String, note: String = "") {
        viewModelScope.launch {
            try {
                val data = MettreAJourLivraisonRequest(
                    statut = statut,
                    noteProbleme = if (note.isNotBlank()) note else null
                )
                RetrofitClient.instance.mettreAJourLivraison(id, data)
                charger()
            } catch (e: Exception) {}
        }
    }
}

@Composable
fun EcranDashboardLivreur(navController: NavController, vm: LivreurViewModel = viewModel()) {
    Scaffold(
        topBar = { BarreApp("Mes Livraisons", actions = {
            IconButton(onClick = { SessionManager.effacerSession(); navController.navigate(Routes.CONNEXION) { popUpTo(0) { inclusive = true } } }) {
                Icon(Icons.Default.Logout, null)
            }
        }) }
    ) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else Column(modifier = Modifier.padding(padding)) {
            Text("Bonjour, ${SessionManager.utilisateurConnecte?.nom} 👋",
                modifier = Modifier.padding(16.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(vm.livraisons) { liv ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Livraison #${liv.id}", fontWeight = FontWeight.Bold)
                                BadgeStatut(liv.statut, liv.statut.traduireStatut())
                            }
                            Text("Client: ${liv.commande?.client?.nom ?: ""}", color = Color.Gray)
                            Text("Adresse: ${liv.commande?.adresseLivraison ?: "Non spécifiée"}", color = Color.Gray)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (liv.statut == "PREPARE") {
                                    Button(onClick = { vm.mettreAJour(liv.id, "EN_ROUTE") }, modifier = Modifier.weight(1f)) { Text("En route") }
                                }
                                if (liv.statut == "EN_ROUTE") {
                                    Button(onClick = { vm.mettreAJour(liv.id, "LIVRE") }, modifier = Modifier.weight(1f)) { Text("Livré ✓") }
                                    OutlinedButton(onClick = { vm.mettreAJour(liv.id, "PROBLEME", "Problème signalé") }, modifier = Modifier.weight(1f)) { Text("Problème") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
