package com.fruitimport.app.ui.pdg

import android.net.Uri
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

    init { charger() }

    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val rep = RetrofitClient.instance.obtenirParametres()
                if (rep.isSuccessful) {
                    val data = rep.body()?.data as? Map<*, *>
                    logoUrl = data?.get("logo_url") as? String
                }
            } catch (e: Exception) { erreur = e.message }
            chargement = false
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
            Spacer(Modifier.height(16.dp))
        }
    }
}
