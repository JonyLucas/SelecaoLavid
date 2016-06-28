public class No {

	private int conteudo;
	private No proximo;
	
	public No(int valor){
		conteudo = valor;
		proximo = null;
	}
	
	public No getProx(){
		
		return proximo;
	}
	
	public void setProx(No proxNo){
		this.proximo = proxNo;
		
	}
	
	public int getDado(){
		
		return this.conteudo;
	}
	
	public void setDado(int dado){
		
		this.conteudo = dado;
	}
}
