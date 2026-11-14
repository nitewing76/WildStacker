package com.bgsoftware.wildstacker.database.sql;

import java.sql.Connection;
import java.sql.SQLException;

public interface IDatabaseTransaction {

    void execute(Connection connection, String tablePrefix) throws SQLException;

}
