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

        Dotenv dotenv = Dotenv.configure()
                .directory("/home/ubuntu/BE")
                .filename(".env")
                .ignoreIfMalformed()    // 형식이 잘못돼도 무시
                .ignoreIfMissing()
                .load();

        HikariDataSource dataSource = new HikariDataSource();

        dataSource.setJdbcUrl(dotenv.get("LOCAL_DB_URL"));
        dataSource.setUsername(dotenv.get("LOCAL_DB_USERNAME"));
        dataSource.setPassword(dotenv.get("LOCAL_DB_PASSWORD"));

//        String appEnv = dotenv.get("APP_ENV", "dev");

//        if ("prod".equalsIgnoreCase(appEnv)) {
//            String endpoint = dotenv.get("PROD_DB_ENDPOINT");
//            String dbName = dotenv.get("PROD_DB_NAME");
//            String jdbcUrl = "jdbc:postgresql://" + endpoint + ":5432/" + dbName;
//
//            dataSource.setJdbcUrl(jdbcUrl);
//            dataSource.setUsername(dotenv.get("PROD_DB_USERNAME"));
//            dataSource.setPassword(dotenv.get("PROD_DB_PASSWORD"));
//        } else {
            // dev 환경
//            dataSource.setJdbcUrl(dotenv.get("LOCAL_DB_URL"));
//            dataSource.setUsername(dotenv.get("LOCAL_DB_USERNAME"));
//            dataSource.setPassword(dotenv.get("LOCAL_DB_PASSWORD"));
//        }

        return dataSource;
    }
}