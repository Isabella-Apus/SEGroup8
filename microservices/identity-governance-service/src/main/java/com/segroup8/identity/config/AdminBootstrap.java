package com.segroup8.identity.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder passwords;
    private final String username;
    private final String password;

    public AdminBootstrap(JdbcTemplate jdbc, BCryptPasswordEncoder passwords,
            @Value("${app.bootstrap-admin.username:admin}") String username,
            @Value("${app.bootstrap-admin.password:}") String password) {
        this.jdbc = jdbc;
        this.passwords = passwords;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (password == null || password.isBlank()) {
            return;
        }
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM `user` WHERE username=?", Long.class, username);
        if (count != null && count == 0) {
            jdbc.update("INSERT INTO `user`(username,password,nickname,role,status,credit_score,buyer_credit_score,seller_credit_score) "
                    + "VALUES(?,?,?,'ADMIN','NORMAL',100,100,100)", username, passwords.encode(password), "管理员");
        }
    }
}
