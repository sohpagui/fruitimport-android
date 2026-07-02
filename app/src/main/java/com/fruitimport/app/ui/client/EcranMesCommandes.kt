package com.fruitimport.app.ui.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.fruitimport.app.data.models.Commande
import com.fruitimport.app.ui.components.*
import com.fruitimport.app.utils.SessionManager
import com.fruitimport.app.utils.toFCFA
import com.fruitimport.app.utils.traduireStatut
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class MesCommandesViewModel : ViewModel() {
    var chargement by mutableStateOf(true)
    var commandes by mutableStateOf<List<Commande>>(emptyList())
    init {
        viewModelScope.launch {
            try {
                val clientId = SessionManager.utilisateurConnecte?.id
                val rep = RetrofitClient.instance.obtenirCommandes(clientId = clientId)
                if (rep.isSuccessful) {
                    val json = Gson().toJson((rep.body()?.data as? Map<*,*>)?.get("commandes"))
                    commandes = Gson().fromJson(json, object : TypeToken<List<Commande>>() {}.type) ?: emptyList()
                }
            } catch (e: Exception) {}
            chargement = false
        }
    }
}

@Composable
fun EcranMesCommandes(navController: NavController, vm: MesCommandesViewModel = viewModel()) {
    Scaffold(topBar = { BarreApp("Mes Commandes", onRetour = { navController.popBackStack() }) }) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else LazyColumn(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vm.commandes) { cmd ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(cmd.numero, fontWeight = FontWeight.Bold)
                            BadgeStatut(cmd.statut, cmd.statut.traduireStatut())
                        }
                        Text(cmd.montantTotal.toFCFA())
                        Text(cmd.date.take(10))
                    }
                }
            }
        }
    }
}
