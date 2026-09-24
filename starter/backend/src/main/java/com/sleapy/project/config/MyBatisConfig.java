package com.sleapy.project.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import javax.sql.DataSource;

/**
 * MyBatis configuration class.
 * 
 * Configures MyBatis mapper scanning and SqlSessionFactory bean.
 * XML mapper files are located in src/main/resources/mappers/
 */
@Configuration
@MapperScan("com.sleapy.project.repositories")
public class MyBatisConfig {
    
    /**
     * Creates the SqlSessionFactory bean that MyBatis uses to create SQL sessions.
     * 
     * @param dataSource the data source configured by Spring Boot
     * @return configured SqlSessionFactory
     * @throws Exception if configuration fails
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        
        // Load mapper XML files from classpath
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        factoryBean.setMapperLocations(resolver.getResources("classpath:mappers/*.xml"));
        
        // Enable auto-mapping of database columns to Java properties
        org.apache.ibatis.session.Configuration config = new org.apache.ibatis.session.Configuration();
        config.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(config);
        
        return factoryBean.getObject();
    }
}
