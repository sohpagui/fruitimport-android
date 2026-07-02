package com.fruitimport.app.ui.secretaire

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
import com.fruitimport.app.data.models.Livraison
import com.fruitimport.app.ui.components.*
import com.fruitimport.app.utils.SessionManager
import com.fruitimport.app.utils.traduireStatut
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class LivraisonsViewModel : ViewModel() {
    var chargement by mutableStateOf(true)
    var livraisons by mutableStateOf<List<Livraison>>(emptyList())
    init { charger() }
    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val agenceId = SessionManager.obtenirAgenceId()
                val rep = RetrofitClient.instance.obtenirLivraisons(agenceId = agenceId)
                if (rep.isSuccessful) {
                    val json = Gson().toJson((rep.body()?.data as? Map<*,*>)?.get("livraisons"))
                    livraisons = Gson().fromJson(json, object : TypeToken<List<Livraison>>() {}.type) ?: emptyList()
                }
            } catch (e: Exception) {}
            chargement = false
        }
    }
}

@Composable
fun EcranLivraisons(navController: NavController, vm: LivraisonsViewModel = viewModel()) {
    Scaffold(topBar = { BarreApp("Livraisons", onRetour = { navController.popBackStack() }) }) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else LazyColumn(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vm.livraisons) { liv ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Livraison #${liv.id}", fontWeight = FontWeight.Bold)
                            BadgeStatut(liv.statut, liv.statut.traduireStatut())
                        }
                        Text("Client: ${liv.commande?.client?.nom ?: ""}", color = Color.Gray)
                        Text("Livreur: ${liv.livreur?.nom ?: ""}", color = Color.Gray)
                    }
                }
            }
        }
    }
}
