# Online adaptation in multi robot system

intro

## Obietivi


## Lavoro svolto
Tutto il codice contenuto nel primo commit 8232c2a è stato ereditato da Alessandro Gnucci il quale ha scritto un controller in linguaggio Lua per il simulatore Argos. Tale controller mette le basi per il progetto in questione: forniva l'implementazione delle reti booleane e l'interfacciamento tra esse ed un eventuale controller.

## Risultati

### Considerazioni

## Appendice A: riprodurre gli esperimenti

I requisiti per eseguire nuovamente gli esperimenti sono:
 - [Argos3](https://www.argos-sim.info/) (v3.0.0)
     - eseuire `argos3 --version` per controllare la versione
 - Il repository `git clone https://github.com/edobrb/progetto-irs.git`
 - 16 GB di RAM libera e 160GB di spazio su disco

### Struttura del repository
Nella cartella `lua` sono contenuti tutti i sorgenti necessari per la simulation Argos: controller, rete booleana, descrizione dell'ambiente di simulazione e alcune utilità.

Nella cartella `analyzer` è contenuto un progetto Scala compilabile ed eseguibile attraverso lo strumento `sbt`. Consiglio di aprire il progetto con IntelliJ IDEA, alternativamente è possibile utilizzare un CLI.

### Lanciare gli esperimenti
Il progetto è suddiviso in tre sezioni principali:
 - **Experiments**: questa sezione ha il compito di lanciare le simulazioni e salvarne gli output su disco. È necessario specificare la locazione della cartella di lavoro `lua`, il file di simulazione da utilizzare (.argos) e la cartella dove verranno salvati i dati raccolti.
     - `sbt "runMain Experiments ../lua config_simulation.argos /storage/data"`
 - **Loader**: una volta ottenuti i risultati dalla sezione Experiments è necessario elaborare i file per ricavarne una versione più compatta con soltato le informazioni d'interesse. Per ogni file generato da Experiments verrà creato un nuovo file in formato JSON con alcune informazioni estratte da esso. Questa fase permetterà di analizzare i risultati con molta più efficienza.
     - `sbt "runMain Loader ../lua config_simulation.argos /storage/data"`
 - **Analyzer**: una volta generati i file intermedi in formato JSON è possibile andare a generare i grafici sulla fitness attraverso questo eseguibile. Inoltre è possibile modificare questa sezione per estrarre e visualizzare le informazioni di più interesse e/o lanciare i robot che hanno ottenuto più fitness per vederne visivamente il comportamento.
     - `sbt "runMain Analyzer ../lua config_simulation.argos /storage/data"`

I file generati da Experiments utilizzati ricavare i risultati descritti in questo documento saranno distribuiti per un periodo non determinato. Il link per il download attraverso protocollo BitTorrent è [magnet link].