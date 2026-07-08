package com.fruitimport.app.ui.pdg
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
import com.fruitimport.app.data.models.Client
import com.fruitimport.app.data.models.FixerEcheanceRequest
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

class EcheanceClientViewModel : ViewModel() {
    var clients by mutableStateOf<List<Client>>(emptyList())
    var chargement by mutableStateOf(true)
    var succes by mutableStateOf<String?>(null)
    var erreur by mutableStateOf<String?>(null)
    init { charger() }
    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val rep = RetrofitClient.instance.obtenirClients()
                if (rep.isSuccessful) {
                    val json = Gson().toJson((rep.body()?.data as? Map<*,*>)?.get("clients"))
                    clients = Gson().fromJson(json, object : TypeToken<List<Client>>() {}.type) ?: emptyList()
                }
            } catch (e: Exception) { erreur = e.message }
            chargement = false
        }
    }
    fun fixerEcheance(clientId: Int, date: String, onDone: () -> Unit) {
        viewModelScope.launch {
            erreur = null; succes = null
            try {
                val rep = RetrofitClient.instance.fixerEcheance(clientId, FixerEcheanceRequest(dateEcheance = date))
                if (rep.isSuccessful && rep.body()?.success == true) { succes = "Echeance fixee !"; charger(); onDone() }
                else erreur = rep.body()?.message ?: "Erreur"
            } catch (e: Exception) { erreur = e.message }
        }
    }
}

@Composable
fun EcranEcheanceClient(navController: NavController, vm: EcheanceClientViewModel = viewModel()) {
    var clientSelectionne by remember { mutableStateOf<Client?>(null) }
    var nouvelleDate by remember { mutableStateOf("") }
    clientSelectionne?.let { client ->
        AlertDialog(
            onDismissRequest = { clientSelectionne = null },
            title = { Text("Fixer echeance") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(client.nom, fontWeight = FontWeight.Bold)
                    client.dateEcheance?.let { Text("Actuelle: ${it.take(10)}", color = Color.Gray, fontSize = 12.sp) }
                    Text("Dette: ${client.creditUtilise.toFCFA()}", color = OrangeFruit)
                    OutlinedTextField(value = nouvelleDate, onValueChange = { nouvelleDate = it }, label = { Text("Date (YYYY-MM-DD)") }, placeholder = { Text("2026-12-31") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = { Button(onClick = { if (nouvelleDate.isNotBlank()) vm.fixerEcheance(client.id, nouvelleDate) { clientSelectionne = null; nouvelleDate = "" } }, colors = ButtonDefaults.buttonColors(containerColor = VertFrais)) { Text("Confirmer") } },
            dismissButton = { TextButton(onClick = { clientSelectionne = null }) { Text("Annuler") } }
        )
    }
    Scaffold(topBar = { BarreApp("Echeances Clients", onRetour = { navController.popBackStack() }) }) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else Column(modifier = Modifier.padding(padding)) {
            vm.succes?.let { Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) { Text(it, modifier = Modifier.padding(12.dp), color = VertFrais) } }
            vm.erreur?.let { Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) { Text(it, modifier = Modifier.padding(12.dp), color = Color.Red) } }
            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(vm.clients) { client ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(client.nom, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                BadgeStatut(client.statutCredit, client.statutCredit.traduireStatut())
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Dette: ${client.creditUtilise.toFCFA()}", color = OrangeFruit, fontSize = 12.sp)
                                Text(if (client.dateEcheance != null) "Echeance: ${client.dateEcheance!!.take(10)}" else "Aucune echeance", color = if (client.dateEcheance != null) VertFrais else Color.Gray, fontSize = 12.sp)
                            }
                            OutlinedButton(onClick = { clientSelectionne = client; nouvelleDate = client.dateEcheance?.take(10) ?: "" }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Fixer echeance")
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
