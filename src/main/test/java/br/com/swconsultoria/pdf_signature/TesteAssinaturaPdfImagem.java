package br.com.swconsultoria.pdf_signature;

import br.com.swconsultoria.certificado.Certificado;
import br.com.swconsultoria.certificado.CertificadoService;
import br.com.swconsultoria.pdf_signature.dom.AssinaturaModel;
import br.com.swconsultoria.pdf_signature.utils.SigUtils;
import org.bouncycastle.cms.CMSSignedData;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Samuel Oliveira - samuk.exe@hotmail.com
 * Data: 12/01/2019 - 14:06
 */
public class TesteAssinaturaPdfImagem {

    public static void main(String[] args) {
        try {

            // Desabilita Logs Internos Do PDFBOX
            Logger.getLogger("org.apache.pdfbox").setLevel(Level.OFF);

            //Cria Certificado
            String caminhoCertificado = "/d/teste/certificado.pfx";
            String senhaCertificado = "123456";
            Certificado certificado = CertificadoService.certificadoPfx(caminhoCertificado, senhaCertificado);

            //Monta Objeto de assinatura
            AssinaturaModel assinaturaModel = new AssinaturaModel();
            assinaturaModel.setCaminhoPdf("/d/teste/PdfSignature/TesteAssinatura.pdf");
            assinaturaModel.setCaminhoPdfAssinado("/d/teste/PdfSignature/TesteAssinaturaAssinado.pdf");
            assinaturaModel.setCertificado(certificado);
            assinaturaModel.setNomeAssinatura("Samuel Oliveira");
            assinaturaModel.setLocalAssinatura("São Paulo - SP - Brasil");
            assinaturaModel.setMotivoAssinatura("Motivo assinatura");
            assinaturaModel.setSenhaCertificado(senhaCertificado.toCharArray());

            //Número da Pagina que será assinado
            assinaturaModel.setPagina(1);
            assinaturaModel.setCaminhoImagem("/d/teste/PdfSignature/Assinatura.png");

            //Posicao da Imagem na Página
            assinaturaModel.setPosicaoX(0);
            assinaturaModel.setPosicaoY(0);

            assinaturaModel.setZoomImagem(-50);

            //Caso queira usar TSA
            assinaturaModel.setTsa("http://sha256timestamp.ws.symantec.com/sha256/timestamp");

            AssinaPdfImagem assinaPdfImagem = new AssinaPdfImagem(assinaturaModel);
            CMSSignedData retorno = assinaPdfImagem.assina();
            SigUtils.criaPKCS7(retorno, "/d/teste/PdfSignature/pkcs7.p7s");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
