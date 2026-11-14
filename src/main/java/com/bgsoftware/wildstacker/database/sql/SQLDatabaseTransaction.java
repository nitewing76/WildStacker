package com.bgsoftware.wildstacker.database.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class SQLDatabaseTransaction<T extends SQLDatabaseTransaction<T>> implements IDatabaseTransaction {

    protected final String tableName;
    protected final List<String> columnNames;
    protected final List<List<Object>> batches = new ArrayList<>();
    protected List<Object> currentBatch = new ArrayList<>();

    protected SQLDatabaseTransaction(String tableName, List<String> columnNames) {
        this.tableName = tableName;
        this.columnNames = columnNames;
    }

    @SuppressWarnings("unchecked")
    public T bindObject(Object value) {
        this.currentBatch.add(value);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T newBatch() {
        if (!this.currentBatch.isEmpty()) {
            this.batches.add(this.currentBatch);
            this.currentBatch = new ArrayList<>();
        }
        return (T) this;
    }

    protected abstract String buildSQL(String tablePrefix);

    @Override
    public void execute(Connection connection, String tablePrefix) throws SQLException {
        if (!this.currentBatch.isEmpty()) {
            this.batches.add(this.currentBatch);
            this.currentBatch = new ArrayList<>();
        }
        if (this.batches.isEmpty()) {
            return;
        }

        String sql = buildSQL(tablePrefix == null ? "" : tablePrefix);
        boolean previousAutoCommit = connection.getAutoCommit();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            for (List<Object> batch : this.batches) {
                for (int i = 0; i < batch.size(); i++) {
                    ps.setObject(i + 1, batch.get(i));
                }
                ps.addBatch();
            }
            ps.executeBatch();
            connection.commit();
        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            throw ex;
        } finally {
            try {
                connection.setAutoCommit(previousAutoCommit);
            } catch (SQLException ignored) {
            }
        }
    }

}
