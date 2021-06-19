package pw.mihou.rosedb.utility;

import jakarta.xml.bind.DatatypeConverter;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class RoseSSL {

    /**
     * Method which returns a SSLContext from a Let's encrypt or IllegalArgumentException on error
     *
     * @return a valid SSLContext
     * @throws IllegalArgumentException when some exception occurred
     */
    public static SSLContext getSSLContextFromLetsEncrypt(String pathTo, String keyPassword) {
        SSLContext context;
        try {
            context = SSLContext.getInstance("TLS");

            byte[] certBytes = parseDERFromPEM(Files.readAllBytes(new File(pathTo + File.separator + "cert.pem").toPath()),
                    "-----BEGIN CERTIFICATE-----", "-----END CERTIFICATE-----");
            byte[] keyBytes = parseDERFromPEM(Files.readAllBytes(new File(pathTo + File.separator + "privkey.pem").toPath()),
                    "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----");

            X509Certificate cert = generateCertificateFromDER(certBytes);
            RSAPrivateKey key = generatePrivateKeyFromDER(keyBytes);

            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(null);
            keystore.setCertificateEntry("cert-alias", cert);

            keystore.setKeyEntry("key-alias", key, keyPassword.toCharArray(),
                    new Certificate[]{CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certBytes))});

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keystore, keyPassword.toCharArray());

            KeyManager[] km = kmf.getKeyManagers();

            context.init(km, null, null);
        } catch (IOException | KeyManagementException | KeyStoreException | InvalidKeySpecException
                | UnrecoverableKeyException | NoSuchAlgorithmException | CertificateException e) {
            throw new IllegalArgumentException();
        }
        return context;
    }

    /**
     * Method which returns a SSLContext from a keystore or IllegalArgumentException on error
     *
     * @return a valid SSLContext
     * @throws IllegalArgumentException when some exception occurred
     */
    public static SSLContext getSSLConextFromKeystore(String storeType, String keystore, String storePassword, String keyPassword) {
        KeyStore ks;
        SSLContext sslContext;
        try {
            ks = KeyStore.getInstance(storeType);
            ks.load(Files.newInputStream(Paths.get("..", keystore)), storePassword.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, keyPassword.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);


            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException | KeyManagementException | UnrecoverableKeyException e) {
            throw new IllegalArgumentException();
        }
        return sslContext;
    }

    protected static byte[] parseDERFromPEM(byte[] pem, String beginDelimiter, String endDelimiter) {
        String data = new String(pem);
        String[] tokens = data.split(beginDelimiter);
        tokens = tokens[1].split(endDelimiter);
        return DatatypeConverter.parseBase64Binary(tokens[0]);
    }

    protected static RSAPrivateKey generatePrivateKeyFromDER(byte[] keyBytes) throws InvalidKeySpecException, NoSuchAlgorithmException {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) factory.generatePrivate(spec);
    }

    protected static X509Certificate generateCertificateFromDER(byte[] certBytes) throws CertificateException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");

        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certBytes));
    }

}
