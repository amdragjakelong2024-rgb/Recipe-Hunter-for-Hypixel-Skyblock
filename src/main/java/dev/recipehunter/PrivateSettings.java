package dev.recipehunter;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.*;
/** Local device encryption prevents accidental disclosure in ordinary config exports.
 * The device key is local too: this is not protection from other software on this account. */
public final class PrivateSettings {
 private final Path dir;
 public PrivateSettings(Path dir){this.dir=dir;}
 private Path key(){return dir.resolve("device.key");}
 private Path data(){return dir.resolve("credentials.bin");}
 private void restrict(Path p)throws java.io.IOException{try{Files.setPosixFilePermissions(p,PosixFilePermissions.fromString(Files.isDirectory(p)?"rwx------":"rw-------"));}catch(UnsupportedOperationException ignored){}}
 public void write(String text)throws Exception{
  Files.createDirectories(dir);restrict(dir);byte[] secret;
  if(Files.exists(key()))secret=Files.readAllBytes(key());else{secret=new byte[32];new SecureRandom().nextBytes(secret);Files.write(key(),secret,StandardOpenOption.CREATE_NEW);restrict(key());}
  byte[] iv=new byte[12];new SecureRandom().nextBytes(iv);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(secret,"AES"),new GCMParameterSpec(128,iv));byte[] encrypted=cipher.doFinal(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  byte[] bytes=new byte[iv.length+encrypted.length];System.arraycopy(iv,0,bytes,0,iv.length);System.arraycopy(encrypted,0,bytes,iv.length,encrypted.length);
  Path temp=dir.resolve("credentials.tmp");Files.write(temp,bytes);restrict(temp);try{Files.move(temp,data(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,data(),StandardCopyOption.REPLACE_EXISTING);}restrict(data());
 }
 public String read()throws Exception{if(!Files.exists(data()))return "";byte[] bytes=Files.readAllBytes(data());if(bytes.length<28)throw new java.io.IOException("Invalid settings");Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(Files.readAllBytes(key()),"AES"),new GCMParameterSpec(128,Arrays.copyOf(bytes,12)));return new String(cipher.doFinal(Arrays.copyOfRange(bytes,12,bytes.length)),java.nio.charset.StandardCharsets.UTF_8);}
}
