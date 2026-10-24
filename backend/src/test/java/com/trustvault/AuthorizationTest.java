package com.trustvault;

import com.trustvault.model.*;
import com.trustvault.repo.*;
import com.trustvault.service.AuthorizationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuthorizationTest {
    @Autowired AppUserRepository users;
    @Autowired DocumentRepository docs;
    @Autowired DocumentPermissionRepository perms;
    @Autowired AuthorizationService auth;
    @Autowired PasswordEncoder encoder;

    private AppUser user(String prefix, Role role){
        return users.save(new AppUser(prefix+System.nanoTime()+"@test.local",encoder.encode("x"),prefix,role));
    }

    private Document document(AppUser owner,String name){
        Document d=new Document();
        d.setName(name); d.setCategory("confidential"); d.setStorageKey("test-"+System.nanoTime());
        d.setContentType("text/plain"); d.setSizeBytes(1); d.setSha256("test-fingerprint"); d.setOwner(owner);
        return docs.save(d);
    }

    @Test
    void viewerCannotReadUnsharedDocument(){
        AppUser owner=user("owner",Role.OWNER); AppUser viewer=user("viewer",Role.VIEWER); Document d=document(owner,"secret.txt");
        assertFalse(auth.canRead(viewer,d));
    }

    @Test
    void viewerCanReadOnlyAfterExplicitGrantAndLosesAccessAfterRevoke(){
        AppUser owner=user("owner2",Role.OWNER); AppUser viewer=user("viewer2",Role.VIEWER); Document d=document(owner,"secret2.txt");
        assertFalse(auth.canRead(viewer,d));
        perms.save(new DocumentPermission(d,viewer,PermissionLevel.READ,owner));
        assertTrue(auth.canRead(viewer,d));
        perms.deleteByDocumentIdAndUserId(d.getId(),viewer.getId());
        assertFalse(auth.canRead(viewer,d));
    }

    @Test
    void readPermissionDoesNotImplyEdit(){
        AppUser owner=user("owner3",Role.OWNER); AppUser viewer=user("viewer3",Role.VIEWER); Document d=document(owner,"read-only.txt");
        perms.save(new DocumentPermission(d,viewer,PermissionLevel.READ,owner));
        assertTrue(auth.canRead(viewer,d));
        assertFalse(auth.canEdit(viewer,d));
    }

    @Test
    void editPermissionAllowsEdit(){
        AppUser owner=user("owner4",Role.OWNER); AppUser viewer=user("viewer4",Role.VIEWER); Document d=document(owner,"editable.txt");
        perms.save(new DocumentPermission(d,viewer,PermissionLevel.EDIT,owner));
        assertTrue(auth.canRead(viewer,d));
        assertTrue(auth.canEdit(viewer,d));
    }

    @Test
    void deletedDocumentCannotBeReadEvenByOwner(){
        AppUser owner=user("owner5",Role.OWNER); Document d=document(owner,"deleted.txt"); d.setDeleted(true); docs.save(d);
        assertFalse(auth.canRead(owner,d));
    }

    @Test
    void searchAppliesFiltersWithoutExposingUnsharedDocuments(){
        AppUser ownerA=user("searchownerA",Role.OWNER);
        AppUser ownerB=user("searchownerB",Role.OWNER);
        AppUser viewer=user("searchviewer",Role.VIEWER);
        Document shared=document(ownerA,"quarterly-report.txt");
        shared.setCategory("Finance"); docs.save(shared);
        Document privateDoc=document(ownerB,"quarterly-report.txt");
        privateDoc.setCategory("Finance"); docs.save(privateDoc);
        perms.save(new DocumentPermission(shared,viewer,PermissionLevel.READ,ownerA));

        var results=docs.search(viewer.getId(),false,"quarterly","Finance",ownerA.getFullName(),null,null);
        assertEquals(1,results.size());
        assertEquals(shared.getId(),results.get(0).getId());

        var inaccessible=docs.search(viewer.getId(),false,"quarterly","Finance",ownerB.getFullName(),null,null);
        assertTrue(inaccessible.isEmpty());
    }

    @Test
    void adminCanReadDocumentWithoutExplicitGrant(){
        AppUser owner=user("owner6",Role.OWNER); AppUser admin=user("admin6",Role.ADMIN); Document d=document(owner,"admin-review.txt");
        assertTrue(auth.canRead(admin,d));
        assertTrue(auth.canEdit(admin,d));
    }
}
