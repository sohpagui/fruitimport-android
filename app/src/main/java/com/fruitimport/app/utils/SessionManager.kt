package com.fruitimport.app.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fruitimport.app.data.api.RetrofitClient
import com.fruitimport.app.data.models.User

object SessionManager {
    var utilisateurConnecte: User? by mutableStateOf(null)
        private set
    var accessToken: String? = null
        private set
    var refreshToken: String? = null
        private set

    fun sauvegarderSession(user: User, access: String, refresh: String) {
        utilisateurConnecte = user
        accessToken = access
        refreshToken = refresh
        RetrofitClient.accessToken = access
    }

    fun effacerSession() {
        utilisateurConnecte = null
        accessToken = null
        refreshToken = null
        RetrofitClient.accessToken = null
    }

    fun estConnecte(): Boolean = accessToken != null
    fun obtenirRole(): String = utilisateurConnecte?.role ?: ""
    fun obtenirAgenceId(): Int? = utilisateurConnecte?.agenceId
    fun estClient(): Boolean = utilisateurConnecte?.isClient == true
    fun mettreAJourPhoto(photoUrl: String) {
        utilisateurConnecte = utilisateurConnecte?.copy(photoUrl = photoUrl)
    }
}
