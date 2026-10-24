package com.trustvault.repo;
import com.trustvault.model.AppUser; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface AppUserRepository extends JpaRepository<AppUser,String>{Optional<AppUser> findByEmailIgnoreCase(String email); List<AppUser> findByRoleAndEnabledTrue(com.trustvault.model.Role role);}
