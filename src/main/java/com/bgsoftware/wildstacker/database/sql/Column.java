package com.bgsoftware.wildstacker.database.sql;

public final class Column {

    private final String name;
    private final String type;

    public Column(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

}
