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
import com.fruitimport.app.data.models.Fruit
import com.fruitimport.app.data.models.Livraison
import com.fruitimport.app.data.models.RetourRequest
import com.fruitimport.app.ui.components.*
import com.fruitimport.app.utils.SessionManager
import com.fruitimport.app.utils.toFCFA
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class RetoursViewModel : ViewModel() {
    var fruits by mutableStateOf<List<Fruit>>(emptyList())
    var livraisons by mutableStateOf<List<Livraison>>(emptyList())
    var succes by mutableStateOf(false)
    var erreur by mutableStateOf<String?>(null)
    var chargement by mutableStateOf(true)

    init { charger() }

    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                fruits = RetrofitClient.instance.obtenirFruits().body()?.data ?: emptyList()
                val agenceId = SessionManager.obtenirAgenceId()
                val rep = RetrofitClient.instance.obtenirLivraisons(agenceId = agenceId)
                if (rep.isSuccessful) {
                    val json = Gson().toJson((rep.body()?.data as? Map<*,*>)?.get("livraisons"))
                    livraisons = Gson().fromJson(json, object : TypeToken<List<Livraison>>() {}.type) ?: emptyList()
                }
            } catch (e: Exception) { erreur = e.message }
            chargement = false
        }
    }

    fun enregistrerRetour(livraisonId: Int, fruitId: Int, calibreId: Int, quantite: Int, raison: String) {
        viewModelScope.launch {
            erreur = null
            try {
                val rep = RetrofitClient.instance.creerRetour(
                    RetourRequest(livraisonId = livraisonId, fruitId = fruitId, calibreId = calibreId, quantite = quantite, raison = raison)
                )
                if (rep.isSuccessful && rep.body()?.success == true) {
                    succes = true
                } else {
                    erreur = rep.body()?.message ?: "Erreur lors du retour"
                }
            } catch (e: Exception) { erreur = e.message }
        }
    }
}

@Composable
fun EcranRetours(navController: NavController, vm: RetoursViewModel = viewModel()) {
    var livraisonSelectionnee by remember { mutableStateOf<Livraison?>(null) }
    var fruitSelectionne by remember { mutableStateOf<Fruit?>(null) }
    var calibreIdx by remember { mutableStateOf(0) }
    var quantite by remember { mutableStateOf("") }
    var raison by remember { mutableStateOf("") }

    LaunchedEffect(vm.succes) { if (vm.succes) navController.popBackStack() }

    Scaffold(topBar = { BarreApp("Enregistrer un retour", onRetour = { navController.popBackStack() }) }) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else Column(
            modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            vm.erreur?.let { Text(it, color = Color.Red) }

            Text("Livraison concernee", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            if (vm.livraisons.isEmpty()) {
                Text("Aucune livraison disponible", color = Color.Gray)
            } else {
                vm.livraisons.forEach { liv ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(selected = livraisonSelectionnee?.id == liv.id, onClick = { livraisonSelectionnee = liv })
                        Column {
                            Text("Livraison #${liv.id}", fontWeight = FontWeight.Bold)
                            Text("Statut: ${liv.statut}", color = Color.Gray)
                        }
                    }
                }
            }

            Text("Fruit retourne", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            vm.fruits.forEach { f ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected = fruitSelectionne?.id == f.id, onClick = { fruitSelectionne = f; calibreIdx = 0 })
                    Text(f.nom)
                }
            }

            fruitSelectionne?.let { f ->
                Text("Calibre", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                f.calibres.forEachIndexed { i, c ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(selected = calibreIdx == i, onClick = { calibreIdx = i })
                        Text(c.valeur)
                    }
                }
            }

            OutlinedTextField(
                value = quantite, onValueChange = { quantite = it },
                label = { Text("Quantite retournee (cartons)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = raison, onValueChange = { raison = it },
                label = { Text("Raison du retour") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Button(
                onClick = {
                    val liv = livraisonSelectionnee
                    val f = fruitSelectionne
                    if (liv == null) { vm.erreur = "Selectionnez une livraison"; return@Button }
                    if (f == null) { vm.erreur = "Selectionnez un fruit"; return@Button }
                    val c = f.calibres.getOrNull(calibreIdx)
                    if (c == null) { vm.erreur = "Selectionnez un calibre"; return@Button }
                    if (quantite.isBlank()) { vm.erreur = "Entrez la quantite"; return@Button }
                    if (raison.isBlank()) { vm.erreur = "Entrez la raison"; return@Button }
                    vm.enregistrerRetour(liv.id, f.id, c.id, quantite.toIntOrNull() ?: 0, raison)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Enregistrer le retour")
            }
        }
    }
}
