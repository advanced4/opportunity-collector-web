package org.jf.opps.ui;

import org.jf.common.utilities.Utils;
import org.jf.opps.ui.config.WebConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.security.KeyPair;


@SpringBootApplication
@EnableConfigurationProperties(WebConfig.class)
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class SpringOppsUiMain implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SpringOppsUiMain.class);
    private static ConfigurableApplicationContext context;
    public static void main(String[] args) {
        context = SpringApplication.run(SpringOppsUiMain.class, args);
    }

    @Autowired
    WebConfig webConfig;

    // ephemeral keypair
    @Bean(name="jwtkeys")
    public KeyPair jwtkeys(){
        return Utils.generateKeyPair(SignatureAlgorithm.ES256);
    }

    @Bean(name="oppsDb")
    public DataSource oppsDb(WebConfig webConfig){
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(webConfig.getOppsdb().getDriver());
        hikariConfig.setJdbcUrl(webConfig.getOppsdb().getJdbcurl());
        hikariConfig.setUsername(webConfig.getOppsdb().getUsername());
        hikariConfig.setPassword(webConfig.getOppsdb().getPassword());
        try {
            return new HikariDataSource(hikariConfig);
        }catch(HikariPool.PoolInitializationException pie){
            log.error("###### !!! Unable to connect to DB !!! ######");
            return null;
        }
    }

    @Override
    public void run(String... args) {
        // just for reference: a place where we can run code prior to spring fully loading
        if(webConfig.getMisc().getDisableloginfordevelopment()){
            log.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            log.warn("################### WARNING #################");
            log.warn("####!!!!! SECURITY IS DISABLED      !!!!!####" );
            log.warn("####!!!!! DO NOT RUN IN PRODUCTION  !!!!!####" );
            log.warn("#############################################");
            log.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }
    }

    public static void restart() {
        ApplicationArguments args = context.getBean(ApplicationArguments.class);

        Thread thread = new Thread(() -> {
            context.close();
            context = SpringApplication.run(SpringOppsUiMain.class, args.getSourceArgs());
        });

        thread.setDaemon(false);
        thread.start();
    }
}
