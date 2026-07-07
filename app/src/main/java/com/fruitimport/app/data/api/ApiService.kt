package com.fruitimport.app.data.api

// ============================================================
// FICHIER : data/api/ApiService.kt
// Rôle : Définit tous les appels HTTP vers notre backend.
//        Retrofit lit ces interfaces et génère automatiquement
//        le code pour faire les requêtes réseau.
//
// Chaque fonction correspond à un endpoint de notre API :
// @GET("/stock") → appelle GET http://serveur/stock
// @POST("/auth/login") → appelle POST http://serveur/auth/login
// ============================================================

import com.fruitimport.app.data.models.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── AUTHENTIFICATION
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponse>>

    @POST("auth/register-client")
    suspend fun inscrireClient(@Body request: InscriptionClientRequest): Response<ApiResponse<Client>>

    @POST("fruits")
    suspend fun creerFruit(@Body data: CreerFruitRequest): Response<ApiResponse<Fruit>>

    @PATCH("fruits/{id}")
    suspend fun modifierFruit(@Path("id") id: Int, @Body data: Map<String, String>): Response<ApiResponse<Fruit>>

    @POST("fruits/{id}/calibres")
    suspend fun ajouterCalibre(@Path("id") id: Int, @Body data: AjouterCalibreRequest): Response<ApiResponse<Calibre>>

    @PATCH("fruits/calibres/{calibreId}")
    suspend fun modifierCalibre(@Path("calibreId") id: Int, @Body data: ModifierCalibreRequest): Response<ApiResponse<Calibre>>

    @POST("clients")
    suspend fun creerClient(@Body data: CreerClientSecretaireRequest): Response<ApiResponse<Client>>

    @PATCH("clients/{id}")
    suspend fun modifierClient(@Path("id") id: Int, @Body data: Map<String, String>): Response<ApiResponse<Client>>

    @GET("commandes/{id}")
    suspend fun obtenirDetailCommande(@Path("id") id: Int): Response<ApiResponse<Any>>

    @POST("clients/{id}/versements")
    suspend fun ajouterVersement(@Path("id") id: Int, @Body data: Map<String, Double>): Response<ApiResponse<Any>>

    @POST("stock/pertes")
    suspend fun declarerPerte(@Body data: PerteRequest): Response<ApiResponse<Any>>

    @POST("stock/reception")
    suspend fun receptionMarchandise(@Body data: ReceptionRequest): Response<ApiResponse<Any>>

    @POST("transferts")
    suspend fun demanderTransfert(@Body data: TransfertRequest): Response<ApiResponse<Any>>

    @GET("stock/alertes")
    suspend fun obtenirAlertesStock(@Query("agence_id") agenceId: Int? = null): Response<ApiResponse<Any>>

    @GET("auth/me")
    suspend fun moi(): Response<ApiResponse<User>>

    // ── DASHBOARD
    @GET("dashboard/pdg")
    suspend fun dashboardPDG(): Response<ApiResponse<DashboardPDG>>

    @GET("dashboard/agence/{id}")
    suspend fun dashboardAgence(@Path("id") agenceId: Int): Response<ApiResponse<StatsAgence>>

    // ── AGENCES
    @GET("agences")
    suspend fun obtenirAgences(): Response<ApiResponse<List<Agence>>>

    // ── FRUITS
    @GET("fruits")
    suspend fun obtenirFruits(): Response<ApiResponse<List<Fruit>>>

    @Multipart
    @POST("fruits/{id}/image")
    suspend fun uploadImageFruit(
        @Path("id") id: Int,
        @Part image: MultipartBody.Part
    ): Response<ApiResponse<Fruit>>

    // ── STOCK
    @GET("stock")
    suspend fun obtenirStocks(
        @Query("agence_id") agenceId: Int? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<ApiResponse<Map<String, Any>>>

    @GET("stock/alertes")
    suspend fun obtenirAlertes(
        @Query("agence_id") agenceId: Int? = null
    ): Response<ApiResponse<List<Stock>>>

    @GET("stock/catalogue")
    suspend fun obtenirCatalogue(
        @Query("agence_id") agenceId: Int
    ): Response<ApiResponse<List<Stock>>>

    @POST("stock/pertes")
    suspend fun receptionnerMarchandise(
        @Body data: ReceptionRequest
    ): Response<ApiResponse<Any>>

    @POST("stock/pertes")

    // ── COMMANDES
    @GET("commandes")
    suspend fun obtenirCommandes(
        @Query("agence_id") agenceId: Int? = null,
        @Query("client_id") clientId: Int? = null,
        @Query("statut") statut: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ApiResponse<Map<String, Any>>>

    @GET("commandes/{id}")
    suspend fun obtenirCommande(@Path("id") id: Int): Response<ApiResponse<Commande>>

    @POST("commandes")
    suspend fun creerCommande(@Body data: CreerCommandeRequest): Response<ApiResponse<Commande>>

    @PATCH("commandes/{id}/statut")
    suspend fun changerStatutCommande(
        @Path("id") id: Int,
        @Body data: Map<String, String>
    ): Response<ApiResponse<Commande>>

    // ── LIVRAISONS
    @GET("livraisons")
    suspend fun obtenirLivraisons(
        @Query("livreur_id") livreurId: Int? = null,
        @Query("statut") statut: String? = null,
        @Query("agence_id") agenceId: Int? = null
    ): Response<ApiResponse<Map<String, Any>>>

    @POST("livraisons")
    suspend fun creerLivraison(@Body data: AssignerLivraisonRequest): Response<ApiResponse<Livraison>>

    @PATCH("livraisons/{id}/statut")
    suspend fun mettreAJourLivraison(
        @Path("id") id: Int,
        @Body data: MettreAJourLivraisonRequest
    ): Response<ApiResponse<Livraison>>

    // ── CLIENTS
    @GET("clients/{id}")
    suspend fun obtenirDetailClient(@Path("id") id: Int): Response<ApiResponse<ClientDetail>>

    @POST("retours")
    suspend fun creerRetour(@Body data: RetourRequest): Response<ApiResponse<Any>>

    @GET("chat/non-lus")
    suspend fun obtenirNonLus(): Response<ApiResponse<Any>>

    @GET("chat/utilisateurs")
    suspend fun obtenirUtilisateursChat(): Response<ApiResponse<List<User>>>

    @GET("chat/conversations")
    suspend fun obtenirConversations(): Response<ApiResponse<List<Conversation>>>

    @POST("chat/conversations")
    suspend fun creerConversation(@Body data: Map<String, Int>): Response<ApiResponse<Conversation>>

    @GET("chat/conversations/{id}/messages")
    suspend fun obtenirMessages(@Path("id") id: Int): Response<ApiResponse<List<Message>>>

    @Multipart
    @POST("chat/conversations/{id}/messages")
    suspend fun envoyerMessageAvecImage(
        @Path("id") id: Int,
        @Part("contenu") contenu: okhttp3.RequestBody,
        @Part image: MultipartBody.Part? = null
    ): Response<ApiResponse<Message>>

    @FormUrlEncoded
    @POST("chat/conversations/{id}/messages")
    suspend fun envoyerMessage(
        @Path("id") id: Int,
        @Field("contenu") contenu: String
    ): Response<ApiResponse<Message>>

    @GET("parametres/rapport")
    suspend fun obtenirRapport(): Response<ApiResponse<Any>>

    @Streaming
    @POST("parametres/rapport/generer")
    suspend fun genererRapport(): Response<okhttp3.ResponseBody>

    @GET("parametres")
    suspend fun obtenirParametres(): Response<ApiResponse<Any>>

    @Multipart
    @POST("parametres/logo")
    suspend fun uploaderLogo(@Part logo: MultipartBody.Part): Response<ApiResponse<Any>>

    suspend fun me(): Response<ApiResponse<Any>>

    @Multipart
    @POST("auth/photo-profil")
    suspend fun uploaderPhotoProfil(@Part photo: MultipartBody.Part): Response<ApiResponse<Any>>

    @PATCH("auth/changer-mot-de-passe")
    suspend fun changerMotDePasse(@Body data: ChangerMotDePasseRequest): Response<ApiResponse<Any>>

    @Streaming
    @GET("commandes/{id}/bon-pdf")
    suspend fun telechargerBonPDF(@Path("id") id: Int): Response<ResponseBody>

    @GET("clients")
    suspend fun obtenirClients(
        @Query("agence_id") agenceId: Int? = null,
        @Query("statut_credit") statutCredit: String? = null,
        @Query("page") page: Int = 1
    ): Response<ApiResponse<Map<String, Any>>>

    @GET("clients/{id}")
    suspend fun obtenirClient(@Path("id") id: Int): Response<ApiResponse<Client>>

    @PATCH("clients/{id}/credit-limite")
    suspend fun modifierLimiteCredit(
        @Path("id") id: Int,
        @Body data: ModifierLimiteCreditRequest
    ): Response<ApiResponse<Client>>

    @POST("clients/{id}/paiements")
    suspend fun enregistrerPaiement(
        @Path("id") id: Int,
        @Body data: Map<String, Any>
    ): Response<ApiResponse<Any>>

    // ── TRANSFERTS
    @GET("transferts")
    suspend fun obtenirTransferts(
        @Query("statut") statut: String? = null
    ): Response<ApiResponse<Map<String, Any>>>

    @POST("transferts")
    suspend fun creerTransfert(@Body data: TransfertRequest): Response<ApiResponse<Transfert>>

    @PATCH("transferts/{id}/approuver")
    suspend fun approuverTransfert(@Path("id") id: Int): Response<ApiResponse<Transfert>>

    @PATCH("transferts/{id}/rejeter")
    suspend fun rejeterTransfert(@Path("id") id: Int): Response<ApiResponse<Transfert>>

    // -- UTILISATEURS (PDG)
    @GET("admin/users/livreurs")
    suspend fun obtenirLivreurs(
        @Query("agence_id") agenceId: Int? = null,
        @Query("role") role: String? = null
    ): Response<ApiResponse<Map<String, Any>>>

    @GET("admin/users")
    suspend fun obtenirEmployes(
        @Query("agence_id") agenceId: Int? = null,
        @Query("role") role: String? = null
    ): Response<ApiResponse<Map<String, Any>>>

    @POST("admin/users")
    suspend fun creerEmploye(@Body data: CreerEmployeRequest): Response<ApiResponse<User>>

    @PATCH("admin/users/{id}")
    suspend fun modifierEmploye(
        @Path("id") id: Int,
        @Body data: Map<String, Any>
    ): Response<ApiResponse<User>>
}
