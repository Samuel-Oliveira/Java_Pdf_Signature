package br.com.swconsultoria.pdf_signature;

import br.com.swconsultoria.certificado.Certificado;
import br.com.swconsultoria.certificado.CertificadoService;
import br.com.swconsultoria.pdf_signature.dom.AssinaturaModel;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Samuel Oliveira - samuk.exe@hotmail.com
 * Data: 12/01/2019 - 14:06
 */
public class TesteAssinaturaPdfImagemForm {

    public static void main(String[] args) {
        try {

            // Desabilita Logs Internos Do PDFBOX
            Logger.getLogger("org.apache.pdfbox").setLevel(Level.OFF);

            //Cria Certificado
            String caminhoCertificado = "/d/teste/certificado.pfx";
            String senhaCertificado = "12345";
            Certificado certificado = CertificadoService.certificadoPfx(caminhoCertificado, senhaCertificado);

            //Monta Objeto de assinatura
            AssinaturaModel assinaturaModel = new AssinaturaModel();
            assinaturaModel.setCaminhoPdf("/d/teste/PdfSignature/TesteAssinaturaForm2.pdf");
            assinaturaModel.setCaminhoPdfAssinado("/d/teste/PdfSignature/TesteAssinaturaAssinadoImg.pdf");
            assinaturaModel.setCertificado(certificado);
            assinaturaModel.setNomeAssinatura("Samuel Oliveira");
            assinaturaModel.setLocalAssinatura("São Paulo - SP - Brasil");
            assinaturaModel.setMotivoAssinatura("Motivo assinatura");
            assinaturaModel.setSenhaCertificado(senhaCertificado.toCharArray());

            //Campos Do Formulario
            Map<String, String> map = new HashMap<>();
            map.put("01_Nome do Paciente", "Teste nome");
            map.put("01_Período", "20 DIas");
            assinaturaModel.setCamposFormulario(map);

            //Número da Pagina que será assinado
            assinaturaModel.setPagina(1);
            assinaturaModel.setCaminhoImagem("/d/teste/PdfSignature/Assinatura.png");

            //Posicao da Imagem na Página
            assinaturaModel.setCampoAssinatura("03_Signature Emitente");

            AssinaPdfImagem assinaPdfImagem = new AssinaPdfImagem(assinaturaModel);
            assinaPdfImagem.assina();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
