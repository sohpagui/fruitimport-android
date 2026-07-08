package com.fruitimport.app.ui.pdg

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
import com.fruitimport.app.ui.components.BarreApp
import com.fruitimport.app.ui.components.ChargementIndicateur
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertFrais
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

data class Perte(
    val id: Int,
    val agenceId: Int,
    val fruitId: Int,
    val quantite: Int,
    val valeurPerdue: Double,
    val date: String,
    val raison: String? = null,
    val fruit: com.fruitimport.app.data.models.Fruit? = null,
    val agence: com.fruitimport.app.data.models.Agence? = null
)

class PertesPDGViewModel : ViewModel() {
    var pertes by mutableStateOf<List<Perte>>(emptyList())
    var chargement by mutableStateOf(true)
    var filtreAgence by mutableStateOf(0)

    init { charger() }

    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val agenceId = if (filtreAgence == 0) null else filtreAgence
                val rep = RetrofitClient.instance.obtenirPertes(agenceId)
                if (rep.isSuccessful) {
                    val json = Gson().toJson((rep.body()?.data as? Map<*,*>)?.get("pertes"))
                    pertes = Gson().fromJson(json, object : TypeToken<List<Perte>>() {}.type) ?: emptyList()
                }
            } catch (e: Exception) {}
            chargement = false
        }
    }
}

@Composable
fun EcranPertesPDG(navController: NavController, vm: PertesPDGViewModel = viewModel()) {
    Scaffold(
        topBar = { BarreApp("Pertes Enregistrees", onRetour = { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.charger() }, containerColor = OrangeFruit) {
                Icon(Icons.Default.Refresh, null, tint = Color.White)
            }
        }
    ) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else Column(modifier = Modifier.padding(padding)) {
            // Filtre agence
            androidx.compose.foundation.lazy.LazyRow(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(0 to "Toutes", 1 to "Douala", 2 to "Yaounde")) { (id, nom) ->
                    FilterChip(selected = vm.filtreAgence == id, onClick = { vm.filtreAgence = id; vm.charger() }, label = { Text(nom) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = VertFrais, selectedLabelColor = Color.White))
                }
            }
            val totalPertes = vm.pertes.sumOf { it.valeurPerdue }
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Total pertes: ${vm.pertes.size}", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                    Text("${totalPertes.toInt()} FCFA", fontWeight = FontWeight.ExtraBold, color = Color(0xFFC62828))
                }
            }
            Spacer(Modifier.height(8.dp))
            if (vm.pertes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucune perte enregistree", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(vm.pertes) { perte ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(perte.fruit?.nom ?: "", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("${perte.quantite} cartons", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(perte.agence?.nom ?: "", color = VertFrais, fontSize = 12.sp)
                                    Text("${perte.valeurPerdue.toInt()} FCFA", color = OrangeFruit, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                }
                                Text(perte.date.take(10), color = Color.Gray, fontSize = 11.sp)
                                perte.raison?.let { Text("Raison: $it", color = Color.Gray, fontSize = 11.sp) }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}
