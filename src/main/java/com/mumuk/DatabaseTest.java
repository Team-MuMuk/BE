package com.mumuk;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class DatabaseTest implements CommandLineRunner {

    private final DataSource dataSource;

    public DatabaseTest(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        try (Connection connection = dataSource.getConnection()) {
            System.out.println("✅ Database connected successfully: " + connection.getMetaData().getURL());
        } catch (Exception e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
        }
    }
}

