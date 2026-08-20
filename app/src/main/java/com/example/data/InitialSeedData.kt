package com.example.data

object InitialSeedData {
    val initialSources = listOf(
        ScraperSource(
            id = "reperform",
            name = "1. Reperform",
            url = "https://www.reperform.com/",
            robotsStatus = "NON_VERIFICATO",
            configStatus = "NON_VERIFICATO",
            activeParserRulesJson = """
                {
                  "listSelector": ".asset-card",
                  "titleSelector": ".asset-title",
                  "priceSelector": ".base-price",
                  "marketValueSelector": ".appraisal-val",
                  "sqmSelector": ".surface-mq",
                  "locationSelector": ".geo-location"
                }
            """.trimIndent(),
            totalDealsFound = 0
        ),
        ScraperSource(
            id = "iresales",
            name = "2. i-Resales",
            url = "https://www.i-resales.it/",
            robotsStatus = "NON_VERIFICATO",
            configStatus = "NON_VERIFICATO",
            activeParserRulesJson = """
                {
                  "listSelector": ".listing-item",
                  "titleSelector": "h3.title",
                  "priceSelector": ".price-tag",
                  "marketValueSelector": ".market-value",
                  "sqmSelector": ".surface",
                  "locationSelector": ".address-label"
                }
            """.trimIndent(),
            totalDealsFound = 0
        ),
        ScraperSource(
            id = "uclam_reimpresa",
            name = "3. UCLAM / RE-Impresa",
            url = "https://www.re-impresa.it/",
            robotsStatus = "NON_VERIFICATO",
            configStatus = "NON_VERIFICATO",
            activeParserRulesJson = """
                {
                  "listSelector": ".annuncio-box",
                  "titleSelector": ".titolo-annuncio",
                  "priceSelector": ".prezzo-richiesta",
                  "marketValueSelector": ".stima-mercato",
                  "sqmSelector": ".metri-quadri",
                  "locationSelector": ".comune-prov"
                }
            """.trimIndent(),
            totalDealsFound = 0
        ),
        ScraperSource(
            id = "bnl_immobili",
            name = "4. BNL Immobili",
            url = "https://immobili.bnl.it/",
            robotsStatus = "NON_VERIFICATO",
            configStatus = "NON_VERIFICATO",
            activeParserRulesJson = """
                {
                  "listSelector": ".immobile-card",
                  "titleSelector": ".card-header-title",
                  "priceSelector": ".prezzo-base",
                  "marketValueSelector": ".valore-stima",
                  "sqmSelector": ".superficie-totale",
                  "locationSelector": ".citta-provincia"
                }
            """.trimIndent(),
            totalDealsFound = 0
        ),
        ScraperSource(
            id = "bper_leasing",
            name = "5. BPER Leasing",
            url = "https://www.bperleasing.it/beni-in-vendita/",
            robotsStatus = "NON_VERIFICATO",
            configStatus = "NON_VERIFICATO",
            activeParserRulesJson = """
                {
                  "listSelector": ".asset-card",
                  "titleSelector": ".asset-name",
                  "priceSelector": ".base-price",
                  "marketValueSelector": ".appraisal-value",
                  "sqmSelector": ".surface",
                  "locationSelector": ".location"
                }
            """.trimIndent(),
            totalDealsFound = 0
        ),
        ScraperSource(
            id = "mps_bandi",
            name = "6. MPS – Bandi e Procedure Competitive",
            url = "https://www.mps.it/comunicazioni-alla-clientela/avvisi/",
            robotsStatus = "NON_VERIFICATO",
            configStatus = "NON_VERIFICATO",
            activeParserRulesJson = """
                {
                  "listSelector": ".avviso-row",
                  "titleSelector": ".descrizione-immobile",
                  "priceSelector": ".prezzo-base",
                  "marketValueSelector": ".valore-perizia",
                  "sqmSelector": ".superficie-commerciale",
                  "locationSelector": ".citta"
                }
            """.trimIndent(),
            totalDealsFound = 0
        ),
        ScraperSource(
            id = "intesa_proprieta",
            name = "7. Intesa Sanpaolo Proprietà",
            url = "https://www.intesasanpaolo.com/it/proprieta.html",
            robotsStatus = "NON_VERIFICATO",
            configStatus = "NON_VERIFICATO",
            activeParserRulesJson = """
                {
                  "listSelector": ".property-item-box",
                  "titleSelector": ".property-title",
                  "priceSelector": ".asking-price",
                  "marketValueSelector": ".appraisal-val",
                  "sqmSelector": ".property-sqm",
                  "locationSelector": ".city-name"
                }
            """.trimIndent(),
            totalDealsFound = 0
        ),
        ScraperSource(
            id = "pvp_quimmo",
            name = "8. PVP e Quimmo",
            url = "https://portalevenditepubbliche.giustizia.it",
            robotsStatus = "NON_VERIFICATO",
            configStatus = "NON_VERIFICATO",
            activeParserRulesJson = """
                {
                  "listSelector": ".scheda-asta",
                  "titleSelector": ".titolo-lotto",
                  "priceSelector": ".offerta-minima",
                  "marketValueSelector": ".valore-perizia",
                  "sqmSelector": ".mq-lotto",
                  "locationSelector": ".ubicazione"
                }
            """.trimIndent(),
            totalDealsFound = 0
        ),
        ScraperSource(
            id = "bper_real_estate",
            name = "9. BPER Real Estate (Contatto Diretto)",
            url = "https://www.bper.it/real-estate",
            robotsStatus = "NON_VERIFICATO",
            configStatus = "NON_VERIFICATO",
            activeParserRulesJson = """
                {
                  "listSelector": ".direct-asset",
                  "titleSelector": ".asset-title",
                  "priceSelector": ".indicative-price",
                  "marketValueSelector": ".book-value",
                  "sqmSelector": ".total-sqm",
                  "locationSelector": ".address"
                }
            """.trimIndent(),
            totalDealsFound = 0
        ),
        ScraperSource(
            id = "cdp_fintecna_bancaditalia",
            name = "10. CDP / Fintecna e Banca d’Italia",
            url = "https://www.cdp.it/immobiliare",
            robotsStatus = "NON_VERIFICATO",
            configStatus = "NON_VERIFICATO",
            activeParserRulesJson = """
                {
                  "listSelector": ".bando-item",
                  "titleSelector": ".bando-title",
                  "priceSelector": ".importo-base",
                  "marketValueSelector": ".valore-stimato",
                  "sqmSelector": ".superficie",
                  "locationSelector": ".regione-comune"
                }
            """.trimIndent(),
            totalDealsFound = 0
        )
    )

    val initialDeals = listOf(
        PropertyDeal(
            id = 101,
            title = "Palazzina Direzionale ex-NPL - Opp. Riconversione",
            sourceKey = "reperform",
            sourceName = "Reperform",
            sourceUrl = "https://www.reperform.com/",
            location = "Milano - Porta Romana (MI)",
            propertyType = "Commerciale / Value-Add",
            askingPrice = 620000.0,
            estimatedMarketValue = 1100000.0,
            surfaceSqm = 850,
            discountPercent = 43,
            estimatedCapRate = 10.2,
            auctionDate = "18/09/2026",
            status = "PRICE_CUT",
            imageUrl = "https://images.unsplash.com/photo-1560518883-ce09059eeffa?auto=format&fit=crop&w=800&q=80",
            notes = "Edificio cielo-terra dismesso con potenziale cambio d'uso in residenziale/micro-living.",
            isBookmarked = true,
            dealStage = "UNDER_CONTRACT"
        ),
        PropertyDeal(
            id = 102,
            title = "Complesso Logistico & Commerciale Distressed",
            sourceKey = "iresales",
            sourceName = "i-Resales",
            sourceUrl = "https://www.i-resales.it/",
            location = "Verona - ZAI (VR)",
            propertyType = "Industriale",
            askingPrice = 480000.0,
            estimatedMarketValue = 820000.0,
            surfaceSqm = 1400,
            discountPercent = 41,
            estimatedCapRate = 11.5,
            auctionDate = null,
            status = "LIVE",
            imageUrl = "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?auto=format&fit=crop&w=800&q=80",
            notes = "Piattaforma remarketing. Ottimo per operazione di frazionamento logistico o trasformazione last-mile.",
            isBookmarked = true,
            dealStage = "CLOSING"
        ),
        PropertyDeal(
            id = 103,
            title = "Area Industriale 4.500 m² da Riqualificare",
            sourceKey = "uclam_reimpresa",
            sourceName = "UCLAM / RE-Impresa",
            sourceUrl = "https://www.re-impresa.it/",
            location = "Torino - Mirafiori (TO)",
            propertyType = "Riconversione",
            askingPrice = 390000.0,
            estimatedMarketValue = 750000.0,
            surfaceSqm = 4500,
            discountPercent = 48,
            estimatedCapRate = 12.0,
            auctionDate = "10/10/2026",
            status = "LIVE",
            imageUrl = "https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=800&q=80",
            notes = "Dismissione portafoglio corporate. Pronta masterplan per parco tecnologico o residenze universitarie.",
            isBookmarked = true,
            dealStage = "PROSPECTING"
        ),
        PropertyDeal(
            id = 104,
            title = "Ex Filiale BNL Piano Terra + Interrato Vetrina",
            sourceKey = "bnl_immobili",
            sourceName = "BNL Immobili",
            sourceUrl = "https://immobili.bnl.it/",
            location = "Roma - Quartiere Prati (RM)",
            propertyType = "Commerciale",
            askingPrice = 310000.0,
            estimatedMarketValue = 520000.0,
            surfaceSqm = 280,
            discountPercent = 40,
            estimatedCapRate = 8.9,
            auctionDate = null,
            status = "PRICE_CUT",
            imageUrl = "https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=800&q=80",
            notes = "Patrimonio BNL BNP Paribas. Posizione angolare ad altissimo passaggio pedonale.",
            isBookmarked = true,
            dealStage = "CLOSED"
        ),
        PropertyDeal(
            id = 105,
            title = "Capannone da Leasing Risolto con Banchina",
            sourceKey = "bper_leasing",
            sourceName = "BPER Leasing",
            sourceUrl = "https://www.bperleasing.it/beni-in-vendita/",
            location = "Bologna - Navile (BO)",
            propertyType = "Industriale",
            askingPrice = 490000.0,
            estimatedMarketValue = 820000.0,
            surfaceSqm = 1200,
            discountPercent = 40,
            estimatedCapRate = 11.2,
            auctionDate = "05/10/2026",
            status = "LIVE",
            imageUrl = "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?auto=format&fit=crop&w=800&q=80",
            notes = "Remarketing BPER Leasing. Banchina di carico presente, altezza sottomagazzino 8 metri.",
            isBookmarked = false
        ),
        PropertyDeal(
            id = 106,
            title = "Edificio Storico Cielo-Terra per Boutique Hotel",
            sourceKey = "mps_bandi",
            sourceName = "MPS – Bandi e Procedure",
            sourceUrl = "https://www.mps.it/comunicazioni-alla-clientela/avvisi/",
            location = "Siena - Centro Storico (SI)",
            propertyType = "Value-Add",
            askingPrice = 750000.0,
            estimatedMarketValue = 1350000.0,
            surfaceSqm = 920,
            discountPercent = 44,
            estimatedCapRate = 9.5,
            auctionDate = "22/10/2026",
            status = "AUCTION_PENDING",
            imageUrl = "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=800&q=80",
            notes = "Bando competitivo MPS. Struttura vincolata con progetto di massima approvato per 18 suite.",
            isBookmarked = true
        ),
        PropertyDeal(
            id = 107,
            title = "Ex Agenzia Intesa Sanpaolo 250 m² Centro",
            sourceKey = "intesa_proprieta",
            sourceName = "Intesa Sanpaolo Proprietà",
            sourceUrl = "https://www.intesasanpaolo.com/it/proprieta.html",
            location = "Firenze - Campo di Marte (FI)",
            propertyType = "Commerciale",
            askingPrice = 280000.0,
            estimatedMarketValue = 460000.0,
            surfaceSqm = 250,
            discountPercent = 39,
            estimatedCapRate = 8.7,
            auctionDate = null,
            status = "LIVE",
            imageUrl = "https://images.unsplash.com/photo-1497215728101-856f4ea42174?auto=format&fit=crop&w=800&q=80",
            notes = "Ex dipendenza bancaria. Canna fumaria installabile, ideale per ristorazione o clinic medicale.",
            isBookmarked = false
        ),
        PropertyDeal(
            id = 108,
            title = "Attico Vista Duomo - Offerta Minima Asta -42%",
            sourceKey = "pvp_quimmo",
            sourceName = "PVP e Quimmo",
            sourceUrl = "https://portalevenditepubbliche.giustizia.it",
            location = "Milano - Centro Storico (MI)",
            propertyType = "Asta Giudiziaria",
            askingPrice = 395000.0,
            estimatedMarketValue = 680000.0,
            surfaceSqm = 135,
            discountPercent = 42,
            estimatedCapRate = 8.1,
            auctionDate = "28/09/2026",
            status = "AUCTION_PENDING",
            imageUrl = "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=800&q=80",
            notes = "Procedura esecutiva tribunale Milano tramite PVP/Quimmo. Terrazzo 40m².",
            isBookmarked = true
        ),
        PropertyDeal(
            id = 109,
            title = "Portafoglio Off-Market 3 Immobili Commerciali Locati",
            sourceKey = "bper_real_estate",
            sourceName = "BPER Real Estate",
            sourceUrl = "https://www.bper.it/real-estate",
            location = "Modena (MO)",
            propertyType = "Off-Market",
            askingPrice = 890000.0,
            estimatedMarketValue = 1400000.0,
            surfaceSqm = 1100,
            discountPercent = 36,
            estimatedCapRate = 9.8,
            auctionDate = null,
            status = "LIVE",
            imageUrl = "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=800&q=80",
            notes = "Trattativa diretta riservata con BPER Real Estate desk. Rendita locativa in essere €85k/anno.",
            isBookmarked = false
        ),
        PropertyDeal(
            id = 110,
            title = "Edificio Istituzionale Ex-Banca d'Italia da Valorizzare",
            sourceKey = "cdp_fintecna_bancaditalia",
            sourceName = "CDP / Fintecna / B d'I",
            sourceUrl = "https://www.cdp.it/immobiliare",
            location = "Genova - Centro (GE)",
            propertyType = "Riconversione",
            askingPrice = 1200000.0,
            estimatedMarketValue = 2100000.0,
            surfaceSqm = 2200,
            discountPercent = 43,
            estimatedCapRate = 10.5,
            auctionDate = "15/11/2026",
            status = "AUCTION_PENDING",
            imageUrl = "https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?auto=format&fit=crop&w=800&q=80",
            notes = "Bando CDP/Fintecna. Struttura monumentale adatta per sede executive o luxury residence.",
            isBookmarked = true
        )
    )

    val initialHistories = listOf(
        PriceHistory(dealId = 101, price = 850000.0, dateRecorded = "15/01/2026", eventLabel = "Stima Perizia NPL"),
        PriceHistory(dealId = 101, price = 720000.0, dateRecorded = "20/04/2026", eventLabel = "Primo Ribasso Scraper"),
        PriceHistory(dealId = 101, price = 620000.0, dateRecorded = "01/08/2026", eventLabel = "Attuale Offerta Radar"),

        PriceHistory(dealId = 108, price = 580000.0, dateRecorded = "10/02/2026", eventLabel = "Valore Asta Base"),
        PriceHistory(dealId = 108, price = 395000.0, dateRecorded = "15/06/2026", eventLabel = "Offerta Minima Consentita"),

        PriceHistory(dealId = 105, price = 650000.0, dateRecorded = "01/03/2026", eventLabel = "Stima Perizia Leasing"),
        PriceHistory(dealId = 105, price = 490000.0, dateRecorded = "05/07/2026", eventLabel = "Listino Remarketing BPER")
    )

    val initialDistressedProperties = listOf(
        DistressedProperty(
            id = 1,
            address = "Via Garibaldi 10, Milano (MI)",
            price = 280000.0,
            distressLevel = "Foreclosure",
            category = "Residential",
            latitude = 45.4642,
            longitude = 9.1900,
            imageUrl = "https://images.unsplash.com/photo-1560518883-ce09059eeffa?auto=format&fit=crop&w=800&q=80",
            notes = "Procedura esecutiva NPL n. 412/2025 Tribunale di Milano. Immobile ad uso residenziale 120 mq."
        ),
        DistressedProperty(
            id = 2,
            address = "Corso Buenos Aires 45, Milano (MI)",
            price = 450000.0,
            distressLevel = "Auction",
            category = "Commercial",
            latitude = 45.4800,
            longitude = 9.2100,
            imageUrl = "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=800&q=80",
            notes = "Asta giudiziaria con offerta minima ribassata del 35%. Negozio monovetrina."
        ),
        DistressedProperty(
            id = 3,
            address = "Via Nazionale 120, Roma (RM)",
            price = 520000.0,
            distressLevel = "Pre-Foreclosure",
            category = "Commercial",
            latitude = 41.9028,
            longitude = 12.4964,
            imageUrl = "https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=800&q=80",
            notes = "Sofferenza bancaria prima dell'asta. Trattativa saldo e stralcio possibile."
        ),
        DistressedProperty(
            id = 4,
            address = "Via Po 18, Torino (TO)",
            price = 210000.0,
            distressLevel = "Tax Lien",
            category = "Industrial",
            latitude = 45.0703,
            longitude = 7.6869,
            imageUrl = "https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=800&q=80",
            notes = "Cartella esattoriale e gravame fiscale. Capannone ed ex laboratorio artigianale."
        ),
        DistressedProperty(
            id = 5,
            address = "Via Indipendenza 32, Bologna (BO)",
            price = 340000.0,
            distressLevel = "Foreclosure",
            category = "Residential",
            latitude = 44.4949,
            longitude = 11.3426,
            imageUrl = "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?auto=format&fit=crop&w=800&q=80",
            notes = "Pignoramento immobiliare. Appartamento piano alto con ascensore."
        ),
        DistressedProperty(
            id = 6,
            address = "Via Cavour 14, Firenze (FI)",
            price = 390000.0,
            distressLevel = "Bank REO",
            category = "Residential",
            latitude = 43.7732,
            longitude = 11.2558,
            imageUrl = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=800&q=80",
            notes = "Rientro in possesso banca (BPER Leasing). Bilocale storico centro."
        ),
        DistressedProperty(
            id = 7,
            address = "Via Toledo 210, Napoli (NA)",
            price = 195000.0,
            distressLevel = "Auction",
            category = "Commercial",
            latitude = 40.8398,
            longitude = 14.2488,
            imageUrl = "https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=800&q=80",
            notes = "Asta giudiziaria Tribunale di Napoli. Sconto -48% per rapida liquidazione."
        ),
        DistressedProperty(
            id = 8,
            address = "Corso Cavour 88, Verona (VR)",
            price = 265000.0,
            distressLevel = "Short Sale",
            category = "Residential",
            latitude = 45.4384,
            longitude = 10.9916,
            imageUrl = "https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=800&q=80",
            notes = "Procedura stragiudiziale saldo e stralcio concordata con creditore ipotecario."
        ),
        DistressedProperty(
            id = 9,
            address = "Cannaregio 1420, Venezia (VE)",
            price = 480000.0,
            distressLevel = "Foreclosure",
            category = "Industrial",
            latitude = 45.4432,
            longitude = 12.3280,
            imageUrl = "https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?auto=format&fit=crop&w=800&q=80",
            notes = "Esecuzione immobiliare n. 88/2025. Deposito e magazzino con porta d'acqua."
        ),
        DistressedProperty(
            id = 10,
            address = "Via Bramante 12, Milano (MI)",
            price = 310000.0,
            distressLevel = "Pre-Foreclosure",
            category = "Residential",
            latitude = 45.4815,
            longitude = 9.1795,
            imageUrl = "https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?auto=format&fit=crop&w=800&q=80",
            notes = "Contatto diretto con esecutato prima dell'avviso d'asta. Zona Chinatown / Sempione."
        )
    )

    val initialProperties = listOf(
        Property(
            id = 1,
            title = "Trilocale Luminoso Porta Romana",
            address = "Via Crema 15, Milano (MI)",
            price = 380000.0,
            surfaceSqm = 95,
            propertyType = "Appartamento",
            distressStatus = "Pre-Asta",
            strategyTags = "Frazionamento, Fix & Flip",
            pipelineStatus = "RENOVATING",
            estimatedRenovationCost = 65000.0,
            actualRenovationCost = 42000.0,
            targetResalePrice = 540000.0,
            actualSalePrice = 0.0,
            projectedRentalIncome = 2400.0,
            escrowClosingDate = "15/04/2026",
            renovationProgressPercent = 65,
            contractorNotes = "Impianto elettrico e idraulico completati. Posa parquet e sanitari sospesi in corso con Ditta EdilNord.",
            notes = "Frazionamento in due unità ad alta redditività. Potenziale margine netto: +€95.000.",
            latitude = 45.4520,
            longitude = 9.2010,
            createdAt = System.currentTimeMillis() - 5L * 24 * 3600 * 1000
        ),
        Property(
            id = 2,
            title = "Palazzina Cielo-Terra da Riconvertire",
            address = "Via Garibaldi 44, Torino (TO)",
            price = 650000.0,
            surfaceSqm = 520,
            propertyType = "Cielo-Terra",
            distressStatus = "NPL",
            strategyTags = "Riconversione, BRRRR",
            pipelineStatus = "IN_ESCROW",
            estimatedRenovationCost = 140000.0,
            actualRenovationCost = 0.0,
            targetResalePrice = 980000.0,
            actualSalePrice = 0.0,
            projectedRentalIncome = 5800.0,
            escrowClosingDate = "28/09/2026",
            renovationProgressPercent = 0,
            contractorNotes = "In attesa di perizia giurata e rogito notarile fissato per fine mese. Preventivi cantiere raccolti.",
            notes = "Ex sede uffici, convertibile in 4 bilocali con box auto. Trattativa saldo e stralcio bancaria avanzata.",
            latitude = 45.0715,
            longitude = 7.6780,
            createdAt = System.currentTimeMillis() - 18L * 24 * 3600 * 1000
        ),
        Property(
            id = 3,
            title = "Attico Panoramico Quartiere Prati",
            address = "Via Cola di Rienzo 88, Roma (RM)",
            price = 720000.0,
            surfaceSqm = 140,
            propertyType = "Attico",
            distressStatus = "Saldo e Stralcio",
            strategyTags = "Messa a Rendita, Luxury",
            pipelineStatus = "RENTED",
            estimatedRenovationCost = 45000.0,
            actualRenovationCost = 48000.0,
            targetResalePrice = 920000.0,
            actualSalePrice = 0.0,
            projectedRentalIncome = 3600.0,
            escrowClosingDate = "10/01/2026",
            renovationProgressPercent = 100,
            contractorNotes = "Ristrutturazione luxury completata e arredata. Conduttore corporate inserito con contratto 4+4.",
            notes = "Trattativa riservata con istituto di credito conclusa. Terrazzo a livello 50mq, rendita netta 6.2%.",
            latitude = 41.9080,
            longitude = 12.4650,
            createdAt = System.currentTimeMillis() - 35L * 24 * 3600 * 1000
        ),
        Property(
            id = 4,
            title = "Bilocale Riqualificato Isola / Gae Aulenti",
            address = "Via Borsieri 22, Milano (MI)",
            price = 260000.0,
            surfaceSqm = 60,
            propertyType = "Appartamento",
            distressStatus = "Asta Giudiziaria",
            strategyTags = "Fix & Flip",
            pipelineStatus = "SOLD",
            estimatedRenovationCost = 35000.0,
            actualRenovationCost = 34200.0,
            targetResalePrice = 365000.0,
            actualSalePrice = 372000.0,
            projectedRentalIncome = 1500.0,
            escrowClosingDate = "20/02/2026",
            renovationProgressPercent = 100,
            contractorNotes = "Vendita formalizzata dal notaio con realizzo superiore al target di +€7.000.",
            notes = "Aggiudicato in asta telematica Tribunale di Milano con ribasso del 32%. ROI netto generato: +26.4%.",
            latitude = 45.4870,
            longitude = 9.1890,
            createdAt = System.currentTimeMillis() - 60L * 24 * 3600 * 1000
        ),
        Property(
            id = 5,
            title = "Spazio Commerciale Open Space Navigli",
            address = "Ripa di Porta Ticinese 73, Milano (MI)",
            price = 310000.0,
            surfaceSqm = 110,
            propertyType = "Commerciale",
            distressStatus = "Sofferenza Bancaria",
            strategyTags = "Cambio Destinazione d'Uso",
            pipelineStatus = "ANALYZED",
            estimatedRenovationCost = 50000.0,
            actualRenovationCost = 0.0,
            targetResalePrice = 460000.0,
            actualSalePrice = 0.0,
            projectedRentalIncome = 2800.0,
            escrowClosingDate = "",
            renovationProgressPercent = 0,
            contractorNotes = "Valutazione urbanistica preliminare positiva per cambio d'uso in residenziale loft.",
            notes = "Analisi preliminare di redditività completata. Valore di mercato comparabile: €4.200/mq.",
            latitude = 45.4510,
            longitude = 9.1720,
            createdAt = System.currentTimeMillis() - 2L * 24 * 3600 * 1000
        ),
        Property(
            id = 6,
            title = "Villa Storica Collina Torinese",
            address = "Strada Val Pattonera 15, Torino (TO)",
            price = 490000.0,
            surfaceSqm = 340,
            propertyType = "Villa",
            distressStatus = "Esecuzione Immobiliare",
            strategyTags = "Frazionamento Luxury",
            pipelineStatus = "LISTED",
            estimatedRenovationCost = 90000.0,
            actualRenovationCost = 88500.0,
            targetResalePrice = 750000.0,
            actualSalePrice = 0.0,
            projectedRentalIncome = 3200.0,
            escrowClosingDate = "05/11/2025",
            renovationProgressPercent = 100,
            contractorNotes = "Home staging e shooting fotografico professionale effettuati. Attualmente sul mercato con 2 agenzie partner.",
            notes = "Completata divisione in due porzioni bifamiliari con giardino esclusivo. 3 visite programmate.",
            latitude = 45.0340,
            longitude = 7.6920,
            createdAt = System.currentTimeMillis() - 85L * 24 * 3600 * 1000
        )
    )
}

