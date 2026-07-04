package com.fruitimport.app.ui.pdg

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.DashboardPDG
import com.fruitimport.app.ui.components.BarreApp
import com.fruitimport.app.ui.components.ChargementIndicateur
import com.fruitimport.app.utils.toFCFA
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.launch

class GraphiquesViewModel : ViewModel() {
    var chargement by mutableStateOf(true)
    var stats by mutableStateOf<DashboardPDG?>(null)

    init { charger() }

    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val rep = RetrofitClient.instance.dashboardPDG()
                if (rep.isSuccessful) stats = rep.body()?.data
            } catch (e: Exception) {}
            chargement = false
        }
    }
}

@Composable
fun EcranGraphiques(navController: NavController, vm: GraphiquesViewModel = viewModel()) {
    Scaffold(topBar = { BarreApp("Graphiques & Stats", onRetour = { navController.popBackStack() }) }) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else {
            val stats = vm.stats
            if (stats == null) {
                Text("Erreur de chargement", modifier = Modifier.padding(16.dp))
            } else {
                Column(
                    modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // KPIs
                    Text("Indicateurs du jour", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Ventes", style = MaterialTheme.typography.labelSmall)
                                Text(stats.kpis.ventesTotalesJour.toFCFA(), fontWeight = FontWeight.Bold)
                            }
                        }
                        Card(modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Creances", style = MaterialTheme.typography.labelSmall)
                                Text(stats.kpis.creancesTotales.toFCFA(), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Graphique Top Fruits (Pie Chart)
                    if (stats.topFruits.isNotEmpty()) {
                        Text("Top Fruits vendus", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Card(modifier = Modifier.fillMaxWidth()) {
                            AndroidView(
                                factory = { context ->
                                    PieChart(context).apply {
                                        val entries = stats.topFruits.map { fruit ->
                                            PieEntry(fruit.montant.toFloat(), fruit.fruit.nom)
                                        }
                                        val dataSet = PieDataSet(entries, "").apply {
                                            colors = ColorTemplate.MATERIAL_COLORS.toList()
                                            valueTextSize = 12f
                                            valueTextColor = AndroidColor.WHITE
                                        }
                                        data = PieData(dataSet)
                                        description.isEnabled = false
                                        legend.isEnabled = true
                                        setUsePercentValues(true)
                                        animateY(1000)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(250.dp)
                            )
                        }
                    }

                    // Comparaison Douala vs Yaounde (Bar Chart)
                    Text("Comparaison Agences", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        AndroidView(
                            factory = { context ->
                                BarChart(context).apply {
                                    val doualaStock = stats.comparaison.douala.stockTotal.toFloat()
                                    val yaoundeStock = stats.comparaison.yaounde.stockTotal.toFloat()
                                    val entries1 = listOf(BarEntry(0f, doualaStock))
                                    val entries2 = listOf(BarEntry(1f, yaoundeStock))
                                    val set1 = BarDataSet(entries1, "Douala").apply { color = AndroidColor.parseColor("#2E7D32") }
                                    val set2 = BarDataSet(entries2, "Yaounde").apply { color = AndroidColor.parseColor("#1565C0") }
                                    data = BarData(set1, set2).apply { barWidth = 0.4f }
                                    description.isEnabled = false
                                    legend.isEnabled = true
                                    xAxis.isEnabled = false
                                    animateY(1000)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                        )
                    }

                    // Stats agences
                    Text("Detail par agence", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("DOUALA", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("Stock: ${stats.comparaison.douala.stockTotal} cartons")
                                Text("Clients: ${stats.comparaison.douala.nbClients}")
                                Text("Employes: ${stats.comparaison.douala.nbEmployes}")
                                Text("Pertes: ${stats.comparaison.douala.perteMois.toFCFA()}")
                            }
                        }
                        Card(modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("YAOUNDE", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("Stock: ${stats.comparaison.yaounde.stockTotal} cartons")
                                Text("Clients: ${stats.comparaison.yaounde.nbClients}")
                                Text("Employes: ${stats.comparaison.yaounde.nbEmployes}")
                                Text("Pertes: ${stats.comparaison.yaounde.perteMois.toFCFA()}")
                            }
                        }
                    }
                }
            }
        }
    }
}
