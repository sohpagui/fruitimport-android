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
import com.fruitimport.app.data.models.Client
import com.fruitimport.app.data.models.Commande
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

class FicheClientViewModel(private val clientId: Int) : ViewModel() {
    var client by mutableStateOf<Client?>(null)
    var commandes by mutableStateOf<List<Commande>>(emptyList())
    var chargement by mutableStateOf(true)
    var succes by mutableStateOf<String?>(null)
    var erreur by mutableStateOf<String?>(null)

    init { charger() }

    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val repClient = RetrofitClient.instance.obtenirDetailClient(clientId)
                if (repClient.isSuccessful) {
                    val json = Gson().toJson(repClient.body()?.data)
                    client = Gson().fromJson(json, Client::class.java)
                }
                val repCmds = RetrofitClient.instance.obtenirCommandes(clientId = clientId)
                if (repCmds.isSuccessful) {
                    val json = Gson().toJson((repCmds.body()?.data as? Map<*,*>)?.get("commandes"))
                    commandes = Gson().fromJson(json, object : TypeToken<List<Commande>>() {}.type) ?: emptyList()
                }
            } catch (e: Exception) { erreur = e.message }
            chargement = false
        }
    }

    fun enregistrerVersement(montant: Double, onDone: () -> Unit) {
        viewModelScope.launch {
            erreur = null
            try {
                val rep = RetrofitClient.instance.ajouterVersement(clientId, mapOf("montant" to montant))
                if (rep.isSuccessful && rep.body()?.success == true) {
                    succes = "Versement de ${montant.toFCFA()} enregistre !"
                    charger()
                    onDone()
                } else erreur = rep.body()?.message ?: "Erreur"
            } catch (e: Exception) { erreur = e.message }
        }
    }
}

@Composable
fun EcranFicheClient(navController: NavController, clientId: Int) {
    val vm = viewModel<FicheClientViewModel>(key = clientId.toString(), factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = FicheClientViewModel(clientId) as T
    })
    var afficherVersement by remember { mutableStateOf(false) }
    var montantVersement by remember { mutableStateOf("") }

    if (afficherVersement) {
        AlertDialog(
            onDismissRequest = { afficherVersement = false },
            title = { Text("Enregistrer un versement") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    vm.client?.let { Text("Client: ${it.nom}", color = Color.Gray) }
                    Text("Dette actuelle: ${vm.client?.creditUtilise?.toFCFA() ?: ""}", color = Color.Red)
                    OutlinedTextField(value = montantVersement, onValueChange = { montantVersement = it }, label = { Text("Montant verse (FCFA)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val montant = montantVersement.toDoubleOrNull() ?: 0.0
                        if (montant > 0) vm.enregistrerVersement(montant) { afficherVersement = false; montantVersement = "" }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VertFrais)
                ) { Text("Confirmer") }
            },
            dismissButton = { TextButton(onClick = { afficherVersement = false }) { Text("Annuler") } }
        )
    }

    Scaffold(topBar = { BarreApp("Fiche Client", onRetour = { navController.popBackStack() }) }) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else vm.client?.let { client ->
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    vm.succes?.let { Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) { Text(it, modifier = Modifier.padding(12.dp), color = VertFrais) } }
                    vm.erreur?.let { Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) { Text(it, modifier = Modifier.padding(12.dp), color = Color.Red) } }
                }
                // Infos client
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(client.nom, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                    Text(client.telephone, color = Color.Gray)
                                    client.email?.let { Text(it, color = Color.Gray, fontSize = 12.sp) }
                                }
                                BadgeStatut(client.statutCredit, client.statutCredit.traduireStatut())
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Limite credit", color = Color.Gray, fontSize = 13.sp)
                                    Text(client.limiteCredit.toFCFA(), fontWeight = FontWeight.Bold, color = VertFrais, fontSize = 13.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Dette actuelle", color = Color.Gray, fontSize = 13.sp)
                                    Text(client.creditUtilise.toFCFA(), fontWeight = FontWeight.Bold, color = if (client.statutCredit == "EN_RETARD") Color.Red else OrangeFruit, fontSize = 13.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Disponible", color = Color.Gray, fontSize = 13.sp)
                                    Text((client.limiteCredit - client.creditUtilise).toFCFA(), fontWeight = FontWeight.Bold, color = VertFrais, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
                // Bouton versement
                item {
                    Button(
                        onClick = { afficherVersement = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VertFrais)
                    ) {
                        Icon(Icons.Default.Payment, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Enregistrer un versement", fontWeight = FontWeight.SemiBold)
                    }
                }
                // Historique commandes
                item { Text("Historique des commandes (${vm.commandes.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                items(vm.commandes) { cmd ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(cmd.numero, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(cmd.date?.take(10) ?: "", color = Color.Gray, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(cmd.montantTotal.toFCFA(), fontWeight = FontWeight.Bold, color = OrangeFruit)
                                BadgeStatut(cmd.statut, cmd.statut.traduireStatut())
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
