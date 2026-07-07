package com.fruitimport.app.ui.pdg

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.User
import com.fruitimport.app.ui.components.BarreApp
import com.fruitimport.app.ui.components.ChargementIndicateur
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertFrais
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class EmployesViewModel : ViewModel() {
    var employes by mutableStateOf<List<User>>(emptyList())
    var chargement by mutableStateOf(true)
    var erreur by mutableStateOf<String?>(null)

    init { charger() }

    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val rep = RetrofitClient.instance.obtenirEmployes()
                if (rep.isSuccessful) {
                    val json = Gson().toJson((rep.body()?.data as? Map<*,*>)?.get("users"))
                    employes = Gson().fromJson(json, object : TypeToken<List<User>>() {}.type) ?: emptyList()
                }
            } catch (e: Exception) { erreur = e.message }
            chargement = false
        }
    }
}

@Composable
fun EcranEmployes(navController: NavController, vm: EmployesViewModel = viewModel()) {
    var recherche by remember { mutableStateOf("") }
    var filtreRole by remember { mutableStateOf("TOUS") }

    Scaffold(
        topBar = { BarreApp("Mes Employes", onRetour = { navController.popBackStack() }) }
    ) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = recherche, onValueChange = { recherche = it },
                label = { Text("Rechercher...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = VertFrais) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true, shape = RoundedCornerShape(12.dp)
            )
            // Filtres par role
            val roles = listOf("TOUS", "SECRETAIRE", "MAGASINIER", "LIVREUR")
            androidx.compose.foundation.lazy.LazyRow(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(roles) { role ->
                    FilterChip(
                        selected = filtreRole == role,
                        onClick = { filtreRole = role },
                        label = { Text(role) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = VertFrais, selectedLabelColor = Color.White)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            val employesFiltres = vm.employes.filter { emp ->
                (filtreRole == "TOUS" || emp.role == filtreRole) &&
                (recherche.isBlank() || emp.nom.contains(recherche, ignoreCase = true) || emp.telephone.contains(recherche))
            }
            Text("${employesFiltres.size} employe(s)", modifier = Modifier.padding(horizontal = 16.dp), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(employesFiltres) { emp ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (emp.photoUrl != null) {
                                AsyncImage(model = emp.photoUrl, contentDescription = null, modifier = Modifier.size(50.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            } else {
                                Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(VertFrais.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                    Text(emp.nom.take(1), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = VertFrais)
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(emp.nom, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(emp.telephone, color = Color.Gray, fontSize = 12.sp)
                                emp.agence?.let { Text(it.nom, color = OrangeFruit, fontSize = 11.sp) }
                            }
                            Surface(shape = RoundedCornerShape(20.dp), color = when(emp.role) {
                                "SECRETAIRE" -> Color(0xFF1565C0).copy(alpha = 0.1f)
                                "MAGASINIER" -> OrangeFruit.copy(alpha = 0.1f)
                                "LIVREUR" -> Color(0xFF6A1B9A).copy(alpha = 0.1f)
                                else -> Color.Gray.copy(alpha = 0.1f)
                            }) {
                                Text(emp.role, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = when(emp.role) {
                                    "SECRETAIRE" -> Color(0xFF1565C0)
                                    "MAGASINIER" -> OrangeFruit
                                    "LIVREUR" -> Color(0xFF6A1B9A)
                                    else -> Color.Gray
                                })
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
