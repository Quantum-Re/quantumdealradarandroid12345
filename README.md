# Quantum Deal Radar

Applicazione avanzata per investitori immobiliari: analisi deal, stima automatica dei prezzi di mercato, calcolo peritale senior (MCA UNI 10750), simulazioni mutui, regimi fiscali italiani e monitoraggio opportunità sottocosto.

---

## Configurazione Firebase e `google-services.json`

Il progetto supporta Firebase per autenticazione multi-dispositivo, sincronizzazione cloud (Firestore) e notifiche push (Firebase Cloud Messaging).

### Comportamento senza `google-services.json`

In assenza del file `google-services.json`:
- La build compila regolarmente grazie al flag `googleServices.missing.passthrough=true` in `gradle.properties`.
- All'avvio, `DealRadarApplication` rileva l'assenza di Firebase e imposta lo stato reattivo globale `isFirebaseConfigured = false`.
- **Tutte le funzioni offline dell'app restano pienamente operative**: stime di mercato, calcolatori fiscali e ROI, analisi peritali, database locale Room, import/export CSV e portafoglio locale.
- Ogni operazione o pulsante dipendente da Firebase (es. Login, Registrazione, Sync Cloud, Attivazione Notifiche Push) presenta un messaggio informativo esplicito per l'utente:
  > *"Funzione non disponibile: Firebase non è configurato in questa build."*

### Funzioni che richiedono `google-services.json`

1. **Autenticazione Utente (Firebase Auth & Google Sign-In)**:
   - Login tramite email/password e Google Credential Manager.
   - Profilo cloud e sincronizzazione preferenze utente.
2. **Cloud Portfolio Sync (Cloud Firestore)**:
   - Salvataggio e condivisione portafoglio e deal su cloud.
   - Backup remoto dei dati.
3. **Notifiche Push in Tempo Reale (Firebase Cloud Messaging - FCM)**:
   - Ricezione alert automatici sui nuovi deal e variazioni di prezzo quando l'app è in background.
4. **Protezione e Integrità (Firebase App Check)**:
   - Verifica di sicurezza per chiamate backend certificate.

Per abilitare queste funzionalità, scaricare il file `google-services.json` dalla console Firebase del proprio progetto e posizionarlo nella cartella `app/` del repository prima di compilare l'applicazione.
