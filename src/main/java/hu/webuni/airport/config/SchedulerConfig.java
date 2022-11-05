package hu.webuni.airport.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

@Configuration
@EnableScheduling
@EnableAsync
//ha valamelyik node lefoglalta a lockot, de ezt követően meghal, akkor hány perc után szabadítsa fel a rendszer
@EnableSchedulerLock(defaultLockAtMostFor = "1m")
public class SchedulerConfig {

	//ez a Bean a Shedlok működéséhez szükséges
	@Bean
	public LockProvider lockProvider(DataSource dataSource) {
	            return new JdbcTemplateLockProvider(
	                JdbcTemplateLockProvider.Configuration.builder()
	                .withJdbcTemplate(new JdbcTemplate(dataSource))
	                .usingDbTime()
	                .build()
	            );
	}
	
}