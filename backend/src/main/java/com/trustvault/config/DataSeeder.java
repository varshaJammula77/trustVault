package com.trustvault.config;

import com.trustvault.model.*;
import com.trustvault.repo.*;
import com.trustvault.service.AuditService;
import com.trustvault.storage.FileStorage;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {
    @Bean CommandLineRunner seed(AppUserRepository users,PasswordEncoder enc,DocumentRepository docs,DocumentPermissionRepository perms,FileStorage storage,AuditService audit){
        return args->{
            AppUser admin=users.findByEmailIgnoreCase("admin@trustvault.local").orElseGet(()->users.save(new AppUser("admin@trustvault.local",enc.encode("Admin@12345"),"TrustVault Admin",Role.ADMIN)));
            AppUser owner=users.findByEmailIgnoreCase("owner@trustvault.local").orElseGet(()->users.save(new AppUser("owner@trustvault.local",enc.encode("Owner@12345"),"Document Owner",Role.OWNER)));
            AppUser viewer=users.findByEmailIgnoreCase("viewer@trustvault.local").orElseGet(()->users.save(new AppUser("viewer@trustvault.local",enc.encode("Viewer@12345"),"Document Viewer",Role.VIEWER)));
            if(docs.count()==0){
                createSeed("Acme-Partner-NDA-Demo.txt","Contracts","This is a non-production demonstration document for the TrustVault evaluation.\n\nThe viewer account is intentionally granted READ access so the revocation scenario can be demonstrated.",owner,viewer,PermissionLevel.READ,docs,perms,storage,audit);
                createSeed("Internal-Compensation-Policy-Demo.txt","Internal","Confidential demonstration document. This file is intentionally not shared with the viewer account.",owner,null,null,docs,perms,storage,audit);
            }
        };
    }
    private void createSeed(String name,String category,String body,AppUser owner,AppUser viewer,PermissionLevel level,DocumentRepository docs,DocumentPermissionRepository perms,FileStorage storage,AuditService audit)throws Exception{
        var saved=storage.saveBytes(body.getBytes(java.nio.charset.StandardCharsets.UTF_8),"seed");
        Document d=new Document();d.setName(name);d.setCategory(category);d.setStorageKey(saved.key());d.setContentType("text/plain");d.setSizeBytes(saved.size());d.setSha256(saved.sha256());d.setOwner(owner);docs.save(d);
        audit.log(owner,AuditAction.UPLOAD,d.getId(),null,null,"Seeded demonstration document "+name+" sha256="+saved.sha256());
        if(viewer!=null){perms.save(new DocumentPermission(d,viewer,level,owner));audit.log(owner,AuditAction.GRANT_ACCESS,d.getId(),viewer.getId(),null,"Seeded READ access for demonstration");}
    }
}
