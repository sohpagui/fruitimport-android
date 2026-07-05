package com.fruitimport.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertClair
import com.fruitimport.app.ui.theme.VertFrais
import com.fruitimport.app.utils.SessionManager
import kotlinx.coroutines.launch

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
                    LoginRequest(
                        identifiant.trim().replace(" ", "").let { id ->
                            if (id.startsWith("+237") && id.length == 13)
                                "+237 ${id.substring(4,7)} ${id.substring(7,10)} ${id.substring(10,13)}"
                            else id
                        },
                        motDePasse
                    )
                )
                if (reponse.isSuccessful && reponse.body()?.success == true) {
                    val data = reponse.body()!!.data!!
                    SessionManager.sauvegarderSession(data.utilisateur, data.accessToken, data.refreshToken)
                    // Recuperer la photo de profil depuis le serveur
                    try {
                        val meRep = RetrofitClient.instance.me()
                        if (meRep.isSuccessful) {
                            val photoUrl = (meRep.body()?.data as? Map<*, *>)?.get("photoUrl") as? String
                            if (photoUrl != null) { SessionManager.mettreAJourPhoto(photoUrl); android.util.Log.d("PHOTO_URL", "Photo: $photoUrl") } else { android.util.Log.d("PHOTO_URL", "Photo NULL") }
                        }
                    } catch (e: Exception) {}
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

@Composable
fun FruitDecoration(emoji: String, x: Float, y: Float, size: Int, alpha: Float = 0.3f) {
    Box(
        modifier = Modifier
            .offset(x.dp, y.dp)
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = alpha * 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = (size * 0.5f).sp)
    }
}

@Composable
fun EcranConnexion(navController: NavController, vm: LoginViewModel = viewModel()) {
    var identifiant by remember { mutableStateOf("") }
    var motDePasse by remember { mutableStateOf("") }
    var mdpVisible by remember { mutableStateOf(false) }

    LaunchedEffect(vm.succes) {
        if (vm.succes) {
            val route = if (SessionManager.estClient()) Routes.DASHBOARD_CLIENT
                        else obtenirRouteAccueil(SessionManager.obtenirRole())
            navController.navigate(route) { popUpTo(Routes.CONNEXION) { inclusive = true } }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Fond degrade vert
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(VertFrais, Color(0xFF1B5E20), Color(0xFF33691E))
                    )
                )
        )

        // Decorations fruits en fond
        FruitDecoration("🍎", -20f, 40f, 80)
        FruitDecoration("🍊", 300f, 20f, 60)
        FruitDecoration("🍇", -10f, 200f, 50)
        FruitDecoration("🥝", 320f, 180f, 70)
        FruitDecoration("🍐", 150f, -10f, 55)
        FruitDecoration("🍎", 280f, 350f, 65)
        FruitDecoration("🍊", -15f, 550f, 75)
        FruitDecoration("🍇", 330f, 500f, 55)
        FruitDecoration("🥝", 100f, 620f, 60)

        // Contenu principal centre
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo et titre
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🍎🍊", fontSize = 36.sp, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "FruitImport",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                "Cameroun",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = OrangeFruit.copy(alpha = 0.9f)
            ) {
                Text(
                    "Gestion des imports de fruits",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(32.dp))

            // Carte de connexion
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Connexion",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1B1F)
                    )

                    vm.erreur?.let {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFEBEE)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Error, null, tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                                Text(it, color = Color(0xFFC62828), fontSize = 13.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = identifiant,
                        onValueChange = { identifiant = it },
                        label = { Text("Telephone ou Email") },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = VertFrais) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VertFrais,
                            focusedLabelColor = VertFrais
                        )
                    )

                    OutlinedTextField(
                        value = motDePasse,
                        onValueChange = { motDePasse = it },
                        label = { Text("Mot de passe") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = VertFrais) },
                        trailingIcon = {
                            IconButton(onClick = { mdpVisible = !mdpVisible }) {
                                Icon(
                                    if (mdpVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null,
                                    tint = Color.Gray
                                )
                            }
                        },
                        visualTransformation = if (mdpVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VertFrais,
                            focusedLabelColor = VertFrais
                        )
                    )

                    Button(
                        onClick = { vm.seConnecter(identifiant, motDePasse) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !vm.chargement,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VertFrais)
                    ) {
                        if (vm.chargement) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Login, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Se connecter", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    HorizontalDivider(color = androidx.compose.ui.graphics.Color.LightGray.copy(alpha = 0.5f))
                    TextButton(onClick = { navController.navigate(Routes.INSCRIPTION_CLIENT) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Nouveau client ? Creer un compte", color = com.fruitimport.app.ui.theme.OrangeFruit, fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            }

        }
    }
}
