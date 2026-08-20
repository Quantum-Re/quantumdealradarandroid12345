package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

enum class AppLanguage(val code: String, val label: String, val flag: String) {
    IT("it", "Italiano", "🇮🇹"),
    EN("en", "English", "🇬🇧")
}

class AppStrings(val language: AppLanguage) {
    val isItalian = language == AppLanguage.IT

    // Navigation
    val navRadar = if (isItalian) "Radar" else "Radar"
    val navMyProperties = if (isItalian) "I Miei Immobili" else "My Properties"
    val navInsights = if (isItalian) "Insights" else "Insights"
    val navMap = if (isItalian) "Mappa" else "Map"
    val navBrief = if (isItalian) "Brief" else "Brief"
    val navSources = if (isItalian) "Fonti" else "Sources"
    val navRoi = if (isItalian) "ROI" else "ROI"
    val navAnalytics = if (isItalian) "Analytics" else "Analytics"

    // Header & Global App
    val appTitle = "Quantum Deal Radar"
    val appSubtitle = if (isItalian) "Analisi Opportunità Immobiliare" else "Real Estate Opportunity Analytics"

    // Filters
    val searchPlaceholder = if (isItalian) "Cerca città, via, codice asta, tipologia..." else "Search city, address, auction code, type..."
    val filterAll = if (isItalian) "Tutti gli Immobili" else "All Properties"
    val filterTargetBrief = if (isItalian) "Target Brief" else "Target Brief"
    val filterHighDiscount = if (isItalian) "Sconto >25%" else "Discount >25%"
    val filterAuctionNpl = if (isItalian) "Aste & NPL" else "Auctions & NPL"
    val filterBookmarked = if (isItalian) "Salvati" else "Bookmarked"

    val sourceAll = if (isItalian) "Tutte le Fonti" else "All Sources"
    val typeAll = if (isItalian) "Tutte le Tipologie" else "All Types"

    // Metrics
    val dealsFound = if (isItalian) "Opp. Trovate" else "Deals Found"
    val avgDiscount = if (isItalian) "Sconto Medio" else "Avg Discount"
    val avgCapRate = if (isItalian) "Cap Rate Medio" else "Avg Cap Rate"

    // Actions & Badges
    val addDeal = if (isItalian) "Aggiungi Deal" else "Add Deal"
    val hideExpiredAuctions = if (isItalian) "Nascondi Aste Scadute" else "Hide Expired Auctions"
    val showExpiredAuctions = if (isItalian) "Mostra Aste Scadute" else "Show Expired Auctions"
    val noDealsFound = if (isItalian) "Nessun immobile trovato con i filtri selezionati." else "No properties found with selected filters."

    val calculateRoi = if (isItalian) "Calcola ROI" else "Calculate ROI"
    val details = if (isItalian) "Dettagli" else "Details"
    val priceLabel = if (isItalian) "Prezzo:" else "Price:"
    val marketValueLabel = if (isItalian) "Stima Mercato:" else "Market Value:"
    val sqmShort = if (isItalian) "mq" else "sqm"

    // Map View
    val mapViewTitle = if (isItalian) "Mappa Opportunità" else "Opportunity Map"
    val mapViewSubtitle = if (isItalian) "Esplora gli immobili sulla mappa interattiva" else "Explore properties on the interactive map"

    // Investor Brief & Firebase Account
    val briefAreaTitle = if (isItalian) "Area Investitore & Brief" else "Investor Area & Brief"
    val briefAreaSubtitle = if (isItalian) "Profilo professionale Firebase e criteri di acquisizione" else "Firebase professional profile & acquisition criteria"
    val verifiedInvestor = if (isItalian) "Investitore Verificato" else "Verified Investor"
    val guestUser = if (isItalian) "Ospite" else "Guest"
    val allocableCapital = if (isItalian) "Capitale Allocabile:" else "Allocable Capital:"
    val manageAccount = if (isItalian) "Gestisci Account" else "Manage Account"
    val signInRegister = if (isItalian) "Accedi / Registrati" else "Sign In / Register"
    val signOut = if (isItalian) "Disconnetti" else "Sign Out"

    val briefSectionTitle = if (isItalian) "Brief di Ricerca Investitore" else "Investor Acquisition Brief"
    val briefActive = if (isItalian) "Attivo" else "Active"
    val briefPaused = if (isItalian) "Pausa" else "Paused"
    val briefDesc = if (isItalian)
        "Imposta le tue regole di investimento per filtrare ed evidenziare automaticamente gli immobili nello stream Radar e nella Mappa."
    else
        "Set your investment rules to automatically filter and highlight properties in the Radar feed and Map."

    val targetLocationsLabel = if (isItalian) "Città e Zone Target (separate da virgola)" else "Target Cities & Zones (comma separated)"
    val quickLocations = if (isItalian) "Esempi rapidi: " else "Quick suggestions: "
    val targetTypesLabel = if (isItalian) "Tipologie Immobile / Operazione Target" else "Target Property / Operation Types"
    val maxBudgetLabel = if (isItalian) "Budget Max (€)" else "Max Budget (€)"
    val minDiscountLabel = if (isItalian) "Sconto Min (%)" else "Min Discount (%)"
    val targetRoiLabel = if (isItalian) "Target ROI (%)" else "Target ROI (%)"
    val maxRenovationLabel = if (isItalian) "Ristruttur. Max (€)" else "Max Renovation (€)"
    val strategyLabel = if (isItalian) "Strategia di Investimento Prevalente:" else "Primary Investment Strategy:"
    val instantAlerts = if (isItalian) "Notifiche Immediati Match" else "Instant Match Alerts"
    val instantAlertsDesc = if (isItalian) "Avviso quando lo scraper trova nuovi immobili in target" else "Alert when the scraper finds new matching properties"
    val saveBriefButton = if (isItalian) "Aggiorna & Salva Brief Investitore" else "Update & Save Investor Brief"

    val matchedTitle = if (isItalian) "Opportunità In Target Brief" else "Brief-Matched Opportunities"
    val matchedFoundCount = if (isItalian) "Immobili Trovati" else "Properties Found"
    val totalVolumeTarget = if (isItalian) "Volume Totale Target" else "Total Target Volume"
    val preferredStrategy = if (isItalian) "Strategia Preferita" else "Preferred Strategy"
    val noMatchedDeals = if (isItalian)
        "Nessun immobile nel database rispetta tutti i criteri stringenti del brief. Prova ad ampliare il budget massimo o le zone target."
    else
        "No properties match all strict criteria in the brief. Try expanding max budget or target zones."

    // Firebase Dialog
    val authDialogSignUpTitle = if (isItalian) "Registrazione Firebase Investitore" else "Firebase Investor Registration"
    val authDialogSignInTitle = if (isItalian) "Accedi all'Account Investitore" else "Sign In to Investor Account"
    val tabSignUp = if (isItalian) "Nuova Registrazione" else "New Registration"
    val tabSignIn = if (isItalian) "Accedi (Login)" else "Sign In (Login)"
    val emailLabel = if (isItalian) "Email Aziendale / PEC" else "Business / Official Email"
    val passwordLabel = if (isItalian) "Password Firebase" else "Firebase Password"
    val fullNameLabel = if (isItalian) "Nome e Cognome Referente" else "Contact Person Full Name"
    val companyNameLabel = if (isItalian) "Nome Società / Fondo" else "Company / Fund Name"
    val categoryLabel = if (isItalian) "Categoria Investitore" else "Investor Category"
    val capitalAllocatedLabel = if (isItalian) "Capitale Allocato (€)" else "Allocated Capital (€)"
    val cancel = if (isItalian) "Annulla" else "Cancel"
    val createFirebaseAccount = if (isItalian) "Crea Account Firebase" else "Create Firebase Account"
    val signIn = if (isItalian) "Accedi" else "Sign In"
    val firebaseSimNote = if (isItalian)
        "Firebase Auth attivo in modalità di simulazione locale (pronto per google-services.json)"
    else
        "Firebase Auth active in local simulation mode (ready for google-services.json)"

    // Parser / Sources Sandbox
    val sourcesTitle = if (isItalian) "Scraper & Parser Sandbox" else "Scraper & Parser Sandbox"
    val sourcesSubtitle = if (isItalian)
        "Integrazione feed portali immobiliari e validazione parser"
    else
        "Real estate portal feeds integration & parser validation"
    val validateSourcesButton = if (isItalian) "Valida Tutte le Configurazioni" else "Validate All Configurations"
    val activeSources = if (isItalian) "Sorgenti Attive" else "Active Sources"
    val warningSources = if (isItalian) "In Allerta" else "Warning Status"
    val errorSources = if (isItalian) "Errore Format" else "Format Error"
    val testExtractor = if (isItalian) "Test Estrattore" else "Test Extractor"
    val editRules = if (isItalian) "Modifica Regole" else "Edit Rules"
    val statusLabel = if (isItalian) "Stato:" else "Status:"
    val jsonTestTitle = if (isItalian) "Test Estrattore Regole JSON" else "JSON Extractor Rules Test"
    val runTestExtraction = if (isItalian) "Esegui Test Estrazione" else "Run Test Extraction"
    val parserExecutionLogs = if (isItalian) "Log di Esecuzione Parser" else "Parser Execution Logs"
    val parsedResult = if (isItalian) "Risultato Parsato:" else "Parsed Result:"

    // ROI Calculator
    val roiTitle = if (isItalian) "Simulatore ROI & Cash Flow" else "ROI & Cash Flow Simulator"
    val roiSubtitle = if (isItalian)
        "Calcola rendimento, margine flip e cash-on-cash"
    else
        "Calculate yield, flip margin, and cash-on-cash return"
    val purchasePrice = if (isItalian) "Prezzo d'Acquisto (€)" else "Purchase Price (€)"
    val renovationCost = if (isItalian) "Costo Ristrutturazione (€)" else "Renovation Cost (€)"
    val legalAuctionFees = if (isItalian) "Spese Legali, Notaio e Asta (€)" else "Legal, Notary & Auction Fees (€)"
    val monthlyRent = if (isItalian) "Affitto Mensile Presunto (€)" else "Estimated Monthly Rent (€)"
    val expectedResale = if (isItalian) "Valore di Rivendita Futuro (€)" else "Expected Resale Price (€)"
    val totalInvestment = if (isItalian) "Investimento Totale" else "Total Capital Invested"
    val grossYield = if (isItalian) "Resa Lorda Annuo" else "Gross Annual Yield"
    val netRoiFlip = if (isItalian) "ROI Rivendita (Flip)" else "Resale ROI (Flip)"
    val annualRentalProfit = if (isItalian) "Profitto Annuo da Affitto" else "Annual Rental Income"
    val netResaleProfit = if (isItalian) "Profitto Rivendita (Netto)" else "Net Resale Profit"

    // Analytics
    val analyticsTitle = if (isItalian) "Market Intelligence & Price Spread" else "Market Intelligence & Price Spread"
    val analyticsSubtitle = if (isItalian)
        "Analisi avanzata sconti, volume e prezzi al metro quadro"
    else
        "Advanced analytics on discounts, volume, and price per sqm"
    val totalDbVolume = if (isItalian) "Volume Totale Database" else "Total Database Volume"
    val avgAuctionDiscount = if (isItalian) "Sconto Medio Aste/NPL" else "Avg Auction/NPL Discount"
    val avgPriceSqm = if (isItalian) "Prezzo Medio €/mq" else "Avg Price €/sqm"
    val discountDistTitle = if (isItalian) "Distribuzione Sconto vs Prezzo di Mercato" else "Discount Distribution vs Market Price"
    val topLocationsTitle = if (isItalian) "Città con Maggior Concentrazione di Opportunità" else "Cities with Highest Deal Concentration"
    val assetBreakdownTitle = if (isItalian) "Ripartizione per Tipologia Immobile" else "Property Type Breakdown"

    // Deal Detail Bottom Sheet
    val valuationLabel = if (isItalian) "Valore di Mercato" else "Market Value"
    val discountAcquisition = if (isItalian) "Sconto di Acquisizione" else "Acquisition Discount"
    val estimatedCapRate = if (isItalian) "Cap Rate Stimato" else "Estimated Cap Rate"
    val sendAlertNotification = if (isItalian) "Invia Notifica Alert" else "Send Alert Notification"
    val generateDueDiligence = if (isItalian) "Genera Documento Due Diligence" else "Generate Due Diligence PDF/Doc"
    val simulateInRoi = if (isItalian) "Simula Operazione in ROI Calculator" else "Simulate Deal in ROI Calculator"
    val priceDropHistory = if (isItalian) "Storico Ribassi Prezzo" else "Price Drop History"
    val recordNewDrop = if (isItalian) "Registra Nuovo Ribasso" else "Record New Price Drop"
    val personalNotes = if (isItalian) "Note Personali Investitore" else "Personal Investor Notes"
    val saveNotes = if (isItalian) "Salva Note" else "Save Notes"
    val deleteProperty = if (isItalian) "Elimina Immobile" else "Delete Property"

    // Add Deal Dialog
    val addDealTitle = if (isItalian) "Aggiungi Nuovo Immobile al Radar" else "Add New Property to Radar"
    val listingTitle = if (isItalian) "Titolo Annuncio" else "Listing Title"
    val uniqueCode = if (isItalian) "Codice Univoco / Asta ID" else "Unique Code / Auction ID"
    val sourceName = if (isItalian) "Nome Fonte / Portale" else "Source Name / Portal"
    val locationInput = if (isItalian) "Località (Città, Via)" else "Location (City, Address)"
    val propertyType = if (isItalian) "Tipologia Immobile" else "Property Type"
    val askingPriceInput = if (isItalian) "Prezzo Richiesto (€)" else "Asking Price (€)"
    val marketValInput = if (isItalian) "Valore di Mercato Stimato (€)" else "Estimated Market Value (€)"
    val surfaceAreaInput = if (isItalian) "Superficie (mq)" else "Surface Area (sqm)"
    val auctionDateInput = if (isItalian) "Data Udienza Asta (Opzionale)" else "Auction Hearing Date (Optional)"
    val addPropertyButton = if (isItalian) "Aggiungi Immobile" else "Add Property"

    // Due Diligence Dialog
    val dueDiligenceTitle = if (isItalian) "Dossier Immobiliare Due Diligence" else "Property Due Diligence Dossier"
    val dueDiligenceDesc = if (isItalian)
        "Genera e scarica analisi completa con dati catastali e di asta"
    else
        "Generate and download complete report with property register and auction details"
    val downloadReport = if (isItalian) "Scarica Report Completo" else "Download Full Report"
    val close = if (isItalian) "Chiudi" else "Close"
}

val LocalAppStrings = compositionLocalOf { AppStrings(AppLanguage.IT) }
