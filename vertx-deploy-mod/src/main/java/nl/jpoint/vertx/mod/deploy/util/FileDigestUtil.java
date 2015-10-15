package nl.jpoint.vertx.mod.deploy.util;

import sun.misc.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileDigestUtil {

    private MessageDigest md;

    public FileDigestUtil() {
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            md = null;
        }
    }

    public byte[] getFileMd5Sum(Path unpackFile) {
        if (md == null || Files.notExists(unpackFile)) {
            return null;
        }
        try (InputStream is = Files.newInputStream(unpackFile)) {
            DigestInputStream dis = new DigestInputStream(is, md);
            while ((dis.read()) != -1);
            byte[] digest = md.digest();
            md.reset();
            return digest;
        } catch (IOException e) {
            return null;
        }
    }
}
