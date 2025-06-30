package com.mumuk.global.config;

import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class EnvConfig {

    @Bean
    public DataSource dataSource() {
        Dotenv dotenv = Dotenv.load();      // .env 파일 자동 로드

        String appEnv = dotenv.get("APP_ENV", "dev"); // 기본값은 dev

        HikariDataSource dataSource = new HikariDataSource();

        if ("prod".equalsIgnoreCase(appEnv)) {
            String endpoint = dotenv.get("PROD_DB_ENDPOINT");
            String dbName = dotenv.get("PROD_DB_NAME");
            String jdbcUrl = "jdbc:postgresql://" + endpoint + ":5432/" + dbName;

            dataSource.setJdbcUrl(jdbcUrl);
            dataSource.setUsername(dotenv.get("PROD_DB_USERNAME"));
            dataSource.setPassword(dotenv.get("PROD_DB_PASSWORD"));
        } else {
            // dev 환경
            dataSource.setJdbcUrl(dotenv.get("LOCAL_DB_URL"));
            dataSource.setUsername(dotenv.get("LOCAL_DB_USERNAME"));
            dataSource.setPassword(dotenv.get("LOCAL_DB_PASSWORD"));
        }

        return dataSource;
    }
}