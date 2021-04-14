package br.com.swconsultoria.pdf_signature;

import br.com.swconsultoria.certificado.Certificado;
import br.com.swconsultoria.certificado.CertificadoService;
import br.com.swconsultoria.certificado.exception.CertificadoException;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cms.*;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.util.Store;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * @author mkl
 */
public class Teste {
    final static File RESULT_FOLDER = new File("/d/teste/PdfSignature/");

    public static KeyStore ks = null;
    public static PrivateKey pk = null;
    public static Certificate[] chain = null;

    public static void main(String[] args) throws IOException, CertificadoException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException {

        //Cria Certificado
        String caminhoCertificado = "/d/teste/certificado.pfx";
        String senhaCertificado = "123456";
        Certificado certificado = CertificadoService.certificadoPfx(caminhoCertificado, senhaCertificado);

        ks = CertificadoService.getKeyStore(certificado);
        pk = (PrivateKey) ks.getKey(certificado.getNome(), certificado.getSenha().toCharArray());
        chain = ks.getCertificateChain(ks.aliases().nextElement());

        testSignWithSeparatedHashing();
    }

    /**
     * <a href="http://stackoverflow.com/questions/41767351/create-pkcs7-signature-from-file-digest">
     * Create pkcs7 signature from file digest
     * </a>
     * <p>
     * This test uses the OP's own <code>sign</code> method: {@link #signBySnox(InputStream)}.
     * There are small errors in it, so the result is rejected by verification. These errors
     * are corrected in {@link #signWithSeparatedHashing(InputStream)} which is 2tested in
     * {@link #testSignWithSeparatedHashing()}.
     * </p>
     */
//    public static void testSignWithSeparatedHashingLikeSnox() throws IOException
//    {
//        try (   InputStream resource = new FileInputStream("/d/teste/PdfSignature/TesteAssinatura.pdf");
//                OutputStream result = new FileOutputStream(new File(RESULT_FOLDER, "testSignedLikeSnox.pdf"));
//                PDDocument pdDocument = PDDocument.load(resource)   )
//        {
//            sign(pdDocument, result, data -> signBySnox(data));
//        }
//    }

    /**
     * <a href="http://stackoverflow.com/questions/41767351/create-pkcs7-signature-from-file-digest">
     * Create pkcs7 signature from file digest
     * </a>
     * <p>
     * This test uses a fixed version of the OP's <code>sign</code> method:
     * {@link #signWithSeparatedHashing(InputStream)}. Here the errors from
     * {@link #signBySnox(InputStream)} are corrected, so the result is not
     * rejected by verification anymore.
     * </p>
     */
    public static void testSignWithSeparatedHashing() throws IOException {
        try (InputStream resource = new FileInputStream("/d/teste/PdfSignature/TesteAssinatura.pdf");
             OutputStream result = new FileOutputStream(new File(RESULT_FOLDER, "testSignedLikeSnox.pdf"));
             PDDocument pdDocument = PDDocument.load(resource)) {
            sign(pdDocument, result, Teste::signWithSeparatedHashing);
        }
    }

    /**
     * <a href="http://stackoverflow.com/questions/41767351/create-pkcs7-signature-from-file-digest">
     * Create pkcs7 signature from file digest
     * </a>
     * <p>
     * A minimal signing frame work merely requiring a {@link SignatureInterface}
     * instance.
     * </p>
     */
    static void sign(PDDocument document, OutputStream output, SignatureInterface signatureInterface) throws IOException {
        PDSignature signature = new PDSignature();
        signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
        signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
        signature.setName("Example User");
        signature.setLocation("Los Angeles, CA");
        signature.setReason("Testing");
        signature.setSignDate(Calendar.getInstance());
        document.addSignature(signature);
        ExternalSigningSupport externalSigning =
                document.saveIncrementalForExternalSigning(output);
        // invoke external signature service
        byte[] cmsSignature = signatureInterface.sign(externalSigning.getContent());
        // set signature bytes received from the service
        externalSigning.setSignature(cmsSignature);
    }

    /**
     * <a href="http://stackoverflow.com/questions/41767351/create-pkcs7-signature-from-file-digest">
     * Create pkcs7 signature from file digest
     * </a>
     * <p>
     * The OP's own <code>sign</code> method which has some errors. These
     * errors are fixed in {@link #signWithSeparatedHashing(InputStream)}.
     * </p>
     */
    public static byte[] signBySnox(InputStream content) throws IOException {
        // testSHA1WithRSAAndAttributeTable
        try {
            BouncyCastleProvider bcp = new BouncyCastleProvider();
            Security.insertProviderAt(bcp, 1);
            MessageDigest md = MessageDigest.getInstance("SHA1", "BC");
            List<Certificate> certList = new ArrayList<>();
            CMSTypedData msg = new CMSProcessableByteArray(IOUtils.toByteArray(content));

            certList.addAll(Arrays.asList(chain));

            Store certs = new JcaCertStore(certList);

            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();

            Attribute attr = new Attribute(CMSAttributes.messageDigest,
                    new DERSet(new DEROctetString(md.digest(IOUtils.toByteArray(content)))));

            ASN1EncodableVector v = new ASN1EncodableVector();

            v.add(attr);

            SignerInfoGeneratorBuilder builder = new SignerInfoGeneratorBuilder(new BcDigestCalculatorProvider())
                    .setSignedAttributeGenerator(new DefaultSignedAttributeTableGenerator(new AttributeTable(v)));

            AlgorithmIdentifier sha1withRSA = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA1withRSA");

            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            InputStream in = new ByteArrayInputStream(chain[0].getEncoded());
            X509Certificate cert = (X509Certificate) certFactory.generateCertificate(in);

            gen.addSignerInfoGenerator(builder.build(
                    new BcRSAContentSignerBuilder(sha1withRSA,
                            new DefaultDigestAlgorithmIdentifierFinder().find(sha1withRSA))
                            .build(PrivateKeyFactory.createKey(pk.getEncoded())),
                    new JcaX509CertificateHolder(cert)));

            gen.addCertificates(certs);

            CMSSignedData s = gen.generate(new CMSAbsentContent(), false);
            return new CMSSignedData(msg, s.getEncoded()).getEncoded();

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }

    /**
     * <a href="http://stackoverflow.com/questions/41767351/create-pkcs7-signature-from-file-digest">
     * Create pkcs7 signature from file digest
     * </a>
     * <p>
     * The OP's <code>sign</code> method after fixing some errors. The
     * OP's original method is {@link #signBySnox(InputStream)}. The
     * errors were
     * </p>
     * <ul>
     * <li>multiple attempts at reading the {@link InputStream} parameter;
     * <li>convoluted creation of final CMS container.
     * </ul>
     * <p>
     * Additionally this method uses SHA256 instead of SHA-1.
     * </p>
     */
    public static byte[] signWithSeparatedHashing(InputStream content) throws IOException {
        try {
            BouncyCastleProvider bcp = new BouncyCastleProvider();
            Security.insertProviderAt(bcp, 1);
            // Digest generation step
            MessageDigest md = MessageDigest.getInstance("SHA256", "BC");
            byte[] digest = md.digest(IOUtils.toByteArray(content));

            // Separate signature container creation step
            List<Certificate> certList = Arrays.asList(chain);
            JcaCertStore certs = new JcaCertStore(certList);

            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();

            Attribute attr = new Attribute(CMSAttributes.messageDigest,
                    new DERSet(new DEROctetString(digest)));

            ASN1EncodableVector v = new ASN1EncodableVector();

            v.add(attr);

            SignerInfoGeneratorBuilder builder = new SignerInfoGeneratorBuilder(new BcDigestCalculatorProvider())
                    .setSignedAttributeGenerator(new DefaultSignedAttributeTableGenerator(new AttributeTable(v)));

            AlgorithmIdentifier sha256withRSA = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256withRSA");

            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            InputStream in = new ByteArrayInputStream(chain[0].getEncoded());
            X509Certificate cert = (X509Certificate) certFactory.generateCertificate(in);

            gen.addSignerInfoGenerator(builder.build(
                    new BcRSAContentSignerBuilder(sha256withRSA,
                            new DefaultDigestAlgorithmIdentifierFinder().find(sha256withRSA))
                            .build(PrivateKeyFactory.createKey(pk.getEncoded())),
                    new JcaX509CertificateHolder(cert)));

            gen.addCertificates(certs);

            CMSSignedData s = gen.generate(new CMSAbsentContent(), false);
            byte[] encoded = s.getEncoded();
            File file = new File(RESULT_FOLDER, "testSignedLikeSnox.p7s");
            FileOutputStream os = new FileOutputStream(file);
            os.write(encoded);
            os.flush();
            os.close();
            return encoded;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }
}