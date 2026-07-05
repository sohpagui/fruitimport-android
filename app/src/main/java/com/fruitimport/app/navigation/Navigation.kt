package com.fruitimport.app.navigation

// ============================================================
// FICHIER : navigation/Navigation.kt
// Rôle : Définit la navigation entre les écrans de l'app.
//        Selon le rôle de l'utilisateur connecté, il voit
//        un graphe de navigation différent.
// ============================================================

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.fruitimport.app.ui.auth.EcranConnexion
import com.fruitimport.app.ui.auth.EcranInscriptionClient
import com.fruitimport.app.ui.pdg.EcranDashboardPDG
import com.fruitimport.app.ui.pdg.EcranGraphiques
import com.fruitimport.app.ui.pdg.EcranParametresPDG
import com.fruitimport.app.ui.pdg.EcranEmployes
import com.fruitimport.app.ui.pdg.EcranTransfertsPDG
import com.fruitimport.app.ui.pdg.EcranClientsPDG
import com.fruitimport.app.ui.secretaire.EcranDashboardSecretaire
import com.fruitimport.app.ui.secretaire.EcranCommandes
import com.fruitimport.app.ui.secretaire.EcranNouvelleCommande
import com.fruitimport.app.ui.secretaire.EcranLivraisons
import com.fruitimport.app.ui.secretaire.EcranRetours
import com.fruitimport.app.ui.secretaire.EcranArrivages
import com.fruitimport.app.ui.chat.EcranConversations
import com.fruitimport.app.ui.chat.EcranChat
import com.fruitimport.app.ui.magasinier.EcranDashboardMagasinier
import com.fruitimport.app.ui.magasinier.EcranStock
import com.fruitimport.app.ui.magasinier.EcranReception
import com.fruitimport.app.ui.magasinier.EcranPertes
import com.fruitimport.app.ui.magasinier.EcranTransferts
import com.fruitimport.app.ui.livreur.EcranDashboardLivreur
import com.fruitimport.app.ui.client.EcranDashboardClient
import com.fruitimport.app.ui.client.EcranCatalogue
import com.fruitimport.app.ui.components.EcranProfil
import com.fruitimport.app.ui.client.EcranMesCommandes
import com.fruitimport.app.utils.SessionManager

// Routes de navigation — chaque écran a une route unique
object Routes {
    const val CONNEXION = "connexion"
    const val INSCRIPTION_CLIENT = "inscription_client"

    // PDG
    const val DASHBOARD_PDG = "dashboard_pdg"
    const val EMPLOYES = "employes"
    const val CLIENTS_PDG = "clients_pdg"
    const val TRANSFERTS_PDG = "transferts_pdg"

    // Secrétaire
    const val DASHBOARD_SECRETAIRE = "dashboard_secretaire"
    const val COMMANDES = "commandes"
    const val NOUVELLE_COMMANDE = "nouvelle_commande"
    const val LIVRAISONS = "livraisons"

    // Magasinier
    const val DASHBOARD_MAGASINIER = "dashboard_magasinier"
    const val STOCK = "stock"
    const val RECEPTION = "reception"
    const val PERTES = "pertes"
    const val TRANSFERTS = "transferts"

    // Livreur
    const val DASHBOARD_LIVREUR = "dashboard_livreur"

    // Client
    const val DASHBOARD_CLIENT = "dashboard_client"
        const val PROFIL = "profil"
        const val GRAPHIQUES = "graphiques"
        const val PARAMETRES_PDG = "parametres_pdg"
        const val RETOURS = "retours"
        const val ARRIVAGES = "arrivages"
        const val CONVERSATIONS = "conversations"
        const val CHAT = "chat"
    const val CATALOGUE = "catalogue"
    const val MES_COMMANDES = "mes_commandes"
}

@Composable
fun NavigationPrincipale(navController: NavHostController) {
    // L'écran de départ dépend de si l'utilisateur est connecté
    val departRoute = if (SessionManager.estConnecte()) {
        obtenirRouteAccueil(SessionManager.obtenirRole())
    } else {
        Routes.CONNEXION
    }

    NavHost(navController = navController, startDestination = departRoute) {

        // ── Écrans publics
        composable(Routes.CONNEXION) {
            EcranConnexion(navController)
        }
        composable(Routes.INSCRIPTION_CLIENT) {
            EcranInscriptionClient(navController)
        }

        // ── Écrans PDG
        composable(Routes.DASHBOARD_PDG) {
            EcranDashboardPDG(navController)
        }
        composable(Routes.EMPLOYES) {
            EcranEmployes(navController)
        }
        composable(Routes.CLIENTS_PDG) {
            EcranClientsPDG(navController)
        }
        composable(Routes.TRANSFERTS_PDG) {
            EcranTransfertsPDG(navController)
        }

        // ── Écrans Secrétaire
        composable(Routes.DASHBOARD_SECRETAIRE) {
            EcranDashboardSecretaire(navController)
        }
        composable(Routes.COMMANDES) {
            EcranCommandes(navController)
        }
        composable(Routes.NOUVELLE_COMMANDE) {
            EcranNouvelleCommande(navController)
        }
        composable(Routes.LIVRAISONS) {
            EcranLivraisons(navController)
        }

        // ── Écrans Magasinier
        composable(Routes.DASHBOARD_MAGASINIER) {
            EcranDashboardMagasinier(navController)
        }
        composable(Routes.STOCK) {
            EcranStock(navController)
        }
        composable(Routes.RECEPTION) {
            EcranReception(navController)
        }
        composable(Routes.PERTES) {
            EcranPertes(navController)
        }
        composable(Routes.TRANSFERTS) {
            EcranTransferts(navController)
        }

        // ── Écrans Livreur
        composable(Routes.DASHBOARD_LIVREUR) {
            EcranDashboardLivreur(navController)
        }

        // ── Écrans Client
        composable(Routes.PROFIL) { EcranProfil(navController) }
        composable(Routes.GRAPHIQUES) { EcranGraphiques(navController) }
        composable(Routes.PARAMETRES_PDG) { EcranParametresPDG(navController) }
        composable(Routes.RETOURS) { EcranRetours(navController) }
        composable(Routes.ARRIVAGES) { EcranArrivages(navController) }
        composable(Routes.CONVERSATIONS) { EcranConversations(navController) }
        composable("chat/{convId}") { back ->
            val convId = back.arguments?.getString("convId")?.toIntOrNull() ?: 0
            EcranChat(navController, convId)
        }
        composable(Routes.DASHBOARD_CLIENT) {
            EcranDashboardClient(navController)
        }
        composable(Routes.CATALOGUE) {
            EcranCatalogue(navController)
        }
        composable(Routes.MES_COMMANDES) {
            EcranMesCommandes(navController)
        }
    }
}

// Détermine l'écran d'accueil selon le rôle
fun obtenirRouteAccueil(role: String): String = when (role) {
    "PDG" -> Routes.DASHBOARD_PDG
    "SECRETAIRE" -> Routes.DASHBOARD_SECRETAIRE
    "MAGASINIER" -> Routes.DASHBOARD_MAGASINIER
    "LIVREUR" -> Routes.DASHBOARD_LIVREUR
    "CLIENT_PARTICULIER", "CLIENT_SUPERMARCHE" -> Routes.DASHBOARD_CLIENT
    else -> Routes.CONNEXION
}
