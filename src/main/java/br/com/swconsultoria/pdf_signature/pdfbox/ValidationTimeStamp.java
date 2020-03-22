package br.com.swconsultoria.pdf_signature.pdfbox;

import org.apache.pdfbox.io.IOUtils;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.Attributes;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Samuel Oliveira - samuk.exe@hotmail.com
 * Data: 16/12/2018 - 15:30
 */
public class ValidationTimeStamp {

    private TSAClient tsaClient;

    ValidationTimeStamp(String tsaUrl) throws NoSuchAlgorithmException, MalformedURLException {
        if (tsaUrl != null) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            this.tsaClient = new TSAClient(new URL(tsaUrl), null, null, digest);
        }
    }

    public byte[] getTimeStampToken(InputStream content) throws IOException {
        return tsaClient.getTimeStampToken(IOUtils.toByteArray(content));
    }

    CMSSignedData addSignedTimeStamp(CMSSignedData signedData)
            throws IOException {
        SignerInformationStore signerStore = signedData.getSignerInfos();
        List<SignerInformation> newSigners = new ArrayList<>();

        for (Object signer : signerStore.getSigners()) {
            newSigners.add(signTimeStamp((SignerInformation) signer));
        }

        return CMSSignedData.replaceSigners(signedData, new SignerInformationStore(newSigners));
    }

    private SignerInformation signTimeStamp(SignerInformation signer)
            throws IOException {
        AttributeTable unsignedAttributes = signer.getUnsignedAttributes();

        ASN1EncodableVector vector = new ASN1EncodableVector();
        if (unsignedAttributes != null) {
            vector = unsignedAttributes.toASN1EncodableVector();
        }

        byte[] token = tsaClient.getTimeStampToken(signer.getSignature());
        ASN1ObjectIdentifier oid = PKCSObjectIdentifiers.id_aa_signatureTimeStampToken;
        ASN1Encodable signatureTimeStamp = new Attribute(oid,
                new DERSet(ASN1Set.fromByteArray(token)));

        vector.add(signatureTimeStamp);
        Attributes signedAttributes = new Attributes(vector);
        return SignerInformation.replaceUnsignedAttributes(signer, new AttributeTable(signedAttributes));
    }
}
