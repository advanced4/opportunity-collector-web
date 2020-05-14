package org.jf.opps.ui.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "opps")
@Validated
public class WebConfig {
    private static Logger log = LoggerFactory.getLogger(WebConfig.class);

    private Jwt jwt;
    public Jwt getJwt() {return jwt;}
    public void setJwt(Jwt jwt) {this.jwt = jwt;}
    public static class Jwt {
        @NotNull
        private String issuer;
        @NotNull
        private String audience;
        @NotNull
        @Min(300000) // 5 min
        private Long at_expiry_ms;

        public String getIssuer() {return issuer;}
        public void setIssuer(String issuer) {this.issuer = issuer;}
        public String getAudience() {return audience;}
        public void setAudience(String audience) {this.audience = audience;}
        public Long getAt_expiry_ms() {return at_expiry_ms;}
        public void setAt_expiry_ms(Long at_expiry_ms) {this.at_expiry_ms = at_expiry_ms;}
    }

    @NotNull
    private DbConfig oppsdb;
    public DbConfig getOppsdb() { return oppsdb; }
    public void setOppsdb(DbConfig oppsdb) { this.oppsdb = oppsdb; }
    public static class DbConfig {
        @NotNull
        private String username;
        @NotNull
        private String password;
        @NotNull
        private String jdbcurl;
        @NotNull
        private String driver;

        public String getUsername() {return username;}
        public void setUsername(String username) {this.username = username;}
        public String getPassword() {return password;}
        public void setPassword(String password) {this.password = password;}
        public String getJdbcurl() {return jdbcurl;}
        public void setJdbcurl(String jdbcurl) {this.jdbcurl = jdbcurl;}
        public String getDriver() {return driver;}
        public void setDriver(String driver) {this.driver = driver;}
    }

    @NotNull
    private Misc misc;
    public Misc getMisc() {return misc;}
    public void setMisc(Misc misc){this.misc = misc;}
    public static class Misc{
        @NotNull
        private String domain;
        private Boolean disableloginfordevelopment;
        private Boolean securessl;
        @NotNull
        private Springadmin springadmin;
        @NotNull
        private Integer max_requests_per_minute;
        private String version;

        private String cname;

        public String getCname() {return cname;}
        public void setCname(String cname) {this.cname = cname;}

        public String getVersion() {return version;}
        public void setVersion(String version) {this.version = version;}

        public Integer getMax_requests_per_minute() {
            return max_requests_per_minute;
        }
        public void setMax_requests_per_minute(Integer max_requests_per_minute) { this.max_requests_per_minute = max_requests_per_minute; }

        public static class Springadmin {
            @NotNull
            private String username;
            @NotNull
            private String password;

            public String getUsername() {
                return username;
            }

            public void setUsername(String username) {
                this.username = username;
            }

            public String getPassword() {
                return password;
            }
            public void setPassword(String password) {this.password = password;}
        }

        public Springadmin getSpringadmin() {return springadmin;}
        public void setSpringadmin(Springadmin springadmin) {this.springadmin = springadmin;}
        public Boolean getDisableloginfordevelopment() {return disableloginfordevelopment;}
        public void setDisableloginfordevelopment(Boolean disableloginfordevelopment) {this.disableloginfordevelopment = disableloginfordevelopment;}
        public String getDomain() {return domain;}
        public void setDomain(String domain) {this.domain = domain;}
        public boolean isSecuressl() {return securessl;}
        public void setSecuressl(boolean securessl) {this.securessl = securessl;}
    }

}
