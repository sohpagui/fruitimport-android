package com.fruitimport.app.ui.secretaire

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.Commande
import com.fruitimport.app.ui.components.*
import com.fruitimport.app.utils.SessionManager
import com.fruitimport.app.utils.toFCFA
import com.fruitimport.app.utils.traduireStatut
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class CommandesViewModel : ViewModel() {
    var chargement by mutableStateOf(true)
    var commandes by mutableStateOf<List<Commande>>(emptyList())
    var pdfChargement by mutableStateOf(false)
    var pdfErreur by mutableStateOf<String?>(null)

    init { charger() }

    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val agenceId = SessionManager.obtenirAgenceId()
                val rep = RetrofitClient.instance.obtenirCommandes(agenceId = agenceId)
                if (rep.isSuccessful) {
                    val json = Gson().toJson((rep.body()?.data as? Map<*,*>)?.get("commandes"))
                    commandes = Gson().fromJson(json, object : TypeToken<List<Commande>>() {}.type) ?: emptyList()
                }
            } catch (e: Exception) {}
            chargement = false
        }
    }

    fun confirmer(id: Int) {
        viewModelScope.launch {
            try {
                RetrofitClient.instance.changerStatutCommande(id, mapOf("statut" to "CONFIRMEE"))
                charger()
            } catch (e: Exception) {}
        }
    }

    fun telechargerPDF(id: Int, context: android.content.Context) {
        viewModelScope.launch {
            pdfChargement = true
            pdfErreur = null
            try {
                val rep = RetrofitClient.instance.telechargerBonPDF(id)
                if (rep.isSuccessful && rep.body() != null) {
                    val file = java.io.File(context.cacheDir, "BC_${id}.pdf")
                    file.outputStream().use { rep.body()!!.byteStream().copyTo(it) }
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } else {
                    pdfErreur = "Erreur telechargement PDF"
                }
            } catch (e: Exception) { pdfErreur = e.message }
            pdfChargement = false
        }
    }
}

@Composable
fun EcranCommandes(navController: NavController, vm: CommandesViewModel = viewModel()) {
    Scaffold(topBar = { BarreApp("Commandes", onRetour = { navController.popBackStack() }) }) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else LazyColumn(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vm.commandes) { cmd ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(cmd.numero, fontWeight = FontWeight.Bold)
                            BadgeStatut(cmd.statut, cmd.statut.traduireStatut())
                        }
                        Text(cmd.client?.nom ?: "Client", color = Color.Gray)
                        Text(cmd.montantTotal.toFCFA())
                        Text(cmd.modePaiement.traduireStatut(), color = Color.Gray)
                        if (cmd.statut == "EN_ATTENTE") {
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { vm.confirmer(cmd.id) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Confirmer")
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { vm.telechargerPDF(cmd.id, navController.context) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !vm.pdfChargement
                        ) {
                            Text(if (vm.pdfChargement) "Telechargement..." else "Telecharger PDF")
                        }
                        vm.pdfErreur?.let { Text(it, color = Color.Red) }
                    }
                }
            }
        }
    }
}
