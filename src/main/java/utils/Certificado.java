/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.cert.Certificate;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemGenerationException;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

/**
 *
 * @author dmmaga
 */
public class Certificado {

    public Certificado() {
        Security.addProvider(new BouncyCastleProvider());
    }
    
    private final static String BC_PROVIDER = BouncyCastleProvider.PROVIDER_NAME;

    /*
    * Escribe el certificado del usuario
     * @param user Nombre el usuario dueño del certificado
     * @param pair Llave privada y certificado del usuario
     */
    public File writeUserCert(String user, Pair<PrivateKey, Certificate> pair) throws FileNotFoundException, IOException {
        File userCert = new File("/home/dmmaga/certificadosRadius/" + user + "Cert.pem");
        FileOutputStream fos = new FileOutputStream(userCert);
        JcaPEMWriter writer = new JcaPEMWriter(new PrintWriter(fos));
        writer.writeObject(pair.getRight());
        writer.flush();
        fos.close();
        return userCert;
    }

    /*
    * Encripta la llave privada del usuario y la esribe
     * @param user Nombre el usuario dueño del certificado
     * @param pair Llave privada y certificado del usuario
     */
    public void writeEncryptedUserKey(String user, Pair<PrivateKey, Certificate> pair) throws NoSuchAlgorithmException, OperatorCreationException, PemGenerationException, IOException {
        //Encriptamos la llave del cliente para mayor seguridad
        JceOpenSSLPKCS8EncryptorBuilder encryptorBuilder = new JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.PBE_SHA1_3DES);
        encryptorBuilder.setRandom(SecureRandom.getInstance("NativePRNGNonBlocking"));
        encryptorBuilder.setPasssword("pdc135".toCharArray());
        OutputEncryptor oe = encryptorBuilder.build();
        JcaPKCS8Generator gen = new JcaPKCS8Generator(pair.getLeft(), oe);
        PemObject obj = gen.generate();
        //Escribimos el resultado de la llave encriptada a su respectivo archivo
        JcaPEMWriter pemWrt = new JcaPEMWriter(new FileWriter("/home/dmmaga/certificadosRadius/" + user + "PrivKey.pem"));
        pemWrt.writeObject(obj);
        pemWrt.close();
    }

    /*
    * Lee un certificado desde un archivo en formato PEM
    * @param path Archivo en formato PEM del que leera el certificado
    * @return Devuelve un certificateHolder que contiene al certificado extraido del archivo
     */
    public X509CertificateHolder readPemCertificates(File path) throws FileNotFoundException, IOException {
        FileReader fr = new FileReader(path);
        PemReader reader = new PemReader(fr);
        PemObject o;
        X509CertificateHolder cert = null;
        while ((o = reader.readPemObject()) != null) {
            cert = new X509CertificateHolder(o.getContent());
        }
        reader.close();
        fr.close();

        return cert;
    }

    /*
    * Extrae un certificado de un certificateHolder
    * @param certHolder certificateHolder que contiene el certificado a extraer
    * @return Devuelve el certificado que contenia el certificateHolder
     */
    public X509Certificate convertCertificate(X509CertificateHolder certHolder) throws CertificateException {
        return new JcaX509CertificateConverter().setProvider(BC_PROVIDER).getCertificate(certHolder);
    }

    /*
    * Genera un certificado firmado por la entidad certificadora que se le pase como parametro
    * @param commonName: CN del certificado que se va a generar
    * @param caCert: Certificado de la entidad certificadora con la que se firmara el certificado
    * @return Devuelve el certificado firmado y su llave privada
     */
    public Pair<PrivateKey, Certificate> generateCert(String commonName, X509Certificate caCert)
            throws NoSuchProviderException, NoSuchAlgorithmException, OperatorCreationException, CertificateException, Exception {
        //Se especifica el algoritmo de las llaves
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", BC_PROVIDER);
        //El tamaño de las llaves
        keyPairGenerator.initialize(2048);
        //Se generan las llaves publica y privada
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        //Se guardan en variables los atributos del certificado
        X500Name dnName = new X500Name("CN=" + commonName);
        BigInteger certSerialNumber = BigInteger.valueOf(System.currentTimeMillis());
        String signatureAlgorithm = "SHA256WithRSA";
        //Quien firma el certificado
        ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm)
                .build(readPemPrivateKey(new File("/home/dmmaga/certificadosRadius/cakey.pem")));
        //Valido desde
        Instant startDate = Instant.now();
        //Valido hasta
        Instant endDate = startDate.plus(2 * 365, ChronoUnit.DAYS);
        //Se construye el certificado
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                caCert, certSerialNumber, Date.from(startDate), Date.from(endDate), dnName,
                keyPair.getPublic());
        //Se firma y finaliza la creacion del certificado
        Certificate certificate = new JcaX509CertificateConverter().setProvider(BC_PROVIDER)
                .getCertificate(certBuilder.build(contentSigner));
        return Pair.of(keyPair.getPrivate(), certificate);
    }

    /*
    * Lee una llave privada que no este encriptada desde un archivo en formato PEM
    * @param path Archivo en formato PEM del que leera el certificado
    * @return Devuelve la llave privada que se obtuvo del archivo
     */
    public PrivateKey readPemPrivateKey(File path) throws FileNotFoundException, IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        FileReader fr = new FileReader(path);
        PemReader reader = new PemReader(fr);
        KeySpec keySpec = new PKCS8EncodedKeySpec(reader.readPemObject().getContent());
        reader.close();
        KeyFactory kf = KeyFactory.getInstance("RSA", BC_PROVIDER);
        System.out.println(kf.getAlgorithm());
        return kf.generatePrivate(keySpec);
    }

}
