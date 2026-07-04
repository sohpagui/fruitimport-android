package com.fruitimport.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.ChangerMotDePasseRequest
import com.fruitimport.app.utils.SessionManager
import kotlinx.coroutines.launch

class ProfilViewModel : ViewModel() {
    var chargement by mutableStateOf(false)
    var succes by mutableStateOf<String?>(null)
    var erreur by mutableStateOf<String?>(null)

    fun changerMotDePasse(ancien: String, nouveau: String, confirmation: String) {
        if (nouveau != confirmation) {
            erreur = "Les mots de passe ne correspondent pas"
            return
        }
        if (nouveau.length < 6) {
            erreur = "Le mot de passe doit avoir au moins 6 caracteres"
            return
        }
        viewModelScope.launch {
            chargement = true
            erreur = null
            succes = null
            try {
                val rep = RetrofitClient.instance.changerMotDePasse(
                    ChangerMotDePasseRequest(ancienMotDePasse = ancien, nouveauMotDePasse = nouveau)
                )
                if (rep.isSuccessful && rep.body()?.success == true) {
                    succes = "Mot de passe change avec succes !"
                } else {
                    erreur = rep.body()?.message ?: "Erreur lors du changement"
                }
            } catch (e: Exception) { erreur = e.message }
            chargement = false
        }
    }
}

@Composable
fun EcranProfil(navController: NavController, vm: ProfilViewModel = viewModel()) {
    var ancienMdp by remember { mutableStateOf("") }
    var ancienVisible by remember { mutableStateOf(false) }
    var nouveauVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var nouveauMdp by remember { mutableStateOf("") }
    var confirmMdp by remember { mutableStateOf("") }
    val user = SessionManager.utilisateurConnecte

    Scaffold(topBar = { BarreApp("Mon Profil", onRetour = { navController.popBackStack() }) }) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Infos utilisateur
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(user?.nom ?: "", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(user?.telephone ?: "", color = Color.Gray)
                            user?.role?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                        }
                    }
                }
            }

            // Changement mot de passe
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Changer le mot de passe", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    vm.erreur?.let { Text(it, color = Color.Red) }
                    vm.succes?.let { Text(it, color = Color(0xFF2E7D32)) }

                    OutlinedTextField(
                        value = ancienMdp, onValueChange = { ancienMdp = it },
                        label = { Text("Ancien mot de passe") },
                        visualTransformation = if (ancienVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = { IconButton(onClick = { ancienVisible = !ancienVisible }) { Icon(if (ancienVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nouveauMdp, onValueChange = { nouveauMdp = it },
                        label = { Text("Nouveau mot de passe") },
                        visualTransformation = if (nouveauVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = { IconButton(onClick = { nouveauVisible = !nouveauVisible }) { Icon(if (nouveauVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmMdp, onValueChange = { confirmMdp = it },
                        label = { Text("Confirmer le nouveau mot de passe") },
                        visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = { IconButton(onClick = { confirmVisible = !confirmVisible }) { Icon(if (confirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { vm.changerMotDePasse(ancienMdp, nouveauMdp, confirmMdp) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !vm.chargement && ancienMdp.isNotBlank() && nouveauMdp.isNotBlank()
                    ) {
                        if (vm.chargement) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        else Text("Changer le mot de passe")
                    }
                }
            }
        }
    }
}
