package com.fruitimport.app.ui.magasinier

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
import com.fruitimport.app.data.models.Stock
import com.fruitimport.app.ui.components.*
import com.fruitimport.app.utils.SessionManager
import com.fruitimport.app.utils.toFCFA
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class StockViewModel : ViewModel() {
    var chargement by mutableStateOf(true)
    var stocks by mutableStateOf<List<Stock>>(emptyList())
    var alertes by mutableStateOf<List<Stock>>(emptyList())
    init { charger() }
    fun charger() {
        viewModelScope.launch {
            chargement = true
            val agenceId = SessionManager.obtenirAgenceId()
            try {
                val rep = RetrofitClient.instance.obtenirStocks(agenceId = agenceId)
                if (rep.isSuccessful) {
                    val json = Gson().toJson((rep.body()?.data as? Map<*,*>)?.get("stocks"))
                    stocks = Gson().fromJson(json, object : TypeToken<List<Stock>>() {}.type) ?: emptyList()
                }
                val repAlertes = RetrofitClient.instance.obtenirAlertes(agenceId = agenceId)
                if (repAlertes.isSuccessful) alertes = repAlertes.body()?.data ?: emptyList()
            } catch (e: Exception) {}
            chargement = false
        }
    }
}

@Composable
fun EcranStock(navController: NavController, vm: StockViewModel = viewModel()) {
    var recherche by remember { mutableStateOf("") }

    Scaffold(topBar = { BarreApp("Stock", onRetour = { navController.popBackStack() }) }) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = recherche,
                onValueChange = { recherche = it },
                label = { Text("Rechercher un fruit...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )
            val stocksFiltres = vm.stocks.filter { stock ->
                recherche.isBlank() || (stock.fruit?.nom?.contains(recherche, ignoreCase = true) == true)
            }
            Text(
                "${stocksFiltres.size} article(s)",
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (vm.alertes.isNotEmpty()) {
                    item { CarteAlerte("⚠️ ${vm.alertes.size} stock(s) bas !", "warning") }
                }
                items(stocksFiltres) { stock ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${stock.fruit?.nom ?: ""} — ${stock.calibre?.valeur ?: ""}", fontWeight = FontWeight.Bold)
                                Text(
                                    "${stock.quantiteCartons} cartons",
                                    color = if (stock.quantiteCartons <= 5) Color.Red else Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text("${stock.origine} • ${stock.categorie}", color = Color.Gray)
                            Text(stock.prixUnitaire.toFCFA())
                        }
                    }
                }
            }
        }
    }
}
