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
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── AUTHENTIFICATION
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponse>>

    @POST("auth/register-client")
    suspend fun inscrireClient(@Body request: InscriptionClientRequest): Response<ApiResponse<Client>>

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

    @POST("stock/reception")
    suspend fun receptionnerMarchandise(
        @Body data: ReceptionRequest
    ): Response<ApiResponse<Any>>

    @POST("stock/pertes")
    suspend fun declarerPerte(
        @Body data: PerteRequest
    ): Response<ApiResponse<Any>>

    // ── COMMANDES
    @GET("commandes")
    suspend fun obtenirCommandes(
        @Query("agence_id") agenceId: Int? = null,
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
    suspend fun creerLivraison(@Body data: Map<String, Int>): Response<ApiResponse<Livraison>>

    @PATCH("livraisons/{id}/statut")
    suspend fun mettreAJourLivraison(
        @Path("id") id: Int,
        @Body data: Map<String, String>
    ): Response<ApiResponse<Livraison>>

    // ── CLIENTS
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
        @Body data: Map<String, Double>
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

    // ── UTILISATEURS (PDG)
    @GET("admin/users")
    suspend fun obtenirEmployes(
        @Query("agence_id") agenceId: Int? = null,
        @Query("role") role: String? = null
    ): Response<ApiResponse<Map<String, Any>>>

    @POST("admin/users")
    suspend fun creerEmploye(@Body data: Map<String, Any>): Response<ApiResponse<User>>

    @PATCH("admin/users/{id}")
    suspend fun modifierEmploye(
        @Path("id") id: Int,
        @Body data: Map<String, Any>
    ): Response<ApiResponse<User>>
}
