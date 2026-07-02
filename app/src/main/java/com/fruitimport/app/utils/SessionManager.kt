package com.fruitimport.app.utils

// ============================================================
// FICHIER : utils/SessionManager.kt
// Rôle : Gère la session de l'utilisateur connecté.
//        Stocke le token JWT et les infos de l'utilisateur
//        en mémoire pour la durée de la session.
// ============================================================

import com.fruitimport.app.data.models.User
import com.fruitimport.app.data.api.RetrofitClient

object SessionManager {

    var utilisateurConnecte: User? = null
        private set

    var accessToken: String? = null
        private set

    var refreshToken: String? = null
        private set

    // Appelé après une connexion réussie
    fun sauvegarderSession(user: User, access: String, refresh: String) {
        utilisateurConnecte = user
        accessToken = access
        refreshToken = refresh
        RetrofitClient.accessToken = access  // Injecte dans Retrofit
    }

    // Déconnexion
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
}
