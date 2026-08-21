# Quantum Deal Radar — regole di progetto

## Principio non negoziabile
L'app non inventa mai un dato per completare una schermata. Se un dato manca,
il calcolo non viene eseguito e l'assenza viene dichiarata all'utente.

## La regola meccanica
Nessun `?:` con valore costante su un campo di misura — di mercato, geografico
o statistico — in NESSUN punto del progetto: motori, repository, ViewModel e
interfaccia compresi. Vale anche per i valori di default nelle dichiarazioni
delle entità Room e per i campi lasciati vuoti dall'utente in un form.
Sono ammessi solo: limiti di range in coerceIn, timeout, indici di lista.

## Perché è scritta così
Questo difetto è stato eliminato tre volte dai motori di calcolo ed è
rientrato ogni volta da un livello diverso: prima dai consumatori, poi dalla
UI. In un caso una singola riga nella scheda di visualizzazione ha annullato
tre interventi e un test verde.

## Regole operative
- Dopo OGNI modifica: `.\gradlew.bat testDebugUnitTest`. Non dichiarare fatto
  nulla che non sia verde.
- Mai `!!` nel codice di produzione per aggirare un tipo nullable.
- Mai rendere nullable un tipo esistente solo per far compilare: se un valore
  può mancare, chi lo usa deve rifiutarsi di calcolare.
- Mai confrontare con 0.0 un campo che è diventato nullable: la condizione
  risulta vera per null e la guardia si inverte in silenzio.
- Non aggiornare AGP o Gradle: fissati a AGP 9.1.1 / Gradle 9.3.1. Con
  AGP 9.3.1 fallisce PropertyRepositoryTest:369.
- Non toccare file non nominati nella richiesta.
- Rispondi in italiano.

## Stato
- 148 test verdi. Invarianti in ProvenanceInvariantsTest,
  MacroRatesInvariantsTest, OpportunityScoreInvariantsTest,
  PredictiveDealAlertEngineTest.
- Repository: Quantum-Re/quantumdealradarandroid12345
