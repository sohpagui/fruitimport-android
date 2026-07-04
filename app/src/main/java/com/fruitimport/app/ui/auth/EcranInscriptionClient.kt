package com.fruitimport.app.ui.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.InscriptionClientRequest
import com.fruitimport.app.navigation.Routes
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertFrais
import kotlinx.coroutines.launch

class InscriptionViewModel : ViewModel() {
    var chargement by mutableStateOf(false)
    var erreur by mutableStateOf<String?>(null)
    var succes by mutableStateOf(false)

    fun inscrire(nom: String, telephone: String, email: String, adresse: String,
                 type: String, agenceId: Int, motDePasse: String) {
        if (nom.isBlank() || telephone.isBlank() || motDePasse.isBlank()) {
            erreur = "Nom, telephone et mot de passe sont obligatoires."
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
fun FruitFlottant(emoji: String, delai: Int, x: Float, taille: Int) {
    val transition = rememberInfiniteTransition(label = "fruit_$emoji")
    val offsetY by transition.animateFloat(
        initialValue = 0f, targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500 + delai, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "offsetY_$emoji"
    )
    val rotation by transition.animateFloat(
        initialValue = -10f, targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000 + delai, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "rotation_$emoji"
    )
    Box(
        modifier = Modifier
            .offset(x.dp, offsetY.dp)
            .size(taille.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
            .rotate(rotation),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = (taille * 0.55f).sp)
    }
}

@Composable
fun EcranInscriptionClient(navController: NavController, vm: InscriptionViewModel = viewModel()) {
    var nom by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var adresse by remember { mutableStateOf("") }
    var motDePasse by remember { mutableStateOf("") }
    var mdpVisible by remember { mutableStateOf(false) }
    var typeClient by remember { mutableStateOf("PARTICULIER") }
    var agenceId by remember { mutableStateOf(1) }

    LaunchedEffect(vm.succes) {
        if (vm.succes) {
            navController.navigate(Routes.CONNEXION) {
                popUpTo(Routes.INSCRIPTION_CLIENT) { inclusive = true }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Fond degrade
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1B5E20), VertFrais, Color(0xFF388E3C))
                )
            )
        )

        // Fruits flottants animes
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FruitFlottant("🍎", 0, 0f, 55)
            FruitFlottant("🍊", 300, 0f, 50)
            FruitFlottant("🍇", 600, 0f, 60)
            FruitFlottant("🥝", 200, 0f, 50)
            FruitFlottant("🍐", 400, 0f, 55)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(80.dp))

            // Titre
            Text("Creer mon compte", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("Rejoignez FruitImport Cameroun", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))

            // Formulaire
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    vm.erreur?.let {
                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFFEBEE)) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Error, null, tint = Color(0xFFC62828), modifier = Modifier.size(16.dp))
                                Text(it, color = Color(0xFFC62828), fontSize = 12.sp)
                            }
                        }
                    }

                    OutlinedTextField(value = nom, onValueChange = { nom = it },
                        label = { Text("Nom complet *") },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = VertFrais) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VertFrais, focusedLabelColor = VertFrais))

                    OutlinedTextField(value = telephone, onValueChange = { telephone = it },
                        label = { Text("Telephone * (+237 6XX XXX XXX)") },
                        leadingIcon = { Icon(Icons.Default.Phone, null, tint = VertFrais) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VertFrais, focusedLabelColor = VertFrais))

                    OutlinedTextField(value = email, onValueChange = { email = it },
                        label = { Text("Email (optionnel)") },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = VertFrais) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VertFrais, focusedLabelColor = VertFrais))

                    OutlinedTextField(value = adresse, onValueChange = { adresse = it },
                        label = { Text("Adresse (optionnel)") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = VertFrais) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VertFrais, focusedLabelColor = VertFrais))

                    OutlinedTextField(
                        value = motDePasse, onValueChange = { motDePasse = it },
                        label = { Text("Mot de passe *") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = VertFrais) },
                        trailingIcon = {
                            IconButton(onClick = { mdpVisible = !mdpVisible }) {
                                Icon(if (mdpVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = Color.Gray)
                            }
                        },
                        visualTransformation = if (mdpVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VertFrais, focusedLabelColor = VertFrais))

                    // Type client
                    Text("Type de client", fontWeight = FontWeight.Medium, color = Color(0xFF1C1B1F))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("PARTICULIER" to "Particulier", "SUPERMARCHE" to "Supermarche").forEach { (val_, lib) ->
                            FilterChip(
                                selected = typeClient == val_,
                                onClick = { typeClient = val_ },
                                label = { Text(lib) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = VertFrais, selectedLabelColor = Color.White)
                            )
                        }
                    }

                    // Agence
                    Text("Agence de reference", fontWeight = FontWeight.Medium, color = Color(0xFF1C1B1F))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1 to "Douala", 2 to "Yaounde").forEach { (id, nom_) ->
                            FilterChip(
                                selected = agenceId == id,
                                onClick = { agenceId = id },
                                label = { Text(nom_) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OrangeFruit, selectedLabelColor = Color.White)
                            )
                        }
                    }

                    Button(
                        onClick = { vm.inscrire(nom, telephone, email, adresse, typeClient, agenceId, motDePasse) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !vm.chargement,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VertFrais)
                    ) {
                        if (vm.chargement) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else { Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.width(8.dp)); Text("Creer mon compte", fontWeight = FontWeight.SemiBold) }
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                    TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Deja un compte ? Se connecter", color = OrangeFruit, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
