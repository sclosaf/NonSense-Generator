# NonSense-Generator

Il prof nel powerpoint di introduzione al progetto consigliava di usare maven
per gestire le librerie, altrimenti bisogna gestire i pacchetti a mano, gli
script che ci sono nella cartella li ho preparati per compilare, fare testing e
documentazione in modo automatico, così da evitare di memorizzare troppe righe di comandi

EDIT:
Per l'accesso alle api google serve fare l'autenticazione presso il sito
di tali api, successivamente vi serve una chiave per il "LanguageServiceClient"
che dovete scaricate in formato .json e una volta scaricato questo file,
rinominatelo credentials.json e lo copiate e incollate dentro a /config nella
root di progetto, il .gitignore lo esclude a priori come file, quindi vi resta
presente solo a voi in locale, ognuno così usa le proprie credenziali e dati di
accesso, in modo che possa fare l'accesso in autonomia.

Per usare le api potete importare la classe
unipd.nonsense.util.GoogleApiClient,
la quale gestisce autonomamente l'autenticazione anche tra più classi che
creiamo all'interno del progetto, così anche se due classi distinte invocano le
api possono comunque usare lo stesso client, quando volete fare delle chiamate
con funzioni del natural language basta che creiate un oggetto
GoogleApiClient manager = new GoogleApiClient("/credentials.json");

successivamente una volta creato l'oggetto le chiamate le potete effettuare
tramite la funzione manager.getClient()

Classi:
    Main,

    1logging, - Fabio

    2interfaccia per terminale, - Davide

    3Sentence analyzer, - Davide

    4elaboratore File JSON, - Fabrizio

    5compositore di frasi, - Fabrizio

    6valutatore di tossicità, - Francesco

    7elementi grammaticali atomici, - Fabio

    8generatore di template, - Francesco

    eccezioni,

    testing (Ognuno il suo)
