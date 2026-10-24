package com.trustvault.service;

import com.trustvault.model.*;
import com.trustvault.repo.*;
import com.trustvault.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
public class AuthService {
    private final AppUserRepository users; private final PasswordEncoder encoder; private final JwtService jwt; private final AuditService audit;
    public AuthService(AppUserRepository u,PasswordEncoder e,JwtService j,AuditService a){users=u;encoder=e;jwt=j;audit=a;}
    public Map<String,Object> login(String email,String password,String ip){
        var u=users.findByEmailIgnoreCase(email).orElse(null);
        if(u==null||!u.isEnabled()||!encoder.matches(password,u.getPasswordHash())){
            audit.log(u,AuditAction.LOGIN_FAILED,null,null,ip,"Invalid credentials");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials");
        }
        audit.log(u,AuditAction.LOGIN_SUCCESS,null,null,ip,"Login successful");
        return Map.of("token",jwt.issue(u.getId(),u.getEmail(),u.getRole().name()),"user",Map.of("id",u.getId(),"email",u.getEmail(),"fullName",u.getFullName(),"role",u.getRole()));
    }
}
