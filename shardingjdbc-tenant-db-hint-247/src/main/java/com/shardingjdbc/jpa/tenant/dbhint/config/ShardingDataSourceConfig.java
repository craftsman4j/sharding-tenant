package com.shardingjdbc.jpa.tenant.dbhint.config;

import cn.hutool.core.util.StrUtil;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.shardingsphere.broadcast.api.config.BroadcastRuleConfiguration;
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.driver.jdbc.core.datasource.ShardingSphereDataSource;
import org.apache.shardingsphere.infra.algorithm.core.config.AlgorithmConfiguration;
import org.apache.shardingsphere.infra.config.rule.RuleConfiguration;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.keygen.KeyGenerateStrategyConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.HintShardingStrategyConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.NoneShardingStrategyConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
public class ShardingDataSourceConfig {

    /**
     * sharding 配置入口
     *
     * @return ShardingRuleConfiguration
     */
    public static ShardingRuleConfiguration createShardingRuleConfiguration(Collection<String> dbNames) {
        ShardingRuleConfiguration configuration = new ShardingRuleConfiguration();
        configuration.getTables().add(createTableRuleConfiguration(dbNames));

        configuration.getKeyGenerators().put("SNOWFLAKE", new AlgorithmConfiguration("SNOWFLAKE", new Properties()));
        configuration.setDefaultDatabaseShardingStrategy(new HintShardingStrategyConfiguration(TenantHintShardingAlgorithm.class.getName()));
        configuration.setDefaultTableShardingStrategy(new NoneShardingStrategyConfiguration());
        configuration.getShardingAlgorithms().put(TenantHintShardingAlgorithm.class.getName(), new AlgorithmConfiguration("TENANT_HINT", new Properties()));
        return configuration;
    }

    /**
     * 分片表配置
     *
     * @return ShardingTableRuleConfiguration
     */
    private static ShardingTableRuleConfiguration createTableRuleConfiguration(Collection<String> dbNames) {
        ShardingTableRuleConfiguration tableRule = new ShardingTableRuleConfiguration("tb_user",
                "tenant-${" + listToStringArray(dbNames) + "}.tb_user");
        tableRule.setKeyGenerateStrategy(new KeyGenerateStrategyConfiguration("id", "SNOWFLAKE"));
        return tableRule;
    }

    public static String listToStringArray(Collection<String> list) {
        // 使用 Hutool 的 StrUtil 将 List 转换为字符串
        String joinedString = StrUtil.join("','", extractSuffixes(list));
        // 构造目标字符串格式
        return "['" + joinedString + "']";
    }

    public static List<String> extractSuffixes(Collection<String> list) {
        // 提取每个字符串的后缀部分
        return list.stream()
                .map(s -> s.substring(s.lastIndexOf('-') + 1))
                .collect(Collectors.toList());
    }

    /**
     * 广播表配置
     *
     * @return BroadcastRuleConfiguration
     */
    private BroadcastRuleConfiguration createBroadcastRuleConfiguration() {
        return new BroadcastRuleConfiguration(Collections.singletonList("t_address"));
    }

    public static Map<String, DataSource> createDataSourceMap() {
        Map<String, DataSource> dataSourceMap = new HashMap<>();

        // 配置第 1 个数据源
        HikariDataSource dataSource1 = new HikariDataSource();
        dataSource1.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource1.setJdbcUrl("jdbc:mysql://43.142.4.174:3306/test_db_tenant_a");
        dataSource1.setUsername("dev");
        dataSource1.setPassword("123456");
        dataSourceMap.put("tenant-a", dataSource1);

        // 配置第 2 个数据源
        HikariDataSource dataSource2 = new HikariDataSource();
        dataSource2.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource2.setJdbcUrl("jdbc:mysql://43.142.4.174:3306/test_db_tenant_b");
        dataSource2.setUsername("dev");
        dataSource2.setPassword("123456");
        dataSourceMap.put("tenant-b", dataSource2);
        return dataSourceMap;
    }

    /**
     * 创建 ShardingSphereDataSource 数据源
     *
     * @return ShardingSphereDataSource
     * @throws SQLException
     */
    public static ShardingSphereDataSource createShardingSphereDataSource(Map<String, DataSource> dataSourceMap,
                                                                          Collection<RuleConfiguration> configs) throws SQLException {
        // 其他配置
        Properties properties = new Properties();
        // 控制台日志展示 sharding-jdbc 的 sql
        properties.put("sql-show", "true");
        return (ShardingSphereDataSource) ShardingSphereDataSourceFactory.createDataSource(dataSourceMap, configs, properties);
    }

    @Primary
    @Bean
    public DataSource dataSource() throws SQLException {
        // 获取 AbstractRoutingData 对象
        DynamicDataSource dynamicDataSource = DynamicDataSource.getInstance();
        // 获取自己配置文件上的普通数据源，该方法忽略展示，key 为数据库的名字，value 为数据源
        Map<String, DataSource> shardingSourceMap = createDataSourceMap();
        // 存储租户的数据源
        shardingSourceMap.keySet().forEach(dynamicDataSource::setTenantDataSource);

        // 配置默认数据源
        HikariDataSource dataSource0 = new HikariDataSource();
        dataSource0.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource0.setJdbcUrl("jdbc:mysql://43.142.4.174:3306/ruoyi-vue-pro");
        dataSource0.setUsername("dev");
        dataSource0.setPassword("123456");
        // 设置默认数据源
        dynamicDataSource.setDefaultTargetDataSource(dataSource0);
        // 添加 sharding-jdbc 数据源
        ShardingSphereDataSource shardingDataSource =
                createShardingSphereDataSource(createDataSourceMap(), Collections.singleton(createShardingRuleConfiguration(shardingSourceMap.keySet())));

        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put("shardingDT", shardingDataSource);
        dynamicDataSource.setTargetDataSources(dataSourceMap);
        return dynamicDataSource;
    }

    public static void main(String[] args) throws SQLException {
        Map<String, DataSource> dataSourceMap = createDataSourceMap();
        ShardingSphereDataSource dataSource = createShardingSphereDataSource(dataSourceMap,
                Collections.singleton(createShardingRuleConfiguration(dataSourceMap.keySet())));
        try (
                Connection conn = dataSource.getConnection();
                Statement statement = conn.createStatement();
        ) {
            /**
             * 1、不能使用 库名.表名指定
             *
             */
            HintManager hintManager = HintManager.getInstance();
            hintManager.setDatabaseShardingValue("tenant-b");
            statement.execute("select * from tb_user limit 10");
            try (ResultSet rs = statement.getResultSet()) {
                while (rs.next()) {
                    System.out.println("result---------" + rs.getLong("id"));
                }
            } finally {
                HintManager.clear();
            }
        }
    }
}
