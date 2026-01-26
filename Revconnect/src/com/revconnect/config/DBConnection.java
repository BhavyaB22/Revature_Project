package com.revconnect.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
	  
	    private static final String URL = "jdbc:oracle:thin:@localhost:1521:xe";
	    private static final String USERNAME = "revconnect"; 
	    private static final String PASSWORD = "rev123";

	    /**
	     * Establishes a connection to the database.
	     * @return Connection object if successful, null otherwise.
	     */
	    public static Connection getConnection() {
	        Connection connection = null;
	        try {
	            // Step 1: Register the Oracle JDBC Driver
	            Class.forName("oracle.jdbc.driver.OracleDriver");

	            // Step 2: Establish the connection using the DriverManager
	            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);

	        } catch (ClassNotFoundException e) {
	            System.err.println("Error: Oracle JDBC Driver not found. Add ojdbc14.jar to your build path.");
	            e.printStackTrace();
	        } catch (SQLException e) {
	            System.err.println("Error: Could not connect to Oracle Database. Check if 'xe' service is running.");
	            e.printStackTrace();
	        }
	        return connection;
	    }

	    public static void closeConnection(Connection conn) {
	        if (conn != null) {
	            try {
	                conn.close();
	            } catch (SQLException e) {
	                System.err.println("Error while closing the connection.");
	                e.printStackTrace();
	            }
	        }
	    }
	}
