package com.fruitimport.app.ui.secretaire

import android.content.Intent
import androidx.compose.foundation.layout.*
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
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.Commande
import com.fruitimport.app.ui.components.BarreApp
import com.fruitimport.app.ui.components.BadgeStatut
import com.fruitimport.app.ui.components.ChargementIndicateur
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertFrais
import com.fruitimport.app.utils.toFCFA
import com.fruitimport.app.utils.traduireStatut
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailCommandeViewModel(private val commandeId: Int) : ViewModel() {
    var commande by mutableStateOf<Commande?>(null)
    var chargement by mutableStateOf(true)
    var erreur by mutableStateOf<String?>(null)
    var succes by mutableStateOf<String?>(null)
    var pdfChargement by mutableStateOf(false)

    init { charger() }

    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val rep = RetrofitClient.instance.obtenirDetailCommande(commandeId)
                if (rep.isSuccessful) {
                    val json = Gson().toJson(rep.body()?.data)
                    commande = Gson().fromJson(json, Commande::class.java)
                }
            } catch (e: Exception) { erreur = e.message }
            chargement = false
        }
    }

    fun changerStatut(statut: String) {
        viewModelScope.launch {
            try {
                val rep = RetrofitClient.instance.changerStatutCommande(commandeId, mapOf("statut" to statut))
                if (rep.isSuccessful) { succes = "Statut mis a jour !"; charger() }
                else erreur = rep.body()?.message ?: "Erreur"
            } catch (e: Exception) { erreur = e.message }
        }
    }

    fun telechargerPDF(context: android.content.Context) {
        viewModelScope.launch {
            pdfChargement = true
            try {
                val rep = RetrofitClient.instance.telechargerBonPDF(commandeId)
                if (rep.isSuccessful && rep.body() != null) {
                    val file = java.io.File(context.cacheDir, "BC_${commandeId}.pdf")
                    withContext(Dispatchers.IO) { file.outputStream().use { rep.body()!!.byteStream().copyTo(it) } }
                    val uri = FileProvider.getUriForFile(context, context.packageName + ".provider", file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            } catch (e: Exception) { erreur = e.message }
            pdfChargement = false
        }
    }
}

@Composable
fun EcranDetailCommande(navController: NavController, commandeId: Int) {
    val vm = viewModel<DetailCommandeViewModel>(key = commandeId.toString(), factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = DetailCommandeViewModel(commandeId) as T
    })
    val context = androidx.compose.ui.platform.LocalContext.current
    var afficherChangerStatut by remember { mutableStateOf(false) }

    if (afficherChangerStatut) {
        AlertDialog(
            onDismissRequest = { afficherChangerStatut = false },
            title = { Text("Changer le statut") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val statutsDisponibles = when (vm.commande?.statut) {
                            "EN_ATTENTE" -> listOf("CONFIRMEE", "ANNULEE")
                            "CONFIRMEE" -> listOf("PREPAREE", "ANNULEE")
                            "PREPAREE" -> listOf("EN_LIVRAISON", "ANNULEE")
                            else -> emptyList()
                        }
                    statutsDisponibles.forEach { s ->
                        OutlinedButton(
                            onClick = { vm.changerStatut(s); afficherChangerStatut = false },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(s.traduireStatut()) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { afficherChangerStatut = false }) { Text("Annuler") } }
        )
    }

    Scaffold(topBar = { BarreApp("Detail Commande", onRetour = { navController.popBackStack() }) }) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else vm.commande?.let { cmd ->
            Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                vm.succes?.let { Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) { Text(it, modifier = Modifier.padding(12.dp), color = VertFrais) } }
                vm.erreur?.let { Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) { Text(it, modifier = Modifier.padding(12.dp), color = Color.Red) } }

                // En-tete commande
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(cmd.numero, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            BadgeStatut(cmd.statut, cmd.statut.traduireStatut())
                        }
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Client", color = Color.Gray, fontSize = 13.sp)
                            Text(cmd.client?.nom ?: "", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Date", color = Color.Gray, fontSize = 13.sp)
                            Text(cmd.date?.take(10) ?: "", fontSize = 13.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Paiement", color = Color.Gray, fontSize = 13.sp)
                            Text(cmd.modePaiement.traduireStatut(), fontSize = 13.sp)
                        }
                        cmd.adresseLivraison?.let {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Adresse", color = Color.Gray, fontSize = 13.sp)
                                Text(it, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Lignes de commande
                if (cmd.lignes != null && cmd.lignes.isNotEmpty()) {
                    Text("Articles commandes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    cmd.lignes.forEach { ligne ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(ligne.fruit?.nom ?: "", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Calibre: ${ligne.calibre?.valeur ?: ""}", color = Color.Gray, fontSize = 12.sp)
                                    Text("${ligne.quantite} cartons x ${ligne.prixUnitaire.toFCFA()}", fontSize = 12.sp)
                                }
                                Text(ligne.sousTotal.toFCFA(), fontWeight = FontWeight.ExtraBold, color = OrangeFruit)
                            }
                        }
                    }
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = VertFrais.copy(alpha = 0.1f))) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TOTAL", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text(cmd.montantTotal.toFCFA(), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = VertFrais)
                        }
                    }
                }

                // Actions
                Button(onClick = { afficherChangerStatut = true }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = OrangeFruit)) {
                    Icon(Icons.Default.Edit, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Changer le statut", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = { vm.telechargerPDF(context) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !vm.pdfChargement,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (vm.pdfChargement) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else { Icon(Icons.Default.PictureAsPdf, null, tint = VertFrais); Spacer(Modifier.width(8.dp)); Text("Telecharger PDF", color = VertFrais, fontWeight = FontWeight.SemiBold) }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
