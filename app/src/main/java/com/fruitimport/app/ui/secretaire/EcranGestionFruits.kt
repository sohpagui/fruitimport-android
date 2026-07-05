package com.fruitimport.app.ui.secretaire

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.fruitimport.app.data.models.Calibre
import com.fruitimport.app.data.models.AjouterCalibreRequest
import com.fruitimport.app.data.models.CreerFruitRequest
import com.fruitimport.app.data.models.Fruit
import com.fruitimport.app.data.models.ModifierCalibreRequest
import com.fruitimport.app.ui.components.BarreApp
import com.fruitimport.app.ui.components.ChargementIndicateur
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertFrais
import kotlinx.coroutines.launch

class GestionFruitsViewModel : ViewModel() {
    var fruits by mutableStateOf<List<Fruit>>(emptyList())
    var chargement by mutableStateOf(true)
    var succes by mutableStateOf<String?>(null)
    var erreur by mutableStateOf<String?>(null)

    init { charger() }

    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val rep = RetrofitClient.instance.obtenirFruits()
                if (rep.isSuccessful) fruits = rep.body()?.data ?: emptyList()
            } catch (e: Exception) { erreur = e.message }
            chargement = false
        }
    }

    fun creerFruit(nom: String, unite: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                val rep = RetrofitClient.instance.creerFruit(CreerFruitRequest(nom = nom, uniteMesure = unite))
                if (rep.isSuccessful) { succes = "Fruit cree !"; charger(); onDone() }
                else erreur = "HTTP ${rep.code()}: ${rep.body()?.message ?: rep.errorBody()?.string() ?: "Erreur inconnue"}"
            } catch (e: Exception) { erreur = e.message }
        }
    }

    fun ajouterCalibre(fruitId: Int, valeur: String, prixAchat: Double, prixVente: Double, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                val rep = RetrofitClient.instance.ajouterCalibre(fruitId, AjouterCalibreRequest(valeur = valeur, prixAchat = prixAchat, prixVente = prixVente))
                if (rep.isSuccessful) { succes = "Calibre ajoute !"; charger(); onDone() }
                else erreur = "HTTP ${rep.code()}: ${rep.body()?.message ?: rep.errorBody()?.string() ?: "Erreur inconnue"}"
            } catch (e: Exception) { erreur = e.message }
        }
    }

    fun modifierCalibre(calibreId: Int, prixVente: Double, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                val rep = RetrofitClient.instance.modifierCalibre(calibreId, ModifierCalibreRequest(prixVente = prixVente))
                if (rep.isSuccessful) { succes = "Prix modifie !"; charger(); onDone() }
                else erreur = "HTTP ${rep.code()}: ${rep.body()?.message ?: rep.errorBody()?.string() ?: "Erreur inconnue"}"
            } catch (e: Exception) { erreur = e.message }
        }
    }
}

@Composable
fun EcranGestionFruits(navController: NavController, vm: GestionFruitsViewModel = viewModel()) {
    var afficherNouveauFruit by remember { mutableStateOf(false) }
    var fruitSelectionne by remember { mutableStateOf<Fruit?>(null) }
    var afficherNouveauCalibre by remember { mutableStateOf(false) }
    var calibreAModifier by remember { mutableStateOf<Calibre?>(null) }

    // Dialog nouveau fruit
    if (afficherNouveauFruit) {
        var nom by remember { mutableStateOf("") }
        var unite by remember { mutableStateOf("carton") }
        AlertDialog(
            onDismissRequest = { afficherNouveauFruit = false },
            title = { Text("Nouveau fruit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom du fruit") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Text("Unite de mesure", fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("carton", "kg").forEach { u ->
                            FilterChip(selected = unite == u, onClick = { unite = u }, label = { Text(u) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { if (nom.isNotBlank()) vm.creerFruit(nom, unite) { afficherNouveauFruit = false } }, colors = ButtonDefaults.buttonColors(containerColor = VertFrais)) {
                    Text("Creer")
                }
            },
            dismissButton = { TextButton(onClick = { afficherNouveauFruit = false }) { Text("Annuler") } }
        )
    }

    // Dialog nouveau calibre
    if (afficherNouveauCalibre && fruitSelectionne != null) {
        var valeur by remember { mutableStateOf("") }
        var prixAchat by remember { mutableStateOf("") }
        var prixVente by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { afficherNouveauCalibre = false },
            title = { Text("Nouveau calibre - ${fruitSelectionne?.nom}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = valeur, onValueChange = { valeur = it }, label = { Text("Calibre (ex: Gros, Moyen, 70/80)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = prixAchat, onValueChange = { prixAchat = it }, label = { Text("Prix achat (FCFA)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = prixVente, onValueChange = { prixVente = it }, label = { Text("Prix vente (FCFA)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val pa = prixAchat.toDoubleOrNull() ?: 0.0
                    val pv = prixVente.toDoubleOrNull() ?: 0.0
                    if (valeur.isNotBlank() && pa > 0 && pv > 0) {
                        vm.ajouterCalibre(fruitSelectionne!!.id, valeur, pa, pv) { afficherNouveauCalibre = false }
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = VertFrais)) { Text("Ajouter") }
            },
            dismissButton = { TextButton(onClick = { afficherNouveauCalibre = false }) { Text("Annuler") } }
        )
    }

    // Dialog modifier calibre
    calibreAModifier?.let { calibre ->
        var nouveauPrix by remember { mutableStateOf(calibre.prixVente.toString()) }
        AlertDialog(
            onDismissRequest = { calibreAModifier = null },
            title = { Text("Modifier prix - Calibre ${calibre.valeur}") },
            text = {
                OutlinedTextField(value = nouveauPrix, onValueChange = { nouveauPrix = it }, label = { Text("Nouveau prix de vente (FCFA)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            },
            confirmButton = {
                Button(onClick = {
                    val pv = nouveauPrix.toDoubleOrNull() ?: 0.0
                    if (pv > 0) vm.modifierCalibre(calibre.id, pv) { calibreAModifier = null }
                }, colors = ButtonDefaults.buttonColors(containerColor = VertFrais)) { Text("Modifier") }
            },
            dismissButton = { TextButton(onClick = { calibreAModifier = null }) { Text("Annuler") } }
        )
    }

    Scaffold(
        topBar = { BarreApp("Gestion des Fruits", onRetour = { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { afficherNouveauFruit = true }, containerColor = VertFrais) {
                Icon(Icons.Default.Add, null, tint = Color.White)
            }
        }
    ) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else Column(modifier = Modifier.padding(padding)) {
            vm.succes?.let { Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) { Text(it, modifier = Modifier.padding(12.dp), color = VertFrais, fontWeight = FontWeight.Medium) } }
            vm.erreur?.let { Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) { Text(it, modifier = Modifier.padding(12.dp), color = Color.Red) } }
            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(vm.fruits) { fruit ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(fruit.nom, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                    Text(fruit.uniteMesure, color = Color.Gray, fontSize = 12.sp)
                                }
                                IconButton(onClick = { fruitSelectionne = fruit; afficherNouveauCalibre = true }) {
                                    Icon(Icons.Default.Add, null, tint = VertFrais)
                                }
                            }
                            if (fruit.calibres.isNotEmpty()) {
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                Text("Calibres :", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color.Gray)
                                fruit.calibres.forEach { calibre ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(calibre.valeur, fontSize = 13.sp)
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("${calibre.prixVente.toInt()} FCFA", color = OrangeFruit, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            IconButton(onClick = { calibreAModifier = calibre }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text("Aucun calibre - Appuyez sur + pour en ajouter", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
