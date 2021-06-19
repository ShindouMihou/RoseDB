package pw.mihou.rosedb.enums;

/**
 * This is where you add more
 * methods to the list, you can add an enum here
 * and register it to your listener under type().
 */
public enum Listening {

    /**
     * The listener is listening to requests
     * with the method GET.
     */
    GET("GET"),

    /**
     * The listener is listening to requests
     * with the method DELETE.
     */
    DELETE("DELETE"),

    /**
     * The listener is listening to requests
     * with the method UPDATE.
     */
    UPDATE("UPDATE"),

    /**
     * The listener is listening to requests
     * with the method ADD.
     */
    ADD("ADD"),

    /**
     * The listener is listening to requests
     * with the method DROP.
     */
    DROP("DROP"),

    /**
     * The listener is listening to requests
     * with the method AGGREGATE.
     */
    AGGREGATE("AGGREGATE"),

    /**
     * The listener is listening to requests
     * with the method REVERT.
     */
    REVERT("REVERT");

    /**
     * The value of the method on the request
     * that it should respond to.
     *
     * For example, if the request specifically mentions
     * that the method is GET then all listeners with GET will
     * respond to it as long as the method matches the GET value here.
     */
    public final String value;

    /**
     * Creates a new Listening type.
     *
     * @param value The method field value it will respond to.
     */
    Listening(String value) {
        this.value = value;
    }

}
