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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.Client
import com.fruitimport.app.data.models.Stock
import com.fruitimport.app.ui.components.*
import com.fruitimport.app.utils.SessionManager
import com.fruitimport.app.utils.toFCFA
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class NouvelleCommandeViewModel : ViewModel() {
    var clients by mutableStateOf<List<Client>>(emptyList())
    var catalogue by mutableStateOf<List<Stock>>(emptyList())
    var chargement by mutableStateOf(true)
    var succes by mutableStateOf(false)
    var erreur by mutableStateOf<String?>(null)

    init { charger() }

    fun charger() {
        viewModelScope.launch {
            val agenceId = SessionManager.obtenirAgenceId() ?: 1
            try {
                val repClients = RetrofitClient.instance.obtenirClients(agenceId = agenceId)
                if (repClients.isSuccessful) {
                    val json = Gson().toJson((repClients.body()?.data as? Map<*,*>)?.get("clients"))
                    clients = Gson().fromJson(json, object : TypeToken<List<Client>>() {}.type) ?: emptyList()
                }
                val repCat = RetrofitClient.instance.obtenirCatalogue(agenceId)
                if (repCat.isSuccessful) catalogue = repCat.body()?.data ?: emptyList()
            } catch (e: Exception) { erreur = e.message }
            chargement = false
        }
    }

    fun creerCommande(clientId: Int, modePaiement: String, lignes: List<Map<String, Any>>, adresse: String) {
        viewModelScope.launch {
            val agenceId = SessionManager.obtenirAgenceId() ?: 1
            try {
                val data = mapOf("agenceId" to agenceId, "clientId" to clientId,
                    "modePaiement" to modePaiement, "adresseLivraison" to adresse, "lignes" to lignes)
                val rep = RetrofitClient.instance.creerCommande(data)
                if (rep.isSuccessful && rep.body()?.success == true) succes = true
                else erreur = rep.body()?.message ?: "Erreur"
            } catch (e: Exception) { erreur = e.message }
        }
    }
}

@Composable
fun EcranNouvelleCommande(navController: NavController, vm: NouvelleCommandeViewModel = viewModel()) {
    var clientSelectionne by remember { mutableStateOf<Client?>(null) }
    var modePaiement by remember { mutableStateOf("ESPECES") }
    var adresse by remember { mutableStateOf("") }
    var stockSelectionne by remember { mutableStateOf<Stock?>(null) }
    var quantite by remember { mutableStateOf("1") }
    val lignes = remember { mutableStateListOf<Map<String, Any>>() }

    LaunchedEffect(vm.succes) { if (vm.succes) navController.popBackStack() }

    Scaffold(topBar = { BarreApp("Nouvelle Commande", onRetour = { navController.popBackStack() }) }) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            vm.erreur?.let { Text(it, color = Color.Red) }

            // Sélection client
            Text("Client", style = MaterialTheme.typography.labelLarge)
            vm.clients.forEach { client ->
                Row {
                    RadioButton(selected = clientSelectionne?.id == client.id, onClick = { clientSelectionne = client })
                    Text("${client.nom} (${client.type})")
                }
            }

            // Mode paiement
            Text("Mode de paiement", style = MaterialTheme.typography.labelLarge)
            Row {
                listOf("ESPECES" to "Espèces", "CREDIT" to "Crédit").forEach { (val_, label) ->
                    Row {
                        RadioButton(selected = modePaiement == val_, onClick = { modePaiement = val_ })
                        Text(label)
                        Spacer(Modifier.width(12.dp))
                    }
                }
            }

            OutlinedTextField(value = adresse, onValueChange = { adresse = it }, label = { Text("Adresse de livraison") }, modifier = Modifier.fillMaxWidth())

            // Ajouter un article
            Text("Ajouter un article", style = MaterialTheme.typography.labelLarge)
            vm.catalogue.take(10).forEach { stock ->
                Row {
                    RadioButton(selected = stockSelectionne?.id == stock.id, onClick = { stockSelectionne = stock })
                    Text("${stock.fruit?.nom ?: ""} ${stock.calibre?.valeur ?: ""} — ${stock.prixUnitaire.toFCFA()}")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = quantite, onValueChange = { quantite = it }, label = { Text("Quantité") }, modifier = Modifier.weight(1f))
                Button(onClick = {
                    stockSelectionne?.let { s ->
                        lignes.add(mapOf(
                            "fruitId" to s.fruitId, "calibreId" to s.calibreId,
                            "categorie" to s.categorie, "quantite" to (quantite.toIntOrNull() ?: 1),
                            "prixUnitaire" to s.prixUnitaire
                        ))
                    }
                }) { Text("Ajouter") }
            }

            // Résumé des lignes
            if (lignes.isNotEmpty()) {
                Text("Articles (${lignes.size})", style = MaterialTheme.typography.labelLarge)
                lignes.forEach { ligne -> Text("• Qté: ${ligne["quantite"]} × ${(ligne["prixUnitaire"] as? Double)?.toFCFA()}") }
            }

            Button(
                onClick = { clientSelectionne?.let { vm.creerCommande(it.id, modePaiement, lignes.toList(), adresse) } },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = clientSelectionne != null && lignes.isNotEmpty()
            ) { Text("Valider la commande") }
        }
    }
}
