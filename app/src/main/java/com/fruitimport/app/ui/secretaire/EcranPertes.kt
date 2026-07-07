package com.fruitimport.app.ui.secretaire

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.fruitimport.app.data.models.Fruit
import com.fruitimport.app.data.models.PerteRequest
import com.fruitimport.app.ui.components.BarreApp
import com.fruitimport.app.ui.components.ChargementIndicateur
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertFrais
import com.fruitimport.app.utils.SessionManager
import kotlinx.coroutines.launch

class PertesViewModel : ViewModel() {
    var fruits by mutableStateOf<List<Fruit>>(emptyList())
    var chargement by mutableStateOf(true)
    var enCours by mutableStateOf(false)
    var succes by mutableStateOf<String?>(null)
    var erreur by mutableStateOf<String?>(null)

    init { chargerFruits() }

    fun chargerFruits() {
        viewModelScope.launch {
            try {
                val rep = RetrofitClient.instance.obtenirFruits()
                if (rep.isSuccessful) fruits = rep.body()?.data ?: emptyList()
            } catch (e: Exception) {}
            chargement = false
        }
    }

    fun declarerPerte(req: PerteRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            enCours = true; erreur = null; succes = null
            try {
                val rep = RetrofitClient.instance.declarerPerte(req)
                if (rep.isSuccessful && rep.body()?.success == true) {
                    succes = "Perte declaree avec succes !"
                    onDone()
                } else erreur = rep.body()?.message ?: "Erreur"
            } catch (e: Exception) { erreur = e.message }
            enCours = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranPertes(navController: NavController, vm: PertesViewModel = viewModel()) {
    val agenceId = SessionManager.obtenirAgenceId() ?: 1
    var fruitExpanded by remember { mutableStateOf(false) }
    var calibreExpanded by remember { mutableStateOf(false) }
    var origineExpanded by remember { mutableStateOf(false) }
    var categorieExpanded by remember { mutableStateOf(false) }
    var raisonExpanded by remember { mutableStateOf(false) }
    var fruitSelectionne by remember { mutableStateOf<Fruit?>(null) }
    var calibreTexte by remember { mutableStateOf("") }
    var origine by remember { mutableStateOf("MAROC") }
    var categorie by remember { mutableStateOf("NORMAL") }
    var raison by remember { mutableStateOf("JAUNISSEMENT") }
    var quantite by remember { mutableStateOf("") }

    Scaffold(topBar = { BarreApp("Declarer une Perte", onRetour = { navController.popBackStack() }) }) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            vm.succes?.let { Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) { Text(it, modifier = Modifier.padding(12.dp), color = VertFrais, fontWeight = FontWeight.Medium) } }
            vm.erreur?.let { Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) { Text(it, modifier = Modifier.padding(12.dp), color = Color.Red) } }

            // Fruit
            Text("Fruit *", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            ExposedDropdownMenuBox(expanded = fruitExpanded, onExpandedChange = { fruitExpanded = it }) {
                OutlinedTextField(value = fruitSelectionne?.nom ?: "", onValueChange = {}, readOnly = true, label = { Text("Selectionner un fruit") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fruitExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp))
                ExposedDropdownMenu(expanded = fruitExpanded, onDismissRequest = { fruitExpanded = false }) {
                    vm.fruits.forEach { fruit -> DropdownMenuItem(text = { Text(fruit.nom) }, onClick = { fruitSelectionne = fruit; calibreTexte = ""; fruitExpanded = false }) }
                }
            }

            // Calibre
            Text("Calibre *", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (fruitSelectionne != null && fruitSelectionne!!.calibres.isNotEmpty()) {
                ExposedDropdownMenuBox(expanded = calibreExpanded, onExpandedChange = { calibreExpanded = it }) {
                    OutlinedTextField(value = calibreTexte, onValueChange = { calibreTexte = it }, label = { Text("Calibre") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = calibreExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp))
                    ExposedDropdownMenu(expanded = calibreExpanded, onDismissRequest = { calibreExpanded = false }) {
                        fruitSelectionne!!.calibres.forEach { c -> DropdownMenuItem(text = { Text(c.valeur) }, onClick = { calibreTexte = c.valeur; calibreExpanded = false }) }
                    }
                }
            } else {
                OutlinedTextField(value = calibreTexte, onValueChange = { calibreTexte = it }, label = { Text("Entrer le calibre") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            }

            // Origine
            Text("Origine *", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            ExposedDropdownMenuBox(expanded = origineExpanded, onExpandedChange = { origineExpanded = it }) {
                OutlinedTextField(value = origine.replace("_", " "), onValueChange = {}, readOnly = true, label = { Text("Origine") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = origineExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp))
                ExposedDropdownMenu(expanded = origineExpanded, onDismissRequest = { origineExpanded = false }) {
                    listOf("MAROC", "AFRIQUE_DU_SUD", "ITALIE", "AUTRE").forEach { o -> DropdownMenuItem(text = { Text(o.replace("_", " ")) }, onClick = { origine = o; origineExpanded = false }) }
                }
            }

            // Categorie et Raison
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Categorie *", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    ExposedDropdownMenuBox(expanded = categorieExpanded, onExpandedChange = { categorieExpanded = it }) {
                        OutlinedTextField(value = categorie, onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categorieExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp))
                        ExposedDropdownMenu(expanded = categorieExpanded, onDismissRequest = { categorieExpanded = false }) {
                            listOf("NORMAL", "SOLDE").forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { categorie = c; categorieExpanded = false }) }
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Raison *", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    ExposedDropdownMenuBox(expanded = raisonExpanded, onExpandedChange = { raisonExpanded = it }) {
                        OutlinedTextField(value = raison, onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = raisonExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp))
                        ExposedDropdownMenu(expanded = raisonExpanded, onDismissRequest = { raisonExpanded = false }) {
                            listOf("JAUNISSEMENT", "POURRISSEMENT", "CHOC", "AUTRE").forEach { r -> DropdownMenuItem(text = { Text(r) }, onClick = { raison = r; raisonExpanded = false }) }
                        }
                    }
                }
            }

            // Quantite
            Text("Quantite *", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            OutlinedTextField(value = quantite, onValueChange = { quantite = it }, label = { Text("Nombre de cartons perdus") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

            // Bouton
            val calibreId = fruitSelectionne?.calibres?.firstOrNull { it.valeur == calibreTexte }?.id ?: 1
            Button(
                onClick = {
                    val req = PerteRequest(
                        agenceId = agenceId,
                        fruitId = fruitSelectionne?.id ?: return@Button,
                        calibreId = calibreId,
                        origine = origine,
                        categorie = categorie,
                        quantite = quantite.toIntOrNull() ?: return@Button,
                        raison = raison
                    )
                    vm.declarerPerte(req) { fruitSelectionne = null; calibreTexte = ""; quantite = "" }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !vm.enCours && fruitSelectionne != null && calibreTexte.isNotBlank() && quantite.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
            ) {
                if (vm.enCours) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Declarer la perte", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
