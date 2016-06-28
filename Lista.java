public class Lista {

	private No cabeca;
	private int tamanho;
	
	public Lista(){
		cabeca = null;
		tamanho = 0;
	}
	
	public int tamanho(){
		
		return this.tamanho;
	}
	
	public boolean hasElement(int valor){
		
		No searchNode;
		
		if(this.cabeca == null){
			return false;
		}else{
			searchNode = cabeca;
		}
		
		int i;
		
		for(i = 1; i <= this.tamanho; i++){
			
			if(searchNode.getDado() == valor){
				return true;
			}
			
			searchNode = searchNode.getProx();
		}
		
		return false;
	}
	
	public void insere(int valor){
		
		if(this.tamanho == 0){
			this.cabeca = new No(valor);
			this.tamanho++;
			return;
		}
		
		No novoNo = new No(valor);
		No aux = cabeca;
		
		while(aux.getProx() != null){
			aux = aux.getProx();
		}
		
		aux.setProx(novoNo);
		this.tamanho++;
		
	}
}
