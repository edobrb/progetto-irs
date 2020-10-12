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

![formula](https://render.githubusercontent.com/render/math?math=\LARGE(1-\theta)*(1-\sqrt{|l-r|})*\frac{l%2Br}{2})


dove:
 - ![formula](https://render.githubusercontent.com/render/math?math=\theta) è il valore di prossimità massimo letto dai sensori presenti sul robot. ![formula](https://render.githubusercontent.com/render/math?math=\theta\in[0,1])  minore è ![formula](https://render.githubusercontent.com/render/math?math=\theta)  più si è lontani da eventuali ostacoli.
 - ![formula](https://render.githubusercontent.com/render/math?math=l) indica la potenza erogata al motore sinistro, ![formula](https://render.githubusercontent.com/render/math?math=l\in[0,1]) con 0 il motore è spento.
 - ![formula](https://render.githubusercontent.com/render/math?math=r) indica la potenza erogata al motore destro, ![formula](https://render.githubusercontent.com/render/math?math=r\in[0,1]) con 0 il motore è spento.

In una fase successiva del progetto si analizzerà un secondo task in cui i robot devono muoversi con un moto rettilineo uniforme, evitando gli ostacoli e rimanendo nella metà orizzontale dell'arena in cui essi sono partiti.
Dunque: 

![formula](https://render.githubusercontent.com/render/math?math=\LARGE(1-\theta)*(1-\sqrt{|l-r|})*\frac{l%2Br}{2}*\alpha)


dove ![formula](https://render.githubusercontent.com/render/math?math=\alpha=1) se il robot si trova nella propria metà, altrimenti ![formula](https://render.githubusercontent.com/render/math?math=\alpha=0).

### Rete Booleana
In questo progetto il *core computazionale* è una **rete booleana** di moderate dimensioni (100 nodi e con k=3). In Figura 1 è mostrata la rappresentazione dell'interazione fra sensori - rete booleana - attuatori
![](https://brb.dynu.net/image/core.png)
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

![](https://brb.dynu.net/image/arena.png)

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

Il progetto nasce da un precedente lavoro svolto da Alessandro Gnucci dal quale è stata ereditata l'implementazione di un controller con esattaente le caratteristiche necessarie per il progetto: una rete booleana immutabile che controlla il funzionamento dei motori e che viene perturbata dai valori dei sensori. Il controller muta le interconnesioni mantenendo la configurazione migliore della rete booleana sulla base della fitness calcolata.
Il lavoro svolto è stato quello di adattare il controller per un contesto multi robotico e per estrarre i dati necessari a trarre conclusioni. Inoltre è stato sviluppato un progetto parallelo che permette di lanciare le simulazioni in modo automatizzato, salvare i dati raccolti e generare le informazioni richieste.

### Controller
Il controller è stato scritto in linguaggio Lua.
Il funzionamento del controller può essere così descritto:

#### Inizializzazione
All'avvio della simulazione il controller inizializza una rete booleana, che rimarrà invariata per tutta la simulazione, sulla base della configurazione ricevuta. La simulazione verrà suddivisa in molteplici test di durata (numero di step) prefissata: durante l'esecuzione di un test il controller del robot rimane invariato, facendo muovere il robot nell'arena sulla base del controllo della rete booleana e accumulando il valore di fitness calcolato ad ogni step (tick).

#### Step
Ad ogni step il controller sovrascrive ogni nodo di input attuale delle rete booleana con il valore binarizzato del corrispettivo sensore. Successivamente viene calcolato il nuovo stato della rete booleana e determinato il valore dei nodi di output. Questi valori vengono utilizzati per controllare i motori del robot. Al termine di ogni step viene valutata la funzione di fitness ed il valore ricavato viene sommato ad un totale parziale relativo al test attuale. Infine vengono stampati in standard output una serie di dati relativi al robot che serviranno per analizzarne il comportamento a posteriori.

#### Test
Al termine di ogni test l'attuale configurazione (nodi di input e nodi di output) viene confrontata con la precedente: se ha ottenuto un migliore o uguale valore di fitness essa diventa la nuova miglior configurazione, altrimenti rimane la precedente. L'attuale miglior configurazione viene mutata (sulla base delle impostazioni della simulazione) andando a modificare gli allacciamenti tra sensori e nodi di input, e tra nodi di output e attuatori. In poche parole vengono scelti casualmente altri nodi di input e di output. Il valore della fitness viene azzerato e un nuovo test ha inizio.


### Esecuzione delle simulazioni

Il lavoro più corposo di questo progetto è stato quello di configurare un ambiente automatizzato per l'esecuzione di vari esperimenti, l'estrazione dei dati e la loro analisi. In questa sezione vengono descritte le varie configurazioni utilizzate per gli esperimenti, come vengono passate al controller e in che modo le simulazioni vengono eseguite. 

#### Configurazione di una simulazione
Ogni simulazione necessita di una configurazione, quest'ultima non potrà più essere alterata dopo l'avvio della simulazione. Sono state individuate 72 configurazioni distinte da simulare. I parametri della configurazione base sono nella seguente lista suddivisi per categoria:
 - simulazione:
     -  ticks per seconds: 10
     -  experiment length: 7200 (seconds)
     -  network test steps: 400 (ticks)
     -  robot count: 10
 - robot:
     - proximity threshold: 0.1
     - max wheel speed: 5 (cm/s)
     - stay on half variation: **variabile**
     - feed position: **variabile**
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
|0.5|1|false|8|true with position feed|
|0.79||||true without position feed|

**Nota**: con feed position si intende che durante l'inizializzazione del controller viene create una rete booleana con un nodo di input in più, questo servirà per comunicare alla rete se il robot si trovi o meno sulla metà arena corretta. Dunque, il nodo di input in più viene collegato ad un sensore virtuale che determina ad ogni step se il robot è nella regione corretta.

Ad ogni configurazione base viene applicata una combinazione di variazioni con un totale di ![formula](https://render.githubusercontent.com/render/math?math=3*2*2*3*2=72) possibili configurazioni.

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

Salvando l'output derivante dalla simulazione si ottiene un file composto da alcune stampe iniziali della simulazione argos seguite da una lunga serie di record JSON suddivisi per righe. L'esecuzione di `Experiments` salva questi flussi output su file denominati sulla base della configurazione lanciata. Inoltre la configurazione stessa viene serializzata in formato JSON ed aggiunta al file come prima riga. Il file viene compresso con il tool `gzip` per risparmiare una notevole quantità di spazio. Lo schema di un file generato da una simulazione è dunque:
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

Una volta che le simulazioni sono terminate si ottiene un insieme di file in formato gzip salvati su disco. In questa fase vengono estratte una serie di informazioni contenute in strutture di alto livello per facilitare l'analisi dei dati raccolti. L'estrazione di queste strutture dati di alto livello richiede molto tempo e per questo si è deciso di introdurre una fase intermedia dove tali strutture vengono estrapolate e salvate in formato JSON su un file provissorio. In questo modo è possibile analizzare i dati prodotti dalle simulazioni in modo molto più efficiente.

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

In questo capitolo si mostrano i risultati raccolti e li si analizzano al fine di soddifare l'obiettivo del progetto.

Di seguito vi è riproposta la tabella dei parametri testati:

| bias |max output rewires|self loop|node input count|half variation
|:----:|:----:|:----:|:----:|:----:|
|0.1|0|true|24|false|
|0.5|1|false|8|true with feed|
|0.79||||true without feed|

Si analizzano i risultati usando principalmente due tipi di grafici: un boxplot contenente la miglior fitness raggiunta da ogni robot in una simulazione, e un grafico della curva di fitness media. Quest'ultima viene calcolata usando la sua curva di fitness di ogni robot per poi farne una media; la curva di fitness è il valore massimo della fitness che un robot è riuscito a raggiungere fino al test in questione, dunque è una curva sempre crescente.

Per prima cosa si analizzano i risultati raggruppandoli sulle singole variazioni che si sono applicate alla configurazione. Dunque per ogni parametro vengono raggrupapte le 7200 simulazioni per un singolo parametro e poi elaborate le due misurazioni.

### Bias
Il primo parametro preso in esame è il bias. Come si può notare dai grafici sottostanti le reti ordinate (bias 0.1) sono quelle che mediamente ottengono peggiori prestazioni e che migliorano con più lentezza. Le reti caotiche invece (bias 0.5) ottengono modeste prestazioni medie, avento anche il miglioramento più veloce nell'arco dei primi 30 test. Le reti critiche (bias 0.79) sono quelle che ottengono prestazioni nella media migliori e che con una simulazione più lunga probabilmente porterebbero a risultati finali migliori.

Dal boxplot si determina che alcune singole prestazioni molto performanti sono ottenute anche da robot con reti ordinate e caotiche, mentre nella media le reti critiche offrono sempre una miglior prestazione. Ciò tuttavia non esclude che con una simulazione molto più lunga le reti caotiche ed oridinate raggiungano prima il loro limite, mentre quelle critiche potrebbero raggiungere livelli ben più elevati di fitness per via della loro natura.


![](https://brb.dynu.net/image/bias-fitness-curve.png) | ![](https://brb.dynu.net/image/bias-boxplot.png)
:-:|:-:

Questi risultati sono in linea con le aspettative in quanto le reti caotiche offrono una gamma di comportamenti più utili al task in questione: le reti caoutiche non hanno un comportamento prevedibile sulla base delle perturbazioni effettuate, le reti ordinate si ristabilizzano in fretta dopo una perturbazione, le reti critiche sono caraterrizate da un comprtamento ne caotico ne ordinato.

### Output rewires

La variazione di almeno un nodo di output alla fine di ogni test porta a grandi benifici in termini di prestazioni ottenute. I seguenti grafici mostrato i risultati nel caso in cui non si effettui mai un cambiamento ai nodi di output e il caso in cui ad ogni test si cambi (rewires) un nodo di output.

Il boxplot mostra che mediamente fare rewires dei nodi di output porta a migliori prestazioni. Tuttavia ciò non ha escluso ad alcuni robot con *output_rewires=0* di raggiungere elevate prestazioni. Ciò è probabilmente dovuto al fatto che tali robot sono caraterizzati da controller inizializzati con di nodi di output validi.

![](https://brb.dynu.net/image/or-fitness-curve.png) | ![](https://brb.dynu.net/image/or-boxplot.png)
:-:|:-:

Questo risultato rientra nelle aspettative in quanto facendo rewires anche ai nodi di output il controller ha molte più configurazioni possibili da esplorare, e nel caso in cui si ritrovi inizializzato a nodi di output inefficaci questi non saranno vincolanti fino al termine della simulazione.



### Node input count

Come mostrato dai seguenti grafici il numero di nodi di input da utilizzare influenza la prestazione e la rapidità con cui la si ottiene in maniera notevole. Ciò rientra nelle aspettative in quanto usare 24 nodi su 100 per l'input implica che il 24% dei nodi debbano avere un effetto utile sui due nodi di output al fine di far muovere il robot nella maniera richiesta. Diminuendo il numero di nodi di input si rilassa questo vincolo, dichiarando che soltanto 8 nodi della rete dovranno avere (sulla base delle percezioni) un effetto utile sui nodi di output.

Inoltre, diminuendo il nuero di input a 8 aumenta la possibilità di individuare nodi che avranno un buon effetto sui due nodi di output. Questo perchè diminuisce lo spazio di combinazioni di configurazioni da esplorare (![formula](https://render.githubusercontent.com/render/math?math=C_{100,8}<C_{100,24})) e ciò garantisce una più veloce convergenza alla miglior fitness ottenibile con la rete a disposizione. I 24 sensori di prossimità in questo caso vengono raggruppati di 3 in 3.

![](https://brb.dynu.net/image/nic-fitness-curve.png) | ![](https://brb.dynu.net/image/nic-boxplot.png)
:-:|:-:

Da tenere in considerazione che sia con 8 che con 24 nodi di input il massimo numero di input rewires ad ogni test è fissato a 2. Se si mantenesse proporzionale il numero di rewires al numero di nodi forse la differenza di prestazioni non si noterebbe o i risultati sarebbero differenti.

### Self loops

Dai seguenti risultati non è possibile determinare se il parametro *self_loops* abbia qualche influenza sulle prestazioni raggiunte.
![](https://brb.dynu.net/image/self-loops-fitness-curve.png) | ![](https://brb.dynu.net/image/self-loops-boxplot.png)
:-:|:-:
Dal test di wilcoxon sui valori del boxplot si evince che ...

### Variazioni

Dai seguenti grafici è possibile notare come la variante della metà arena determini un peggioramento in termini di fitness ottenuta dei robot. Questo è un risultato aspettato in quanto in certi momenti i robot si trovano in zone dove la fitness è per definizione 0. Un fenomeno inaspettato è quello che le due varianti con e senza feed hanno prodotto il medesimo risultato in termini di fitness. Ci si aspettava che i controller che sapevano se si trovavano sulla metà corretta otennessero miglior risultati rispetto ai controller che dovevano basarsi solo sul feedback del valore della fitness al termine del test. 

Tuttavia, i risultati in entrambi i casi mostrano che i controller riescono ad utilizzare in qualche modo il feedback fornito dalla funzione di fitness. Se non fosse stato così i controller della variante "metà arena" avrebbero dovuto ottenere mediamente la metà della fitness ottenuta dalla variante "intera arena". I risultati invece mostrano che mediamente i robot si muovono in linea retta maggiormente nella metà corretta.
![](https://brb.dynu.net/image/variation-fitness-curve.png) | ![](https://brb.dynu.net/image/variation-boxplot.png)
:-:|:-:

Non si esclude tuttavia la possibilità della presenza presenza di un bug all'interno del progetto che può aver portato a risultati scoretti. In linea generale i risultati sono come da previsione, nei casi contrari è bene verificare la correttezza delle simulazioni per evitare di fare ipotesi azzardate.




### Dettaglio
Per completezza sono esposti i grafici delle singole configurazioni senza reaggruppamenti di configurazioni diverse (ad eccezioen del self-loop che viene comunque raggruppato). Sono stati suddivisi in 3 categorie per il tipo di variante usata.

Nelle legende i termini significano:
 - B: bias
 - OR: numero di output rewires
 - NIC: numero di nodi di input

|Variante|Curva di fitness media|Miglior fitness al termine|
|:--:|:--:|:--:|
|Intera arena|![](https://brb.dynu.net/image/overall-fitness-curve.png)|![](https://brb.dynu.net/image/overall-boxplot.png)|
|Metà arena|![](https://brb.dynu.net/image/half-overall-fitness-curve.png)|![](https://brb.dynu.net/image/half-overall-boxplot.png)|
|Metà arena - feed|![](https://brb.dynu.net/image/half-feed-overall-fitness-curve.png)|![](https://brb.dynu.net/image/half-feed-overall-boxplot.png)|





### Comportamenti emersi



Visivamente sono state rilanciate alcune simulazioni composte da 10 robot aventi la miglior rete booleana ottenuta durante un insieme di test.

Da questa prova è stato riscontrato che nella variante "intera arena" (con bias=0.79) il miglior controller fa emergere una sorta di comportamento complessivo: tutti i robot finiscono per muoversi in senso antiorario intorno all'ostacolo al centro dell'arena. Questo comportamento fa si che vi siano minori collisioni fra robot e quindi la fitness finale sarà complessivamente più elevata. Questo comportamento può essere categorizzato come cooperativo in quanto tutti i robot ne traggono beneficio.
Il comportamento visivamente è il seguente:

![https://brb.dynu.net/image/example.gif](https://brb.dynu.net/image/example.gif)


Nella variante "metà arena" invece non è visibilmente emerso nessun comportamento complessivo. 

Il comportamento cooperativo descritto nel primo caso probabilmente non è nato durante la simulazione originaria ma bensì soltato durante la simulazione dove tutti i 10 robot hanno la stessa rete booleana. Infatti, è difficile che nasca una qualche sorta di interazione fra robot durante le simulazioni perchè tutti i 10 robot non hanno modo di interagire se non attraverso la loro presenza / non presenza intorno ad altri robot. Inoltre, i robot all'interno di una simulazione sono caratterizzati da reti booleane dello stesso tipo ma di istanza totalmente diversa, causando comportamenti totalmente differenti e ciò sfavorisce ulteriormente la nascita di comportamenti cooperativi.

## Conclusioni

I parametri testati che fanno raggiungere fitness più elevate ai robot in tempi più brevi sono complessivamente: bias: 0.79, output rewires: 1, nodi di input: 8. Mentre non è stato chiaro l'effetto dei parametri self-loops e feed-position. In generale i risultati sono serviti per confermare delle ipotesi iniziali come il fatto che le reti booleane critiche offrano migliori possibilità di sviluppo rispetto a quelle caotiche e ordinate. 

Non sono emersi comportamenti copperativi o competitivi particolari a meno di prendere il controller migliore e copiarlo su tutti i 10 robot in una nuova simulazione.


### Lavori futuri
Sarebbe interessante provare ulteriori variazioni partendo dai parametri più efficaci che si sono individuati per determinare se con una rete così piccola si riescano a raggiungere prestazioni più elevate. Inoltre, sarebbe interessante prolungare la durata di una simulazione per determinare quale sia il limite massimo di fitness raggiungibile. Si potrebbe inoltre variare la composizione della rete booleana incrementando il numero di nodi o di interconnesioni (k) per determinare se quest'ultime offrano più possibilità di adattamento al robot.

Sarebbe interessante introdurre una forma di interazione esplicita fra i robot come ad esempio il range and bearing per scoprire se i robot si adattassero utilizzando anche la comunicazione al fine di cooperare.

Si potrebbe variare il task, l'arena o il robot per determinare come la stessa configurazione si riesca ad adattare in situazioni differenti.

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

[comment]: <> (I file generati da *Experiments* e *Loader* utilizzati per ricavare i risultati descritti in questo documento saranno distribuiti per un periodo non determinato. Il link per il download attraverso protocollo BitTorrent è [magnet link].)






[comment]: <> ( - risultati variante metà - pericolo di come è stato implementato -> se va nella zona a 0 fitness magari ci rimane e non evolve -> comporta sono una più lenta convergenza? - i robot migliori presentano un comportamento avanti, ruota 180, avanti -> se si incastrano fra due muri otterrano una buona fitness)