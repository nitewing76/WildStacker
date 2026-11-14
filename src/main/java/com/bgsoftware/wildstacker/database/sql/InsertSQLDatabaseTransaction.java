package com.bgsoftware.wildstacker.database.sql;

import java.util.Collections;
import java.util.List;

public final class InsertSQLDatabaseTransaction extends SQLDatabaseTransaction<InsertSQLDatabaseTransaction> {

    public InsertSQLDatabaseTransaction(String tableName, List<String> columnNames) {
        super(tableName, columnNames);
    }

    @Override
    protected String buildSQL(String tablePrefix) {
        String columns = String.join(", ", this.columnNames);
        String placeholders = String.join(", ", Collections.nCopies(this.columnNames.size(), "?"));
        return "REPLACE INTO " + tablePrefix + this.tableName + " (" + columns + ") VALUES (" + placeholders + ");";
    }

}
