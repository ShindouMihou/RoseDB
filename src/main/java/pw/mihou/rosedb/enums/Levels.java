package pw.mihou.rosedb.enums;

public enum Levels {

    INFO("INFO", -1), ERROR("ERROR", 1), DEBUG("DEBUG", 2), WARNING("WARNING", 0);

    public final String name;
    public final int id;

    Levels(String name, int id){
        this.name = name; this.id = id;
    }

}
