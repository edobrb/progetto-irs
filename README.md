# Online adaptation in multi robot system

Questo progetto è il risultato del lavoro svolto per il corso di Sistemi Intelligenti e Robotici presente nel corso di laurea magistrare di Scienze e Ingegneria Informatica di Cesena.

## Descrizione del progetto e obiettivi

L'obiettivo di questo progetto è quello di analizzare una tecnica di adattamento online in un contesto multi-robotico.

![](https://i.imgur.com/ilKsUR1.png)

Il progetto ha i seguenti obiettivi:
 - determinare quali reti booleane offrano migliori possibilità di adattamento del robot. Veranno provate una serie di reti booleane (caotiche, ordinate, critiche).
 - determinare quali variazioni interne portino ad un più veloce e/o migliore adattamento del robot.
 - notare se emergono comportamenti particolari (sia individuali che complessivi)
 - che ruolo giocano le misure di complessità?

## Lavoro svolto

 - cosa ho ereditato:
     - implementazione rete booleana
     - controller singolo robot

 - cose ho implementato:
     - modificato il controller per un sistema multi-robot
     - determinato quali informazioni estrarre dalla simulazione, come estrarle
     - esecutore dei test
     - loader

### Funzione obiettivo dei robot
Il task principale di ogni robot è quello di muoversi il più velocemente possibile in direzione rettilinea evitando ostacoli e altri robot. Per codificare questo task in uno scenario 


## Risultati

 - risultati base
     - bias 0.79 migliore
     - meno input meglio è -> pochi nodi non tutti sono utili, non ci serve una granularità di 24
     - self loop -> non chiaro se meglio o peggio
     - se si rewira anche l'output convergenza più veloce, rischio di fissare il robot ad 2 output inefficaci
     - robot migliori cosa fanno?
 - risultati variante metà
     - le considerazioni fatte sopra valgono anche per queste varianti
     - fitness inferiore -> normale
     - pericolo di come è stato implementato -> se va nella zona a 0 fitness magari ci rimane e non evolve -> (comporta sono una più lenta convergenza?)
     - differenze feed/no feed -> no feed stranamente va cmq bene
     - i robot migliori presentano un comportamento avanti, ruota 180, avanti -> se si incastrano fra due muri otterrano una buona fitness
 - possibilità che ci sia un bug
 - misura di complessità gzip?
 - Non vi è un comportamento complessivo perchè evolvono in modo indipendente?


## Appendice A: riprodurre gli esperimenti

I requisiti per eseguire nuovamente gli esperimenti sono:
 - [Argos3](https://www.argos-sim.info/) (v3.0.0)
     - eseuire `argos3 --version` per controllare la versione
 - Il repository `git clone https://github.com/edobrb/progetto-irs.git`
 - 16 GB di RAM 
 - 160GB di spazio su disco

### Struttura del repository
Nella cartella `lua` sono contenuti tutti i sorgenti necessari per le simulazioni eseguite con Argos: controller, rete booleana, descrizione dell'ambiente di simulazione e alcune utilità.

Nella cartella `analyzer` è contenuto un progetto Scala compilabile ed eseguibile attraverso lo strumento `sbt`. È necessario usare una CLI per lanciare il tool `sbt` o eventualmente è possibile utilizzare un IDE di preferenza.

### Lanciare gli esperimenti
Per prima cosa è necessario spostarsi all'interno del progetto `analyzer`.
Il progetto è suddiviso in tre sezioni principali:
 - **Experiments**: questa sezione ha il compito di lanciare le simulazioni e salvarne gli output su disco. È necessario specificare la locazione della cartella di lavoro `lua`, il file di simulazione da utilizzare (.argos) e la cartella dove verranno salvati i dati raccolti.
     - `sbt "runMain Experiments ../lua config_simulation.argos /storage/data"`
 - **Loader**: una volta ottenuti i risultati dalla sezione Experiments è necessario elaborare i file per ricavarne una versione più compatta con soltato le informazioni d'interesse. Per ogni file generato da Experiments verrà creato un nuovo file in formato JSON con alcune informazioni estratte da esso. Questa fase permetterà di analizzare i risultati con molta più efficienza.
     - `sbt "runMain Loader ../lua config_simulation.argos /storage/data"`
 - **Analyzer**: una volta generati i file intermedi in formato JSON è possibile andare a generare i grafici sulla fitness attraverso questo eseguibile. Inoltre è possibile modificare questa sezione per estrarre e visualizzare le informazioni di più interesse e/o lanciare i robot che hanno ottenuto più fitness per vederne visivamente il comportamento.
     - `sbt "runMain Analyzer ../lua config_simulation.argos /storage/data"`

I file generati da *Experiments* e *Loader* utilizzati per ricavare i risultati descritti in questo documento saranno distribuiti per un periodo non determinato. Il link per il download attraverso protocollo BitTorrent è [magnet link].



I parametri della configurazione base degli esperimenti che non verranno alterati sono:
 - simulazione:
     -  ticks per seconds: 10
     -  experiment length: 7200 (seconds)
     -  network test steps: 400 (ticks)
     -  robot count: 10
 - robot:
     - proximity threshold: 0.1
     - max wheel speed: 5 (cm/s)
     - stay on half variation: **variabile**
 - rete booleana:
     - max input rewires: 2
     - input rewires probability: 1
     - max output rewires: **variabile**
     - output rewires probability: 1
     - dual encoding: false
     - node count: 100
     - k: 3
     - bias: **variabile**
     - nodi in input: **variabile**
     - nodi di output: 2
     - self loops: **variabile**
     - override output nodes bias: true

Le variazioni sono

| bias |max output rewires|self loop|node input count|half variation
|:----:|:----:|:----:|:----:|:----:|
|0.1|0|true|24|false|
|0.5|1|false|8|true with feed|
|0.79||||true without feed|

Ad ogni configurazione base viene applicata una combinazione di variazioni con un totale di $3*2*2*3*2 = 72$ possibili configurazioni

I nomi dei file generati rispecchiano alcuni parametri della ...


