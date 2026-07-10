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
import com.fruitimport.app.ui.components.BarreApp
import com.fruitimport.app.ui.components.ChargementIndicateur
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertFrais
import com.fruitimport.app.utils.toFCFA
import com.google.gson.Gson
import kotlinx.coroutines.launch

data class BeneficesData(
    val periode: String,
    val chiffreAffaires: Double,
    val coutAchat: Double,
    val beneficeBrut: Double,
    val valeurPertes: Double,
    val beneficeNet: Double,
    val margePercent: String,
    val parFruit: List<BeneficeFruit>
)
data class BeneficeFruit(val nom: String, val benefice: Double, val quantite: Int)

class BeneficesViewModel : ViewModel() {
    var data by mutableStateOf<BeneficesData?>(null)
    var chargement by mutableStateOf(true)
    var periode by mutableStateOf("jour")
    init { charger() }
    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val rep = RetrofitClient.instance.obtenirBenefices(periode)
                if (rep.isSuccessful) {
                    val json = Gson().toJson(rep.body()?.data)
                    data = Gson().fromJson(json, BeneficesData::class.java)
                }
            } catch (e: Exception) {}
            chargement = false
        }
    }
}

@Composable
fun EcranBenefices(navController: NavController, vm: BeneficesViewModel = viewModel()) {
    Scaffold(
        topBar = { BarreApp("Benefices & Pertes", onRetour = { navController.popBackStack() }) }
    ) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else Column(modifier = Modifier.padding(padding)) {
            // Filtres periode
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("jour" to "Aujourd hui", "semaine" to "7 jours", "mois" to "Ce mois").forEach { (key, label) ->
                    FilterChip(selected = vm.periode == key, onClick = { vm.periode = key; vm.charger() }, label = { Text(label) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = VertFrais, selectedLabelColor = Color.White))
                }
            }
            val d = vm.data
            if (d != null) {
                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        // Carte benefice net
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = if (d.beneficeNet >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Benefice Net", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
                                Text(d.beneficeNet.toFCFA(), fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = if (d.beneficeNet >= 0) VertFrais else Color.Red)
                                Text("Marge: ${d.margePercent}%", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                    item {
                        // Details financiers
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Details financiers", fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Chiffre d affaires", color = Color.Gray)
                                    Text(d.chiffreAffaires.toFCFA(), fontWeight = FontWeight.Bold, color = VertFrais)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Benefice brut", color = Color.Gray)
                                    Text(d.beneficeBrut.toFCFA(), fontWeight = FontWeight.Bold, color = VertFrais)
                                }
                                Divider()
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Valeur pertes", color = Color.Gray)
                                    Text("- ${d.valeurPertes.toFCFA()}", fontWeight = FontWeight.Bold, color = Color.Red)
                                }
                                Divider()
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("BENEFICE NET", fontWeight = FontWeight.Bold)
                                    Text(d.beneficeNet.toFCFA(), fontWeight = FontWeight.ExtraBold, color = if (d.beneficeNet >= 0) VertFrais else Color.Red)
                                }
                            }
                        }
                    }
                    item {
                        // Top fruits les plus rentables
                        if (d.parFruit.isNotEmpty()) {
                            Text("Top fruits les plus rentables", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                    items(d.parFruit.size) { i ->
                        val fruit = d.parFruit[i]
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("${i+1}.", fontWeight = FontWeight.Bold, color = OrangeFruit)
                                    Column {
                                        Text(fruit.nom, fontWeight = FontWeight.Bold)
                                        Text("${fruit.quantite} cartons vendus", color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                                Text(fruit.benefice.toFCFA(), fontWeight = FontWeight.ExtraBold, color = VertFrais)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}
