package br.com.swconsultoria.pdf_signature;

import br.com.swconsultoria.certificado.Certificado;
import br.com.swconsultoria.certificado.CertificadoService;
import br.com.swconsultoria.pdf_signature.dom.AssinaturaModel;

/**
 * @author Samuel Oliveira - samuk.exe@hotmail.com
 * Data: 16/12/2018 - 14:06
 */
public class TesteAssinaturaPdf {

    public static void main(String[] args) {
        try {

            //Cria Certificado
            String caminhoCertificado = "d:/teste/certificado.pfx";
            String senhaCertificado = "123456";
            Certificado certificado = CertificadoService.certificadoPfx(caminhoCertificado, senhaCertificado);

            //Monta Objeto de assinatura
            AssinaturaModel assinaturaModel = new AssinaturaModel();
            assinaturaModel.setCaminhoPdf("d:/termo.pdf");
            assinaturaModel.setCaminhoPdfAssinado("d:/termoAssinado.pdf");
            assinaturaModel.setCertificado(certificado);
            assinaturaModel.setNomeAssinatura("Samuel Oliveira");
            assinaturaModel.setLocalAssinatura("São Paulo - SP - Brasil");
            assinaturaModel.setMotivoAssinatura("Motivo assinatura");
            assinaturaModel.setSenhaCertificado(senhaCertificado.toCharArray());

			//Caso queira usar TSA
			//assinaturaModel.setTsa("http://sha256timestamp.ws.symantec.com/sha256/timestamp");

            AssinaPdf assinaPdf = new AssinaPdf(assinaturaModel);
            assinaPdf.assina();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
