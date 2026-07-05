package com.fruitimport.app.ui.pdg

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.ui.components.BarreApp
import com.fruitimport.app.ui.components.ChargementIndicateur
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertFrais
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ParametresPDGViewModel : ViewModel() {
    var logoUrl by mutableStateOf<String?>(null)
    var chargement by mutableStateOf(true)
    var uploadEnCours by mutableStateOf(false)
    var succes by mutableStateOf<String?>(null)
    var erreur by mutableStateOf<String?>(null)
    var rapportUrl by mutableStateOf<String?>(null)
    var rapportDate by mutableStateOf<String?>(null)
    var rapportEnCours by mutableStateOf(false)

    init { charger() }

    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val rep = RetrofitClient.instance.obtenirParametres()
                if (rep.isSuccessful) {
                    val data = rep.body()?.data as? Map<*, *>
                    logoUrl = data?.get("logo_url") as? String
                // Charger info rapport
                try {
                    val repRapport = RetrofitClient.instance.obtenirRapport()
                    if (repRapport.isSuccessful) {
                        val dataRapport = repRapport.body()?.data as? Map<*, *>
                        rapportUrl = dataRapport?.get("url") as? String
                        rapportDate = dataRapport?.get("date") as? String
                    }
                } catch (e2: Exception) {}
                }
            } catch (e: Exception) { erreur = e.message }
            chargement = false
        }
    }

    var pdfUri by mutableStateOf<android.net.Uri?>(null)

    fun genererEtOuvrirRapport(context: android.content.Context) {
        viewModelScope.launch {
            rapportEnCours = true; erreur = null; succes = null
            try {
                val rep = RetrofitClient.instance.genererRapport()
                if (rep.isSuccessful && rep.body() != null) {
                    val file = java.io.File(context.cacheDir, "rapport.pdf")
                    withContext(Dispatchers.IO) {
                        file.outputStream().use { rep.body()!!.byteStream().copyTo(it) }
                    }
                    val uri = FileProvider.getUriForFile(context, context.packageName + ".provider", file)
                    pdfUri = uri
                    succes = "Rapport genere !"
                } else erreur = "Erreur generation rapport"
            } catch (e: Exception) { erreur = e.message }
            rapportEnCours = false
        }
    }

    fun uploaderLogo(context: android.content.Context, uri: Uri) {
        viewModelScope.launch {
            uploadEnCours = true; erreur = null; succes = null
            try {
                val stream = context.contentResolver.openInputStream(uri)
                val bytes = stream?.readBytes() ?: return@launch
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("logo", "logo.jpg", requestBody)
                val rep = RetrofitClient.instance.uploaderLogo(part)
                if (rep.isSuccessful && rep.body()?.success == true) {
                    succes = "Logo mis a jour !"
                    val data = rep.body()?.data as? Map<*, *>
                    logoUrl = data?.get("logoUrl") as? String
                } else erreur = rep.body()?.message ?: "Erreur upload"
            } catch (e: Exception) { erreur = e.message }
            uploadEnCours = false
        }
    }
}

@Composable
fun EcranParametresPDG(navController: NavController, vm: ParametresPDGViewModel = viewModel()) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.uploaderLogo(context, it) }
    }

    LaunchedEffect(vm.pdfUri) {
        vm.pdfUri?.let { uri ->
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    Scaffold(topBar = { BarreApp("Parametres", onRetour = { navController.popBackStack() }) }) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            vm.succes?.let {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = VertFrais)
                        Text(it, color = VertFrais)
                    }
                }
            }
            vm.erreur?.let {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Error, null, tint = Color(0xFFC62828))
                        Text(it, color = Color(0xFFC62828))
                    }
                }
            }

            // Section Logo
            Text("Logo de l entreprise", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (vm.logoUrl != null) {
                        AsyncImage(
                            model = vm.logoUrl,
                            contentDescription = "Logo",
                            modifier = Modifier.size(120.dp).clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Text("Logo actuel", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        Box(modifier = Modifier.size(120.dp).clip(RoundedCornerShape(16.dp)).background(VertFrais.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🍎", fontSize = 40.sp)
                                Text("FruitImport", fontWeight = FontWeight.Bold, color = VertFrais, fontSize = 12.sp)
                            }
                        }
                        Text("Aucun logo configure", color = Color.Gray, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { launcher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = !vm.uploadEnCours,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VertFrais)
                    ) {
                        if (vm.uploadEnCours) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Upload, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (vm.logoUrl != null) "Changer le logo" else "Ajouter un logo", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text("Format recommande: PNG ou JPG carre (ex: 200x200px)", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
            // Section Rapport
            Text("Rapport Journalier", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (vm.rapportUrl != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Dernier rapport", fontWeight = FontWeight.Bold)
                                Text(vm.rapportDate ?: "", color = Color.Gray, fontSize = 12.sp)
                            }
                            Icon(Icons.Default.CheckCircle, null, tint = VertFrais, modifier = Modifier.size(24.dp))
                        }
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(vm.rapportUrl))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                        ) {
                            Icon(Icons.Default.Download, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Telecharger le rapport PDF", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Text("Aucun rapport disponible", color = Color.Gray)
                    }
                    OutlinedButton(
                        onClick = { vm.genererEtOuvrirRapport(context) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (vm.rapportEnCours) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Generer le rapport maintenant")
                        }
                    }
                    Text("Le rapport est genere automatiquement chaque soir a 22h.", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
