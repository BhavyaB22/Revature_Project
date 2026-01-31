package com.revconnect.util;

import com.revconnect.config.DBConnection;
import java.sql.*;

public class DBUtil {

    public static Connection getConnection() throws SQLException {
        return DBConnection.getConnection();
    }

   
    public static void close(Connection conn, Statement stmt) {
        close(conn, stmt, null);
    }

    
    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing ResultSet: " + e.getMessage());
        }

        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing Statement: " + e.getMessage());
        }

        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing Connection: " + e.getMessage());
        }
    }
}