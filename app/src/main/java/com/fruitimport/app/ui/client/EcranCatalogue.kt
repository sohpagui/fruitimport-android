package com.fruitimport.app.ui.client

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
                val rep = RetrofitClient.instance.obtenirCatalogue(agenceId)
                if (rep.isSuccessful) stocks = rep.body()?.data ?: emptyList()
            } catch (e: Exception) {}
            chargement = false
        }
    }
}

@Composable
fun EcranCatalogue(navController: NavController, vm: CatalogueViewModel = viewModel()) {
    var recherche by remember { mutableStateOf("") }

    Scaffold(
        topBar = { BarreApp("Catalogue des Fruits", onRetour = { navController.popBackStack() }) }
    ) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = recherche, onValueChange = { recherche = it },
                label = { Text("Rechercher un fruit...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = VertFrais) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VertFrais, focusedLabelColor = VertFrais)
            )
            val stocksFiltres = vm.stocks.filter { stock ->
                recherche.isBlank() || (stock.fruit?.nom?.contains(recherche, ignoreCase = true) == true)
            }
            val parFruit = stocksFiltres.groupBy { it.fruit?.nom ?: "" }
            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                parFruit.forEach { (nomFruit, stocksFruit) ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column {
                                val imageUrl = stocksFruit.firstOrNull()?.fruit?.imageUrl
                                if (imageUrl != null) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = nomFruit,
                                        modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)).background(VertFrais.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) { Text("🍎", fontSize = 48.sp) }
                                }
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(nomFruit, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF1C1B1F))
                                        Surface(shape = RoundedCornerShape(20.dp), color = VertFrais.copy(alpha = 0.1f)) {
                                            Text("${stocksFruit.sumOf { it.quantiteCartons }} cartons dispo", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = VertFrais, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                    stocksFruit.forEach { stock ->
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text("Calibre ${stock.calibre?.valeur ?: ""}", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                                Text("${stock.quantiteCartons} cartons disponibles", fontSize = 12.sp, color = Color.Gray)
                                            }
                                            Surface(shape = RoundedCornerShape(12.dp), color = OrangeFruit.copy(alpha = 0.1f)) {
                                                Text(stock.prixUnitaire.toFCFA(), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = OrangeFruit, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
