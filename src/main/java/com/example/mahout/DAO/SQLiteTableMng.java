package com.example.mahout.DAO;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLiteTableMng {

    public static void main(String[] args) throws SQLException {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:database/mahout_api.db")) {
            String sql = "CREATE TABLE IF NOT EXISTS file_model_documents " +
                    "(COMPANY_NAME VARCHAR(128) NOT NULL," +
                    " PROPERTY VARCHAR(128) NOT NULL," +
                    " MODEL BLOB NOT NULL," +
                    " LABELINDEX BLOB NOT NULL," +
                    " DICTIONARY BLOB NOT NULL," +
                    " FREQUENCIES BLOB NOT NULL," +
                    " PRIMARY KEY(COMPANY_NAME, PROPERTY))";
            try (Statement stmt = c.createStatement()) {
                stmt.execute(sql);
            }
        }
    }
}
