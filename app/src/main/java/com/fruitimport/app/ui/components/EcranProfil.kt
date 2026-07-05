package com.fruitimport.app.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.ChangerMotDePasseRequest
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertFrais
import com.fruitimport.app.utils.SessionManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ProfilViewModel : ViewModel() {
    var chargement by mutableStateOf(false)
    var succes by mutableStateOf<String?>(null)
    var erreur by mutableStateOf<String?>(null)
    var photoUrl by mutableStateOf(SessionManager.utilisateurConnecte?.photoUrl)

    fun changerMotDePasse(ancien: String, nouveau: String, confirmation: String) {
        if (nouveau != confirmation) { erreur = "Les mots de passe ne correspondent pas"; return }
        if (nouveau.length < 6) { erreur = "Le mot de passe doit avoir au moins 6 caracteres"; return }
        viewModelScope.launch {
            chargement = true; erreur = null; succes = null
            try {
                val rep = RetrofitClient.instance.changerMotDePasse(ChangerMotDePasseRequest(ancienMotDePasse = ancien, nouveauMotDePasse = nouveau))
                if (rep.isSuccessful && rep.body()?.success == true) succes = "Mot de passe change avec succes !"
                else erreur = rep.body()?.message ?: "Erreur"
            } catch (e: Exception) { erreur = e.message }
            chargement = false
        }
    }

    fun uploaderPhoto(context: android.content.Context, uri: Uri) {
        viewModelScope.launch {
            chargement = true; erreur = null
            try {
                val stream = context.contentResolver.openInputStream(uri)
                val bytes = stream?.readBytes() ?: return@launch
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("photo", "photo.jpg", requestBody)
                val rep = RetrofitClient.instance.uploaderPhotoProfil(part)
                if (rep.isSuccessful && rep.body()?.success == true) {
                    // Recuperer l URL Cloudinary depuis la reponse
                    val data = rep.body()?.data as? Map<*, *>
                    val cloudinaryUrl = data?.get("photoUrl") as? String
                    if (cloudinaryUrl != null) {
                        photoUrl = cloudinaryUrl
                        // Mettre a jour la session
                        SessionManager.mettreAJourPhoto(cloudinaryUrl)
                    }
                } else erreur = rep.body()?.message ?: "Erreur upload"
            } catch (e: Exception) { erreur = e.message }
            chargement = false
        }
    }
}

@Composable
fun EcranProfil(navController: NavController, vm: ProfilViewModel = viewModel()) {
    var ancienMdp by remember { mutableStateOf("") }
    var nouveauMdp by remember { mutableStateOf("") }
    var confirmMdp by remember { mutableStateOf("") }
    var ancienVisible by remember { mutableStateOf(false) }
    var nouveauVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    val user = SessionManager.utilisateurConnecte
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.uploaderPhoto(context, it) }
    }

    Scaffold(topBar = { BarreApp("Mon Profil", onRetour = { navController.popBackStack() }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Photo de profil
            Card(modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        if (vm.photoUrl != null) {
                            AsyncImage(model = vm.photoUrl, contentDescription = null, modifier = Modifier.size(90.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            Box(modifier = Modifier.size(90.dp).clip(CircleShape).background(VertFrais.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                Text(user?.nom?.take(1) ?: "?", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = VertFrais)
                            }
                        }
                        Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(OrangeFruit).clickable { launcher.launch("image/*") }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(user?.nom ?: "", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(user?.telephone ?: "", color = Color.Gray, fontSize = 13.sp)
                    user?.role?.let { Surface(shape = RoundedCornerShape(20.dp), color = VertFrais.copy(alpha = 0.1f)) { Text(it, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = VertFrais, fontSize = 12.sp, fontWeight = FontWeight.Medium) } }
                    Text("Appuyez sur l icone pour changer la photo", fontSize = 11.sp, color = Color.Gray)
                }
            }

            // Messages
            vm.succes?.let { Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFE8F5E9)) { Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Default.CheckCircle, null, tint = VertFrais, modifier = Modifier.size(18.dp)); Text(it, color = VertFrais) } } }
            vm.erreur?.let { Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFFEBEE)) { Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Default.Error, null, tint = Color(0xFFC62828), modifier = Modifier.size(18.dp)); Text(it, color = Color(0xFFC62828)) } } }

            // Changement mot de passe
            Card(modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Changer le mot de passe", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    OutlinedTextField(value = ancienMdp, onValueChange = { ancienMdp = it }, label = { Text("Ancien mot de passe") }, visualTransformation = if (ancienVisible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { ancienVisible = !ancienVisible }) { Icon(if (ancienVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VertFrais, focusedLabelColor = VertFrais))
                    OutlinedTextField(value = nouveauMdp, onValueChange = { nouveauMdp = it }, label = { Text("Nouveau mot de passe") }, visualTransformation = if (nouveauVisible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { nouveauVisible = !nouveauVisible }) { Icon(if (nouveauVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VertFrais, focusedLabelColor = VertFrais))
                    OutlinedTextField(value = confirmMdp, onValueChange = { confirmMdp = it }, label = { Text("Confirmer le mot de passe") }, visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { confirmVisible = !confirmVisible }) { Icon(if (confirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VertFrais, focusedLabelColor = VertFrais))
                    Button(onClick = { vm.changerMotDePasse(ancienMdp, nouveauMdp, confirmMdp) }, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = !vm.chargement, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = VertFrais)) {
                        if (vm.chargement) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Changer le mot de passe", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
