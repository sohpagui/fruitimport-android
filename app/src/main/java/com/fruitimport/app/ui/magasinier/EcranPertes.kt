package com.fruitimport.app.ui.magasinier

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.fruitimport.app.ui.components.*
import com.fruitimport.app.utils.SessionManager
import kotlinx.coroutines.launch

class PertesViewModel : ViewModel() {
    var fruits by mutableStateOf<List<Fruit>>(emptyList())
    var succes by mutableStateOf(false)
    var erreur by mutableStateOf<String?>(null)
    init { viewModelScope.launch { try { fruits = RetrofitClient.instance.obtenirFruits().body()?.data ?: emptyList() } catch (e: Exception) {} } }
    fun declarer(fruitId: Int, calibreId: Int, quantite: Int, raison: String) {
        viewModelScope.launch {
            try {
                val data = mapOf("agenceId" to (SessionManager.obtenirAgenceId() ?: 1),
                    "fruitId" to fruitId, "calibreId" to calibreId, "origine" to "MAROC",
                    "categorie" to "NORMAL", "quantite" to quantite, "raison" to raison)
                val rep = RetrofitClient.instance.declarerPerte(data)
                if (rep.isSuccessful) succes = true else erreur = rep.body()?.message
            } catch (e: Exception) { erreur = e.message }
        }
    }
}

@Composable
fun EcranPertes(navController: NavController, vm: PertesViewModel = viewModel()) {
    var fruitSelectionne by remember { mutableStateOf<Fruit?>(null) }
    var calibreIdx by remember { mutableStateOf(0) }
    var quantite by remember { mutableStateOf("") }
    var raison by remember { mutableStateOf("JAUNISSEMENT") }
    LaunchedEffect(vm.succes) { if (vm.succes) navController.popBackStack() }
    Scaffold(topBar = { BarreApp("Déclarer une Perte", onRetour = { navController.popBackStack() }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            vm.erreur?.let { Text(it, color = Color.Red) }
            Text("Fruit", style = MaterialTheme.typography.labelLarge)
            vm.fruits.forEach { f -> Row { RadioButton(selected = fruitSelectionne?.id == f.id, onClick = { fruitSelectionne = f }); Text(f.nom) } }
            fruitSelectionne?.let { f ->
                Text("Calibre", style = MaterialTheme.typography.labelLarge)
                f.calibres.forEachIndexed { i, c -> Row { RadioButton(selected = calibreIdx == i, onClick = { calibreIdx = i }); Text(c.valeur) } }
            }
            OutlinedTextField(value = quantite, onValueChange = { quantite = it }, label = { Text("Quantité perdue") }, modifier = Modifier.fillMaxWidth())
            Text("Raison", style = MaterialTheme.typography.labelLarge)
            listOf("JAUNISSEMENT","POURRISSEMENT","CHOC","AUTRE").forEach { r ->
                Row { RadioButton(selected = raison == r, onClick = { raison = r }); Text(r) }
            }
            Button(onClick = {
                val f = fruitSelectionne ?: return@Button
                val c = f.calibres.getOrNull(calibreIdx) ?: return@Button
                vm.declarer(f.id, c.id, quantite.toIntOrNull() ?: 0, raison)
            }, modifier = Modifier.fillMaxWidth()) { Text("Déclarer la perte") }
        }
    }
}
