package com.werewolf.game.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 数据库初始化配置
 */
@Configuration
public class DatabaseInitializer {

    @Autowired
    private DataSource dataSource;

    @Bean
    public ApplicationRunner initDatabase() {
        return args -> {
            try (Connection connection = dataSource.getConnection()) {
                // 执行初始化脚本
                ClassPathResource initScript = new ClassPathResource("sql/init.sql");
                if (initScript.exists()) {
                    ScriptUtils.executeSqlScript(connection, initScript);
                    System.out.println("数据库初始化脚本执行成功");
                }
            } catch (Exception e) {
                System.err.println("数据库初始化失败: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
}
