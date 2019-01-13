package br.com.swconsultoria.pdf_signature.dom;


import br.com.swconsultoria.certificado.Certificado;

/**
 * @author Samuel Oliveira - samuk.exe@hotmail.com
 * Data: 16/12/2018 - 14:48
 */
public class AssinaturaModel {

    private String caminhoPdf;
    private String caminhoPdfAssinado;
    private Certificado certificado;
    private char[] senhaCertificado;
    private String nomeAssinatura;
    private String localAssinatura;
    private String motivoAssinatura;
    private String tsa;
    private String caminhoImagem;
    private int posicaoX;
    private int posicaoY;
    private int zoomImagem;
    private int pagina;

    public String getCaminhoPdf() {
        return caminhoPdf;
    }

    public void setCaminhoPdf(String caminhoPdf) {
        this.caminhoPdf = caminhoPdf;
    }

    public String getCaminhoPdfAssinado() {
        return caminhoPdfAssinado;
    }

    public void setCaminhoPdfAssinado(String caminhoPdfAssinado) {
        this.caminhoPdfAssinado = caminhoPdfAssinado;
    }

    public Certificado getCertificado() {
        return certificado;
    }

    public void setCertificado(Certificado certificado) {
        this.certificado = certificado;
    }

    public String getTsa() {
        return tsa;
    }

    public void setTsa(String tsa) {
        this.tsa = tsa;
    }

    public char[] getSenhaCertificado() {
        return senhaCertificado;
    }

    public void setSenhaCertificado(char[] senhaCertificado) {
        this.senhaCertificado = senhaCertificado;
    }

    public String getNomeAssinatura() {
        return nomeAssinatura;
    }

    public void setNomeAssinatura(String nomeAssinatura) {
        this.nomeAssinatura = nomeAssinatura;
    }

    public String getLocalAssinatura() {
        return localAssinatura;
    }

    public void setLocalAssinatura(String localAssinatura) {
        this.localAssinatura = localAssinatura;
    }

    public String getMotivoAssinatura() {
        return motivoAssinatura;
    }

    public void setMotivoAssinatura(String motivoAssinatura) {
        this.motivoAssinatura = motivoAssinatura;
    }

    public String getCaminhoImagem() {
        return caminhoImagem;
    }

    public void setCaminhoImagem(String caminhoImagem) {
        this.caminhoImagem = caminhoImagem;
    }

    public int getPosicaoX() {
        return posicaoX;
    }

    public void setPosicaoX(int posicaoX) {
        this.posicaoX = posicaoX;
    }

    public int getPosicaoY() {
        return posicaoY;
    }

    public void setPosicaoY(int posicaoY) {
        this.posicaoY = posicaoY;
    }

    public int getZoomImagem() {
        return zoomImagem;
    }

    public void setZoomImagem(int zoomImagem) {
        this.zoomImagem = zoomImagem;
    }

    public int getPagina() {
        return pagina;
    }

    public void setPagina(int pagina) {
        this.pagina = pagina;
    }
}
