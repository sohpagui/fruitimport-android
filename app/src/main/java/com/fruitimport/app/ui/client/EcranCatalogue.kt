package com.fruitimport.app.ui.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.fruitimport.app.data.models.Stock
import com.fruitimport.app.ui.components.*
import com.fruitimport.app.utils.SessionManager
import com.fruitimport.app.utils.toFCFA
import kotlinx.coroutines.launch

class CatalogueViewModel : ViewModel() {
    var chargement by mutableStateOf(true)
    var stocks by mutableStateOf<List<Stock>>(emptyList())
    init {
        viewModelScope.launch {
            try {
                val agenceId = SessionManager.utilisateurConnecte?.agenceId ?: 1
                stocks = RetrofitClient.instance.obtenirCatalogue(agenceId).body()?.data ?: emptyList()
            } catch (e: Exception) {}
            chargement = false
        }
    }
}

@Composable
fun EcranCatalogue(navController: NavController, vm: CatalogueViewModel = viewModel()) {
    Scaffold(topBar = { BarreApp("Catalogue", onRetour = { navController.popBackStack() }) }) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else LazyColumn(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vm.stocks) { stock ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("${stock.fruit?.nom ?: ""}", fontWeight = FontWeight.Bold)
                        Text("Calibre : ${stock.calibre?.valeur ?: ""}", color = Color.Gray)
                        Text("Prix : ${stock.prixUnitaire.toFCFA()}")
                        Text("Disponible : ${stock.quantiteCartons} cartons", color = Color(0xFF2E7D32))
                    }
                }
            }
        }
    }
}
