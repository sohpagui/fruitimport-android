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
import com.fruitimport.app.data.models.ReceptionRequest
import com.fruitimport.app.ui.components.BarreApp
import com.fruitimport.app.ui.components.ChargementIndicateur
import com.fruitimport.app.ui.theme.VertFrais
import com.fruitimport.app.utils.SessionManager
import kotlinx.coroutines.launch

class ReceptionViewModel : ViewModel() {
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

    fun enregistrerReception(req: ReceptionRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            enCours = true; erreur = null; succes = null
            try {
                val rep = RetrofitClient.instance.receptionMarchandise(req)
                if (rep.isSuccessful && rep.body()?.success == true) {
                    succes = "Reception enregistree avec succes !"
                    onDone()
                } else erreur = rep.body()?.message ?: "Erreur"
            } catch (e: Exception) { erreur = e.message }
            enCours = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranReception(navController: NavController, vm: ReceptionViewModel = viewModel()) {
    val agenceId = SessionManager.obtenirAgenceId() ?: 1
    var fruitExpanded by remember { mutableStateOf(false) }
    var calibreExpanded by remember { mutableStateOf(false) }
    var origineExpanded by remember { mutableStateOf(false) }
    var fruitSelectionne by remember { mutableStateOf<Fruit?>(null) }
    var calibreTexte by remember { mutableStateOf("") }
    var origine by remember { mutableStateOf("MAROC") }
    var cartonsNormal by remember { mutableStateOf("") }
    var cartonsSolde by remember { mutableStateOf("0") }
    var prixNormal by remember { mutableStateOf("") }
    var prixSolde by remember { mutableStateOf("") }

    Scaffold(topBar = { BarreApp("Nouvelle Reception", onRetour = { navController.popBackStack() }) }) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            vm.succes?.let {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                    Text(it, modifier = Modifier.padding(12.dp), color = VertFrais, fontWeight = FontWeight.Medium)
                }
            }
            vm.erreur?.let {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                    Text(it, modifier = Modifier.padding(12.dp), color = Color.Red)
                }
            }

            // Fruit dropdown
            Text("Fruit *", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            ExposedDropdownMenuBox(expanded = fruitExpanded, onExpandedChange = { fruitExpanded = it }) {
                OutlinedTextField(
                    value = fruitSelectionne?.nom ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Selectionner un fruit") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fruitExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = fruitExpanded, onDismissRequest = { fruitExpanded = false }) {
                    vm.fruits.forEach { fruit ->
                        DropdownMenuItem(text = { Text(fruit.nom) }, onClick = { fruitSelectionne = fruit; calibreTexte = ""; fruitExpanded = false })
                    }
                }
            }

            // Calibre - champ libre
            Text("Calibre *", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (fruitSelectionne != null && fruitSelectionne!!.calibres.isNotEmpty()) {
                ExposedDropdownMenuBox(expanded = calibreExpanded, onExpandedChange = { calibreExpanded = it }) {
                    OutlinedTextField(
                        value = calibreTexte,
                        onValueChange = { calibreTexte = it },
                        label = { Text("Calibre") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = calibreExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = calibreExpanded, onDismissRequest = { calibreExpanded = false }) {
                        fruitSelectionne!!.calibres.forEach { calibre ->
                            DropdownMenuItem(text = { Text(calibre.valeur) }, onClick = { calibreTexte = calibre.valeur; calibreExpanded = false })
                        }
                    }
                }
            } else {
                OutlinedTextField(value = calibreTexte, onValueChange = { calibreTexte = it }, label = { Text("Entrer le calibre") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            }

            // Origine dropdown
            Text("Origine *", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            ExposedDropdownMenuBox(expanded = origineExpanded, onExpandedChange = { origineExpanded = it }) {
                OutlinedTextField(
                    value = origine.replace("_", " "),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Origine") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = origineExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = origineExpanded, onDismissRequest = { origineExpanded = false }) {
                    listOf("MAROC", "AFRIQUE_DU_SUD", "ITALIE", "AUTRE").forEach { o ->
                        DropdownMenuItem(text = { Text(o.replace("_", " ")) }, onClick = { origine = o; origineExpanded = false })
                    }
                }
            }

            // Quantites
            Text("Quantites *", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = cartonsNormal, onValueChange = { cartonsNormal = it }, label = { Text("Cartons normaux") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = cartonsSolde, onValueChange = { cartonsSolde = it }, label = { Text("Cartons solde") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
            }

            // Prix
            Text("Prix *", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = prixNormal, onValueChange = { prixNormal = it }, label = { Text("Prix normal (FCFA)") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = prixSolde, onValueChange = { prixSolde = it }, label = { Text("Prix solde (opt)") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
            }

            // Bouton enregistrer
            val fruitId = fruitSelectionne?.id ?: 0
            val calibreId = fruitSelectionne?.calibres?.firstOrNull { it.valeur == calibreTexte }?.id ?: 0
            val peutEnregistrer = fruitSelectionne != null && calibreTexte.isNotBlank() && cartonsNormal.isNotBlank() && prixNormal.isNotBlank()

            Button(
                onClick = {
                    val req = ReceptionRequest(
                        agenceId = agenceId,
                        fruitId = fruitId,
                        calibreId = if (calibreId == 0) 1 else calibreId,
                        origine = origine,
                        cartonsNormal = cartonsNormal.toIntOrNull() ?: 0,
                        cartonsSolde = cartonsSolde.toIntOrNull() ?: 0,
                        prixNormal = prixNormal.toDoubleOrNull() ?: 0.0,
                        prixSolde = prixSolde.toDoubleOrNull()
                    )
                    vm.enregistrerReception(req) {
                        fruitSelectionne = null; calibreTexte = ""
                        cartonsNormal = ""; cartonsSolde = "0"
                        prixNormal = ""; prixSolde = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !vm.enCours && peutEnregistrer,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VertFrais)
            ) {
                if (vm.enCours) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Enregistrer la reception", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
