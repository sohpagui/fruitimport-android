package com.fruitimport.app.ui.secretaire

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.Commande
import com.fruitimport.app.data.models.AssignerLivraisonRequest
import com.fruitimport.app.data.models.Livraison
import com.fruitimport.app.data.models.User
import com.fruitimport.app.ui.components.*
import com.fruitimport.app.utils.SessionManager
import com.fruitimport.app.utils.toFCFA
import com.fruitimport.app.utils.traduireStatut
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class LivraisonsViewModel : ViewModel() {
    var chargement by mutableStateOf(true)
    var livraisons by mutableStateOf<List<Livraison>>(emptyList())
    var commandesPretees by mutableStateOf<List<Commande>>(emptyList())
    var livreurs by mutableStateOf<List<User>>(emptyList())
    var afficherFormulaire by mutableStateOf(false)
    var succes by mutableStateOf(false)
    var erreur by mutableStateOf<String?>(null)

    init { charger() }

    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val agenceId = SessionManager.obtenirAgenceId()

                // Livraisons existantes
                val repLiv = RetrofitClient.instance.obtenirLivraisons(agenceId = agenceId)
                if (repLiv.isSuccessful) {
                    val json = Gson().toJson((repLiv.body()?.data as? Map<*,*>)?.get("livraisons"))
                    livraisons = Gson().fromJson(json, object : TypeToken<List<Livraison>>() {}.type) ?: emptyList()
                }

                // Commandes pretes a livrer
                val repCmd = RetrofitClient.instance.obtenirCommandes(agenceId = agenceId, statut = "PREPAREE")
                if (repCmd.isSuccessful) {
                    val json = Gson().toJson((repCmd.body()?.data as? Map<*,*>)?.get("commandes"))
                    commandesPretees = Gson().fromJson(json, object : TypeToken<List<Commande>>() {}.type) ?: emptyList()
                }

                // Livreurs de l agence
                val repEmp = RetrofitClient.instance.obtenirLivreurs(role = "LIVREUR", agenceId = agenceId)
                if (repEmp.isSuccessful) {
                    val json = Gson().toJson((repEmp.body()?.data as? Map<*,*>)?.get("users"))
                    livreurs = Gson().fromJson(json, object : TypeToken<List<User>>() {}.type) ?: emptyList()
                }
            } catch (e: Exception) { erreur = e.message }
            chargement = false
        }
    }

    fun assignerLivraison(commandeId: Int, livreurId: Int) {
        viewModelScope.launch {
            try {
                val rep = RetrofitClient.instance.creerLivraison(
                    AssignerLivraisonRequest(commandeId = commandeId, livreurId = livreurId)
                )
                if (rep.isSuccessful && rep.body()?.success == true) {
                    afficherFormulaire = false
                    succes = true
                    charger()
                } else {
                    erreur = rep.body()?.message ?: "Erreur lors de l assignation"
                }
            } catch (e: Exception) { erreur = e.message }
        }
    }
}

@Composable
fun EcranLivraisons(navController: NavController, vm: LivraisonsViewModel = viewModel()) {
    var commandeSelectionnee by remember { mutableStateOf<Commande?>(null) }
    var livreurSelectionne by remember { mutableStateOf<User?>(null) }

    Scaffold(
        topBar = { BarreApp("Livraisons", onRetour = { navController.popBackStack() }) },
        floatingActionButton = {
            if (vm.commandesPretees.isNotEmpty()) {
                FloatingActionButton(onClick = { vm.afficherFormulaire = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Assigner livraison")
                }
            }
        }
    ) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (vm.commandesPretees.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(Modifier.width(8.dp))
                            Text("${vm.commandesPretees.size} commande(s) prete(s) a livrer", color = Color(0xFF2E7D32))
                        }
                    }
                }
            }

            vm.erreur?.let { err ->
                item { Text(err, color = Color.Red) }
            }

            items(vm.livraisons) { liv ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Livraison #${liv.id}", fontWeight = FontWeight.Bold)
                            BadgeStatut(liv.statut, liv.statut.traduireStatut())
                        }
                        Text("Client: ${liv.commande?.client?.nom ?: ""}", color = Color.Gray)
                        Text("Commande: ${liv.commande?.numero ?: ""}", color = Color.Gray)
                        Text("Livreur: ${liv.livreur?.nom ?: "Non assigne"}", color = Color.Gray)
                        liv.commande?.montantTotal?.let {
                            Text("Montant: ${it.toFCFA()}")
                        }
                    }
                }
            }

            if (vm.livraisons.isEmpty()) {
                item { Text("Aucune livraison pour le moment.", color = Color.Gray) }
            }
        }
    }

    if (vm.afficherFormulaire) {
        AlertDialog(
            onDismissRequest = { vm.afficherFormulaire = false },
            title = { Text("Assigner une livraison") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Commande a livrer", style = MaterialTheme.typography.labelLarge)
                    vm.commandesPretees.forEach { cmd ->
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            RadioButton(
                                selected = commandeSelectionnee?.id == cmd.id,
                                onClick = { commandeSelectionnee = cmd }
                            )
                            Column {
                                Text(cmd.numero, fontWeight = FontWeight.Bold)
                                Text(cmd.client?.nom ?: "", color = Color.Gray)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("Livreur", style = MaterialTheme.typography.labelLarge)
                    if (vm.livreurs.isEmpty()) {
                        Text("Aucun livreur disponible", color = Color.Gray)
                    } else {
                        vm.livreurs.forEach { livreur ->
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                RadioButton(
                                    selected = livreurSelectionne?.id == livreur.id,
                                    onClick = { livreurSelectionne = livreur }
                                )
                                Text(livreur.nom)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cmd = commandeSelectionnee
                        val liv = livreurSelectionne
                        if (cmd != null && liv != null) {
                            vm.assignerLivraison(cmd.id, liv.id)
                        } else {
                            vm.erreur = "Selectionnez une commande et un livreur"
                        }
                    },
                    enabled = commandeSelectionnee != null && livreurSelectionne != null
                ) { Text("Assigner") }
            },
            dismissButton = {
                TextButton(onClick = { vm.afficherFormulaire = false }) { Text("Annuler") }
            }
        )
    }
}
