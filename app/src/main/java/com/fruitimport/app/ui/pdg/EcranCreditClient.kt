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
import com.fruitimport.app.data.models.ModifierLimiteCreditRequest
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

class CreditClientViewModel : ViewModel() {
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

    fun modifierLimite(clientId: Int, limite: Double, onDone: () -> Unit) {
        viewModelScope.launch {
            erreur = null; succes = null
            try {
                val rep = RetrofitClient.instance.modifierLimiteCredit(clientId, ModifierLimiteCreditRequest(limiteCredit = limite))
                if (rep.isSuccessful && rep.body()?.success == true) {
                    succes = "Limite modifiee avec succes !"
                    charger()
                    onDone()
                } else erreur = rep.body()?.message ?: "Erreur"
            } catch (e: Exception) { erreur = e.message }
        }
    }
}

@Composable
fun EcranCreditClient(navController: NavController, vm: CreditClientViewModel = viewModel()) {
    var clientSelectionne by remember { mutableStateOf<Client?>(null) }
    var nouvelleLimite by remember { mutableStateOf("") }
    var recherche by remember { mutableStateOf("") }

    clientSelectionne?.let { client ->
        AlertDialog(
            onDismissRequest = { clientSelectionne = null },
            title = { Text("Modifier limite credit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Client: ${client.nom}", fontWeight = FontWeight.Bold)
                    Text("Limite actuelle: ${client.limiteCredit.toFCFA()}", color = Color.Gray)
                    Text("Dette actuelle: ${client.creditUtilise.toFCFA()}", color = if (client.statutCredit == "EN_RETARD") Color.Red else OrangeFruit)
                    OutlinedTextField(
                        value = nouvelleLimite,
                        onValueChange = { nouvelleLimite = it },
                        label = { Text("Nouvelle limite (FCFA)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limite = nouvelleLimite.toDoubleOrNull() ?: return@Button
                        vm.modifierLimite(client.id, limite) { clientSelectionne = null; nouvelleLimite = "" }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VertFrais)
                ) { Text("Confirmer") }
            },
            dismissButton = { TextButton(onClick = { clientSelectionne = null }) { Text("Annuler") } }
        )
    }

    Scaffold(topBar = { BarreApp("Limites de Credit", onRetour = { navController.popBackStack() }) }) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else Column(modifier = Modifier.padding(padding)) {
            vm.succes?.let { Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) { Text(it, modifier = Modifier.padding(12.dp), color = VertFrais) } }
            vm.erreur?.let { Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) { Text(it, modifier = Modifier.padding(12.dp), color = Color.Red) } }
            OutlinedTextField(
                value = recherche, onValueChange = { recherche = it },
                label = { Text("Rechercher un client...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = VertFrais) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true, shape = RoundedCornerShape(12.dp)
            )
            val clientsFiltres = vm.clients.filter {
                recherche.isBlank() || it.nom.contains(recherche, ignoreCase = true)
            }
            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(clientsFiltres) { client ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(client.nom, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(client.telephone, color = Color.Gray, fontSize = 12.sp)
                                }
                                BadgeStatut(client.statutCredit, client.statutCredit.traduireStatut())
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Limite", color = Color.Gray, fontSize = 11.sp)
                                    Text(client.limiteCredit.toFCFA(), fontWeight = FontWeight.Bold, color = VertFrais, fontSize = 13.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Dette", color = Color.Gray, fontSize = 11.sp)
                                    Text(client.creditUtilise.toFCFA(), fontWeight = FontWeight.Bold, color = if (client.statutCredit == "EN_RETARD") Color.Red else OrangeFruit, fontSize = 13.sp)
                                }
                            }
                            OutlinedButton(
                                onClick = { clientSelectionne = client; nouvelleLimite = client.limiteCredit.toInt().toString() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Modifier la limite")
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
