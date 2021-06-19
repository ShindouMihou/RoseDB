package pw.mihou.rosedb.enums;

public enum Levels {

    /**
     * Record only logs below or equal to INFO.
     */
    INFO("INFO", -1),

    /**
     * Record only logs below or equal to ERROR.
     */
    ERROR("ERROR", 1),

    /**
     * Record only logs below or equal to DEBUG.
     */
    DEBUG("DEBUG", 2),

    /**
     * Record only logs below or equal to WARNING.
     */
    WARNING("WARNING", 0);

    /**
     * The equivalent name of the log level.
     */
    public final String name;

    /**
     * The integer value of the log level.
     */
    public final int id;

    /**
     * Creates a log level with the integer value.
     * @param name The name of the log level.
     * @param id The integer value of the log level.
     */
    Levels(String name, int id) {
        this.name = name;
        this.id = id;
    }

}
