package org.jf.opps.ui.dao;

import org.jf.common.models.User;
import org.jf.common.utilities.BCrypt;
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

@Component
public class UserAccountsDao {
    public static class UserNotFoundException extends Exception{
        UserNotFoundException(String message){
            super(message);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(UserAccountsDao.class);

    public UserAccountsDao(@Qualifier("oppsDb") DataSource dataSource){
        jdbcTemplate =new JdbcTemplate(dataSource);
    }
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<User> user_rm = (rs, i) ->{
        User user = new User();
        user.setId(UUID.fromString(rs.getString("id")));
        user.setUsername( rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setEnabled(rs.getBoolean("enabled"));
        user.setAdmin(rs.getBoolean("isadmin"));

        // not all queries may return a password
        try {
            user.setPassword(rs.getString("password"));
        }catch(java.sql.SQLException ignored){}

        return user;
    };

    private final RowMapper<User> user_name_id = (rs, i) ->{
        User user = new User();
        user.setUsername( rs.getString("username"));
        user.setEmail( rs.getString("email"));
        user.setId(UUID.fromString(rs.getString("id")));
        return user;
    };

    public List<User> getAdmins() {
        String sql = "select id,username,email from members where isadmin=1 ";

        List<User> result = jdbcTemplate.query(sql, user_name_id);
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }

    public List<User> getNonAdmins() {
        String sql = "select id,username,email,enabled,isadmin from members where isadmin=0 ";

        List<User> result = jdbcTemplate.query(sql, user_rm);
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }

    public boolean deleteUser(UUID id){
        String sql = "DELETE FROM members WHERE id = ?";

        PreparedStatementSetter pss = ps -> ps.setObject(
                1, id, Types.VARCHAR
        );

        int res = jdbcTemplate.update(sql, pss);

        if(res < 1 ){
            log.error("Failed to delete user");
            return false;
        }
        return true;
    }


    public boolean isAdmin(UUID uid) {
        String sql = "select id,username,email,enabled,isadmin from members where id = ? limit 1";

        PreparedStatementSetter pss = ps ->{
            ps.setObject(1, uid, Types.VARCHAR);
        };

        List<User> result = jdbcTemplate.query(sql, pss, user_rm);
        return result.get(0).isAdmin();
    }

    public boolean enableUser(UUID id){
        String sql = "UPDATE members SET enabled=? WHERE id=?";

        PreparedStatementSetter pss = ps ->{
            ps.setBoolean(1, true);
            ps.setObject(2, id, Types.VARCHAR);
        };

        int res = jdbcTemplate.update(sql, pss);

        if(res < 1 ){
            log.error("failed to enable user");
            return false;
        }
        return true;
    }

    public boolean disableUser(UUID id){
        String sql = "UPDATE members SET enabled=? WHERE id=?";

        PreparedStatementSetter pss = ps ->{
            ps.setBoolean(1, false);
            ps.setObject(2, id, Types.VARCHAR);
        };

        int res = jdbcTemplate.update(sql, pss);

        if(res < 1 ){
            log.error("Failed to disable user");
            return false;
        }
        return true;
    }

    public boolean insertNewUser(UUID uid, String username, String email, String pwd){
        String sql = "INSERT INTO members (id, username, email, password, enabled, isadmin) VALUES (?, ?, ?, ?, ?, ?)";

        PreparedStatementSetter pss = ps ->{
            ps.setObject(1, uid, Types.VARCHAR);
            ps.setString(2, username);
            ps.setString(3, email);
            ps.setString(4, pwd);
            ps.setBoolean(5, true); // account enabled by default (why would u create it otherwise)
            ps.setBoolean(6, false); // not admin by default
        };

        int res = jdbcTemplate.update(sql, pss);

        if(res < 1 ){
            log.error("Failed to insert new user");
            return false;
        }
        return true;
    }

    public boolean userExistsByEmail(String email) {
        try {
            findByEmail(email);
            return true;
        } catch (UserNotFoundException e) {
            return false;
        }
    }

    public List<User> getEveryone() {
        String sql = "select id,username,email from members";

        List<User> result = jdbcTemplate.query(sql, user_name_id);
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }


    public User findByUid(UUID uid) throws UserNotFoundException {
        String sql = "select id,username,email,enabled,isadmin from members where id = ? limit 1";

        PreparedStatementSetter pss = ps ->{
            ps.setObject(1, uid, Types.VARCHAR);
        };

        List<User> result = jdbcTemplate.query(sql, pss, user_rm);
        if (result.isEmpty()) {
            throw new UserNotFoundException("No matching user found");
        }
        return result.get(0);
    }


    public boolean bcryptAndChangePassword(UUID uid, String hash){
        String sql = "UPDATE members SET password=? WHERE id=?";

        PreparedStatementSetter pss = ps ->{
            ps.setString(1, BCrypt.hashpw(hash));
            ps.setObject(2, uid, Types.VARCHAR);
        };

        int res = jdbcTemplate.update(sql, pss);

        if(res < 1 ){
            log.error("Failed to change password");
            return false;
        }
        return true;
    }

    public User findByEmail(String email) throws UserNotFoundException {
        String sql = "select id,username,email,enabled,isadmin from members where email = ? limit 1";

        PreparedStatementSetter pss = ps ->{
            ps.setString(1, email);
        };

        List<User> result = jdbcTemplate.query(sql, pss, user_rm);
        if (result.isEmpty()) {
            throw new UserNotFoundException("No user found");
        }
        return result.get(0);
    }

    public User findByEmailFullLogin(String email) throws UserNotFoundException {
        String sql = "select * from members where email = ?";

        PreparedStatementSetter pss = ps ->{
            ps.setString(1, email);
        };

        List<User> result = jdbcTemplate.query(sql, pss, user_rm);
        if (result.isEmpty()) {
            throw new UserNotFoundException("No user found");
        }
        return result.get(0);
    }
}
