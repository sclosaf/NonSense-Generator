package unipd.nonsense.util;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/*
 * Aggiungere il logInfo come funzione
 *
 * Configurare il file .xml affinché stampi su file tutti i 6 livelli fino al
 * trace, mentre a terminale solo da info in su, escludendo trace e debug
 *
 * Vedi se si può ridurre la rotazione dei log in modo tale che i file non
 * diventino infiniti se si fa girare più volte il progetto, cioè se ne creino
 * di distinti ad ogni esecuzione ma si rimuova l'ultimo se per esempio sono
 * più di un tot
 *
 * So che alcune librerie possono usare anche loro dei logger e quindi creano
 * "rumore" nel file di log con stampe loro, vedi se esiste qualche
 * configurazione possibile per mantenere le stampe su file solo per il nostro
 * progetto, a livello trace e magari alzarlo per tutto il resto, ad esempio
 * fino a error pensavo
 *
 * Vedi se esiste un modo per impostare a runtime nella classe il livello di
 * severità ad esempio se da terminale io passo al CLI un comando che imposta
 * l'opzione programmatore, allora abbassa il livello di severità (della sola
 * console a debug al posto che di info, per avere più stampe) mentre se lo
 * riseleziono posso reimpostarlo per le successive stampe, come fosse un
 * interruttore essenzialmente se si riesce a creare qualche meccanismo di
 * gestione della verbosità a runtime è possibile che possa servire un'observer
 * affinché se chiamato aggiorni la verbosità da terminale (la gestione
 * dell'input da cli verrà fatta in un altra classe, a te basta implementare un
 * booleano che definisca se si logga in modalità sviluppatore o meno)
 *
 * Se riesci vedi se si può configurare il file .xml per effettuare log
 * asincroni, in modo tale che il logging non pesi sul carico del programma,
 * potrebbe servire una dipendenza tipo disruptor
 */

public class LoggerManager
{
	private final Logger logger;

	public LoggerManager(Class <?> myClass)
	{
		this.logger = LogManager.getLogger(myClass);
	}

	public void logTrace(String entry)
	{
		logger.trace(entry);
	}

	public void logDebug(String entry)
	{
		logger.debug(entry);
	}

	public void logWarn(String entry)
	{
		logger.warn(entry);
	}

	public void logWarn(String entry, Throwable eccept)
	{
		logger.warn(entry, eccept);
	}

	public void logError(String entry)
	{
		logger.error(entry);
	}

	public void logError(String entry, Throwable eccept)
	{
		logger.error(entry, eccept);
	}

	public void logFatal(String entry)
	{
		logger.fatal(entry);
	}

	public void logFatal(String entry, Throwable eccept)
	{
		logger.fatal(entry, eccept);
	}
}
