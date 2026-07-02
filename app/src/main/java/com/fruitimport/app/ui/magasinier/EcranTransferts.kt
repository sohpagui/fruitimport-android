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
import kotlinx.coroutines.launch

class TransfertsViewModel : ViewModel() {
    var fruits by mutableStateOf<List<Fruit>>(emptyList())
    var succes by mutableStateOf(false)
    var erreur by mutableStateOf<String?>(null)
    init { viewModelScope.launch { try { fruits = RetrofitClient.instance.obtenirFruits().body()?.data ?: emptyList() } catch (e: Exception) {} } }
    fun demanderTransfert(fruitId: Int, calibreId: Int, quantite: Int, note: String) {
        viewModelScope.launch {
            try {
                val data = mapOf("agenceDestinationId" to 2, "fruitId" to fruitId, "calibreId" to calibreId, "quantite" to quantite, "note" to note)
                val rep = RetrofitClient.instance.creerTransfert(data)
                if (rep.isSuccessful) succes = true else erreur = rep.body()?.message
            } catch (e: Exception) { erreur = e.message }
        }
    }
}

@Composable
fun EcranTransferts(navController: NavController, vm: TransfertsViewModel = viewModel()) {
    var fruitSelectionne by remember { mutableStateOf<Fruit?>(null) }
    var calibreIdx by remember { mutableStateOf(0) }
    var quantite by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    LaunchedEffect(vm.succes) { if (vm.succes) navController.popBackStack() }
    Scaffold(topBar = { BarreApp("Transfert vers Yaoundé", onRetour = { navController.popBackStack() }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            vm.erreur?.let { Text(it, color = Color.Red) }
            Text("Fruit", style = MaterialTheme.typography.labelLarge)
            vm.fruits.forEach { f -> Row { RadioButton(selected = fruitSelectionne?.id == f.id, onClick = { fruitSelectionne = f }); Text(f.nom) } }
            fruitSelectionne?.let { f ->
                Text("Calibre", style = MaterialTheme.typography.labelLarge)
                f.calibres.forEachIndexed { i, c -> Row { RadioButton(selected = calibreIdx == i, onClick = { calibreIdx = i }); Text(c.valeur) } }
            }
            OutlinedTextField(value = quantite, onValueChange = { quantite = it }, label = { Text("Quantité") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note (optionnel)") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                val f = fruitSelectionne ?: return@Button
                val c = f.calibres.getOrNull(calibreIdx) ?: return@Button
                vm.demanderTransfert(f.id, c.id, quantite.toIntOrNull() ?: 0, note)
            }, modifier = Modifier.fillMaxWidth()) { Text("Envoyer la demande") }
        }
    }
}
