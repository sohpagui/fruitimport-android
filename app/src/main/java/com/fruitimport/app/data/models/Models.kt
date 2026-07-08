package com.fruitimport.app.data.models

data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val error: String? = null
)

data class User(
    val id: Int,
    val nom: String,
    val prenom: String? = null,
    val telephone: String,
    val email: String? = null,
    val role: String,
    val agenceId: Int? = null,
    val isClient: Boolean = false,
    val agence: Agence? = null,
    val actif: Boolean = true,
    val photoUrl: String? = null,
    val limiteCredit: String? = null,
    val creditUtilise: String? = null,
    val statutCredit: String? = null,
    val dateEcheance: String? = null,
    val tauxInteretMensuel: String? = null
)

data class Client(
    val id: Int,
    val nom: String,
    val type: String,
    val telephone: String,
    val email: String? = null,
    val adresse: String? = null,
    val limiteCredit: Double = 0.0,
    val creditUtilise: Double = 0.0,
    val statutCredit: String = "EN_REGLE",
    val dateEcheance: String? = null,
    val agenceId: Int,
    val agence: Agence? = null,
    val lignes: List<LigneCommande>? = null,
    val date: String? = null
)

data class Agence(
    val id: Int,
    val nom: String,
    val ville: String? = null,
    val adresse: String? = null,
    val telephone: String? = null
)

data class Fruit(
    val id: Int,
    val nom: String,
    val uniteMesure: String,
    val imageUrl: String? = null,
    val calibres: List<Calibre> = emptyList()
)

data class Calibre(
    val id: Int,
    val fruitId: Int,
    val valeur: String,
    val prixAchat: Double = 0.0,
    val prixVente: Double = 0.0,
    val ordreAffichage: Int = 0
)

data class Stock(
    val id: Int,
    val agenceId: Int,
    val fruitId: Int,
    val calibreId: Int,
    val origine: String,
    val categorie: String,
    val quantiteCartons: Int,
    val prixUnitaire: Double,
    val fruit: Fruit? = null,
    val calibre: Calibre? = null,
    val agence: Agence? = null,
    val lignes: List<LigneCommande>? = null,
    val date: String? = null
)

data class Commande(
    val id: Int,
    val numero: String,
    val agenceId: Int,
    val clientId: Int,
    val modePaiement: String,
    val statut: String,
    val montantTotal: Double,
    val adresseLivraison: String? = null,
    val note: String? = null,
    val date: String,
    val client: Client? = null,
    val agence: Agence? = null,
    val lignes: List<LigneCommande> = emptyList(),
    val livraison: Livraison? = null
)

data class LigneCommande(
    val id: Int,
    val commandeId: Int,
    val fruitId: Int,
    val calibreId: Int,
    val categorie: String,
    val quantite: Int,
    val prixUnitaire: Double,
    val sousTotal: Double,
    val fruit: Fruit? = null,
    val calibre: Calibre? = null
)

data class Livraison(
    val id: Int,
    val commandeId: Int,
    val livreurId: Int,
    val statut: String,
    val noteProbleme: String? = null,
    val dateAssignation: String,
    val dateLivraison: String? = null,
    val livreur: User? = null,
    val commande: Commande? = null
)

data class Transfert(
    val id: Int,
    val agenceSourceId: Int,
    val agenceDestinationId: Int,
    val fruitId: Int,
    val calibreId: Int,
    val quantite: Int,
    val statut: String,
    val note: String? = null,
    val dateDemande: String,
    val agenceSource: Agence? = null,
    val agenceDestination: Agence? = null,
    val fruit: Fruit? = null,
    val calibre: Calibre? = null,
    val demandeur: User? = null
)

data class LoginRequest(val identifiant: String, val motDePasse: String)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val utilisateur: User
)

data class DashboardPDG(
    val kpis: KPIs,
    val comparaison: Comparaison,
    val alertes: Alertes,
    val synthese: List<String>,
    val topFruits: List<TopFruit> = emptyList()
)

data class KPIs(
    val ventesTotalesJour: Double,
    val nbCommandesJour: Int,
    val creancesTotales: Double
)

data class Comparaison(
    val douala: StatsAgence,
    val yaounde: StatsAgence,
    val meilleureAgence: String
)

data class StatsAgence(
    val agenceId: Int,
    val ventesJour: VentesJour,
    val stockTotal: Int,
    val nbClients: Int,
    val nbEmployes: Int,
    val perteMois: Double,
    val livraisonsEnCours: Int,
    val topFruits: List<TopFruit> = emptyList()
)

data class VentesJour(val montant: Double, val nbCommandes: Int)
data class TopFruit(val fruit: Fruit, val montant: Double, val quantite: Int)

data class Alertes(
    val clientsEnRetard: Int,
    val clientsARelancer: Int,
    val transfertsEnAttente: Int,
    val commandesEnAttente: Int,
    val stockBas: Int
)

data class InscriptionClientRequest(
    val nom: String,
    val telephone: String,
    val type: String,
    val agenceId: Int,
    val motDePasse: String,
    val email: String? = null,
    val adresse: String? = null
)

data class LigneCommandeRequest(
    val fruitId: Int,
    val calibreId: Int,
    val categorie: String,
    val quantite: Int,
    val prixUnitaire: Double
)

data class CreerCommandeRequest(
    val agenceId: Int,
    val clientId: Int,
    val modePaiement: String,
    val adresseLivraison: String,
    val lignes: List<LigneCommandeRequest>
)

data class ReceptionRequest(
    val agenceId: Int,
    val fruitId: Int,
    val calibreId: Int,
    val origine: String,
    val cartonsNormal: Int,
    val prixNormal: Double,
    val cartonsSolde: Int = 0,
    val prixSolde: Double? = null
)

data class PerteRequest(
    val agenceId: Int,
    val fruitId: Int,
    val calibreId: Int,
    val origine: String,
    val categorie: String,
    val quantite: Int,
    val raison: String
)

data class TransfertRequest(
    val agenceDestinationId: Int,
    val fruitId: Int,
    val calibreId: Int,
    val quantite: Int,
    val note: String? = null
)

data class ClientDetail(
    val id: Int,
    val nom: String,
    val type: String,
    val telephone: String,
    val email: String? = null,
    val adresse: String? = null,
    val limiteCredit: String = "0",
    val creditUtilise: String = "0",
    val statutCredit: String = "EN_REGLE",
    val dateEcheance: String? = null,
    val tauxInteretMensuel: String = "0",
    val agence: Agence? = null,
    val lignes: List<LigneCommande>? = null,
    val date: String? = null
)

data class CreerEmployeRequest(
    val nom: String,
    val telephone: String,
    val motDePasse: String,
    val role: String,
    val agenceId: Int
)

data class ModifierLimiteCreditRequest(
    val limiteCredit: Double
)

data class AssignerLivraisonRequest(
    val commandeId: Int,
    val livreurId: Int
)

data class MettreAJourLivraisonRequest(
    val statut: String,
    val noteProbleme: String? = null
)

data class ChangerMotDePasseRequest(
    val ancienMotDePasse: String,
    val nouveauMotDePasse: String
)

data class RetourRequest(
    val livraisonId: Int,
    val fruitId: Int,
    val calibreId: Int,
    val quantite: Int,
    val raison: String
)

data class MessageSender(
    val id: Int,
    val nom: String,
    val photoUrl: String? = null
)

data class Message(
    val id: Int,
    val conversationId: Int,
    val senderId: Int,
    val contenu: String,
    val type: String = "TEXTE",
    val imageUrl: String? = null,
    val lu: Boolean = false,
    val createdAt: String,
    val sender: MessageSender? = null
)

data class ConversationUser(
    val id: Int,
    val nom: String,
    val actif: Boolean = true,
    val photoUrl: String? = null,
    val role: String = ""
)

data class ConversationParticipant(
    val id: Int,
    val userId: Int,
    val user: ConversationUser? = null
)

data class Conversation(
    val id: Int,
    val nom: String? = null,
    val type: String = "PRIVE",
    val createdAt: String,
    val participants: List<ConversationParticipant> = emptyList(),
    val messages: List<Message> = emptyList()
)

data class CreerFruitRequest(
    val nom: String,
    val uniteMesure: String
)

data class AjouterCalibreRequest(
    val valeur: String,
    val prixAchat: Double,
    val prixVente: Double,
    val ordreAffichage: Int = 0
)

data class ModifierCalibreRequest(
    val prixVente: Double
)

data class CreerClientSecretaireRequest(
    val nom: String,
    val telephone: String,
    val type: String,
    val agenceId: Int,
    val email: String? = null,
    val adresse: String? = null,
    val limiteCredit: Double = 0.0
)

data class FixerEcheanceRequest(
    val dateEcheance: String,
    val tauxInteretMensuel: Int = 0
)
