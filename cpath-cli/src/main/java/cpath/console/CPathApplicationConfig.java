package cpath.console;

import cpath.config.CPathSettings;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableConfigurationProperties(CPathSettings.class)
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = {"cpath.jpa"})
@ComponentScan(basePackages = "cpath.service")
public class CPathApplicationConfig {

	@Bean
	LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
		LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
		entityManagerFactoryBean.setDataSource(dataSource);
		entityManagerFactoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		entityManagerFactoryBean.setPackagesToScan("cpath.jpa");
		return entityManagerFactoryBean;
	}

	@Bean
	JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(entityManagerFactory);
		return transactionManager;
	}

// TODO: (auto)configure datasource, hibernate...

//	@Bean
//	HibernateJpaVendorAdapter hibernateJpaVendorAdapter() {
//		HibernateJpaVendorAdapter bean = new HibernateJpaVendorAdapter();
//		return bean;
//	}

//	<beans profile="default">
//		<bean id="hibernateJpaVendorAdapter"
//	class="org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter">
//			<property name="database" value="H2" />
//			<property name="generateDdl" value="true" />
//		</bean>
//
//		<jdbc:embedded-database id="metaDataSource" type="H2" />
//	</beans>
//
//	<beans profile="prod">
//		<bean id="hibernateJpaVendorAdapter"
//	class="org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter">
//			<property name="database" value="H2" />
//		</bean>
//
//		<bean id="metaDataSource" class="org.h2.jdbcx.JdbcConnectionPool" destroy-method="dispose">
//		  <constructor-arg>
//			<bean class="org.h2.jdbcx.JdbcDataSource">
//				<property name="URL" value="jdbc:h2:./cpath2;MV_STORE=FALSE;MVCC=FALSE" />
//				<!-- MVStore is disabled, for it uses 10-100 times more disk space! -->
//				<property name="user" value="sa" />
//				<property name="password" value="" />
//			</bean>
//		  </constructor-arg>
//		</bean>
//	</beans>

}
