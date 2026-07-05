package com.fruitimport.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.navigation.Routes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BoutonMessages(navController: NavController) {
    var nonLus by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                val rep = RetrofitClient.instance.obtenirNonLus()
                if (rep.isSuccessful) {
                    val data = rep.body()?.data as? Map<*, *>
                    nonLus = (data?.get("count") as? Double)?.toInt() ?: 0
                }
            } catch (e: Exception) {}
            delay(5000)
        }
    }

    Box {
        IconButton(onClick = { navController.navigate(Routes.CONVERSATIONS) }) {
            Icon(Icons.Default.Message, null, tint = Color.White)
        }
        if (nonLus > 0) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp).size(16.dp).background(Color.Red, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(if (nonLus > 9) "9+" else nonLus.toString(), color = Color.White, fontSize = 9.sp)
            }
        }
    }
}
