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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.PrixComptoirRequest
import com.fruitimport.app.ui.components.BarreApp
import com.fruitimport.app.ui.components.ChargementIndicateur
import com.fruitimport.app.ui.secretaire.InfosComptoir
import com.fruitimport.app.ui.secretaire.StockComptoirItem
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertFrais
import com.fruitimport.app.utils.toFCFA
import com.google.gson.Gson
import kotlinx.coroutines.launch

data class StatsComptoir(
    val versementsJour: VersementsJour,
    val versementsMois: Double,
    val pertesJour: PertesJour,
    val stock: List<StockComptoirItem>
)
data class VersementsJour(val montant: Double, val nb: Int)
data class PertesJour(val quantite: Int, val nb: Int)

class ComptoirPDGViewModel : ViewModel() {
    var stats by mutableStateOf<StatsComptoir?>(null)
    var comptoir by mutableStateOf<InfosComptoir?>(null)
    var chargement by mutableStateOf(true)
    var succes by mutableStateOf<String?>(null)
    var erreur by mutableStateOf<String?>(null)
    init { charger() }
    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val r1 = RetrofitClient.instance.statsComptoir()
                if (r1.isSuccessful) {
                    val json = Gson().toJson(r1.body()?.data)
                    stats = Gson().fromJson(json, StatsComptoir::class.java)
                }
                val r2 = RetrofitClient.instance.obtenirComptoir()
                if (r2.isSuccessful) {
                    val json = Gson().toJson(r2.body()?.data)
                    comptoir = Gson().fromJson(json, InfosComptoir::class.java)
                }
            } catch (e: Exception) { erreur = e.message }
            chargement = false
        }
    }
    fun modifierPrix(fruitId: Int, calibreId: Int, prix: Double, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                val rep = RetrofitClient.instance.modifierPrixComptoir(PrixComptoirRequest(fruitId, calibreId, prix))
                if (rep.isSuccessful) { succes = "Prix mis a jour !"; charger(); onDone() }
                else erreur = rep.body()?.message ?: "Erreur"
            } catch (e: Exception) { erreur = e.message }
        }
    }
}

@Composable
fun EcranComptoirPDG(navController: NavController, vm: ComptoirPDGViewModel = viewModel()) {
    var stockSelectionne by remember { mutableStateOf<StockComptoirItem?>(null) }
    var nouveauPrix by remember { mutableStateOf("") }

    stockSelectionne?.let { stock ->
        AlertDialog(
            onDismissRequest = { stockSelectionne = null },
            title = { Text("Modifier prix detail") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${stock.fruit?.nom} - Calibre ${stock.calibre?.valeur}", fontWeight = FontWeight.Bold)
                    Text("Prix actuel: ${stock.prixDetail.toFCFA()}", color = Color.Gray)
                    OutlinedTextField(value = nouveauPrix, onValueChange = { nouveauPrix = it }, label = { Text("Nouveau prix (FCFA)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val prix = nouveauPrix.toDoubleOrNull() ?: return@Button
                    vm.modifierPrix(stock.fruit?.id ?: 0, stock.calibre?.id ?: 0, prix) { stockSelectionne = null; nouveauPrix = "" }
                }, colors = ButtonDefaults.buttonColors(containerColor = VertFrais)) { Text("Confirmer") }
            },
            dismissButton = { TextButton(onClick = { stockSelectionne = null }) { Text("Annuler") } }
        )
    }

    Scaffold(
        topBar = { BarreApp("Comptoir Yaounde - PDG", onRetour = { navController.popBackStack() }) },
        floatingActionButton = { FloatingActionButton(onClick = { vm.charger() }, containerColor = VertFrais) { Icon(Icons.Default.Refresh, null, tint = Color.White) } }
    ) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else LazyColumn(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                vm.succes?.let { Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) { Text(it, modifier = Modifier.padding(12.dp), color = VertFrais) } }
                vm.erreur?.let { Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) { Text(it, modifier = Modifier.padding(12.dp), color = Color.Red) } }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("👤", fontSize = 24.sp)
                        Column {
                            Text("Gerant actuel", color = Color.Gray, fontSize = 12.sp)
                            Text(vm.comptoir?.gerantActuel?.nom ?: "Non defini", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
            item {
                Text("Finances du comptoir", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                val s = vm.stats
                if (s != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("💰", fontSize = 24.sp)
                                Text(s.versementsJour.montant.toFCFA(), fontWeight = FontWeight.ExtraBold, color = VertFrais, fontSize = 13.sp)
                                Text("Versements jour", color = Color.Gray, fontSize = 10.sp)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📅", fontSize = 24.sp)
                                Text(s.versementsMois.toFCFA(), fontWeight = FontWeight.ExtraBold, color = OrangeFruit, fontSize = 13.sp)
                                Text("Versements mois", color = Color.Gray, fontSize = 10.sp)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📉", fontSize = 24.sp)
                                Text("${s.pertesJour.quantite} cartons", fontWeight = FontWeight.ExtraBold, color = Color.Red, fontSize = 13.sp)
                                Text("Pertes jour", color = Color.Gray, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
            item { Text("Stock comptoir - cliquer pour modifier prix", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            items(vm.comptoir?.stockComptoir?.size ?: 0) { i ->
                val stock = vm.comptoir!!.stockComptoir[i]
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(stock.fruit?.nom ?: "", fontWeight = FontWeight.Bold)
                            Text("Calibre: ${stock.calibre?.valeur ?: ""}", color = Color.Gray, fontSize = 12.sp)
                            Text("${stock.quantite} cartons", color = VertFrais, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stock.prixDetail.toFCFA(), fontWeight = FontWeight.Bold, color = OrangeFruit)
                            TextButton(onClick = { stockSelectionne = stock; nouveauPrix = stock.prixDetail.toInt().toString() }) { Text("Modifier prix", fontSize = 11.sp) }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
