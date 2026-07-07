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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.Stock
import com.fruitimport.app.ui.components.BarreApp
import com.fruitimport.app.ui.components.ChargementIndicateur
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertFrais
import com.fruitimport.app.utils.SessionManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class AlertesStockViewModel : ViewModel() {
    var alertes by mutableStateOf<List<Stock>>(emptyList())
    var chargement by mutableStateOf(true)

    init { charger() }

    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val agenceId = SessionManager.obtenirAgenceId() ?: 1
                val rep = RetrofitClient.instance.obtenirAlertesStock(agenceId)
                if (rep.isSuccessful) {
                    val json = Gson().toJson(rep.body()?.data)
                    alertes = Gson().fromJson(json, object : TypeToken<List<Stock>>() {}.type) ?: emptyList()
                }
            } catch (e: Exception) {}
            chargement = false
        }
    }
}

@Composable
fun EcranAlertesStock(navController: NavController, vm: AlertesStockViewModel = viewModel()) {
    Scaffold(
        topBar = { BarreApp("Alertes Stock Bas", onRetour = { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.charger() }, containerColor = OrangeFruit) {
                Icon(Icons.Default.Refresh, null, tint = Color.White)
            }
        }
    ) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else if (vm.alertes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("✅", fontSize = 64.sp)
                    Text("Aucune alerte", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = VertFrais)
                    Text("Tous les stocks sont suffisants", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("⚠️", fontSize = 20.sp)
                            Text("${vm.alertes.size} article(s) en stock bas (≤ 5 cartons)", color = Color(0xFFC62828), fontWeight = FontWeight.Medium)
                        }
                    }
                }
                items(vm.alertes) { stock ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(stock.fruit?.nom ?: "", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Calibre: ${stock.calibre?.valeur ?: ""}", color = Color.Gray, fontSize = 12.sp)
                                Text("Prix: ${stock.prixUnitaire.toInt()} FCFA", color = OrangeFruit, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${stock.quantiteCartons}", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = Color(0xFFC62828))
                                Text("cartons", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
