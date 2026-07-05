package com.fruitimport.app.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.Message
import com.fruitimport.app.ui.components.PhotoViewer
import com.fruitimport.app.ui.theme.OrangeFruit
import com.fruitimport.app.ui.theme.VertFrais
import com.fruitimport.app.utils.SessionManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ChatViewModel(private val conversationId: Int) : ViewModel() {
    var messages by mutableStateOf<List<Message>>(emptyList())
    var chargement by mutableStateOf(true)
    var envoi by mutableStateOf(false)

    init {
        charger()
        pollingMessages()
    }

    fun charger() {
        viewModelScope.launch {
            try {
                val rep = RetrofitClient.instance.obtenirMessages(conversationId)
                if (rep.isSuccessful) {
                    val json = Gson().toJson(rep.body()?.data)
                    messages = Gson().fromJson(json, object : TypeToken<List<Message>>() {}.type) ?: emptyList()
                }
            } catch (e: Exception) {}
            chargement = false
        }
    }

    private fun pollingMessages() {
        viewModelScope.launch {
            while (true) {
                delay(3000)
                try {
                    val rep = RetrofitClient.instance.obtenirMessages(conversationId)
                    if (rep.isSuccessful) {
                        val json = Gson().toJson(rep.body()?.data)
                        val nouveaux = Gson().fromJson<List<Message>>(json, object : TypeToken<List<Message>>() {}.type) ?: emptyList()
                        if (nouveaux.size != messages.size) messages = nouveaux
                    }
                } catch (e: Exception) {}
            }
        }
    }

    fun envoyerTexte(contenu: String, onDone: () -> Unit) {
        if (contenu.isBlank()) return
        viewModelScope.launch {
            envoi = true
            try {
                RetrofitClient.instance.envoyerMessage(conversationId, contenu)
                charger()
                onDone()
            } catch (e: Exception) {}
            envoi = false
        }
    }

    fun envoyerImage(context: android.content.Context, uri: Uri) {
        viewModelScope.launch {
            envoi = true
            try {
                val stream = context.contentResolver.openInputStream(uri)
                val bytes = stream?.readBytes() ?: return@launch
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("image", "image.jpg", requestBody)
                val contenuBody = "".toRequestBody("text/plain".toMediaTypeOrNull())
                RetrofitClient.instance.envoyerMessageAvecImage(conversationId, contenuBody, part)
                charger()
            } catch (e: Exception) {}
            envoi = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranChat(navController: NavController, conversationId: Int, nomContact: String = "") {
    val vm = viewModel<ChatViewModel>(key = conversationId.toString(), factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ChatViewModel(conversationId) as T
    })
    val myId = SessionManager.utilisateurConnecte?.id ?: 0
    var texte by remember { mutableStateOf("") }
    var fondCouleur by remember { mutableStateOf(Color(0xFFECE5DD)) }
    var afficherFonds by remember { mutableStateOf(false) }
    var photoAGrandir by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.envoyerImage(context, it) }
    }

    LaunchedEffect(vm.messages.size) {
        if (vm.messages.isNotEmpty()) listState.animateScrollToItem(vm.messages.size - 1)
    }

    photoAGrandir?.let { url ->
        PhotoViewer(url = url, onFermer = { photoAGrandir = null })
    }

    // Dialog choix fond
    if (afficherFonds) {
        AlertDialog(
            onDismissRequest = { afficherFonds = false },
            title = { Text("Fond de conversation") },
            text = {
                val fonds = listOf(
                    Color(0xFFECE5DD) to "Classique WhatsApp",
                    Color(0xFFE8F5E9) to "Vert clair",
                    Color(0xFFE3F2FD) to "Bleu clair",
                    Color(0xFFFFF8E1) to "Jaune doux",
                    Color(0xFFF3E5F5) to "Mauve doux",
                    Color(0xFFFFEBEE) to "Rose doux",
                    Color(0xFFF5F5F5) to "Gris clair",
                    Color(0xFF1C1B1F) to "Sombre"
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    fonds.forEach { (couleur, nom) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { fondCouleur = couleur; afficherFonds = false }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(couleur))
                            Text(nom)
                            if (fondCouleur == couleur) Icon(Icons.Default.Check, null, tint = VertFrais)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Text(nomContact, fontWeight = FontWeight.Bold, color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { afficherFonds = true }) {
                        Icon(Icons.Default.Palette, null, tint = Color.White)
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = VertFrais)
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp, vertical = 6.dp).imePadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(onClick = { launcher.launch("image/*") }) {
                    Icon(Icons.Default.Image, null, tint = Color.Gray)
                }
                OutlinedTextField(
                    value = texte,
                    onValueChange = { texte = it },
                    placeholder = { Text("Message...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { vm.envoyerTexte(texte) { texte = "" }; keyboard?.hide() }),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VertFrais, unfocusedBorderColor = Color.LightGray)
                )
                FloatingActionButton(
                    onClick = { vm.envoyerTexte(texte) { texte = "" }; keyboard?.hide() },
                    containerColor = VertFrais,
                    modifier = Modifier.size(44.dp)
                ) {
                    if (vm.envoi) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Icon(Icons.Default.Send, null, tint = Color.White)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(fondCouleur).padding(padding)) {
            if (vm.chargement) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = VertFrais) }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(vm.messages) { msg ->
                        val estMoi = msg.senderId == myId
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (estMoi) Arrangement.End else Arrangement.Start
                        ) {
                            if (!estMoi) {
                                if (msg.sender?.photoUrl != null) {
                                    AsyncImage(model = msg.sender.photoUrl, contentDescription = null, modifier = Modifier.size(32.dp).clip(CircleShape).align(Alignment.Bottom), contentScale = ContentScale.Crop)
                                } else {
                                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(OrangeFruit.copy(alpha = 0.3f)).align(Alignment.Bottom), contentAlignment = Alignment.Center) {
                                        Text(msg.sender?.nom?.take(1) ?: "?", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OrangeFruit)
                                    }
                                }
                                Spacer(Modifier.width(6.dp))
                            }
                            Column(
                                modifier = Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(
                                    topStart = if (estMoi) 18.dp else 4.dp,
                                    topEnd = if (estMoi) 4.dp else 18.dp,
                                    bottomStart = 18.dp, bottomEnd = 18.dp
                                )).background(if (estMoi) Color(0xFFDCF8C6) else Color.White).padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                if (!estMoi) {
                                    Text(msg.sender?.nom ?: "", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OrangeFruit)
                                }
                                if (msg.type == "IMAGE" && msg.imageUrl != null) {
                                    AsyncImage(
                                        model = msg.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(200.dp).clip(RoundedCornerShape(8.dp)).clickable { photoAGrandir = msg.imageUrl },
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(msg.contenu, fontSize = 15.sp, color = Color(0xFF1C1B1F))
                                }
                                Row(modifier = Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(msg.createdAt.take(16).replace("T", " ").takeLast(5), fontSize = 10.sp, color = Color.Gray)
                                    if (estMoi) Text(if (msg.lu) "✓✓" else "✓", fontSize = 10.sp, color = if (msg.lu) Color(0xFF4FC3F7) else Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
