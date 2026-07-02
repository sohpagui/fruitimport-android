package com.fruitimport.app.ui.auth

// ============================================================
// FICHIER : ui/auth/EcranConnexion.kt
// Rôle : Écran de connexion.
//        - Champ identifiant (téléphone ou email)
//        - Champ mot de passe avec affichage/masquage
//        - Bouton connexion
//        - Lien vers inscription client
//        - Après connexion : redirige vers le bon dashboard
// ============================================================

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.LoginRequest
import com.fruitimport.app.navigation.Routes
import com.fruitimport.app.navigation.obtenirRouteAccueil
import com.fruitimport.app.utils.SessionManager
import kotlinx.coroutines.launch

// ── ViewModel de la connexion
class LoginViewModel : ViewModel() {
    var chargement by mutableStateOf(false)
    var erreur by mutableStateOf<String?>(null)
    var succes by mutableStateOf(false)

    fun seConnecter(identifiant: String, motDePasse: String) {
        if (identifiant.isBlank() || motDePasse.isBlank()) {
            erreur = "Veuillez remplir tous les champs."
            return
        }

        viewModelScope.launch {
            chargement = true
            erreur = null
            try {
                val reponse = RetrofitClient.instance.login(
                    LoginRequest(identifiant.trim().replace(" ", "").let { id -> if (id.startsWith("+237") && id.length == 13) "+237 ${id.substring(4,7)} ${id.substring(7,10)} ${id.substring(10,13)}" else id }, motDePasse)
                )
                if (reponse.isSuccessful && reponse.body()?.success == true) {
                    val data = reponse.body()!!.data!!
                    SessionManager.sauvegarderSession(
                        data.utilisateur,
                        data.accessToken,
                        data.refreshToken
                    )
                    succes = true
                } else {
                    erreur = reponse.body()?.message ?: "Identifiant ou mot de passe incorrect."
                }
            } catch (e: Exception) {
                erreur = "Impossible de contacter le serveur. Vérifiez votre connexion."
            }
            chargement = false
        }
    }
}

// ── Écran de connexion
@Composable
fun EcranConnexion(navController: NavController, vm: LoginViewModel = viewModel()) {
    var identifiant by remember { mutableStateOf("") }
    var motDePasse by remember { mutableStateOf("") }
    var mdpVisible by remember { mutableStateOf(false) }

    // Redirection après connexion réussie
    LaunchedEffect(vm.succes) {
        if (vm.succes) {
            val route = if (SessionManager.estClient()) Routes.DASHBOARD_CLIENT else obtenirRouteAccueil(SessionManager.obtenirRole())
            navController.navigate(route) {
                popUpTo(Routes.CONNEXION) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo et titre
        Icon(
            Icons.Default.LocalFlorist,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "FruitImport Cameroun",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Gestion des importations de fruits",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Message d'erreur
        vm.erreur?.let { err ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(err, color = Color.Red, fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Champ identifiant
        OutlinedTextField(
            value = identifiant,
            onValueChange = { identifiant = it },
            label = { Text("Téléphone ou Email") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Champ mot de passe
        OutlinedTextField(
            value = motDePasse,
            onValueChange = { motDePasse = it },
            label = { Text("Mot de passe") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { mdpVisible = !mdpVisible }) {
                    Icon(
                        if (mdpVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null
                    )
                }
            },
            visualTransformation = if (mdpVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Bouton connexion
        Button(
            onClick = { vm.seConnecter(identifiant, motDePasse) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !vm.chargement
        ) {
            if (vm.chargement) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            } else {
                Icon(Icons.Default.Login, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Se connecter", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Lien inscription client
        TextButton(onClick = { navController.navigate(Routes.INSCRIPTION_CLIENT) }) {
            Text("Nouveau client ? Créer un compte", color = MaterialTheme.colorScheme.primary)
        }
    }
}
