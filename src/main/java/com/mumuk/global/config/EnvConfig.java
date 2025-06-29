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

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(dotenv.get("LOCAL_DB_URL"));
        dataSource.setUsername(dotenv.get("LOCAL_DB_USERNAME"));
        dataSource.setPassword(dotenv.get("LOCAL_DB_PASSWORD"));
        return dataSource;
    }
}