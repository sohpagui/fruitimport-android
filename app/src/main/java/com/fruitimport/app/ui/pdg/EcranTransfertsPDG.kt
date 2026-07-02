package com.fruitimport.app.ui.pdg

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.Transfert
import com.fruitimport.app.ui.components.*
import com.fruitimport.app.utils.traduireStatut
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class TransfertsPDGViewModel : ViewModel() {
    var chargement by mutableStateOf(true)
    var transferts by mutableStateOf<List<Transfert>>(emptyList())
    init { charger() }
    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val rep = RetrofitClient.instance.obtenirTransferts(statut = "EN_ATTENTE")
                if (rep.isSuccessful) {
                    val json = Gson().toJson((rep.body()?.data as? Map<*,*>)?.get("transferts"))
                    transferts = Gson().fromJson(json, object : TypeToken<List<Transfert>>() {}.type) ?: emptyList()
                }
            } catch (e: Exception) {}
            chargement = false
        }
    }
    fun approuver(id: Int) { viewModelScope.launch { try { RetrofitClient.instance.approuverTransfert(id); charger() } catch (e: Exception) {} } }
    fun rejeter(id: Int) { viewModelScope.launch { try { RetrofitClient.instance.rejeterTransfert(id); charger() } catch (e: Exception) {} } }
}

@Composable
fun EcranTransfertsPDG(navController: NavController, vm: TransfertsPDGViewModel = viewModel()) {
    Scaffold(topBar = { BarreApp("Transferts en attente", onRetour = { navController.popBackStack() }) }) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else LazyColumn(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vm.transferts) { t ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("${t.fruit?.nom ?: "Fruit"} — ${t.calibre?.valeur ?: ""}", fontWeight = FontWeight.Bold)
                        Text("${t.agenceSource?.nom ?: ""} → ${t.agenceDestination?.nom ?: ""}")
                        Text("Quantité : ${t.quantite} cartons")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.approuver(t.id) }, modifier = Modifier.weight(1f)) { Text("Approuver") }
                            OutlinedButton(onClick = { vm.rejeter(t.id) }, modifier = Modifier.weight(1f)) { Text("Rejeter") }
                        }
                    }
                }
            }
        }
    }
}
