package com.trustvault.service;

import com.trustvault.model.*;
import com.trustvault.repo.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthorizationService {
    private final DocumentRepository docs; private final DocumentPermissionRepository perms; private final AppUserRepository users; private final AuditService audit;
    public AuthorizationService(DocumentRepository d,DocumentPermissionRepository p,AppUserRepository u,AuditService a){docs=d;perms=p;users=u;audit=a;}
    public AppUser user(String id){return users.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"User account not found"));}
    public Document document(String id){return docs.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Document not found"));}
    public PermissionLevel permissionFor(AppUser u,Document d){
        if(u.getRole()==Role.ADMIN||d.getOwner().getId().equals(u.getId())) return PermissionLevel.EDIT;
        return perms.findByDocumentIdAndUserId(d.getId(),u.getId()).map(DocumentPermission::getPermission).orElse(null);
    }
    public boolean canRead(AppUser u,Document d){return !d.isDeleted()&&permissionFor(u,d)!=null;}
    public boolean canEdit(AppUser u,Document d){return !d.isDeleted()&&permissionFor(u,d)==PermissionLevel.EDIT;}
    public void requireRead(AppUser u,Document d){if(!canRead(u,d)){audit.log(u,AuditAction.PERMISSION_DENIED,d.getId(),null,null,"Read denied");throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You are not authorized for this document");}}
    public void requireEdit(AppUser u,Document d){if(!canEdit(u,d)){audit.log(u,AuditAction.PERMISSION_DENIED,d.getId(),null,null,"Edit denied");throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Edit permission required");}}
    public void requireOwnerOrAdmin(AppUser u,Document d){if(u.getRole()!=Role.ADMIN&&!d.getOwner().getId().equals(u.getId())){audit.log(u,AuditAction.PERMISSION_DENIED,d.getId(),null,null,"Owner/admin action denied");throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Owner access required");}}
}
