package pw.mihou.rosedb.enums;

public enum Listening {

    GET("GET"), DELETE("DELETE"), UPDATE("UPDATE"), ADD("ADD"), DROP("DROP"), AGGREGATE("AGGREGATE"), REVERT("REVERT");

    public final String value;

    Listening(String value) {
        this.value = value;
    }

}
