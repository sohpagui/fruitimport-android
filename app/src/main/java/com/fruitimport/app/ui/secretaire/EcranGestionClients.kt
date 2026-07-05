package com.fruitimport.app.ui.secretaire

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.Client
import com.fruitimport.app.data.models.CreerClientSecretaireRequest
import com.fruitimport.app.ui.components.BarreApp
import com.fruitimport.app.ui.components.BadgeStatut
import com.fruitimport.app.ui.components.ChargementIndicateur
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertFrais
import com.fruitimport.app.utils.SessionManager
import com.fruitimport.app.utils.toFCFA
import com.fruitimport.app.utils.traduireStatut
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class GestionClientsViewModel : ViewModel() {
    var clients by mutableStateOf<List<Client>>(emptyList())
    var chargement by mutableStateOf(true)
    var succes by mutableStateOf<String?>(null)
    var erreur by mutableStateOf<String?>(null)

    init { charger() }

    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val agenceId = SessionManager.obtenirAgenceId()
                val rep = RetrofitClient.instance.obtenirClients(agenceId = agenceId)
                if (rep.isSuccessful) {
                    val json = Gson().toJson((rep.body()?.data as? Map<*,*>)?.get("clients"))
                    clients = Gson().fromJson(json, object : TypeToken<List<Client>>() {}.type) ?: emptyList()
                }
            } catch (e: Exception) { erreur = "Exception: ${e.javaClass.simpleName}: ${e.message}" }
            chargement = false
        }
    }

    fun creerClient(nom: String, telephone: String, type: String, email: String, adresse: String, onDone: () -> Unit) {
        viewModelScope.launch {
            erreur = null
            try {
                val agenceId = SessionManager.obtenirAgenceId() ?: 1
                val rep = RetrofitClient.instance.creerClient(
                    CreerClientSecretaireRequest(
                        nom = nom, telephone = telephone, type = type,
                        agenceId = agenceId,
                        email = if (email.isNotBlank()) email else null,
                        adresse = if (adresse.isNotBlank()) adresse else null
                    )
                )
                if (rep.isSuccessful && rep.body()?.success == true) {
                    succes = "Client cree avec succes !"
                    charger()
                    onDone()
                } else {
                    val msg = rep.body()?.message ?: "Erreur creation client"
                    erreur = if (msg.contains("telephone")) "Ce numero de telephone existe deja" 
                             else if (msg.contains("email")) "Cet email existe deja"
                             else msg
                }
            } catch (e: Exception) { erreur = "Exception: ${e.javaClass.simpleName}: ${e.message}" }
        }
    }
}

@Composable
fun EcranGestionClients(navController: NavController, vm: GestionClientsViewModel = viewModel()) {
    var recherche by remember { mutableStateOf("") }
    var afficherNouveauClient by remember { mutableStateOf(false) }

    // Dialog nouveau client
    if (afficherNouveauClient) {
        var nom by remember { mutableStateOf("") }
        var telephone by remember { mutableStateOf("+237 ") }
        var email by remember { mutableStateOf("") }
        var adresse by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("PARTICULIER") }
        AlertDialog(
            onDismissRequest = { afficherNouveauClient = false },
            title = { Text("Nouveau client", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom complet *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = telephone, onValueChange = { telephone = it }, label = { Text("Telephone *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email (optionnel)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = adresse, onValueChange = { adresse = it }, label = { Text("Adresse (optionnel)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Text("Type de client", fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("PARTICULIER", "SUPERMARCHE").forEach { t ->
                            FilterChip(
                                selected = type == t, onClick = { type = t },
                                label = { Text(if (t == "PARTICULIER") "Particulier" else "Supermarche") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = VertFrais, selectedLabelColor = Color.White)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { if (nom.isNotBlank() && telephone.isNotBlank()) vm.creerClient(nom, telephone, type, email, adresse) { afficherNouveauClient = false } },
                    colors = ButtonDefaults.buttonColors(containerColor = VertFrais)
                ) { Text("Creer") }
            },
            dismissButton = { TextButton(onClick = { afficherNouveauClient = false }) { Text("Annuler") } }
        )
    }

    Scaffold(
        topBar = { BarreApp("Clients", onRetour = { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { afficherNouveauClient = true }, containerColor = VertFrais) {
                Icon(Icons.Default.PersonAdd, null, tint = Color.White)
            }
        }
    ) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else Column(modifier = Modifier.padding(padding)) {
            vm.succes?.let { Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) { Text(it, modifier = Modifier.padding(12.dp), color = VertFrais, fontWeight = FontWeight.Medium) } }
            vm.erreur?.let { Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) { Text(it, modifier = Modifier.padding(12.dp), color = Color.Red) } }
            OutlinedTextField(
                value = recherche, onValueChange = { recherche = it },
                label = { Text("Rechercher un client...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = VertFrais) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true, shape = RoundedCornerShape(12.dp)
            )
            val clientsFiltres = vm.clients.filter {
                recherche.isBlank() || it.nom.contains(recherche, ignoreCase = true) || it.telephone.contains(recherche)
            }
            Text("${clientsFiltres.size} client(s)", modifier = Modifier.padding(horizontal = 16.dp), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(clientsFiltres) { client ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { navController.navigate("fiche_client/${client.id}") }, shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(client.nom, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(client.telephone, color = Color.Gray, fontSize = 12.sp)
                                }
                                BadgeStatut(client.statutCredit, client.statutCredit.traduireStatut())
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (client.type == "SUPERMARCHE") "Supermarche" else "Particulier", color = Color.Gray, fontSize = 12.sp)
                                Text("Dette: ${client.creditUtilise.toFCFA()}", color = if (client.statutCredit == "EN_RETARD") Color.Red else VertFrais, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
