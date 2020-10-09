# Online adaptation in multi robot system

Questo progetto è il risultato del lavoro svolto per il corso di Sistemi Intelligenti e Robotici presente nel corso di laurea magistrale di Scienze e Ingegneria Informatica di Cesena.

## Indice
1. [Descrizione del progetto](#Descrizione-del-progetto)
    - [Taks](#Task)
    - [Rete Booleana](#Rete-Booleana)
    - [Simulazione fisica](#Simulazione-fisica)
    - [Obiettivi](#Obiettivi) 
2. [Lavoro svolto](#Lavoro-svolto)
3. [Risultati](#Risultati)
4. [Riproduzione dei risultati](#Riproduzione-dei-risultati)

## Descrizione del progetto

L'obiettivo di questo progetto è quello di analizzare una tecnica di adattamento online in un contesto multi-robotico.

Si vuole dotare ogni robot di un *core computazionale* immutabile, in grado di fornire la capacità di compiere operazioni complesse ma non conosciute a priori. L'adattamento dei robot consiste nel capire come meglio utilizzare questo *core* al fine di massimizzare una certa funzione di *fitness*. Tale funzione modella il task che il robot deve compiere. Il contesto è multi-robotico perchè nello stesso ambiente sono presenti molteplici robot con la stessa la funzione di *fitness*.

Il robot cerca di sfruttare al meglio la propria capacità computazionale per massimizzare la funzione di fitness nell'ambiante in cui è immerso; questo avviene modificando gli allacciamenti tra il *core* e i propri sensori/attuatori. Monitorando l'andamento della *fitness* il robot determina se una particolare configurazione è migliore o peggiore della precedente, mantenendo sempre la migliore trovata. L'adattamento segue una logica di computazione evulutiva, senza però andare a mutare la logica interna del *core*.

//In natura?

### Task
La funzione di fitness dei robot modella il task del moto rettilineo uniforme con evitamento degli ostacoli, viene definita in questo modo:

![](https://i.imgur.com/BrqgC6I.png)


dove:
 - $\theta$ è il valore di prossimità massimo letto dai sensori presenti sul robot. $\theta \in [0, 1]$ minore è $\theta$ più si è lontani da eventuali ostacoli.
 - $l$ indica la potenza erogata al motore sinistro, $l \in [0, 1]$ con 0 il motore è spento.
 - $r$ indica la potenza erogata al motore destro, $r \in [0, 1]$ con 0 il motore è spento.

In una fase successiva del progetto si analizzerà un secondo task in cui i robot devono muoversi con un moto rettilineo uniforme, evitando gli ostacoli e rimanendo nella metà orizzontale dell'arena in cui essi sono partiti.
Dunque: 

![](https://i.imgur.com/ohImSzw.png)


dove $\alpha = 1$ se il robot si trova nella propria metà, altrimenti $\alpha = 0$.

### Rete Booleana
In questo progetto il *core computazionale* è una **rete booleana** di moderate dimensioni (100 nodi e con $k=3$). In Figura 1 è mostrata la rappresentazione dell'interazione fra sensori - rete booleana - attuatori
![](https://i.imgur.com/ilKsUR1.png)
*Figura 1: i sensori dei robot sono collegati in maniere non fissa ad alcuni nodi della rete booleana. Questi nodi verranno sovrascritti con il valore (binarizzato) dei sensori ad ogni step di simulazione. Lo stato della rete booleana avanza aggiornando i valori di ogni nodo ad ogni step. Gli attuatori sono connessi in modo non fisso ad alcuni nodi. Gli attuatori hanno dunque un comportamento on/off: in questo caso i motori vengono accesi a piena potenza o spenti sulla base dell'attivazione del nodo di output.*

Per ciascun robot la rete booleana viene inizializzata all'avvio della simulazione sulla base dei parametri forniti dall'esterno:
 - nodi: numero di nodi della rete
 - bias: bias sulla generazione delle funzioni booleane dei nodi
 - k: numero di interconnesioni fra i nodi
 - self_loops: indica se è possibile avere dei nodi il quale output è anche un proprio input
 - override_output_nodes_bias: se è impostato a _true_ le funzioni dei nodi di output vengono sovrascritti temporamenante (finchè gli allacciamenti ai nodi di output non variano) con una funzione a bias 0.5

Ogni robot genera una propria rete booleana: la configurazione è uguale per tutti i robot ma le varie istanze sono diverse in quanto il random seed è diverso per ciascun robot. La rete booleana generata rimarrà invariata durante tutta la simulazione.

### Robot e ambiente
Dal punto di vista fisico ogni robot è simulato attraverso il modello del *foot-bot*. Ogni robot è dotato di:
 - **differential steering**: attuatore che permette di controllare i motori delle due ruote.
 - **footbot proximity**: sensori che permettono di individuare oggetti intorno al robot.
 - **positioning**: sensore virtuale che permette di conoscere l'esatta posizione del robot all'interno dell'arena.
 
L'ambiente invece è definito come un'arena quadrata con al centro un ostacolo. La superfice è piana e priva di colorazione.
L'arena e i robot si presentano in questo modo:

<p align="center">
  <img src="https://i.imgur.com/JoYvIJq.png"/>
</p>

I robot si muovono con una velocità più elevata del normale (5 cm/s) per garantire una più elevata interazione fra i robot. L'unico mezzo di interazione tra i robot sono i sensori di prossimità e i motori, dunque non possono distinguere un ostacolo fissato (muro) da un robot in alcun modo.

Le simulazioni sono composte da 10 robot con un posizionamento e rotazione iniziale casuale.

### Obiettivi
Il progetto ha i seguenti obiettivi:
 - determinare quali classi di reti booleane offrono migliori possibilità di adattamento del robot (caotiche, ordinate, critiche, self loops) sulla base del valore di fitness calcolato.
 - determinare quali mutazioni portino ad un più veloce e/o migliore adattamento del robot (numero di rewires, numero di nodi di input) sulla base del valore di fitness calcolato.
 - determinare l'emergere di comportamenti particolari, sia individuali che complessivi, tra i controller che hanno prodotto un elevato valore fitness.

Sarà interessante analizzare l'interazione che i robot troveranno più conveniente: cooperativa, competitiva o neutrale. Da sottolineare che il task in question è indipendente dagli altri robot: può essere perseguito con successo senza l'ausilio o il contrasto da parte di altri robot, tuttavia è ovvio che il risultato di una prestazione è influenzata in vari modi dal comportamento degli altri.

[//]: # (che ruolo giocano le misure di complessità?)

## Lavoro svolto

Il progetto nasce da un precedente lavoro svolto da **TODO** dal quale è stata ereditata l'implementazione di un controller con esattaente le caratteristiche necessarie per il progetto: una rete booleana immutabile che controlla il funzionamento dei motori e che viene perturbata dai valori dei sensori. Il controller muta le interconnesioni mantenendo la configurazione migliore della rete booleana sulla base della prestazione calcolate dalla funzione di fitness.
Il lavoro svolto è stato quello di adattare il controller per un contesto multi robotico e per estrarre i dati necessari a trarre conclusioni. Inoltre è stato sviluppato un progetto che permette di lanciare le simulazioni in modo automatizzato, salvare i dati raccolti e generare le informazioni richieste.

### Controller
Il controller è stato scritto in linguaggio Lua.
Il funzionamento del controller può essere così descritto:

#### Inizializzazione
All'avvio della simulazione il controller inizializza una rete booleana sulla base della configurazione ricevuta, essa verrà mantenuta per l'intera durata della simulazione. Quest'ultima verrà suddivisa in molteplici test di durata (numero di step) prefissata: durante l'esecuzione di un test il controller del robot rimane invariato, facendo muovere il robot nell'arena sulla base del controllo della rete booleana e accumulando il valore di fitness calcolato ad ogni step (tick).

#### Step
Ad ogni step il controller sovrascrive ogni nodo di input attuale delle rete booleana con il valore binarizzato del corrispettivo sensore. Successivamente viene calcolato il nuovo stato della rete booleana e determinato il valore dei nodi di output. Questi valori vengono utilizzati per controllare i motori del robot. Al termine di ogni step viene valutata la funzione di fitness ed il valore ricavato viene sommato ad un totale parziale relativo al test attuale. Infine vengono stampati in standard output una serie di dati relativi al robot che serviranno per analizzarne il comportamento a posteriori.

#### Test
Al termine di ogni test l'attuale configurazione (nodi di input e nodi di output) viene confrontata con la precedente: se ha ottenuto un migliore o uguale valore di fitness essa diventa la nuova miglior configurazione, altrimenti rimane la precedente. L'attuale miglior configurazione viene mutata (sulla base delle impostazioni della simulazione) andando a modificare gli allacciamenti tra sensori e nodi di input, e tra nodi di output e attuatori. In poche parole vengono scelti casualmente altri nodi di input e di output. Il valore della fitness viene azzerato e un nuovo test ha inizio.


### Esecuzione delle simulazioni

Il lavoro più corposo di questo progetto è stato quello di configurare un ambiente automatizzato per l'esecuzione di vari esperimenti, l'estrazione dei dati e la loro analisi. In questa sezione vengono descritte le varie configurazioni utilizzate per gli esperimenti, come vengono passate al controller e in che modo le simulazioni vengono eseguite. 

#### Configurazione di una simulazione
Ogni simulazione necessità di una configurazione, quest'ultima non potrà più essere alterata dopo l'avvio della simulazione. Sono state individuate 72 configurazioni distinte da simulare. I parametri della configurazione base che non vengono alterati sono nella seguente lista suddivisi per categoria:
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
     - input node count: **variabile**
     - output node count: 2
     - self loops: **variabile**
     - override output nodes bias: true

I valori applicati nei campi variabili sono:

| bias |max output rewires|self loop|node input count|half variation
|:----:|:----:|:----:|:----:|:----:|
|0.1|0|true|24|false|
|0.5|1|false|8|true with feed|
|0.79||||true without feed|

Ad ogni configurazione base viene applicata una combinazione di variazioni con un totale di $3*2*2*3*2 = 72$ possibili configurazioni.

Un esempio di istanza di configurazione in formato JSON è la seguente:
```json
{
   "simulation":{
      "ticks_per_seconds":10,
      "experiment_length":7200,
      "network_test_steps":400,
      "print_analytics":true
   },
   "robot":{
      "proximity_threshold":0.1,
      "max_wheel_speed":5,
      "stay_on_half":false,
      "feed_position":false
   },
   "bn":{
      "max_input_rewires":2,
      "input_rewires_probability":1,
      "max_output_rewires":0,
      "output_rewires_probability":1,
      "use_dual_encoding":false,
      "options":{
         "node_count":100,
         "nodes_input_count":3,
         "bias":0.1,
         "network_inputs_count":8,
         "network_outputs_count":2,
         "self_loops":false,
         "override_output_nodes_bias":true
      }
   }
}
```

#### Lancio automatizzato delle simulazioni

Per il lancio delle simulazioni, la raccolta e analisi dei dati è stato sviluppato un progetto parallelo in linguaggio Scala. Il progetto è nella cartella `analyzer` e contiene tre sezioni eseguibili. La sezione che si occupa del lancio delle simulazioni è denominata `Experiments`.

Per ogni configurazione distinta vengono lanciate 100 simulazioni al fine di ottenere risultati statisticamente validi.

Per ogniuna delle 7200 configurazioni (72*100) è necessario avviare una relativa simulazione. Una determinata configurazione viene passata alla simulazione attraverso una fase di preprocessing del file di simulazione *.argos*. La configurazione stessa viene passata al controller di ogni robot in formato JSON sfruttando il tag `params` all'interno del tag `lua_controller` nel file *.argos*. Nel file `lua/config_simulation.argos` è possibile notare che in alcuni punti sono presenti dei _placeholder_ che verranno sostituiti in fase di preprocessing con i valori della configurazione. Questo permette di definire un singolo file di simulazione .argos che muta in base alla configurazione che si vuole simulare. Ugualmente per il controller la configurazione giungerà in formato JSON e ne varierà il funzionamento.


#### Dati estratti

Ogni simulazione genera in standard output un flusso di informazioni derivanti dai robot. Come accennato in precedenza ogni robot ad ogni step stampa in standard output un blocco di dati relativo a sè stesso. 
All'inizio di ogni test il robot genera le seguenti informazioni in formato JSON:
 - id del robot `"id"`
 - step attuale `"step"`
 - fitness attuale `"fitness"`
 - informazioni sulla configurazione della rete booleana `"boolean_network"`
     - matrice di boolean che determina le funzioni booleane dei nodi `"functions"`
     - matrice di indici che determinano le connesioni tra i nodi `"connections"`
     - indici dei nodi di input `"inputs"`
     - indici dei nodi di output `"outputs "`
 - array di boolean indicanti lo stato dei nodi della rete booleana `"states"`
 - array di due valore double che indicano la posizione del robot nell'arena`"position"` 

Per esempio:
```json
{
   "step":0,
   "fitness":0,
   "id":"fb0",
   "states":[true,...,true],
   "boolean_network":{
      "outputs":[71,84],
      "overridden_output_functions":[
         [true,true,false,false,true,true,false,false],
         [true,false,false,false,true,true,true,false]],
      "functions":[
         [false,false,true,false,false,false,false,false],
         ...
         ...
         [false,false,false,true,false,false,false,false]
      ],
      "connections":[
         [70,34,38],
         ...
         ...
         [45,88,19]
      ],
      "inputs":[76,11,30,38,2,21,56,1]
   },
   "position":[1.27648,0.29925]
}
```

Ad ogni step il robot genera le stesse informazioni sopra citate omettendo il campo `"boolean_network"` siccome rimane invariato, per esempio:
```json
{
   "states":[false,...,false],
   "id":"fb5",
   "fitness":0.1274,
   "position":[-0.3812343555514,0.91377662004311],
   "step":22478
}
```
Ogni JSON è compattato in una singola riga e stampato in standard output. Ad ogni step dunque vengono stampati _n_ JSON in _n_ righe dove _n_ è il numero di robot presenti nella simulazione. Un'intera simulazione dunque genera un totale di 7200 (lunghezza esperimento) * 10 (tick/second) * 10 (numero di robot) = 720000 record JSON.

Salvando l'output derivante dalla simulazione si ottiene un file composto da alcune stampe iniziali del simulazione argos seguite da una lunga serie di record JSON suddivisi per righe. L'esecuzione di `Experiments` salva questo flusso output su un file denominato sulla base della configurazione lanciata. Inoltre la configurazione stessa viene serializzata in formato JSON ed aggiunta al file come prima riga. Il file viene compresso con il tool `gzip` per risparmiare una notevole quantità di spazio. Lo schema di un file generato da una simulazione è dunque:
```
{configurazione in formato JSON}
alcune stampe argos
...
{record JSON robot 1 step 1}
{record JSON robot 2 step 1}
{record JSON robot 3 step 1}
...
{record JSON robot 1 step 1000}
{record JSON robot 2 step 1000}
{record JSON robot 3 step 1000}
...
```



### Analisi dei dati

Una volta che le simulazioni sono terminate si ottiene un insieme di file in formato gzip salvati su disco. In questa fase vengono estratte una serie di informazioni contenute in strutture di alto livello per facilitare l'analisi delle simulazioni. L'estrazione di queste strutture dati di alto livello richiede molto tempo e per questo si è deciso di introdurre una fase intermedia dove tali strutture vengono estrapolate e salvate in formato JSON su un file provissorio. In questo modo è possibile analizzare i dati prodotti dalle simulazioni in modo molto più efficiente.

La sezione del progetto che si occupa di questa fase è denominata `Loader`. Se messo in esecuzione esso elabora per ogni file di simulazione gzip un file JSON contenente una struttura di questo tipo:
 - Per ogni robot viene generato un record con i seguenti campi:
     - id del robot `robot_id`
     - filename percorso del file da cui sono stati estratti i dati `filename`
     - la configurazione della simulazione `config`
     - per ogni test il valore finale della fitness `fitness_values`
     - la miglior rete booleana del robot durante l'esperimento `best_bn`
 - I record sono messi in un struttura ad array, ad esempio:
    ```json
    [
        {
            "robot_id": "fb1",
            "filename": "/data/el=7200-rc=10-bs=0.1-mir=2-mor=0-sl=false-nic=24-hv=false-fp=false-97.json",
            "config": { "simulation": { "ticks_per_seconds": 10, ... }, ...}
            "fitness_values": [0, 2.34, 0, ..., 1.23],
            "best_bn": { "functions": [[...], ..., [...]], "connections": [...], "inputs": [...], "outputs": [...] }
        },
        {
            "robot_id": "fb2",
            ...
        }
        ...
    ]
    ```


Al termine della fase di loading è possibile andare a generare alcuni grafici atraverso la sezione `Analyzer`. Questo eseguibile utilizzerà le strutture di alto livello generate dal loader. 

Questa struttura `Experiments` -> `Loader` -> `Analyzer` ha permesso di lavorare in una modalità molto più agile, in quanto un cambiamento di qualsiasi natura nel loader o nell'analyzer non implica un riesecuzione delle simulazioni. Inoltre si possiedono salvati su disco tutti i dati grezzi generati dalle simulazioni e ciò può tornare molto utile in una fase di review / debugging.
In un calcolatore moderno `Experiments` impiega circa una settimana per terminare , il `Loader` impiega ore e l'`Analyzer` secondi. Questi ordini di grandezza giustificano anche la suddivisione del progetto in tre sotto sezioni.




## Risultati

In questo capitolo si mostrano i risultati raccolti e si analizzano al fine di soddifare l'obiettivo del progetto.

Di seguito vi è riproposta la tabella di parametri testati nelle simulazioni:

| bias |max output rewires|self loop|node input count|half variation
|:----:|:----:|:----:|:----:|:----:|
|0.1|0|true|24|false|
|0.5|1|false|8|true with feed|
|0.79||||true without feed|

### Bias
per prima cosa si indaga su quale effetto abbia il parametro bias sulle performance ottenute. Il seguente grafico raffigura l'andamento media della fitness. I gruppi sono per bias (0.1, 0.5, 0.79) e sono utilizzati i dati di tutti i 7200 esperimenti. Come si può notare le reti caotiche (bias 0.5) sono quelle che mediamente ottengono peggiori prestazioni e che migliorano con più lentezza. Le reti ordinate invece (bias 0.1) ottengono modeste prestazioni medie, avento anche il miglioramento più veloce nei primi 30 test. Le reti critiche (bias 0.79) sono quelle che ottengono prestazioni media migliori.
![](https://i.imgur.com/geXnGXi.png)

Dal seguente box si determina che tuttavia alcune singole prestazioni molto buone sono ottenute anche da robot con reti ordinate, mentre nella media le reti critiche offrono sempre una miglior prestazione.
![](https://i.imgur.com/H9YIMfd.png)

### Output rewires

### Node input count

### Dettaglio
Ora sono esposti alcuni grafici con configurazioni singole, senza quindi raggruppare configurazioni differenti sotto parametri comuni.


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
 - Non vi è un comportamento complessivo perchè evolvono in modo indipendente? nel senso che non vi è un concetto di epoca nella quale nell'epoca successiva si copiano le migliori reti booleane ma invece ognuno si tiene la sua.


## Riproduzione dei risultati

I requisiti per eseguire nuovamente gli esperimenti sono:
 - [Argos3](https://www.argos-sim.info/) (v3.0.0)
     - eseuire `argos3 --version` per controllare la versione
 - Il repository `git clone https://github.com/edobrb/progetto-irs.git`
 - 16 GB di RAM liberi
 - 160GB di spazio su disco

### Struttura del repository
Nella cartella `lua` sono contenuti tutti i sorgenti necessari per le simulazioni eseguite con Argos: controller, rete booleana, descrizione dell'ambiente di simulazione e alcune utilità.

Nella cartella `analyzer` è contenuto un progetto Scala compilabile ed eseguibile attraverso lo strumento `sbt`. È necessario usare una CLI per lanciare il tool `sbt` o eventualmente è possibile utilizzare un IDE (consiglio IntelliJ).

### Lanciare gli esperimenti
Per prima cosa è necessario spostarsi all'interno del progetto `analyzer`.
Il progetto è suddiviso in tre sezioni principali:
 - **Experiments**: questa sezione ha il compito di lanciare le simulazioni e salvarne gli output su disco. È necessario specificare la locazione della cartella di lavoro `lua`, il file di simulazione da utilizzare (.argos), la cartella dove verranno salvati i dati raccolti e opzionalmente il grado di parallelismo che si vuole utilizzare (quante simulazioni lanciare simultaneamente).
     - `sbt "runMain Experiments ../lua config_simulation.argos /storage/data 8"`
 - **Loader**: una volta ottenuti i risultati dalla sezione Experiments è necessario elaborare i file per ricavarne una versione più compatta con soltato le informazioni d'interesse. Per ogni file generato da Experiments verrà creato un nuovo file in formato JSON con alcune informazioni estratte da esso. Questa fase permetterà di analizzare i risultati con molta più efficienza.
     - `sbt "runMain Loader ../lua config_simulation.argos /storage/data 4"`
     - per ogni grado di parallelismo servono circa 4 GB di memoria RAM
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


