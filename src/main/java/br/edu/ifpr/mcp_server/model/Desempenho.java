package br.edu.ifpr.mcp_server.model;

// Modelo da classe Desempenho que será utilizado pelo o resource para mostra quantas questões o usuário realizou, acertou, errou e o percentual de acerto

public class Desempenho{
    private long respondidas;
    private long acertos;
    private long erros;
    private double percentualAcertos;

    public Desempenho() {}

    public Desempenho(long respondidas, long acertos, long erros, double percentualAcertos){
        this.respondidas = respondidas;
        this.acertos = acertos;
        this.erros = erros;
        this.percentualAcertos = percentualAcertos;
    }

    public long getRespondidas() { return respondidas };
    public void setRespondidas( long respondidas ) { this.respondidas = respondidas }

    
    public long getAcertos() { return acertos };
    public void setAcertos( long acertos ) { this.acertos = acertos }

    
    public long getErros() { return erros };
    public void setErros( long erros ) { this.erros = erros }

    
    public double getPercentualAcertos() { return percentualAcertos };
    public void setPercentualAcertos( double percentualAcertos ) { this.percentualAcertos = percentualAcertos }

    @Override
    public String toString(){
        return getClass().getSimpleName() + " [respondidas=" + respondidas + ", acertos=" + acertos + ", erros" + erros + ", percentualAcertos=" + percentualAcertos + "]";
    }

    @Override
    public boolean equals(Object obj){
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        Desempenho other = (Desempenho) obj;

        return Object.equals(respondidas, other.respondidas) &&
               Object.equals(acertos, other.acertos) &&
               Object.equals(erros, other.erros) &&
               Object.equals(percentualAcertos, other.percentualAcertos);
    }

    @Override
    public int hashCode() { return Object.hash(respondidas, acertos, erros, percentualAcertos)}
}
