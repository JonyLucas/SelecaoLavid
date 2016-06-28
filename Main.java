package projetolavid;

import javax.swing.JOptionPane;
import java.io.*;

public class Main {

	public static void main(String[] args) {
		
		try{
			/**d://Programação//Atividade Lavid//video.ts*/
			String dir = JOptionPane.showInputDialog(null, "Digite o diretório do arquivo");
			FileReader file = new FileReader(dir); /**Localizacao do arquivo .ts*/
			BufferedReader arq = new BufferedReader(file);
			
			FileWriter arqTxt; /**Utilizado para a escrita em Arquivo*/
			PrintWriter pw;
			
			/**Dá ao usuário a opção de exibir em arquivo ou em janela*/
			int op = Integer.parseInt(JOptionPane.showInputDialog(null, "Informe como deseja exibir os pacotes: \n(1) -  Exibição em janela \n(2) - Exibição em arquivo .txt (Default)"));
			
			/**As classes MpegReceptor e MpegReceptorTxt são essencialmente as mesmas
			***A primeira possui uma estrutura de dados da classe Lista e é para exibição em janela, cada PID é exibida apenas uma vez
			***A segunda para escrita em arquivo .txt e cada PID pode ser exibida mais de uma vez
			**/
			
			if(op == 1){
				arqTxt = null;
				pw = null;
				MpegReceptor.mpegTS(arq);
			}else{
				dir = JOptionPane.showInputDialog(null, "Informe o nome e o diretório do arquivo: ");
				arqTxt = new FileWriter(dir);
				pw = new PrintWriter(arqTxt);
				MpegReceptorTxt.mpegTS(arq, pw);
			}
			
			arqTxt.close();
			arq.close();
			file.close();
			
		}catch(FileNotFoundException fnf){
			System.out.println("Arquivo nao encontrado");
		}catch(IOException ioe){
			ioe.printStackTrace();
		}catch(Exception e){
			System.out.println("Erro!");
		}
		

	}

}
