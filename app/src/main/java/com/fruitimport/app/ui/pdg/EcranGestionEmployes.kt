package com.fruitimport.app.ui.pdg
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.User
import com.fruitimport.app.data.models.CreerEmployeRequest
import com.fruitimport.app.ui.components.BarreApp
import com.fruitimport.app.ui.components.ChargementIndicateur
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertFrais
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class GestionEmployesViewModel : ViewModel() {
    var employes by mutableStateOf<List<User>>(emptyList())
    var chargement by mutableStateOf(true)
    var succes by mutableStateOf<String?>(null)
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
    fun creerEmploye(req: CreerEmployeRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            erreur = null; succes = null
            try {
                val rep = RetrofitClient.instance.creerEmploye(req)
                if (rep.isSuccessful && rep.body()?.success == true) { succes = "Employe cree !"; charger(); onDone() }
                else erreur = rep.body()?.message ?: "Erreur"
            } catch (e: Exception) { erreur = e.message }
        }
    }
    fun modifierActif(id: Int, actif: Boolean) {
        viewModelScope.launch {
            try {
                RetrofitClient.instance.modifierEmploye(id, mapOf("actif" to actif))
                charger()
            } catch (e: Exception) { erreur = e.message }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranGestionEmployes(navController: NavController, vm: GestionEmployesViewModel = viewModel()) {
    var afficherNouvelEmploye by remember { mutableStateOf(false) }
    var nom by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("+237 ") }
    var motDePasse by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("SECRETAIRE") }
    var agenceId by remember { mutableStateOf(1) }
    var roleExpanded by remember { mutableStateOf(false) }
    var agenceExpanded by remember { mutableStateOf(false) }

    if (afficherNouvelEmploye) {
        AlertDialog(
            onDismissRequest = { afficherNouvelEmploye = false },
            title = { Text("Nouvel employe", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom complet *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = telephone, onValueChange = { telephone = it }, label = { Text("Telephone *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = motDePasse, onValueChange = { motDePasse = it }, label = { Text("Mot de passe *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), visualTransformation = PasswordVisualTransformation())
                    ExposedDropdownMenuBox(expanded = roleExpanded, onExpandedChange = { roleExpanded = it }) {
                        OutlinedTextField(value = role, onValueChange = {}, readOnly = true, label = { Text("Role") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp))
                        ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                            listOf("SECRETAIRE", "MAGASINIER", "LIVREUR").forEach { r -> DropdownMenuItem(text = { Text(r) }, onClick = { role = r; roleExpanded = false }) }
                        }
                    }
                    ExposedDropdownMenuBox(expanded = agenceExpanded, onExpandedChange = { agenceExpanded = it }) {
                        OutlinedTextField(value = if (agenceId == 1) "Douala" else "Yaounde", onValueChange = {}, readOnly = true, label = { Text("Agence") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = agenceExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp))
                        ExposedDropdownMenu(expanded = agenceExpanded, onDismissRequest = { agenceExpanded = false }) {
                            DropdownMenuItem(text = { Text("Douala") }, onClick = { agenceId = 1; agenceExpanded = false })
                            DropdownMenuItem(text = { Text("Yaounde") }, onClick = { agenceId = 2; agenceExpanded = false })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (nom.isNotBlank() && telephone.isNotBlank() && motDePasse.isNotBlank()) {
                        vm.creerEmploye(CreerEmployeRequest(nom, telephone, motDePasse, role, agenceId)) {
                            afficherNouvelEmploye = false; nom = ""; telephone = "+237 "; motDePasse = ""
                        }
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = VertFrais)) { Text("Creer") }
            },
            dismissButton = { TextButton(onClick = { afficherNouvelEmploye = false }) { Text("Annuler") } }
        )
    }

    Scaffold(
        topBar = { BarreApp("Gestion Employes", onRetour = { navController.popBackStack() }) },
        floatingActionButton = { FloatingActionButton(onClick = { afficherNouvelEmploye = true }, containerColor = VertFrais) { Icon(Icons.Default.PersonAdd, null, tint = Color.White) } }
    ) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else Column(modifier = Modifier.padding(padding)) {
            vm.succes?.let { Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) { Text(it, modifier = Modifier.padding(12.dp), color = VertFrais) } }
            vm.erreur?.let { Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) { Text(it, modifier = Modifier.padding(12.dp), color = Color.Red) } }
            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(vm.employes) { emp ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(emp.nom, fontWeight = FontWeight.Bold)
                                    Text(emp.telephone, color = Color.Gray, fontSize = 12.sp)
                                    Text(emp.role, color = OrangeFruit, fontSize = 11.sp)
                                    emp.agence?.let { Text(it.nom, color = VertFrais, fontSize = 11.sp) }
                                }
                                Switch(checked = emp.actif, onCheckedChange = { vm.modifierActif(emp.id, it) }, colors = SwitchDefaults.colors(checkedThumbColor = VertFrais, checkedTrackColor = VertFrais.copy(alpha = 0.5f)))
                            }
                            Text(if (emp.actif) "Actif" else "Desactive", color = if (emp.actif) VertFrais else Color.Red, fontSize = 11.sp)
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
