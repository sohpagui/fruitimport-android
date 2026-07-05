package com.fruitimport.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.fruitimport.app.data.models.Conversation
import com.fruitimport.app.data.models.User
import com.fruitimport.app.ui.components.BarreApp
import com.fruitimport.app.ui.components.ChargementIndicateur
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertFrais
import com.fruitimport.app.utils.SessionManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class ConversationsViewModel : ViewModel() {
    var conversations by mutableStateOf<List<Conversation>>(emptyList())
    var utilisateurs by mutableStateOf<List<User>>(emptyList())
    var chargement by mutableStateOf(true)
    var afficherNouvelleConv by mutableStateOf(false)

    init { charger() }

    fun charger() {
        viewModelScope.launch {
            chargement = true
            try {
                val repConv = RetrofitClient.instance.obtenirConversations()
                if (repConv.isSuccessful) {
                    val json = Gson().toJson(repConv.body()?.data)
                    conversations = Gson().fromJson(json, object : TypeToken<List<Conversation>>() {}.type) ?: emptyList()
                }
                val repUsers = RetrofitClient.instance.obtenirUtilisateursChat()
                if (repUsers.isSuccessful) utilisateurs = repUsers.body()?.data ?: emptyList()
            } catch (e: Exception) {}
            chargement = false
        }
    }

    fun ouvrirOuCreerConversation(userId: Int, onSuccess: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val rep = RetrofitClient.instance.creerConversation(mapOf("userId" to userId))
                if (rep.isSuccessful) {
                    val json = Gson().toJson(rep.body()?.data)
                    val conv = Gson().fromJson(json, Conversation::class.java)
                    afficherNouvelleConv = false
                    charger()
                    onSuccess(conv.id)
                }
            } catch (e: Exception) {}
        }
    }
}

@Composable
fun EcranConversations(navController: NavController, vm: ConversationsViewModel = viewModel()) {
    val myId = SessionManager.utilisateurConnecte?.id ?: 0

    Scaffold(
        topBar = { BarreApp("Messages", onRetour = { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.afficherNouvelleConv = true }, containerColor = VertFrais) {
                Icon(Icons.Default.Edit, null, tint = Color.White)
            }
        }
    ) { padding ->
        if (vm.chargement) ChargementIndicateur()
        else if (vm.conversations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("💬", fontSize = 64.sp)
                    Text("Aucun message", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Gray)
                    Text("Appuyez sur + pour demarrer une conversation", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(vm.conversations) { conv ->
                    val autreParticipant = conv.participants.firstOrNull { it.userId != myId }?.user
                    val dernierMessage = conv.messages.firstOrNull()
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { navController.navigate("chat/${conv.id}") }.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (autreParticipant?.photoUrl != null) {
                            AsyncImage(model = autreParticipant.photoUrl, contentDescription = null, modifier = Modifier.size(54.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            Box(modifier = Modifier.size(54.dp).clip(CircleShape).background(VertFrais.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                Text(autreParticipant?.nom?.take(1) ?: "?", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = VertFrais)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(autreParticipant?.nom ?: "Conversation", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                if (dernierMessage?.type == "IMAGE") "📷 Photo"
                                else dernierMessage?.contenu ?: "Demarrer la conversation",
                                color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (dernierMessage != null) {
                            Text(dernierMessage.createdAt.take(10), color = Color.LightGray, fontSize = 11.sp)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(start = 82.dp), color = Color.LightGray.copy(alpha = 0.3f))
                }
            }
        }
    }

    if (vm.afficherNouvelleConv) {
        AlertDialog(
            onDismissRequest = { vm.afficherNouvelleConv = false },
            title = { Text("Nouvelle conversation") },
            text = {
                LazyColumn {
                    items(vm.utilisateurs) { user ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { vm.ouvrirOuCreerConversation(user.id) { convId -> navController.navigate("chat/$convId") } }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (user.photoUrl != null) {
                                AsyncImage(model = user.photoUrl, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            } else {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(OrangeFruit.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                    Text(user.nom.take(1), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OrangeFruit)
                                }
                            }
                            Column {
                                Text(user.nom, fontWeight = FontWeight.Bold)
                                Text(user.role, color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { vm.afficherNouvelleConv = false }) { Text("Annuler") } }
        )
    }
}
