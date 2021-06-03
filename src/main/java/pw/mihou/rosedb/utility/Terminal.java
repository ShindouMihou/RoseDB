package pw.mihou.rosedb.utility;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import pw.mihou.rosedb.enums.Levels;

public class Terminal {

    public static Logger log = (Logger) LoggerFactory.getLogger("RoseDB");
    static {
        log.setLevel(Level.INFO);
    }

    public static Level root = Level.INFO;

    public static void setLoggingLevel(Level level) {
        log.setLevel(level);
        root = level;
    }

    public static void log(Levels level, String message) {
        if(level == Levels.DEBUG) log.debug(message);
        if(level == Levels.ERROR) log.error(message);
        if(level == Levels.INFO) log.info(message);
        if(level == Levels.WARNING) log.warn(message);
    }

    public static void log(Levels level, String message, Object... values) {
        if(level == Levels.DEBUG) log.debug(message, values);
        if(level == Levels.ERROR) log.error(message, values);
        if(level == Levels.INFO) log.info(message, values);
        if(level == Levels.WARNING) log.warn(message, values);
    }

}
