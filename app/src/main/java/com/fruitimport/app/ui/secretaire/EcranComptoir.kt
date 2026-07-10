package com.fruitimport.app.ui.secretaire
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.fruitimport.app.data.models.*
import com.fruitimport.app.ui.components.BarreApp
import com.fruitimport.app.ui.components.ChargementIndicateur
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertFrais
import com.fruitimport.app.utils.SessionManager
import com.fruitimport.app.utils.toFCFA
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

data class InfosComptoir(
    val id: Int,
    val agenceId: Int,
    val gerantActuel: GerantComptoir? = null,
    val stockComptoir: List<StockComptoirItem> = emptyList()
)
data class GerantComptoir(val id: Int, val nom: String, val telephone: String? = null, val photoUrl: String? = null)
data class StockComptoirItem(val id: Int, val quantite: Int, val prixDetail: Double, val fruit: FruitSimple? = null, val calibre: CalibreSimple? = null)
data class FruitSimple(val id: Int, val nom: String, val imageUrl: String? = null)
data class CalibreSimple(val id: Int, val valeur: String)

class ComptoirViewModel : ViewModel() {
    var comptoir by mutableStateOf<InfosComptoir?>(null)
    var employes by mutableStateOf<List<User>>(emptyList())
    var fruits by mutableStateOf<List<Fruit>>(emptyList())
    var chargement by mutableStateOf(true)
    var succes by mutableStateOf<String?>(null)
    var erreur by mutableStateOf<String?>(null)

    init { charger() }

    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val r1 = RetrofitClient.instance.obtenirComptoir()
                if (r1.isSuccessful) {
                    val json = Gson().toJson(r1.body()?.data)
                    comptoir = Gson().fromJson(json, InfosComptoir::class.java)
                }
                val r2 = RetrofitClient.instance.obtenirEmployes()
                if (r2.isSuccessful) {
                    val json = Gson().toJson((r2.body()?.data as? Map<*,*>)?.get("users"))
                    employes = Gson().fromJson(json, object : TypeToken<List<User>>() {}.type) ?: emptyList()
                }
                val r3 = RetrofitClient.instance.obtenirFruits()
                if (r3.isSuccessful) fruits = r3.body()?.data ?: emptyList()
            } catch (e: Exception) { erreur = e.message }
            chargement = false
        }
    }

    fun changerGerant(gerantId: Int) {
        viewModelScope.launch {
            try {
                val rep = RetrofitClient.instance.changerGerant(mapOf("gerantId" to gerantId))
                if (rep.isSuccessful) { succes = "Gerant mis a jour !"; charger() }
                else erreur = rep.body()?.message ?: "Erreur"
            } catch (e: Exception) { erreur = e.message }
        }
    }

    fun approvisionner(req: ApprovisionnerComptoirRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                val rep = RetrofitClient.instance.approvisionnerComptoir(req)
                if (rep.isSuccessful && rep.body()?.success == true) { succes = "Comptoir approvisionne !"; charger(); onDone() }
                else erreur = rep.errorBody()?.string() ?: "Erreur"
            } catch (e: Exception) { erreur = e.message }
        }
    }

    fun versement(montant: Double, note: String?, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                val rep = RetrofitClient.instance.versementComptoir(VersementComptoirRequest(montant, note))
                if (rep.isSuccessful && rep.body()?.success == true) { succes = "Versement enregistre !"; onDone() }
                else erreur = rep.errorBody()?.string() ?: "Erreur"
            } catch (e: Exception) { erreur = e.message }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranComptoir(navController: NavController, vm: ComptoirViewModel = viewModel()) {
    var afficherGerant by remember { mutableStateOf(false) }
    var afficherAppro by remember { mutableStateOf(false) }
    var afficherVersement by remember { mutableStateOf(false) }
    var fruitSelectionne by remember { mutableStateOf<Fruit?>(null) }
    var calibreTexte by remember { mutableStateOf("") }
    var quantiteAppro by remember { mutableStateOf("") }
    var prixDetail by remember { mutableStateOf("") }
    var montantVersement by remember { mutableStateOf("") }
    var noteVersement by remember { mutableStateOf("") }
    var fruitExpanded by remember { mutableStateOf(false) }
    var gerantExpanded by remember { mutableStateOf(false) }

    // Dialog changer gerant
    if (afficherGerant) {
        AlertDialog(
            onDismissRequest = { afficherGerant = false },
            title = { Text("Changer le gerant du comptoir") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Gerant actuel: ${vm.comptoir?.gerantActuel?.nom ?: "Aucun"}", color = Color.Gray)
                    var nomGerant by remember { mutableStateOf("") }
                    OutlinedTextField(value = nomGerant, onValueChange = { nomGerant = it }, label = { Text("Nom du gerant") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Spacer(Modifier.height(4.dp))
                    Text("Ou choisir dans la liste:", color = Color.Gray, fontSize = 12.sp)
                    vm.employes.filter { it.agenceId == 2 }.forEach { emp ->
                        TextButton(onClick = { vm.changerGerant(emp.id); afficherGerant = false }) {
                            Text("${emp.nom} - ${emp.role}", color = Color(0xFF1565C0))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { afficherGerant = false }) { Text("Fermer") } }
        )
    }

    // Dialog approvisionner
    if (afficherAppro) {
        AlertDialog(
            onDismissRequest = { afficherAppro = false },
            title = { Text("Approvisionner le comptoir") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(expanded = fruitExpanded, onExpandedChange = { fruitExpanded = it }) {
                        OutlinedTextField(value = fruitSelectionne?.nom ?: "", onValueChange = {}, readOnly = true, label = { Text("Fruit") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fruitExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp))
                        ExposedDropdownMenu(expanded = fruitExpanded, onDismissRequest = { fruitExpanded = false }) {
                            vm.fruits.forEach { f -> DropdownMenuItem(text = { Text(f.nom) }, onClick = { fruitSelectionne = f; fruitExpanded = false }) }
                        }
                    }
                    OutlinedTextField(value = calibreTexte, onValueChange = { calibreTexte = it }, label = { Text("Calibre") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = quantiteAppro, onValueChange = { quantiteAppro = it }, label = { Text("Quantite (cartons)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = prixDetail, onValueChange = { prixDetail = it }, label = { Text("Prix detail (FCFA)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val fruit = fruitSelectionne ?: return@Button
                    val calibreId = fruit.calibres.firstOrNull { it.valeur == calibreTexte }?.id ?: 1
                    val qte = quantiteAppro.toIntOrNull() ?: return@Button
                    val prix = prixDetail.toDoubleOrNull() ?: return@Button
                    vm.approvisionner(ApprovisionnerComptoirRequest(fruit.id, calibreId, qte, prix)) { afficherAppro = false; quantiteAppro = ""; prixDetail = ""; calibreTexte = "" }
                }, colors = ButtonDefaults.buttonColors(containerColor = VertFrais)) { Text("Confirmer") }
            },
            dismissButton = { TextButton(onClick = { afficherAppro = false }) { Text("Annuler") } }
        )
    }

    // Dialog versement
    if (afficherVersement) {
        AlertDialog(
            onDismissRequest = { afficherVersement = false },
            title = { Text("Versement du soir") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Gerant: ${vm.comptoir?.gerantActuel?.nom ?: "Non defini"}", fontWeight = FontWeight.Medium)
                    OutlinedTextField(value = montantVersement, onValueChange = { montantVersement = it }, label = { Text("Montant (FCFA)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = noteVersement, onValueChange = { noteVersement = it }, label = { Text("Note (optionnel)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val montant = montantVersement.toDoubleOrNull() ?: return@Button
                    vm.versement(montant, noteVersement.ifBlank { null }) { afficherVersement = false; montantVersement = ""; noteVersement = "" }
                }, colors = ButtonDefaults.buttonColors(containerColor = VertFrais)) { Text("Confirmer") }
            },
            dismissButton = { TextButton(onClick = { afficherVersement = false }) { Text("Annuler") } }
        )
    }

    Scaffold(
        topBar = { BarreApp("Comptoir Yaounde", onRetour = { navController.popBackStack() }) },
        floatingActionButton = { FloatingActionButton(onClick = { vm.charger() }, containerColor = VertFrais) { Icon(Icons.Default.Refresh, null, tint = Color.White) } }
    ) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else LazyColumn(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            item {
                vm.succes?.let { Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) { Text(it, modifier = Modifier.padding(12.dp), color = VertFrais) } }
                vm.erreur?.let { Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) { Text(it, modifier = Modifier.padding(12.dp), color = Color.Red) } }
            }

            // Carte gerant
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Gerant du comptoir", color = Color.Gray, fontSize = 12.sp)
                                Text(vm.comptoir?.gerantActuel?.nom ?: "Non defini", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                vm.comptoir?.gerantActuel?.telephone?.let { Text(it, color = Color.Gray, fontSize = 12.sp) }
                            }
                            Button(onClick = { afficherGerant = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)), shape = RoundedCornerShape(12.dp)) { Text("Changer") }
                        }
                    }
                }
            }

            // Boutons actions
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { afficherAppro = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = VertFrais)) { Text("📥 Approvisionner") }
                    Button(onClick = { afficherVersement = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = OrangeFruit)) { Text("💰 Versement") }
                }
            }

            // Stock comptoir
            item { Text("Stock actuel du comptoir", fontWeight = FontWeight.Bold, fontSize = 15.sp) }

            if (vm.comptoir?.stockComptoir.isNullOrEmpty()) {
                item { Text("Aucun stock pour le moment", color = Color.Gray) }
            }

            items(vm.comptoir?.stockComptoir?.size ?: 0) { i ->
                val stock = vm.comptoir!!.stockComptoir[i]
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(stock.fruit?.nom ?: "", fontWeight = FontWeight.Bold)
                            Text("Calibre: ${stock.calibre?.valeur ?: ""}", color = Color.Gray, fontSize = 12.sp)
                            Text("Prix detail: ${stock.prixDetail.toFCFA()}", color = OrangeFruit, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${stock.quantite}", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = if (stock.quantite <= 2) Color.Red else VertFrais)
                            Text("cartons", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
