package com.fruitimport.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.InscriptionClientRequest
import com.fruitimport.app.navigation.Routes
import com.fruitimport.app.ui.components.BarreApp
import kotlinx.coroutines.launch

class InscriptionViewModel : ViewModel() {
    var chargement by mutableStateOf(false)
    var erreur by mutableStateOf<String?>(null)
    var succes by mutableStateOf(false)

    fun inscrire(nom: String, telephone: String, email: String, adresse: String,
                 type: String, agenceId: Int, motDePasse: String) {
        if (nom.isBlank() || telephone.isBlank() || motDePasse.isBlank()) {
            erreur = "Nom, téléphone et mot de passe sont obligatoires."
            return
        }
        viewModelScope.launch {
            chargement = true
            erreur = null
            try {
                val request = InscriptionClientRequest(
                    nom = nom, telephone = telephone,
                    type = type, agenceId = agenceId,
                    motDePasse = motDePasse,
                    email = email.trim().let { if (it.isNotEmpty() && it.contains("@")) it else null },
                    adresse = if (adresse.isNotBlank()) adresse else null
                )
                val reponse = RetrofitClient.instance.inscrireClient(request)
                if (reponse.isSuccessful && reponse.body()?.success == true) {
                    succes = true
                } else {
                    erreur = reponse.body()?.message ?: "Erreur lors de l'inscription."
                }
            } catch (e: Exception) {
                erreur = "EXCEPTION: ${e.javaClass.simpleName}: ${e.message}"
            }
            chargement = false
        }
    }
}

@Composable
fun EcranInscriptionClient(navController: NavController, vm: InscriptionViewModel = viewModel()) {
    var nom by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var adresse by remember { mutableStateOf("") }
    var motDePasse by remember { mutableStateOf("") }
    var typeClient by remember { mutableStateOf("PARTICULIER") }
    var agenceId by remember { mutableStateOf(1) }

    LaunchedEffect(vm.succes) {
        if (vm.succes) {
            navController.navigate(Routes.CONNEXION + "?message=Compte créé ! Connectez-vous.") {
                popUpTo(Routes.INSCRIPTION_CLIENT) { inclusive = true }
            }
        }
    }

    Scaffold(topBar = { BarreApp("Créer un compte client", onRetour = { navController.popBackStack() }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            vm.erreur?.let { Text(it, color = Color.Red) }

            OutlinedTextField(value = nom, onValueChange = { nom = it },
                label = { Text("Nom complet *") }, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = telephone, onValueChange = { telephone = it },
                label = { Text("Téléphone *") }, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = email, onValueChange = { email = it },
                label = { Text("Email (optionnel)") }, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = adresse, onValueChange = { adresse = it },
                label = { Text("Adresse") }, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = motDePasse, onValueChange = { motDePasse = it },
                label = { Text("Mot de passe *") }, modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation())

            // Type de compte
            Text("Type de compte", style = MaterialTheme.typography.labelLarge)
            Row {
                listOf("PARTICULIER" to "Particulier", "SUPERMARCHE" to "Supermarché").forEach { (val_, label) ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(selected = typeClient == val_, onClick = { typeClient = val_ })
                        Text(label)
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }
            }

            // Agence
            Text("Agence de référence", style = MaterialTheme.typography.labelLarge)
            Row {
                listOf(1 to "Douala", 2 to "Yaoundé").forEach { (id, nom_) ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(selected = agenceId == id, onClick = { agenceId = id })
                        Text(nom_)
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }
            }

            Button(
                onClick = { vm.inscrire(nom, telephone, email, adresse, typeClient, agenceId, motDePasse) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !vm.chargement
            ) {
                if (vm.chargement) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                else Text("Créer mon compte")
            }
        }
    }
}
