package com.werewolf.game.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

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
                // 执行创建表脚本
                ClassPathResource createTablesScript = new ClassPathResource("sql/create_tables.sql");
                if (createTablesScript.exists()) {
                    ScriptUtils.executeSqlScript(connection, createTablesScript);
                    System.out.println("数据库创建表脚本执行成功");
                }
                
                // 执行初始化脚本
                ClassPathResource initScript = new ClassPathResource("sql/init.sql");
                if (initScript.exists()) {
                    ScriptUtils.executeSqlScript(connection, initScript);
                    System.out.println("数据库初始化脚本执行成功");
                }

                ensureColumn(connection, "ai_player", "avatar_url", "LONGTEXT NULL");
                ensureColumn(connection, "game_state_snapshot", "saved_at", "BIGINT NOT NULL DEFAULT 0");
            } catch (Exception e) {
                System.err.println("数据库初始化失败: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }

    private void ensureColumn(Connection connection, String tableName, String columnName, String definition) throws Exception {
        if (columnExists(connection, tableName, columnName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
            System.out.println("数据库字段补充成功: " + tableName + "." + columnName);
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws Exception {
        String catalog = connection.getCatalog();
        for (String tableVariant : new String[]{tableName, tableName.toUpperCase()}) {
            for (String columnVariant : new String[]{columnName, columnName.toUpperCase()}) {
                try (ResultSet columns = connection.getMetaData().getColumns(catalog, null, tableVariant, columnVariant)) {
                    if (columns.next()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
