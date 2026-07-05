package com.fruitimport.app.ui.secretaire

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.Fruit
import com.fruitimport.app.ui.components.BarreApp
import com.fruitimport.app.ui.components.ChargementIndicateur
import com.fruitimport.app.ui.components.PhotoViewer
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertFrais
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ArrivagesViewModel : ViewModel() {
    var fruits by mutableStateOf<List<Fruit>>(emptyList())
    var chargement by mutableStateOf(true)
    var uploadEnCours by mutableStateOf<Int?>(null)
    var succes by mutableStateOf<String?>(null)
    var erreur by mutableStateOf<String?>(null)

    init { charger() }

    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val rep = RetrofitClient.instance.obtenirFruits()
                if (rep.isSuccessful) fruits = rep.body()?.data ?: emptyList()
            } catch (e: Exception) { erreur = e.message }
            chargement = false
        }
    }

    fun uploaderPhoto(context: android.content.Context, fruitId: Int, uri: Uri) {
        viewModelScope.launch {
            uploadEnCours = fruitId
            erreur = null
            try {
                val stream = context.contentResolver.openInputStream(uri)
                val bytes = stream?.readBytes() ?: return@launch
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("image", "fruit.jpg", requestBody)
                val rep = RetrofitClient.instance.uploadImageFruit(fruitId, part)
                if (rep.isSuccessful && rep.body()?.success == true) {
                    succes = "Photo mise a jour !"
                    charger()
                } else erreur = rep.body()?.message ?: "Erreur upload"
            } catch (e: Exception) { erreur = e.message }
            uploadEnCours = null
        }
    }
}

@Composable
fun EcranArrivages(navController: NavController, vm: ArrivagesViewModel = viewModel()) {
    val context = LocalContext.current
    var fruitSelectionne by remember { mutableStateOf<Int?>(null) }
    var photoAGrandir by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { fruitSelectionne?.let { id -> vm.uploaderPhoto(context, id, uri) } }
    }

    photoAGrandir?.let { url ->
        PhotoViewer(url = url, onFermer = { photoAGrandir = null })
    }

    Scaffold(topBar = { BarreApp("Photos des Fruits", onRetour = { navController.popBackStack() }) }) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else Column(modifier = Modifier.padding(padding)) {
            vm.succes?.let {
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = VertFrais)
                        Text(it, color = VertFrais, fontWeight = FontWeight.Medium)
                    }
                }
            }
            vm.erreur?.let {
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Error, null, tint = Color(0xFFC62828))
                        Text(it, color = Color(0xFFC62828))
                    }
                }
            }
            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(vm.fruits) { fruit ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (fruit.imageUrl != null) {
                                AsyncImage(
                                    model = fruit.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(70.dp).clip(RoundedCornerShape(12.dp)).clickable { photoAGrandir = fruit.imageUrl },
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(modifier = Modifier.size(70.dp).clip(RoundedCornerShape(12.dp)).background(VertFrais.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                    Text("🍎", fontSize = 28.sp)
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(fruit.nom, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(if (fruit.imageUrl != null) "Photo disponible" else "Aucune photo", color = if (fruit.imageUrl != null) VertFrais else Color.Gray, fontSize = 12.sp)
                            }
                            if (vm.uploadEnCours == fruit.id) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = OrangeFruit, strokeWidth = 2.dp)
                            } else {
                                IconButton(onClick = { fruitSelectionne = fruit.id; launcher.launch("image/*") }) {
                                    Icon(Icons.Default.CameraAlt, null, tint = OrangeFruit)
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
