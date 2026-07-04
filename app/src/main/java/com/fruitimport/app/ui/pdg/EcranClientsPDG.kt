package com.fruitimport.app.ui.pdg

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.Client
import com.fruitimport.app.data.models.ModifierLimiteCreditRequest
import com.fruitimport.app.ui.components.*
import com.fruitimport.app.utils.toFCFA
import com.fruitimport.app.utils.traduireStatut
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class ClientsPDGViewModel : ViewModel() {
    var chargement by mutableStateOf(true)
    var clients by mutableStateOf<List<Client>>(emptyList())
    init { charger() }
    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val rep = RetrofitClient.instance.obtenirClients()
                if (rep.isSuccessful) {
                    val json = Gson().toJson((rep.body()?.data as? Map<*,*>)?.get("clients"))
                    clients = Gson().fromJson(json, object : TypeToken<List<Client>>() {}.type) ?: emptyList()
                }
            } catch (e: Exception) {}
            chargement = false
        }
    }
    fun modifierLimite(clientId: Int, limite: Double) {
        viewModelScope.launch {
            try { RetrofitClient.instance.modifierLimiteCredit(clientId, ModifierLimiteCreditRequest(limiteCredit = limite)) } catch (e: Exception) {}
            charger()
        }
    }
}

@Composable
fun EcranClientsPDG(navController: NavController, vm: ClientsPDGViewModel = viewModel()) {
    var recherche by remember { mutableStateOf("") }
    var filtreStatut by remember { mutableStateOf("TOUS") }

    Scaffold(topBar = { BarreApp("Clients", onRetour = { navController.popBackStack() }) }) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else Column(modifier = Modifier.padding(padding)) {
            // Barre de recherche
            OutlinedTextField(
                value = recherche,
                onValueChange = { recherche = it },
                label = { Text("Rechercher un client...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )
            // Filtres par statut
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("TOUS", "EN_REGLE", "A_RELANCER", "EN_RETARD").forEach { statut ->
                    FilterChip(
                        selected = filtreStatut == statut,
                        onClick = { filtreStatut = statut },
                        label = { Text(when(statut) {
                            "TOUS" -> "Tous"
                            "EN_REGLE" -> "OK"
                            "A_RELANCER" -> "Relancer"
                            "EN_RETARD" -> "Retard"
                            else -> statut
                        }, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            // Liste filtrée
            val clientsFiltres = vm.clients.filter { client ->
                val matchRecherche = recherche.isBlank() ||
                    client.nom.contains(recherche, ignoreCase = true) ||
                    client.telephone.contains(recherche)
                val matchStatut = filtreStatut == "TOUS" || client.statutCredit == filtreStatut
                matchRecherche && matchStatut
            }
            Text(
                "${clientsFiltres.size} client(s)",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(clientsFiltres) { client ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(client.nom, fontWeight = FontWeight.Bold)
                                BadgeStatut(client.statutCredit, client.statutCredit.traduireStatut())
                            }
                            Text(client.type.traduireStatut(), color = Color.Gray)
                            Text("Credit utilise : ${client.creditUtilise.toFCFA()} / ${client.limiteCredit.toFCFA()}")
                            Text(client.telephone, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
