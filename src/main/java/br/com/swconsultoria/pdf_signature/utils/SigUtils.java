package br.com.swconsultoria.pdf_signature.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDPushButton;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.*;

public class SigUtils {
    private static final Log LOG = LogFactory.getLog(SigUtils.class);

    private SigUtils() {
    }

    /**
     * Verifica se um objeto e vazio.
     *
     * @param obj
     * @return <b>true</b> se o objeto for vazio(empty).
     */
    public static <T> Optional<T> verifica(T obj) {
        if (obj == null)
            return Optional.empty();
        if (obj instanceof Collection)
            return ((Collection<?>) obj).size() == 0 ? Optional.empty() : Optional.of(obj);

        final String s = String.valueOf(obj).trim();

        return s.length() == 0 || s.equalsIgnoreCase("null") ? Optional.empty() : Optional.of(obj);
    }

    public static int getMDPPermission(PDDocument doc) {
        COSBase base = doc.getDocumentCatalog().getCOSObject().getDictionaryObject(COSName.PERMS);
        if (base instanceof COSDictionary) {
            COSDictionary permsDict = (COSDictionary) base;
            base = permsDict.getDictionaryObject(COSName.DOCMDP);
            if (base instanceof COSDictionary) {
                COSDictionary signatureDict = (COSDictionary) base;
                base = signatureDict.getDictionaryObject("Reference");
                if (base instanceof COSArray) {
                    COSArray refArray = (COSArray) base;
                    for (int i = 0; i < refArray.size(); ++i) {
                        base = refArray.getObject(i);
                        if (base instanceof COSDictionary) {
                            COSDictionary sigRefDict = (COSDictionary) base;
                            if (COSName.DOCMDP.equals(sigRefDict.getDictionaryObject("TransformMethod"))) {
                                base = sigRefDict.getDictionaryObject("TransformParams");
                                if (base instanceof COSDictionary) {
                                    COSDictionary transformDict = (COSDictionary) base;
                                    int accessPermissions = transformDict.getInt(COSName.P, 2);
                                    if (accessPermissions < 1 || accessPermissions > 3) {
                                        accessPermissions = 2;
                                    }
                                    return accessPermissions;
                                }
                            }
                        }
                    }
                }
            }
        }
        return 0;
    }

    static public void setMDPPermission(PDDocument doc, PDSignature signature, int accessPermissions) {
        COSDictionary sigDict = signature.getCOSObject();

        COSDictionary transformParameters = new COSDictionary();
        transformParameters.setItem(COSName.TYPE, COSName.getPDFName("TransformParams"));
        transformParameters.setInt(COSName.P, accessPermissions);
        transformParameters.setName(COSName.V, "1.2");
        transformParameters.setNeedToBeUpdated(true);

        COSDictionary referenceDict = new COSDictionary();
        referenceDict.setItem(COSName.TYPE, COSName.getPDFName("SigRef"));
        referenceDict.setItem("TransformMethod", COSName.DOCMDP);
        referenceDict.setItem("DigestMethod", COSName.getPDFName("SHA1"));
        referenceDict.setItem("TransformParams", transformParameters);
        referenceDict.setNeedToBeUpdated(true);

        COSArray referenceArray = new COSArray();
        referenceArray.add(referenceDict);
        sigDict.setItem("Reference", referenceArray);
        referenceArray.setNeedToBeUpdated(true);

        // Catalog
        COSDictionary catalogDict = doc.getDocumentCatalog().getCOSObject();
        COSDictionary permsDict = new COSDictionary();
        catalogDict.setItem(COSName.PERMS, permsDict);
        permsDict.setItem(COSName.DOCMDP, signature);
        catalogDict.setNeedToBeUpdated(true);
        permsDict.setNeedToBeUpdated(true);
    }

    public static void checkCertificateUsage(X509Certificate x509Certificate)
            throws CertificateParsingException {
        boolean[] keyUsage = x509Certificate.getKeyUsage();
        if (keyUsage != null && !keyUsage[0] && !keyUsage[1]) {
            LOG.error("Certificate key usage does not include " +
                    "digitalSignature nor nonRepudiation");
        }
        List<String> extendedKeyUsage = x509Certificate.getExtendedKeyUsage();
        if (extendedKeyUsage != null &&
                !extendedKeyUsage.contains(KeyPurposeId.id_kp_emailProtection.toString()) &&
                !extendedKeyUsage.contains(KeyPurposeId.id_kp_codeSigning.toString()) &&
                !extendedKeyUsage.contains(KeyPurposeId.anyExtendedKeyUsage.toString()) &&
                !extendedKeyUsage.contains("1.2.840.113583.1.1.5") &&
                !extendedKeyUsage.contains("1.3.6.1.4.1.311.10.3.12")) {
            LOG.error("Certificate extended key usage does not include " +
                    "emailProtection, nor codeSigning, nor anyExtendedKeyUsage, " +
                    "nor 'Adobe Authentic Documents Trust'");
        }
    }

    public static void checkTimeStampCertificateUsage(X509Certificate x509Certificate)
            throws CertificateParsingException {
        List<String> extendedKeyUsage = x509Certificate.getExtendedKeyUsage();
        // https://tools.ietf.org/html/rfc5280#section-4.2.1.12
        if (extendedKeyUsage != null &&
                !extendedKeyUsage.contains(KeyPurposeId.id_kp_timeStamping.toString())) {
            LOG.error("Certificate extended key usage does not include timeStamping");
        }
    }

    public static void checkResponderCertificateUsage(X509Certificate x509Certificate)
            throws CertificateParsingException {
        List<String> extendedKeyUsage = x509Certificate.getExtendedKeyUsage();
        // https://tools.ietf.org/html/rfc5280#section-4.2.1.12
        if (extendedKeyUsage != null &&
                !extendedKeyUsage.contains(KeyPurposeId.id_kp_OCSPSigning.toString())) {
            LOG.error("Certificate extended key usage does not include OCSP responding");
        }
    }

    public static PDSignature getLastRelevantSignature(PDDocument document) throws IOException {
        SortedMap<Integer, PDSignature> sortedMap = new TreeMap<>();
        for (PDSignature signature : document.getSignatureDictionaries()) {
            int sigOffset = signature.getByteRange()[1];
            sortedMap.put(sigOffset, signature);
        }
        if (sortedMap.size() > 0) {
            PDSignature lastSignature = sortedMap.get(sortedMap.lastKey());
            COSBase type = lastSignature.getCOSObject().getItem(COSName.TYPE);
            if (type.equals(COSName.SIG) || type.equals(COSName.DOC_TIME_STAMP)) {
                return lastSignature;
            }
        }
        return null;
    }

    public static CMSSignedData getPKCS7(String caminhoPdf) throws CMSException, IOException {
        PDDocument doc = PDDocument.load(new File(caminhoPdf));
        PDSignature signature = doc.getSignatureDictionaries().get(0);

        byte[] pdfByte = Files.readAllBytes(Paths.get(caminhoPdf));
        byte[] signatureAsBytes = signature.getContents(pdfByte);
        byte[] signedContentAsBytes = signature.getSignedContent(pdfByte);
        doc.close();
        return new CMSSignedData(new CMSProcessableByteArray(signedContentAsBytes), signatureAsBytes);
    }

    public static void criaPKCS7(CMSSignedData signedData, String caminhoArquivo) throws IOException {

        File file = new File(caminhoArquivo);
        FileOutputStream os = new FileOutputStream(file);
        os.write(signedData.getEncoded());
        os.flush();
        os.close();
    }

    public static void preencheFormulario(PDDocument document, PDAcroForm acroForm, Map<String, String> campos) {
        acroForm.getFields().forEach(field -> {
            String valor = campos.get(field.getFullyQualifiedName());
            if (verifica(valor).isPresent()) {
                try {

                    if (field instanceof PDPushButton) {
                        preencheImagemForm(document, field, valor);
                    } else {
                        field.setValue(valor);
                    }
                    field.setReadOnly(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static void preencheImagemForm(PDDocument document, PDField field, String valor) throws IOException {

        PDPushButton pdPushButton = (PDPushButton) field;

        List<PDAnnotationWidget> widgets = pdPushButton.getWidgets();
        if (widgets != null && widgets.size() > 0) {
            PDAnnotationWidget annotationWidget = widgets.get(0);

            File imageFile = new File(valor);
            if (!imageFile.exists()) {
                throw new RuntimeException("Imagem " + valor + " não existe para o campo " + field.getFullyQualifiedName());
            }

            PDImageXObject pdImageXObject = PDImageXObject.createFromFile(valor, document);
            float imageScaleRatio = (float) pdImageXObject.getHeight() / (float) pdImageXObject.getWidth();

            PDRectangle buttonPosition = getFieldArea(pdPushButton);
            float height = buttonPosition.getHeight();
            float width = height / imageScaleRatio;
            float x = buttonPosition.getLowerLeftX();
            float y = buttonPosition.getLowerLeftY();
            PDAppearanceStream pdAppearanceStream = new PDAppearanceStream(document);
            pdAppearanceStream.setResources(new PDResources());
            try (PDPageContentStream pdPageContentStream = new PDPageContentStream(document, pdAppearanceStream)) {
                pdPageContentStream.drawImage(pdImageXObject, x, y, width, height);
            }
            pdAppearanceStream.setBBox(new PDRectangle(x, y, width, height));
            PDAppearanceDictionary pdAppearanceDictionary = annotationWidget.getAppearance();
            if (pdAppearanceDictionary == null) {
                pdAppearanceDictionary = new PDAppearanceDictionary();
                annotationWidget.setAppearance(pdAppearanceDictionary);
            }
            pdAppearanceDictionary.setNormalAppearance(pdAppearanceStream);
        }
    }

    private static PDRectangle getFieldArea(PDField field) {
        COSDictionary fieldDict = field.getCOSObject();
        COSArray fieldAreaArray = (COSArray) fieldDict.getDictionaryObject(COSName.RECT);
        return new PDRectangle(fieldAreaArray);
    }
}