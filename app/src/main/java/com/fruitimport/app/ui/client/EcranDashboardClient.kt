package com.fruitimport.app.ui.client

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fruitimport.app.navigation.Routes
import com.fruitimport.app.ui.components.BarreApp
import com.fruitimport.app.utils.SessionManager

@Composable
fun EcranDashboardClient(navController: NavController) {
    Scaffold(
        topBar = { BarreApp("Mon Espace", actions = {
            IconButton(onClick = { SessionManager.effacerSession(); navController.navigate(Routes.CONNEXION) { popUpTo(0) { inclusive = true } } }) {
                Icon(Icons.Default.Logout, null)
            }
        }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Accueil") })
                NavigationBarItem(selected = false, onClick = { navController.navigate(Routes.CATALOGUE) }, icon = { Icon(Icons.Default.ShoppingBag, null) }, label = { Text("Catalogue") })
                NavigationBarItem(selected = false, onClick = { navController.navigate(Routes.MES_COMMANDES) }, icon = { Icon(Icons.Default.Receipt, null) }, label = { Text("Commandes") })
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.Person, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
            Text(SessionManager.utilisateurConnecte?.nom ?: "", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(SessionManager.utilisateurConnecte?.role?.replace("CLIENT_","") ?: "", color = Color.Gray)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { navController.navigate(Routes.CATALOGUE) }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Icon(Icons.Default.ShoppingBag, null); Spacer(Modifier.width(8.dp)); Text("Voir le catalogue")
            }
            OutlinedButton(onClick = { navController.navigate(Routes.MES_COMMANDES) }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Icon(Icons.Default.Receipt, null); Spacer(Modifier.width(8.dp)); Text("Mes commandes")
            }
        }
    }
}
