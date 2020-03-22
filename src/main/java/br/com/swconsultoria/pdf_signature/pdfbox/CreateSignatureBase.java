package br.com.swconsultoria.pdf_signature.pdfbox;

/**
 * @author Samuel Oliveira - samuk.exe@hotmail.com
 * Data: 16/12/2018 - 15:27
 */

import br.com.swconsultoria.certificado.Certificado;
import br.com.swconsultoria.certificado.CertificadoService;
import br.com.swconsultoria.certificado.exception.CertificadoException;
import br.com.swconsultoria.pdf_signature.utils.SigUtils;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public abstract class CreateSignatureBase implements SignatureInterface {
    private PrivateKey privateKey;
    private Certificate[] certificateChain;
    private String tsaUrl;
    private boolean externalSigning;

    public CreateSignatureBase(Certificado certificado)
            throws KeyStoreException, UnrecoverableEntryException, NoSuchAlgorithmException, CertificateException, CertificadoException {

        KeyStore keyStore = CertificadoService.getKeyStore(certificado);
        KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(certificado.getNome(),
                new KeyStore.PasswordProtection(certificado.getSenha().toCharArray()));
        this.privateKey = pkEntry.getPrivateKey();

        Certificate[] certChain = keyStore.getCertificateChain(certificado.getNome());
        this.certificateChain = certChain;

        Certificate cert = certChain[0];
        if (cert instanceof X509Certificate) {
            ((X509Certificate) cert).checkValidity();

            SigUtils.checkCertificateUsage((X509Certificate) cert);
        }
    }

    protected void setTsaUrl(String tsaUrl) {
        this.tsaUrl = tsaUrl;
    }

    @Override
    public byte[] sign(InputStream content) throws IOException {
        try {
            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
            X509Certificate cert = (X509Certificate) certificateChain[0];
            ContentSigner sha1Signer = new JcaContentSignerBuilder("SHA256WithRSA").build(privateKey);
            gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build()).build(sha1Signer, cert));
            gen.addCertificates(new JcaCertStore(Arrays.asList(certificateChain)));
            CMSProcessableInputStream msg = new CMSProcessableInputStream(content);
            CMSSignedData signedData = gen.generate(msg, false);
            if (tsaUrl != null && tsaUrl.length() > 0) {
                ValidationTimeStamp validation = new ValidationTimeStamp(tsaUrl);
                signedData = validation.addSignedTimeStamp(signedData);
            }
            return signedData.getEncoded();
        } catch (GeneralSecurityException | CMSException | OperatorCreationException e) {
            throw new IOException(e);
        }
    }

}