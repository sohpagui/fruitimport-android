package com.fruitimport.app.ui.magasinier

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.Fruit
import com.fruitimport.app.data.models.ReceptionRequest
import com.fruitimport.app.ui.components.*
import com.fruitimport.app.utils.SessionManager
import kotlinx.coroutines.launch

class ReceptionViewModel : ViewModel() {
    var fruits by mutableStateOf<List<Fruit>>(emptyList())
    var chargement by mutableStateOf(true)
    var succes by mutableStateOf(false)
    var erreur by mutableStateOf<String?>(null)
    init { charger() }
    fun charger() {
        viewModelScope.launch {
            try { fruits = RetrofitClient.instance.obtenirFruits().body()?.data ?: emptyList() } catch (e: Exception) {}
            chargement = false
        }
    }
    fun receptionner(fruitId: Int, calibreId: Int, origine: String, cartonsNormal: Int, prixNormal: Double) {
        viewModelScope.launch {
            try {
                val data = ReceptionRequest(
                    agenceId = SessionManager.obtenirAgenceId() ?: 1,
                    fruitId = fruitId, calibreId = calibreId, origine = origine,
                    cartonsNormal = cartonsNormal, prixNormal = prixNormal
                )
                val rep = RetrofitClient.instance.receptionnerMarchandise(data)
                if (rep.isSuccessful) succes = true else erreur = rep.body()?.message
            } catch (e: Exception) { erreur = e.message }
        }
    }
}

@Composable
fun EcranReception(navController: NavController, vm: ReceptionViewModel = viewModel()) {
    var fruitSelectionne by remember { mutableStateOf<Fruit?>(null) }
    var calibreIdx by remember { mutableStateOf(0) }
    var origine by remember { mutableStateOf("MAROC") }
    var cartonsNormal by remember { mutableStateOf("") }
    var prixNormal by remember { mutableStateOf("") }

    LaunchedEffect(vm.succes) { if (vm.succes) navController.popBackStack() }

    Scaffold(topBar = { BarreApp("Réceptionner Marchandise", onRetour = { navController.popBackStack() }) }) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            vm.erreur?.let { Text(it, color = Color.Red) }

            Text("Fruit", style = MaterialTheme.typography.labelLarge)
            vm.fruits.forEach { fruit ->
                Row { RadioButton(selected = fruitSelectionne?.id == fruit.id, onClick = { fruitSelectionne = fruit; calibreIdx = 0 }); Text(fruit.nom) }
            }

            fruitSelectionne?.let { fruit ->
                Text("Calibre", style = MaterialTheme.typography.labelLarge)
                fruit.calibres.forEachIndexed { i, cal ->
                    Row { RadioButton(selected = calibreIdx == i, onClick = { calibreIdx = i }); Text(cal.valeur) }
                }
            }

            Text("Origine", style = MaterialTheme.typography.labelLarge)
            listOf("MAROC","AFRIQUE_DU_SUD","ITALIE","AUTRE").forEach { o ->
                Row { RadioButton(selected = origine == o, onClick = { origine = o }); Text(o) }
            }

            OutlinedTextField(value = cartonsNormal, onValueChange = { cartonsNormal = it }, label = { Text("Nombre de cartons") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = prixNormal, onValueChange = { prixNormal = it }, label = { Text("Prix unitaire (FCFA)") }, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = {
                    val fruit = fruitSelectionne ?: return@Button
                    val calibre = fruit.calibres.getOrNull(calibreIdx) ?: return@Button
                    vm.receptionner(fruit.id, calibre.id, origine, cartonsNormal.toIntOrNull() ?: 0, prixNormal.toDoubleOrNull() ?: 0.0)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("Valider la réception") }
        }
    }
}
