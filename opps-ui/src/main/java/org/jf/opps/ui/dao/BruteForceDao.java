package org.jf.opps.ui.dao;

import org.jf.common.constants.OppsConstants;
import org.jf.common.utilities.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Types;
import java.util.List;
import java.util.UUID;

// there's two dao's because each dao == table
@Component
public class BruteForceDao {
    private static final Logger log = LoggerFactory.getLogger(BruteForceDao.class);
    private JdbcTemplate jdbcTemplate;

    public static class BruteForceEntry{
        private UUID id;
        private int ts;
        public UUID getId() {
            return id;
        }
        public void setId(UUID id) {
            this.id = id;
        }
        public int getTs() {
            return ts;
        }
        public void setTs(int ts) {
            this.ts = ts;
        }
    }

    private final RowMapper<BruteForceEntry> rm = (rs, i) -> new BruteForceEntry();

    BruteForceDao(@Qualifier("oppsDb") DataSource dataSource){
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public boolean checkBruteForce(UUID id) {
        long now = Utils.getCurrentTimeSeconds();
        long valid_attempts = now - OppsConstants.LoginSec.bruteForceLookbackTimeSeconds;

        String sql = "select time from login_attempts where user_id = ? and time > ?";

        PreparedStatementSetter pss = ps ->{
            ps.setObject(1, id, Types.VARCHAR);
            ps.setLong(2, valid_attempts);
        };

        List<BruteForceEntry> result = jdbcTemplate.query(sql, pss, rm);

        return result.size() > OppsConstants.LoginSec.failedloginAttemptLimit;
    }

    public void insertLoginAttempt(UUID id){
        long now = Utils.getCurrentTimeSeconds();
        String sql = "insert into login_attempts(user_id, time) values(?,?)";

        PreparedStatementSetter pss = ps ->{
            ps.setObject(1, id, Types.VARCHAR);
            ps.setLong(2, now);
        };

        int res = jdbcTemplate.update(sql, pss);

        if(res < 1 ){
            log.error("Failed to insert failed login attempt");
        }
    }
}
