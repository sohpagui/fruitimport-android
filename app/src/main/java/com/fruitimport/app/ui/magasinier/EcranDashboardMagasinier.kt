package com.fruitimport.app.ui.magasinier

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
import com.fruitimport.app.data.models.StatsAgence
import com.fruitimport.app.navigation.Routes
import com.fruitimport.app.ui.components.ChargementIndicateur
import com.fruitimport.app.ui.components.PhotoViewer
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.fruitimport.app.ui.secretaire.BoutonAction
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertFrais
import com.fruitimport.app.utils.SessionManager
import kotlinx.coroutines.launch

class DashboardMagasinierViewModel : ViewModel() {
    var chargement by mutableStateOf(true)
    var stats by mutableStateOf<StatsAgence?>(null)
    init { charger() }
    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val rep = RetrofitClient.instance.dashboardAgence(SessionManager.obtenirAgenceId() ?: 1)
                if (rep.isSuccessful) stats = rep.body()?.data
            } catch (e: Exception) {}
            chargement = false
        }
    }
}

@Composable
fun EcranDashboardMagasinier(navController: NavController, vm: DashboardMagasinierViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.charger() }
    var afficherPhoto by remember { mutableStateOf(false) }
    val agenceNom = if (SessionManager.obtenirAgenceId() == 1) "Douala" else "Yaounde"

    if (afficherPhoto && SessionManager.utilisateurConnecte?.photoUrl != null) {
        PhotoViewer(url = SessionManager.utilisateurConnecte?.photoUrl!!, onFermer = { afficherPhoto = false })
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color(0xFFFFF8E1), Color(0xFFF5F5F5)))))
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(Color(0xFFE65100), OrangeFruit))).padding(horizontal = 20.dp, vertical = 14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (SessionManager.utilisateurConnecte?.photoUrl != null) {
                            AsyncImage(model = SessionManager.utilisateurConnecte?.photoUrl, contentDescription = null, modifier = Modifier.size(45.dp).clip(CircleShape).clickable { afficherPhoto = true }, contentScale = ContentScale.Crop)
                        } else {
                            Box(modifier = Modifier.size(45.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                Text(SessionManager.utilisateurConnecte?.nom?.take(1) ?: "M", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        Column {
                            Text("Magasinier $agenceNom", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
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
            else Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val s = vm.stats
                Text("Mon entrepot", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (s != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(modifier = Modifier.weight(1f).shadow(4.dp, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = VertFrais.copy(alpha = 0.1f))) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📦", fontSize = 32.sp)
                                Text("${s.stockTotal}", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = VertFrais)
                                Text("Cartons en stock", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                            }
                        }
                        Card(modifier = Modifier.weight(1f).shadow(4.dp, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = OrangeFruit.copy(alpha = 0.1f))) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🚚", fontSize = 32.sp)
                                Text("${s.livraisonsEnCours}", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = OrangeFruit)
                                Text("Livraisons en cours", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
                Text("Mes actions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BoutonAction("📦", "Mon Stock", VertFrais, { navController.navigate(Routes.STOCK) }, Modifier.weight(1f))
                    BoutonAction("📥", "Reception", OrangeFruit, { navController.navigate(Routes.RECEPTION) }, Modifier.weight(1f))
                    BoutonAction("🗑", "Pertes", Color(0xFFC62828), { navController.navigate(Routes.PERTES) }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BoutonAction("🔄", "Transfert", Color(0xFF1565C0), { navController.navigate(Routes.TRANSFERTS) }, Modifier.weight(1f))
                    BoutonAction("⚙", "Profil", Color.Gray, { navController.navigate(Routes.PROFIL) }, Modifier.weight(1f))
                    Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
