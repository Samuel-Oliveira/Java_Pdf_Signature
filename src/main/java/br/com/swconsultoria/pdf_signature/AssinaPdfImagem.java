package br.com.swconsultoria.pdf_signature;

import br.com.swconsultoria.certificado.CertificadoService;
import br.com.swconsultoria.pdf_signature.dom.AssinaturaModel;
import br.com.swconsultoria.pdf_signature.pdfbox.CreateSignatureBase;
import br.com.swconsultoria.pdf_signature.utils.SigUtils;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSigProperties;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSignDesigner;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Calendar;

/**
 * @author Samuel Oliveira
 */
public class AssinaPdfImagem extends CreateSignatureBase {

    private AssinaturaModel assinaturaModel;
    private final PDVisibleSigProperties visibleSignatureProperties = new PDVisibleSigProperties();

    public AssinaPdfImagem(AssinaturaModel assinaturaModel) throws Exception {
        super(CertificadoService.getKeyStore(assinaturaModel.getCertificado()), assinaturaModel.getSenhaCertificado());
        validaInformacoes(assinaturaModel);
        this.assinaturaModel = assinaturaModel;
        if (assinaturaModel.getTsa() != null && !assinaturaModel.getTsa().equals("")) {
            setTsaUrl(assinaturaModel.getTsa());
        }

        PDVisibleSignDesigner visibleSignDesigner = new PDVisibleSignDesigner(assinaturaModel.getCaminhoPdf(),
                new FileInputStream(new File(assinaturaModel.getCaminhoImagem())),
                assinaturaModel.getPagina());
        visibleSignDesigner.xAxis(assinaturaModel.getPosicaoX()).
                yAxis(assinaturaModel.getPosicaoY()).
                zoom(assinaturaModel.getZoomImagem()).adjustForRotation();

        visibleSignatureProperties.signerName(assinaturaModel.getNomeAssinatura()).
                signerLocation(assinaturaModel.getLocalAssinatura()).
                signatureReason(assinaturaModel.getMotivoAssinatura()).
                preferredSize(0).page(assinaturaModel.getPagina()).visualSignEnabled(true).
                setPdVisibleSignature(visibleSignDesigner);

    }

    public void assina() throws Exception {

        if (!new File(assinaturaModel.getCaminhoPdf()).exists()) {
            throw new Exception("Pdf não encontrado");
        }

        SignatureOptions signatureOptions;
        try (FileOutputStream fos = new FileOutputStream(assinaturaModel.getCaminhoPdfAssinado());
             PDDocument doc = PDDocument.load(new File(assinaturaModel.getCaminhoPdf()))) {
            int accessPermissions = SigUtils.getMDPPermission(doc);
            if (accessPermissions == 1) {
                throw new IllegalStateException("Mudanças no documento não são permitidas.");
            }

            PDSignature signature = new PDSignature();

            if (doc.getVersion() >= 1.5f && accessPermissions == 0) {
                SigUtils.setMDPPermission(doc, signature, 2);
            }

            PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
            if (acroForm != null && acroForm.getNeedAppearances()) {
                if (acroForm.getFields().isEmpty()) {
                    acroForm.getCOSObject().removeItem(COSName.NEED_APPEARANCES);
                } else {
                    System.out.println("/NeedAppearances está setado, assinatura pode ser ignorada pelo Adobe Reader");
                }
            }

            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);

            visibleSignatureProperties.buildSignature();

            signature.setName(visibleSignatureProperties.getSignerName());
            signature.setLocation(visibleSignatureProperties.getSignerLocation());
            signature.setReason(visibleSignatureProperties.getSignatureReason());

            signature.setSignDate(Calendar.getInstance());

            signatureOptions = new SignatureOptions();
            signatureOptions.setVisualSignature(visibleSignatureProperties.getVisibleSignature());
            signatureOptions.setPage(visibleSignatureProperties.getPage() - 1);
            doc.addSignature(signature, this, signatureOptions);
            doc.saveIncremental(fos);
        }

        IOUtils.closeQuietly(signatureOptions);
    }

    /**
     * Válida os Dados
     *
     * @param assinaturaModel
     * @throws Exception
     */
    private void validaInformacoes(AssinaturaModel assinaturaModel) throws Exception {
        SigUtils.verifica(assinaturaModel.getCaminhoPdf()).orElseThrow(() -> new Exception("É necessário informar o caminho do Pdf."));
        SigUtils.verifica(assinaturaModel.getCertificado()).orElseThrow(() -> new Exception("É necessário informar o certificado."));
        SigUtils.verifica(assinaturaModel.getSenhaCertificado()).orElseThrow(() -> new Exception("É necessário informar a senha do certificado"));
        SigUtils.verifica(assinaturaModel.getCaminhoPdfAssinado()).orElseThrow(() -> new Exception("É necessário informar o destino do pdf"));
        SigUtils.verifica(assinaturaModel.getLocalAssinatura()).orElseThrow(() -> new Exception("É necessário informar o local da assinatura"));
        SigUtils.verifica(assinaturaModel.getNomeAssinatura()).orElseThrow(() -> new Exception("É necessário informar o nome da assinatura"));
        SigUtils.verifica(assinaturaModel.getMotivoAssinatura()).orElseThrow(() -> new Exception("É necessário informar o motivo da assinatura"));
        SigUtils.verifica(assinaturaModel.getZoomImagem()).orElseThrow(() -> new Exception("É necessário informar p Zoom"));
        SigUtils.verifica(assinaturaModel.getPosicaoX()).orElseThrow(() -> new Exception("É necessário informar a posição X"));
        SigUtils.verifica(assinaturaModel.getPosicaoY()).orElseThrow(() -> new Exception("É necessário informar a posição Y"));
        SigUtils.verifica(assinaturaModel.getCaminhoImagem()).orElseThrow(() -> new Exception("É necessário informar o caminho da imagem"));
        SigUtils.verifica(assinaturaModel.getPagina()).orElseThrow(() -> new Exception("É necessário informar o número da página"));

    }
}
