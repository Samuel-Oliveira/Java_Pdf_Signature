package br.com.swconsultoria.pdf_signature;

import br.com.swconsultoria.pdf_signature.dom.AssinaturaModel;
import br.com.swconsultoria.pdf_signature.pdfbox.CreateSignatureBase;
import br.com.swconsultoria.pdf_signature.utils.SigUtils;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;

/**
 * @author Samuel Oliveira
 */
public class AssinaPdf extends CreateSignatureBase {

    private AssinaturaModel assinaturaModel;

    public AssinaPdf(AssinaturaModel assinaturaModel) throws Exception {
        super(assinaturaModel.getCertificado());
        this.assinaturaModel = assinaturaModel;
        if (assinaturaModel.getTsa() != null && !assinaturaModel.getTsa().equals("")) {
            setTsaUrl(assinaturaModel.getTsa());
        }
    }

    public void assina() throws Exception {

        if (!new File(assinaturaModel.getCaminhoPdf()).exists()) {
            throw new Exception("Pdf não encontrado");
        }

        try (FileOutputStream fos = new FileOutputStream(new File(assinaturaModel.getCaminhoPdfAssinado()));
             PDDocument doc = PDDocument.load(new File(assinaturaModel.getCaminhoPdf()))) {
            if (doc.isEncrypted()) {
                try {
                    doc.setAllSecurityToBeRemoved(true);
                } catch (Exception e) {
                    throw new Exception("O Documento está criptografado.", e);
                }
            }
            criaAssinatura(doc, fos);
        }

    }

    private void criaAssinatura(PDDocument document, OutputStream saida) throws IOException {

        int accessPermissions = SigUtils.getMDPPermission(document);
        if (accessPermissions == 1) {
            throw new IllegalStateException("Mudanças no documento não são permitidas.");
        }

        // Cria Dicionario de Assinatura
        PDSignature signature = new PDSignature();
        signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
        signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
        signature.setName(assinaturaModel.getNomeAssinatura());
        signature.setLocation(assinaturaModel.getLocalAssinatura());
        signature.setReason(assinaturaModel.getMotivoAssinatura());

        // Adicionada Data Assinatura
        signature.setSignDate(Calendar.getInstance());

        if (accessPermissions == 0) {
            SigUtils.setMDPPermission(document, signature, 2);
        }

        if (SigUtils.verifica(assinaturaModel.getCamposFormulario()).isPresent()) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            SigUtils.preencheFormulario(document, acroForm, assinaturaModel.getCamposFormulario());
        }

        SignatureOptions signatureOptions = new SignatureOptions();
        signatureOptions.setPreferredSignatureSize(SignatureOptions.DEFAULT_SIGNATURE_SIZE * 2);
        document.addSignature(signature, this, signatureOptions);
        document.save(saida);
        document.close();
        IOUtils.closeQuietly(signatureOptions);

    }

    /**
     * Valida os dados
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
    }

}
