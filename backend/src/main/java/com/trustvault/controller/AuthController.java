package com.trustvault.controller;
import com.trustvault.dto.LoginRequest; import com.trustvault.service.AuthService; import jakarta.servlet.http.HttpServletRequest; import jakarta.validation.Valid; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/auth") public class AuthController {private final AuthService auth;public AuthController(AuthService a){auth=a;} @PostMapping("/login") public Map<String,Object> login(@Valid @RequestBody LoginRequest r,HttpServletRequest req){return auth.login(r.email(),r.password(),req.getRemoteAddr());}}
