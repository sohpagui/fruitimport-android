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
import com.fruitimport.app.data.models.TransfertRequest
import com.fruitimport.app.ui.components.BarreApp
import com.fruitimport.app.ui.components.ChargementIndicateur
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertFrais
import com.fruitimport.app.utils.SessionManager
import kotlinx.coroutines.launch

class TransfertViewModel : ViewModel() {
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

    fun demanderTransfert(req: TransfertRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            enCours = true; erreur = null; succes = null
            try {
                val rep = RetrofitClient.instance.demanderTransfert(req)
                if (rep.isSuccessful && rep.body()?.success == true) {
                    succes = "Transfert demande avec succes ! En attente approbation PDG."
                    onDone()
                } else {
                    val msg = rep.body()?.message ?: rep.errorBody()?.string() ?: "Erreur inconnue"
                    erreur = if (msg.contains("insuffisant")) "Stock insuffisant pour ce transfert"
                             else if (msg.contains("Stock")) msg
                             else msg
                }
            } catch (e: Exception) { erreur = e.message }
            enCours = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranTransfert(navController: NavController, vm: TransfertViewModel = viewModel()) {
    val agenceId = SessionManager.obtenirAgenceId() ?: 1
    val agenceDestination = if (agenceId == 1) 2 else 1
    val nomDestination = if (agenceId == 1) "Yaounde" else "Douala"
    var fruitExpanded by remember { mutableStateOf(false) }
    var calibreExpanded by remember { mutableStateOf(false) }
    var fruitSelectionne by remember { mutableStateOf<Fruit?>(null) }
    var calibreTexte by remember { mutableStateOf("") }
    var fruitTexte by remember { mutableStateOf("") }
    var quantite by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Scaffold(topBar = { BarreApp("Demander un Transfert", onRetour = { navController.popBackStack() }) }) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            vm.succes?.let { Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) { Text(it, modifier = Modifier.padding(12.dp), color = VertFrais, fontWeight = FontWeight.Medium) } }
            vm.erreur?.let { Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) { Text(it, modifier = Modifier.padding(12.dp), color = Color.Red) } }

            // Info destination
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔄", fontSize = 20.sp)
                    Text("Transfert vers agence $nomDestination", fontWeight = FontWeight.Medium, color = Color(0xFF1565C0))
                }
            }

            // Fruit
            Text("Fruit *", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            ExposedDropdownMenuBox(expanded = fruitExpanded, onExpandedChange = { fruitExpanded = it }) {
                OutlinedTextField(
                    value = fruitTexte,
                    onValueChange = { fruitTexte = it; fruitExpanded = true; fruitSelectionne = vm.fruits.firstOrNull { f -> f.nom.equals(it, ignoreCase = true) } },
                    label = { Text("Fruit") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fruitExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                val fruitsFiltres = vm.fruits.filter { it.nom.contains(fruitTexte, ignoreCase = true) }
                if (fruitsFiltres.isNotEmpty()) {
                    ExposedDropdownMenu(expanded = fruitExpanded, onDismissRequest = { fruitExpanded = false }) {
                        fruitsFiltres.forEach { fruit -> DropdownMenuItem(text = { Text(fruit.nom) }, onClick = { fruitSelectionne = fruit; fruitTexte = fruit.nom; calibreTexte = ""; fruitExpanded = false }) }
                    }
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

            // Quantite
            Text("Quantite *", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            OutlinedTextField(value = quantite, onValueChange = { quantite = it }, label = { Text("Nombre de cartons") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

            // Note
            Text("Note (optionnel)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Ajouter une note...") }, modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(12.dp), maxLines = 4)

            // Bouton
            val calibreId = fruitSelectionne?.calibres?.firstOrNull { it.valeur == calibreTexte }?.id ?: 1
            Button(
                onClick = {
                    val req = TransfertRequest(
                        agenceDestinationId = agenceDestination,
                        fruitId = fruitSelectionne?.id ?: return@Button,
                        calibreId = calibreId,
                        quantite = quantite.toIntOrNull() ?: return@Button,
                        note = if (note.isBlank()) null else note
                    )
                    vm.demanderTransfert(req) { fruitSelectionne = null; calibreTexte = ""; quantite = ""; note = "" }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !vm.enCours && fruitSelectionne != null && calibreTexte.isNotBlank() && quantite.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) {
                if (vm.enCours) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Demander le transfert", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
