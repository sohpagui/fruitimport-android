package com.fruitimport.app.ui.pdg

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.Stock
import com.fruitimport.app.ui.components.BarreApp
import com.fruitimport.app.ui.components.ChargementIndicateur
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertFrais
import com.fruitimport.app.utils.toFCFA
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class StockPDGViewModel : ViewModel() {
    var stockDouala by mutableStateOf<List<Stock>>(emptyList())
    var stockYaounde by mutableStateOf<List<Stock>>(emptyList())
    var chargement by mutableStateOf(true)

    init { charger() }

    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val rep1 = RetrofitClient.instance.obtenirStocks(agenceId = 1)
                if (rep1.isSuccessful) {
                    val json = Gson().toJson((rep1.body()?.data as? Map<*,*>)?.get("stocks"))
                    stockDouala = Gson().fromJson(json, object : TypeToken<List<Stock>>() {}.type) ?: emptyList()
                }
                val rep2 = RetrofitClient.instance.obtenirStocks(agenceId = 2)
                if (rep2.isSuccessful) {
                    val json = Gson().toJson((rep2.body()?.data as? Map<*,*>)?.get("stocks"))
                    stockYaounde = Gson().fromJson(json, object : TypeToken<List<Stock>>() {}.type) ?: emptyList()
                }
            } catch (e: Exception) {}
            chargement = false
        }
    }
}

@Composable
fun SectionStock(titre: String, stocks: List<Stock>, couleur: androidx.compose.ui.graphics.Color) {
    val parFruit = stocks.groupBy { it.fruit?.nom ?: "" }
    Text(titre, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = couleur)
    Text("${stocks.sumOf { it.quantiteCartons }} cartons au total", color = Color.Gray, fontSize = 12.sp)
    Spacer(Modifier.height(8.dp))
    parFruit.forEach { (nomFruit, stocksFruit) ->
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                val imageUrl = stocksFruit.firstOrNull()?.fruit?.imageUrl
                if (imageUrl != null) {
                    AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.size(48.dp).padding(2.dp), contentScale = ContentScale.Fit)
                } else {
                    Text("🍎", fontSize = 28.sp, modifier = Modifier.size(48.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(nomFruit, fontWeight = FontWeight.Bold)
                    stocksFruit.forEach { s ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cal. ${s.calibre?.valeur ?: ""}", fontSize = 11.sp, color = Color.Gray)
                            Text("${s.quantiteCartons} cartons - ${s.prixUnitaire.toFCFA()}", fontSize = 11.sp, color = OrangeFruit)
                        }
                    }
                }
                val total = stocksFruit.sumOf { it.quantiteCartons }
                Text("$total", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = if (total <= 5) Color.Red else couleur)
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
fun EcranStockPDG(navController: NavController, vm: StockPDGViewModel = viewModel()) {
    var agenceSelectionnee by remember { mutableStateOf(0) }

    Scaffold(
        topBar = { BarreApp("Stock des Agences", onRetour = { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.charger() }, containerColor = VertFrais) {
                Icon(Icons.Default.Refresh, null, tint = Color.White)
            }
        }
    ) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = agenceSelectionnee, containerColor = VertFrais, contentColor = Color.White) {
                Tab(selected = agenceSelectionnee == 0, onClick = { agenceSelectionnee = 0 }, text = { Text("Douala (${vm.stockDouala.sumOf { it.quantiteCartons }})", fontWeight = FontWeight.Bold) })
                Tab(selected = agenceSelectionnee == 1, onClick = { agenceSelectionnee = 1 }, text = { Text("Yaounde (${vm.stockYaounde.sumOf { it.quantiteCartons }})", fontWeight = FontWeight.Bold) })
            }
            LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    if (agenceSelectionnee == 0) SectionStock("Agence Douala", vm.stockDouala, VertFrais)
                    else SectionStock("Agence Yaounde", vm.stockYaounde, Color(0xFF1565C0))
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
