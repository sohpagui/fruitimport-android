package com.fruitimport.app.ui.secretaire

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.Stock
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.fruitimport.app.ui.components.BarreApp
import com.fruitimport.app.ui.components.ChargementIndicateur
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertFrais
import com.fruitimport.app.utils.SessionManager
import com.fruitimport.app.utils.toFCFA
import kotlinx.coroutines.launch

class StockSecretaireViewModel : ViewModel() {
    var stocks by mutableStateOf<List<Stock>>(emptyList())
    var chargement by mutableStateOf(true)

    init { charger() }

    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val agenceId = SessionManager.obtenirAgenceId() ?: 1
                val rep = RetrofitClient.instance.obtenirStocks(agenceId = agenceId)
                if (rep.isSuccessful) {
                    val data = rep.body()?.data
                    val json = Gson().toJson((data as? Map<*,*>)?.get("stocks"))
                    val parsed = Gson().fromJson<List<Stock>>(json, object : TypeToken<List<Stock>>() {}.type) ?: emptyList()
                    android.util.Log.d("STOCK", "parsed=${parsed.size} json=$json")
                    stocks = parsed
                }
            } catch (e: Exception) {}
            chargement = false
        }
    }
}

@Composable
fun EcranStockSecretaire(navController: NavController, vm: StockSecretaireViewModel = viewModel()) {
    var recherche by remember { mutableStateOf("") }

    Scaffold(
        topBar = { BarreApp("Mon Stock", onRetour = { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.charger() }, containerColor = VertFrais) {
                Icon(Icons.Default.Refresh, null, tint = Color.White)
            }
        }
    ) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = recherche, onValueChange = { recherche = it },
                label = { Text("Rechercher un fruit...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = VertFrais) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true, shape = RoundedCornerShape(12.dp)
            )
            val stocksFiltres = vm.stocks.filter {
                recherche.isBlank() || (it.fruit?.nom?.contains(recherche, ignoreCase = true) == true)
            }
            val parFruit = stocksFiltres.groupBy { it.fruit?.nom ?: "" }
            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                parFruit.forEach { (nomFruit, stocksFruit) ->
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                val imageUrl = stocksFruit.firstOrNull()?.fruit?.imageUrl
                                if (imageUrl != null) {
                                    AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.size(60.dp).padding(4.dp), contentScale = ContentScale.Fit)
                                } else {
                                    Text("🍎", fontSize = 36.sp, modifier = Modifier.size(60.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(nomFruit, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                        val totalCartons = stocksFruit.sumOf { it.quantiteCartons }
                                        Surface(shape = RoundedCornerShape(20.dp), color = if (totalCartons <= 5) Color(0xFFFFEBEE) else VertFrais.copy(alpha = 0.1f)) {
                                            Text("${totalCartons} cartons", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = if (totalCartons <= 5) Color.Red else VertFrais, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    stocksFruit.forEach { stock ->
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Calibre ${stock.calibre?.valeur ?: ""}", fontSize = 12.sp, color = Color.Gray)
                                            Text("${stock.quantiteCartons} cartons - ${stock.prixUnitaire.toFCFA()}", fontSize = 12.sp, color = OrangeFruit, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
