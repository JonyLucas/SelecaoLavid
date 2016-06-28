package projetolavid;

import javax.swing.JOptionPane;
import java.io.*;


public class MpegReceptor {	
	
	static int PMT;
	static Lista pids; /**Estrutura de dados da classe (Lista.java) para armazenar os PIDs lidos, para exibi-los apenas uma vez*/
	
	public static void main(String[] args) {
		
		try{
			String dir = JOptionPane.showInputDialog(null, "Digite o diretório do arquivo");
			FileReader file = new FileReader(dir); /**Localizacao do arquivo .ts*/
			BufferedReader arq = new BufferedReader(file);
	
			MpegReceptor.mpegTS(arq);
			
			arq.close();
			file.close();
			
		}catch(FileNotFoundException fnf){
			System.out.println("Arquivo nao encontrado");
		}catch(IOException ioe){
			ioe.printStackTrace();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void mpegTS(BufferedReader buff) throws IOException{
	/** Sync Byte do cabecalho da Transport Packet --- o Sync Byte possui um valor fixo de 0x47 H(71 D)*/
		pids = new Lista();
		int bytes;
		String data;
		do{
			bytes = buff.read();
			while(bytes != 71){ /**O laço loopa até encontrar o Sync Byte (71 em decimal / 0x47 em hexadecimal) que delimita o inicio do pacote do pacote TS*/
				bytes = buff.read();
			}
			if(bytes == 71){ /**Valor definido do Sync Byte (0x47)*/				
				data = "Sync_Byte: " + String.format("%X", bytes) + "\n"; /** Exibe em Hexadecimal*/
				transportPacket(buff, data);
				bytes = buff.read();
			}

		}while(buff.ready());
		
	}
	
	public static void transportPacket(BufferedReader buff, String syncByte) throws IOException{
		String data = "Transport Stream Packet Layer\n\n" + syncByte;
		int bytes = 0, bitMSB = 0; //MSB => Most Significant bit
		int adpFieldC, PID;
		boolean hasPID; /**Verifica se o PID ja foi exibido, caso afirmativo, não o exibe novamente*/
		
		/**Transport error indicator --- 1 bit*/
		bytes = buff.read();
		bitMSB = bytes;
		bitMSB >>>= 7;
		data += "Transport error indicator: " + bitMSB + "\n"; /** Ocupa o bit mais a esquerda (MSB) do byte lido, entao desloca-se o byte 7 casas a direita sem sinal (unsigned right shift)*/
		
		/**Payload unit start indicator --- 1 bit*/
		bitMSB = bytes;
		bitMSB >>>=6;
		bitMSB = (bitMSB & 1);
		data += "Playload unit start indicator: " + bitMSB + "\n"; /** Ocupa o segundo bit do byte lido, deve-se deslocar 6 casas a direita e utilizar o operando & com o numero 1 para manter apenas o bit mais a direita*/
		
		/**Transport Priority --- 1 bit*/
		bitMSB = bytes;
		bitMSB >>>=5;
		bitMSB = (bitMSB & 1);
		data += "Transport Priority: " + bitMSB + "\n"; /** Ocupa o terceiro bit do byte lido, deve-se deslocar 5 casas a direita e utilizar o operando & com o numero 1 para manter apenas o bit mais a direita*/
		
		/**PID --- 13 bits*/
		bitMSB = bytes;
		bitMSB = (bitMSB & 31); /** Faz-se a operacao de bitwise com o operando & com os 5 bits mais a esquerda, por isso utiliza-se o numero 31(0001 1111)*/
		bytes = buff.read();
		bitMSB <<= 8;
		PID = bitMSB = (bitMSB | bytes);
		data += "PID: " + bitMSB; /** PID Ocupa os 5 bits restantes mais o byte seguinte*/
		
		if(pids.hasElement(bitMSB)){
			hasPID = true;
		}
		else{
			hasPID = false;
			pids.insere(PID);
		}
		
		if(PID == 0){
			data += " - Program Association Table (PAT)\n";
		}else if(PID == 1){
			data += " - Conditional Acess Table (CAT)\n";
		}else if(PID == 2){
			data += " - Transport Stream Description Table (TSDT)\n";
		}else if(PID >= 3 && bitMSB <= 15){
			data += " - Reserved\n";
		}else if(PID >= 16 && bitMSB <= 8190){
			data += " - Other porpuses\n";
		}else{
			data += " - Null packet\n";
		}
		
		/**Transport Scrambling control --- 2 bits*/
		bytes = buff.read();
		bitMSB = bytes;
		bitMSB >>>=6;
		data += "Transport Scrambling control: " + bitMSB + "\n"; /** Ocupa dois bits do byte lido, entao desloca-se 6 bits para a direita*/
		
		/**Adaptation field control --- 2 bits*/
		bitMSB = bytes;
		bitMSB >>>= 4;
		adpFieldC = bitMSB = (bitMSB & 3);
		data += "Adaptation field control: " + bitMSB + "\n"; /**Ocupa dois bits do byte lido, desloca-se 4 bits para a direita e entao utiliza-se operacao &(and) com o numero 3 para considerar apenas os dois bits mais a direita*/
		
		if(adpFieldC == 0){
			data += "Reserved for future use by ISO/IEC\n";
		}else if(adpFieldC == 1){
			data += "No adaptation field, payload only\n";
		}else if(adpFieldC == 2){
			data += "Adaptation field only, no payload\n";
		}else{
			data += "Adaptation field followed by payload\n";
		}
		
		/**Continuity counter --- 4 bits*/
		bitMSB = bytes;
		bitMSB = (bitMSB & 15);
		data += "Continuity counter: " + bitMSB + "\n"; /**Ocupa os 4 bits restantes do byte, entao utiliza-se a operacao and para manter os 4 bits mais a direita*/
		
		if(adpFieldC == 2 || adpFieldC == 3){
			//data += adaptationField(buff);
		}
		
		/**Data byte --- 1 byte*/
		if(adpFieldC == 1 || adpFieldC == 3){
			bytes = buff.read();
			data += "Data byte: " + bytes + "\n";
			
			if(PID == 0){
				data += programAssociationSection(buff, PMT); /**Trecho com informações da tabela PAT*/
			}else if(PMT == PID){
				data += programMapSection(buff); /**Trecho com informações da tabela PMT*/
			}
		}
		
		if(PID != 0 && PID != PMT){ /**Exibir apenas as tabelas PAT e PMT*/
			return;
		}
		
		if(!hasPID){
			JOptionPane.showMessageDialog(null, data);
		}
	}
	
	public static String programAssociationSection(BufferedReader buff, Integer PMD) throws IOException{
		String assoSection = "\nProgram Association Section\n\n";
		int bitMSB = 0;
		int bytes;
		
		/**Table ID --- 1 byte ---> 0x00*/
		bytes = buff.read();
		assoSection += "Table ID: " + bytes + "\n";
		
		/**Section syntax indicator --- 1 bit*/
		bytes = buff.read();
		bitMSB = bytes;
		bitMSB >>>= 7;
		assoSection += "Section syntax indicator: " + bitMSB + "\n";
		
		/**'0' e reserved nao serao exibidos --- 3 bits*/
		/**Section Lenght --- 12 bits*/
		bitMSB = bytes;
		bitMSB = (bitMSB & 15);
		bitMSB <<= 8;
		bytes = buff.read();
		bitMSB = (bitMSB | bytes);
		assoSection += "Section Lenght: " + bitMSB + "\n";
		
		/**Transport Stream ID --- 2 bytes*/
		bytes = buff.read();
		bitMSB = bytes;
		bitMSB <<= 8;
		bytes = buff.read();
		bitMSB = (bitMSB | bytes);
		assoSection += "Transport Stream ID: " + bitMSB + "\n";
		
		/**Reserved --- 2 bits -> nao sera exibido*/
		/**Version number --- 5 bits*/
		bytes = buff.read();
		bitMSB >>>= 1;
		bitMSB = (bitMSB & 31);
		assoSection += "Version number: " + bitMSB + "\n";
		
		/**Current next indicator --- 1 bit*/
		bitMSB = bytes;
		bitMSB = (bitMSB & 1);
		assoSection += "Current next indicator: " + ((bitMSB == 1) ? "True" : "False") + "\n";
		
		/**Section number --- 1 byte*/
		bytes = buff.read();
		assoSection += "Section number: " + bytes + "\n";
		
		/**Last section number --- 1 byte*/
		bytes = buff.read();
		assoSection += "Last Section number: " + bytes + "\n";
		
		/**Program number --- 2 bytes*/
		bytes = buff.read();
		bitMSB = bytes;
		bitMSB <<= 8;
		bytes = buff.read();
		bitMSB = (bitMSB | bytes);
		assoSection += "Program Number: " + bitMSB + "\n";
		
		/**Reserved --- 3 bits --- Nao sera exibido*/
		/**Program Map PID || Program Network PID --- 13 bits*/
		bytes = buff.read();
		bitMSB = bytes;
		bitMSB = (bitMSB & 31);
		bitMSB <<= 8;
		bytes = buff.read();
		bitMSB = (bitMSB | bytes);
		if(bitMSB == 0){
			assoSection += "Program Network PID: " + bitMSB + "\n";
		}else{
			PMT = bitMSB;
			assoSection += "Program Map PID: " + bitMSB + "\n";
		}
		
		/**CRC 32 --- 32 bits -> 4 bytes*/
		bytes = buff.read();
		bitMSB = bytes;
		bitMSB <<= 8;
		bytes = buff.read();
		bitMSB = (bitMSB | bytes);
		bitMSB <<= 8;
		bytes = buff.read();
		bitMSB = (bitMSB | bytes);
		bitMSB <<= 8;
		bytes = buff.read();
		bitMSB = (bitMSB | bytes);
		assoSection += "CRC Section: " + String.format("%X\n", bitMSB);
		
		return assoSection;
		
	}
	
	public static String programMapSection(BufferedReader buff) throws IOException{
		String mapSection = "\nProgram Map Section\n\n";
		int bitMSB = 0;
		int bytes, strType;
		
		/**Table ID --- 1 byte*/
		bytes = buff.read();
		mapSection += "Table ID: " + bytes + "\n";
		
		/**Section syntax indicator --- 1 bit*/
		bytes = buff.read();
		bitMSB = bytes;
		bitMSB >>>= 7;
		mapSection += "Section syntax indicator: " + bitMSB + "\n";
		
		/**'0' e reserved nao serao exibidos --- 3 bits*/
		/**Section Lenght --- 12 bits*/
		bitMSB = bytes;
		bitMSB = (bitMSB & 15);
		bitMSB <<= 8;
		bytes = buff.read();
		bitMSB = (bitMSB | bytes);
		mapSection += "Section Lenght: " + bitMSB + "\n";
		
		/**Program Number --- 2 bytes*/
		bytes = buff.read();
		bitMSB = bytes;
		bitMSB <<= 8;
		bytes = buff.read();
		bitMSB = (bitMSB | bytes);
		mapSection += "Program number: " + bitMSB + "\n";
		
		/**Reserved --- 2 bits -> nao sera exibido*/
		/**Version number --- 5 bits*/
		bytes = buff.read();
		bitMSB >>>= 1;
		bitMSB = (bitMSB & 31);
		mapSection += "Version number: " + bitMSB + "\n";
		
		/**Current next indicator --- 1 bit*/
		bitMSB = bytes;
		bitMSB = (bitMSB & 1);
		mapSection += "Current next indicator: " + ((bitMSB == 1) ? "True" : "False") + "\n";
		
		/**Section number --- 1 byte*/
		bytes = buff.read();
		mapSection += "Section number: " + bytes + "\n";
		
		/**Last section number --- 1 byte*/
		bytes = buff.read();
		mapSection += "Last Section number: " + bytes + "\n";
		
		/**Reserved --- 3 bits --- Nao sera exibido*/
		/**PCR PID --- 13 bits*/
		bytes = buff.read();
		bitMSB = bytes;
		bitMSB = (bitMSB & 31);
		bitMSB <<= 8;
		bytes = buff.read();
		bitMSB = (bitMSB | bytes);
		mapSection += "PCR PID: " + bitMSB + "\n";
		
		/**Reserved --- 4 bits --- Nao sera exibido*/
		/**Program info lenght --- 12 bits*/
		bytes = buff.read();
		bitMSB = bytes;
		bitMSB = (bitMSB & 15);
		bitMSB <<= 8;
		bytes = buff.read();
		bitMSB = (bitMSB | bytes);
		mapSection += "Program info lenght: " + bitMSB + "\n";
		
		for(int i = 0; i < 2; i++){
			
			mapSection += "\n----------------------------------------------\nDescriptor Loop";
			
			/**Stream Type --- 1 byte*/
			strType = bytes = buff.read();
			mapSection += "Stream Type: " + bytes;
			
			if(strType == 0){
				mapSection += " - ITU-T | ISO/IEC Reserved";
			}else if(strType == 1){
				mapSection += " - ISO/IEC 11172 Video";
			}else if(strType == 2){
				mapSection += " - 13818-2 Video or 11172-2 constrained parameter video stream";
			}else if(strType == 3){
				mapSection += " - ISO/IEC 11172 Audio";
			}else if(strType == 4){
				mapSection += " - ISO/IEC 13818-3 Audio";
			}else if(strType == 5){
				mapSection += " - private sections";
			}else if(strType ==  27){
				mapSection += " - H.264 AVC Video Stream";
			}else if(strType == 15){
				mapSection += " - Audio with ADTS transport syntax";
			}
			
			/**Reserved --- 3 bits*/
			/**Elementary PID --- 13 bits*/
			bytes = buff.read();
			bitMSB = bytes;
			bitMSB = (bitMSB & 31);
			bitMSB <<= 8;
			bytes = buff.read();
			bitMSB = (bitMSB | bytes);
			mapSection += "\nElementary PID: " + bitMSB + "\n";
			
			/**Reserved --- 4 bits*/
			/**ES info lenght --- 12 bits*/
			bytes = buff.read();
			bitMSB = bytes;
			bitMSB = (bitMSB & 15);
			bitMSB <<= 8;
			bytes = buff.read();
			bitMSB = (bitMSB | bytes);
			mapSection += "ES info lenght: " + bitMSB + "\n";
		}
		
		/**CRC 32 --- 32 bits -> 4 bytes*/
		bytes = buff.read();
		bitMSB = bytes;
		bitMSB <<= 8;
		bytes = buff.read();
		bitMSB = (bitMSB | bytes);
		bitMSB <<= 8;
		bytes = buff.read();
		bitMSB = (bitMSB | bytes);
		bitMSB <<= 8;
		bytes = buff.read();
		bitMSB = (bitMSB | bytes);
		mapSection += "\nCRC Section: " + String.format("%X\n", bitMSB);
		
		return mapSection;
		
	}

}
