package com.fruitimport.app.utils

// ============================================================
// FICHIER : utils/Extensions.kt
// Rôle : Fonctions utilitaires réutilisables partout.
// ============================================================

import java.text.NumberFormat
import java.util.Locale

// Formate un montant en FCFA : 150000 → "150 000 FCFA"
fun Double.toFCFA(): String {
    val format = NumberFormat.getNumberInstance(Locale.FRANCE)
    return "${format.format(this.toLong())} FCFA"
}

// Traduit les statuts en français lisible
fun String.traduireStatut(): String = when (this) {
    "EN_ATTENTE" -> "En attente"
    "CONFIRMEE" -> "Confirmée"
    "PREPAREE" -> "Préparée"
    "EN_LIVRAISON" -> "En livraison"
    "LIVREE" -> "Livrée"
    "ANNULEE" -> "Annulée"
    "PREPARE" -> "Préparée"
    "EN_ROUTE" -> "En route"
    "LIVRE" -> "Livrée"
    "PROBLEME" -> "Problème"
    "APPROUVE" -> "Approuvé"
    "REJETE" -> "Rejeté"
    "EN_REGLE" -> "En règle"
    "A_RELANCER" -> "À relancer"
    "EN_RETARD" -> "En retard"
    "ESPECES" -> "Espèces"
    "CREDIT" -> "Crédit"
    "NORMAL" -> "Normal"
    "SOLDE" -> "Soldé"
    "PDG" -> "PDG"
    "SECRETAIRE" -> "Secrétaire"
    "MAGASINIER" -> "Magasinier"
    "LIVREUR" -> "Livreur"
    "CLIENT_PARTICULIER" -> "Client Particulier"
    "CLIENT_SUPERMARCHE" -> "Client Supermarché"
    else -> this
}

// Couleur selon le statut de crédit
fun String.couleurStatutCredit(): androidx.compose.ui.graphics.Color = when (this) {
    "EN_REGLE" -> androidx.compose.ui.graphics.Color(0xFF10B981)
    "A_RELANCER" -> androidx.compose.ui.graphics.Color(0xFFF59E0B)
    "EN_RETARD" -> androidx.compose.ui.graphics.Color(0xFFEF4444)
    else -> androidx.compose.ui.graphics.Color.Gray
}
