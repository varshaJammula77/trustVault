package com.trustvault.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.UUID;

@Service
public class FileStorage {
    private final Path root;
    public FileStorage(@Value("${trustvault.storage-dir}") String dir){root=Paths.get(dir).toAbsolutePath().normalize();try{Files.createDirectories(root);}catch(IOException e){throw new IllegalStateException("Cannot initialize private storage",e);}}
    public Stored save(MultipartFile f,String id)throws Exception{return saveStream(f.getInputStream(),id);}
    public Stored saveBytes(byte[] bytes,String id)throws Exception{return saveStream(new ByteArrayInputStream(bytes),id);}
    private Stored saveStream(InputStream in,String id)throws Exception{
        String safe=id+"-"+UUID.randomUUID(); Path p=root.resolve(safe).normalize(); if(!p.getParent().equals(root))throw new SecurityException("Invalid storage key");
        try(InputStream input=in; OutputStream out=Files.newOutputStream(p,StandardOpenOption.CREATE_NEW)){
            MessageDigest md=MessageDigest.getInstance("SHA-256"); byte[] buf=new byte[8192]; long size=0; int n;
            while((n=input.read(buf))>0){md.update(buf,0,n);out.write(buf,0,n);size+=n;}
            return new Stored(safe,size,toHex(md.digest()));
        } catch (Exception ex) {
            Files.deleteIfExists(p);
            throw ex;
        }
    }
    private Path resolveKey(String key) {
        if (key == null || !key.matches("[A-Za-z0-9-]{1,100}")) throw new SecurityException("Invalid storage key");
        Path p = root.resolve(key).normalize();
        if (!p.getParent().equals(root)) throw new SecurityException("Invalid storage key");
        return p;
    }
    public InputStream open(String key)throws IOException{
        Path p=resolveKey(key);
        if (!Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)) throw new FileNotFoundException("Stored document not found");
        return Files.newInputStream(p,StandardOpenOption.READ,LinkOption.NOFOLLOW_LINKS);
    }
    public void delete(String key)throws IOException{Path p=resolveKey(key);if(Files.exists(p, LinkOption.NOFOLLOW_LINKS))Files.delete(p);}
    private String toHex(byte[] b){StringBuilder h=new StringBuilder();for(byte v:b)h.append(String.format("%02x",v));return h.toString();}
    public record Stored(String key,long size,String sha256){}
}
