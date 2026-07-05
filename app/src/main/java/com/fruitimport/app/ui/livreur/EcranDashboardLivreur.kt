package com.fruitimport.app.ui.livreur

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.Livraison
import com.fruitimport.app.data.models.MettreAJourLivraisonRequest
import com.fruitimport.app.navigation.Routes
import com.fruitimport.app.ui.components.ChargementIndicateur
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.fruitimport.app.ui.components.BadgeStatut
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertFrais
import com.fruitimport.app.utils.SessionManager
import com.fruitimport.app.utils.traduireStatut
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class LivreurViewModel : ViewModel() {
    var chargement by mutableStateOf(true)
    var livraisons by mutableStateOf<List<Livraison>>(emptyList())
    var erreur by mutableStateOf<String?>(null)
    init { charger() }
    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val livreurId = SessionManager.utilisateurConnecte?.id
                val rep = RetrofitClient.instance.obtenirLivraisons(livreurId = livreurId)
                if (rep.isSuccessful) {
                    val json = Gson().toJson((rep.body()?.data as? Map<*,*>)?.get("livraisons"))
                    livraisons = Gson().fromJson(json, object : TypeToken<List<Livraison>>() {}.type) ?: emptyList()
                }
            } catch (e: Exception) { erreur = e.message }
            chargement = false
        }
    }
    fun mettreAJour(id: Int, statut: String, note: String = "") {
        viewModelScope.launch {
            try {
                val data = MettreAJourLivraisonRequest(statut = statut, noteProbleme = if (note.isBlank()) null else note)
                RetrofitClient.instance.mettreAJourLivraison(id, data)
                charger()
            } catch (e: Exception) { erreur = e.message }
        }
    }
}

@Composable
fun EcranDashboardLivreur(navController: NavController, vm: LivreurViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.charger() }
    var noteProbleme by remember { mutableStateOf("") }
    var livraisonProblem by remember { mutableStateOf<Int?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color(0xFFE8EAF6), Color(0xFFF5F5F5)))))
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(Color(0xFF4527A0), Color(0xFF6A1B9A)))).padding(horizontal = 20.dp, vertical = 14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val photoUrl = SessionManager.utilisateurConnecte?.photoUrl
                        if (photoUrl != null) {
                            AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.size(45.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            Box(modifier = Modifier.size(45.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                Text(SessionManager.utilisateurConnecte?.nom?.take(1) ?: "L", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        Column {
                            Text("Livreur", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            Text(SessionManager.utilisateurConnecte?.nom ?: "", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Row {
                        IconButton(onClick = { navController.navigate(Routes.PROFIL) }) { Icon(Icons.Default.Person, null, tint = Color.White) }
                        IconButton(onClick = { SessionManager.effacerSession(); navController.navigate(Routes.CONNEXION) { popUpTo(0) { inclusive = true } } }) { Icon(Icons.Default.Logout, null, tint = Color.White) }
                    }
                }
            }

            if (vm.chargement) ChargementIndicateur()
            else {
                val enCours = vm.livraisons.filter { it.statut in listOf("PREPARE", "EN_ROUTE") }
                val terminees = vm.livraisons.filter { it.statut in listOf("LIVRE", "PROBLEME") }

                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFF4527A0).copy(alpha = 0.1f))) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🚚", fontSize = 28.sp)
                                    Text("${enCours.size}", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color(0xFF4527A0))
                                    Text("En cours", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = VertFrais.copy(alpha = 0.1f))) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("✅", fontSize = 28.sp)
                                    Text("${terminees.size}", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = VertFrais)
                                    Text("Terminees", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }

                    if (enCours.isEmpty() && terminees.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("🎉", fontSize = 64.sp)
                                    Text("Aucune livraison", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Gray)
                                    Text("Vous avez termine toutes vos livraisons !", color = Color.Gray, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }

                    if (enCours.isNotEmpty()) {
                        item { Text("A livrer maintenant", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                    }
                    items(enCours) { liv ->
                        Card(modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Livraison #${liv.id}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    BadgeStatut(liv.statut, liv.statut.traduireStatut())
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("👤", fontSize = 16.sp)
                                    Text(liv.commande?.client?.nom ?: "Client", color = Color.Gray)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("📦", fontSize = 16.sp)
                                    Text(liv.commande?.numero ?: "", color = Color.Gray, fontSize = 13.sp)
                                }
                                if (liv.statut == "PREPARE") {
                                    Button(onClick = { vm.mettreAJour(liv.id, "EN_ROUTE") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))) {
                                        Text("🚗  Partir en livraison", fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (liv.statut == "EN_ROUTE") {
                                    Button(onClick = { vm.mettreAJour(liv.id, "LIVRE") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = VertFrais)) {
                                        Text("✅  Livraison effectuee", fontWeight = FontWeight.Bold)
                                    }
                                    if (livraisonProblem == liv.id) {
                                        OutlinedTextField(value = noteProbleme, onValueChange = { noteProbleme = it }, label = { Text("Decrivez le probleme") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                                        OutlinedButton(onClick = { vm.mettreAJour(liv.id, "PROBLEME", noteProbleme); livraisonProblem = null }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                            Text("Confirmer le probleme", color = Color(0xFFC62828))
                                        }
                                    } else {
                                        OutlinedButton(onClick = { livraisonProblem = liv.id }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                            Text("⚠️  Signaler un probleme", color = OrangeFruit)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (terminees.isNotEmpty()) {
                        item { Text("Terminees", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Gray) }
                    }
                    items(terminees) { liv ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(if (liv.statut == "LIVRE") "✅" else "⚠️", fontSize = 20.sp)
                                    Column {
                                        Text("Livraison #${liv.id}", fontWeight = FontWeight.Medium)
                                        Text(liv.commande?.client?.nom ?: "", color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                                BadgeStatut(liv.statut, liv.statut.traduireStatut())
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}
