package com.bgsoftware.wildstacker.database.sql;

import java.util.List;
import java.util.stream.Collectors;

public final class DeleteSQLDatabaseTransaction extends SQLDatabaseTransaction<DeleteSQLDatabaseTransaction> {

    public DeleteSQLDatabaseTransaction(String tableName, List<String> columnNames) {
        super(tableName, columnNames);
    }

    @Override
    protected String buildSQL(String tablePrefix) {
        String whereClause = this.columnNames.stream()
                .map(col -> col + " = ?")
                .collect(Collectors.joining(" AND "));
        return "DELETE FROM " + tablePrefix + this.tableName + " WHERE " + whereClause + ";";
    }

}
