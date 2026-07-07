package com.fruitimport.app.ui.pdg

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.Commande
import com.fruitimport.app.ui.components.BarreApp
import com.fruitimport.app.ui.components.BadgeStatut
import com.fruitimport.app.ui.components.ChargementIndicateur
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertFrais
import com.fruitimport.app.utils.toFCFA
import com.fruitimport.app.utils.traduireStatut
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class CommandesPDGViewModel : ViewModel() {
    var commandes by mutableStateOf<List<Commande>>(emptyList())
    var chargement by mutableStateOf(true)

    init { charger() }

    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val rep = RetrofitClient.instance.obtenirCommandes()
                if (rep.isSuccessful) {
                    val json = Gson().toJson((rep.body()?.data as? Map<*,*>)?.get("commandes"))
                    commandes = Gson().fromJson(json, object : TypeToken<List<Commande>>() {}.type) ?: emptyList()
                }
            } catch (e: Exception) {}
            chargement = false
        }
    }
}

@Composable
fun EcranCommandesPDG(navController: NavController, vm: CommandesPDGViewModel = viewModel()) {
    var recherche by remember { mutableStateOf("") }
    var filtreStatut by remember { mutableStateOf("TOUS") }
    var filtreAgence by remember { mutableStateOf("TOUS") }

    Scaffold(topBar = { BarreApp("Toutes les Commandes", onRetour = { navController.popBackStack() }) }) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = recherche, onValueChange = { recherche = it },
                label = { Text("Rechercher...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = VertFrais) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true, shape = RoundedCornerShape(12.dp)
            )
            // Filtre agence
            androidx.compose.foundation.lazy.LazyRow(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("TOUS", "DOUALA", "YAOUNDE")) { agence ->
                    FilterChip(selected = filtreAgence == agence, onClick = { filtreAgence = agence }, label = { Text(agence) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = VertFrais, selectedLabelColor = Color.White))
                }
            }
            Spacer(Modifier.height(4.dp))
            // Filtre statut
            androidx.compose.foundation.lazy.LazyRow(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("TOUS", "EN_ATTENTE", "CONFIRMEE", "PREPAREE", "EN_LIVRAISON", "LIVREE", "ANNULEE")) { statut ->
                    FilterChip(selected = filtreStatut == statut, onClick = { filtreStatut = statut }, label = { Text(if (statut == "TOUS") "TOUS" else statut.traduireStatut()) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OrangeFruit, selectedLabelColor = Color.White))
                }
            }
            Spacer(Modifier.height(4.dp))
            val commandesFiltrees = vm.commandes.filter { cmd ->
                (filtreStatut == "TOUS" || cmd.statut == filtreStatut) &&
                (filtreAgence == "TOUS" || cmd.agence?.nom?.contains(filtreAgence, ignoreCase = true) == true) &&
                (recherche.isBlank() || cmd.numero.contains(recherche, ignoreCase = true) || cmd.client?.nom?.contains(recherche, ignoreCase = true) == true)
            }
            Text("${commandesFiltrees.size} commande(s)", modifier = Modifier.padding(horizontal = 16.dp), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(commandesFiltrees) { cmd ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { navController.navigate("detail_commande/${cmd.id}") }, shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(cmd.numero, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                BadgeStatut(cmd.statut, cmd.statut.traduireStatut())
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(cmd.client?.nom ?: "", color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text(cmd.montantTotal.toFCFA(), fontWeight = FontWeight.Bold, color = OrangeFruit, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(cmd.date?.take(10) ?: "", color = Color.Gray, fontSize = 11.sp)
                                cmd.agence?.let { Text(it.nom, color = VertFrais, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
