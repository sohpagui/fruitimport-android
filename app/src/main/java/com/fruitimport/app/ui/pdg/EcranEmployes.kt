package com.fruitimport.app.ui.pdg

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.User
import com.fruitimport.app.ui.components.*
import com.fruitimport.app.utils.traduireStatut
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class EmployesViewModel : ViewModel() {
    var chargement by mutableStateOf(true)
    var employes by mutableStateOf<List<User>>(emptyList())
    var erreur by mutableStateOf<String?>(null)
    var afficherFormulaire by mutableStateOf(false)

    init { charger() }

    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val rep = RetrofitClient.instance.obtenirEmployes()
                if (rep.isSuccessful) {
                    val data = rep.body()?.data
                    val usersJson = Gson().toJson((data as? Map<*, *>)?.get("users"))
                    employes = Gson().fromJson(usersJson, object : TypeToken<List<User>>() {}.type) ?: emptyList()
                }
            } catch (e: Exception) { erreur = e.message }
            chargement = false
        }
    }

    fun creerEmploye(nom: String, telephone: String, motDePasse: String, role: String, agenceId: Int) {
        viewModelScope.launch {
            try {
                RetrofitClient.instance.creerEmploye(mapOf(
                    "nom" to nom, "telephone" to telephone,
                    "motDePasse" to motDePasse, "role" to role, "agenceId" to agenceId
                ))
                afficherFormulaire = false
                charger()
            } catch (e: Exception) { erreur = e.message }
        }
    }
}

@Composable
fun EcranEmployes(navController: NavController, vm: EmployesViewModel = viewModel()) {
    var nom by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }
    var motDePasse by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("SECRETAIRE") }
    var agenceId by remember { mutableStateOf(1) }

    Scaffold(
        topBar = { BarreApp("Employés", onRetour = { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.afficherFormulaire = true }) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter")
            }
        }
    ) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else LazyColumn(modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vm.employes) { employe ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(employe.nom, fontWeight = FontWeight.Bold)
                            Text(employe.telephone, color = Color.Gray)
                        }
                        BadgeStatut(employe.role, employe.role.traduireStatut())
                    }
                }
            }
        }
    }

    // Formulaire de création
    if (vm.afficherFormulaire) {
        AlertDialog(
            onDismissRequest = { vm.afficherFormulaire = false },
            title = { Text("Nouvel employé") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = telephone, onValueChange = { telephone = it }, label = { Text("Téléphone") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = motDePasse, onValueChange = { motDePasse = it }, label = { Text("Mot de passe") }, modifier = Modifier.fillMaxWidth())
                    // Sélection du rôle
                    listOf("SECRETAIRE","MAGASINIER","LIVREUR").forEach { r ->
                        Row { RadioButton(selected = role == r, onClick = { role = r }); Text(r.traduireStatut()) }
                    }
                    // Sélection de l'agence
                    listOf(1 to "Douala", 2 to "Yaoundé").forEach { (id, n) ->
                        Row { RadioButton(selected = agenceId == id, onClick = { agenceId = id }); Text(n) }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { vm.creerEmploye(nom, telephone, motDePasse, role, agenceId) }) { Text("Créer") }
            },
            dismissButton = {
                TextButton(onClick = { vm.afficherFormulaire = false }) { Text("Annuler") }
            }
        )
    }
}
