import javax.swing.JOptionPane;
import java.io.*;


public class MpegReceptorX {	
	
	static int PMT;
	
	public static void main(String[] args) {
		
		try{
			/**d://Programação//Atividade Lavid//video.ts*/
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
	
	static Lista pids; /**Estrutura de dados da classe (Lista.java) para armazenar os PIDs lidos, para exibi-los apenas uma vez*/
	
	public static void mpegTS(BufferedReader buff) throws IOException{
	/** Sync Byte do cabecalho da Transport Packet --- o Sync Byte possui um valor fixo de 0x47 H(71 D)*/
		pids = new Lista();
		int bytes;
		String data;
		do{
			bytes = buff.read();
			while(bytes != 71){ /**Depois de obter os dados do Service Information do pacote de transporte, os valores seguintes são "FF" Hexa (255 Dec)*/
				bytes = buff.read();
				System.out.println("Valor lido:" + bytes);
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
			for(int i = 0; i < 15; i++){
				buff.read();
			}
		}
		
		/**Continuity counter --- 4 bits*/
		bitMSB = bytes;
		bitMSB = (bitMSB & 15);
		data += "Continuity counter: " + bitMSB + "\n"; /**Ocupa os 4 bits restantes do byte, entao utiliza-se a operacao and para manter os 4 bits mais a direita*/
		
		if(adpFieldC == 2 || adpFieldC == 3){
			data += adaptationField(buff);
		}
		
		/**Data byte --- 1 byte*/
		if(adpFieldC == 1 || adpFieldC == 3){
			bytes = buff.read();
			data += "Data byte: " + bytes + "\n";
			
			if(PID == 0){
				data += programAssociationSection(buff, PMT);
			}else if(PMT == PID){
				data += programMapSection(buff);
			}
		}
		
		System.out.println("Proximo valor:" + buff.read());
		
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
	
	public static String videoDescriptor(BufferedReader buff) throws IOException{
		String vDescrip = "\nVideo Descriptor\n\n";
		int bytes, bitMSB, mpegF;
		
		/**Descriptor tag --- 1 byte*/
		bytes = buff.read();
		vDescrip += "Descriptor tag: " + bytes + "\n";
		
		/**Descriptor lenght --- 1 byte*/
		bytes = buff.read();
		vDescrip += "Descriptor lenght: " + bytes + "\n";
		
		/**Multiple frame rate tag --- 1 bit*/
		bytes = buff.read();
		bitMSB = bytes >>> 7;
		vDescrip += "Multiple frame rate tag: " + (bitMSB == 1 ? "True" : "False") + "\n";
		
		/**Frame rate code ---  4 bits*/
		bitMSB = bytes;
		bitMSB >>>= 3;
		bitMSB = (bitMSB & 15);
		vDescrip += "Frame rate code: " + bitMSB + "\n";
		
		/**Mpeg only flag --- 1 bit*/
		bitMSB = bytes;
		bitMSB >>>= 2;
		mpegF = bitMSB = (bitMSB & 1);
		vDescrip += "Mpeg only flag: " + bitMSB + "\n";
		
		/**Constrained_parameter_flag --- 1 bit*/
		bitMSB = bytes >>> 1;
		bitMSB = (bitMSB & 1);
		vDescrip += "Constrained parameter flag: " + bitMSB + "\n";
		
		/**Still picture flag --- 1 bit*/
		bitMSB = (bytes & 1);
		vDescrip += "Still picture flag: " + bitMSB + "\n";
		
		if(mpegF == 1){
			/**profile_and_level_indication --- 1 byte*/
			bytes = buff.read();
			vDescrip += "Profile and level indication: " + bytes + "\n";
			
			/**Chroma format --- 2 bits*/
			bytes = buff.read();
			bitMSB = bytes >>> 6;
			vDescrip += "Chroma format: " + bitMSB + "\n";
			
			/**Frame rate extension flag --- 1 bit*/
			bitMSB = bytes;
			bitMSB >>>= 5;
			bitMSB = (bitMSB & 1);
			vDescrip += "Frame rate extension flag: " + bitMSB + "\n";
			
			/**Reserved --- 5 bits*/
		
		}
		
		return vDescrip;
		
	}
	
	public static String audioDescriptor(BufferedReader buff) throws IOException{
		String aDescrip = "\nAudio Description\n";
		int bytes, bitMSB;
		
		/**Descriptor tag --- 1 byte*/
		bytes = buff.read();
		aDescrip += "Descriptor tag: " + bytes + "\n";
		
		/**Descriptor lenght --- 1 byte*/
		bytes = buff.read();
		aDescrip += "Descriptor lenght: " + bytes + "\n";
		
		/**Free format flag --- 1 bit*/
		bytes = buff.read();
		bitMSB = bytes >>>= 7;
		aDescrip += "Free format flag: " + bitMSB + "\n";
		
		/**ID --- 1 bit*/
		bitMSB = bytes;
		bitMSB >>>= 6;
		bitMSB &= 1;
		aDescrip += "ID: " + bitMSB + "\n";
		
		/**Layer --- 2 bits*/
		bitMSB = bytes >>> 4;
		bitMSB &= 3;
		aDescrip += "Layer: " + bitMSB + "\n";
		
		/**Variable rate audio indicator --- 1 bit*/
		bitMSB = bytes >>> 3;
		bitMSB &= 1;
		aDescrip += "Variable rate audio indacator: " + (bitMSB == 1 ? "True" : "False") + "\n";
		
		return aDescrip;
	}
	
	public static String adaptationField(BufferedReader buff) throws IOException{
		String adpField =  "\nAdaptation Field\n\n";
		int bytes, bitMSB, pcrFlag, oPcrFlag, splicingPF, privDataF, adpFieldExF;
		
		/**Adaptation Field Lenght --- 1 byte*/
		bytes = buff.read();
		adpField += "Adptation Field Lenght: " + bytes + "\n";
		if(bytes > 0){
			/**Discontinuity indicator --- 1 bit*/
			bytes = buff.read();
			bitMSB = bytes;
			bitMSB >>>= 7;
			adpField += "Discontinuity indicator: " + (bitMSB == 1 ? "True" : "False") + "\n";
			
			/**Random Acess Indicator --- 1 bit*/
			bitMSB = bytes;
			bitMSB >>>= 6;
			bitMSB = (bitMSB & 1);
			adpField += "Random Acess Indicator" + (bitMSB == 1 ? "True" : "False") + "\n";
			
			/**Elementary Stream Priority indicator --- 1 bit*/
			bitMSB = bytes;
			bitMSB >>>=5;
			bitMSB = (bitMSB & 1);
			adpField += "Elementary Stream Priority indicator: " + (bitMSB == 1 ? "True" : "False") + "\n";
			
			/**PCR flag --- 1 bit*/
			bitMSB = bytes;
			bitMSB >>>= 4;
			pcrFlag = bitMSB = (bitMSB & 1);
			adpField += "PCR flag: " + (bitMSB == 1 ? "True" : "False") + "\n";
			
			/**OPCR flag --- 1 bit*/
			bitMSB = bytes;
			bitMSB >>>= 3;
			oPcrFlag = bitMSB = (bitMSB & 1);
			adpField += "OPCR flag: " + (bitMSB == 1 ? "True" : "False") + "\n";
			
			/**splicing point flag --- 1 bit*/
			bitMSB = bytes;
			bitMSB >>>= 2;
			splicingPF = bitMSB = (bitMSB & 1);
			adpField += "Splicing point flag: " + (bitMSB == 1 ? "True" : "False") + "\n";
			
			/**Transport private data flag --- 1 bit*/
			bitMSB = bytes;
			bitMSB >>>= 1;
			privDataF = bitMSB = (bitMSB & 1);
			adpField += "Transport private data flag: " + (bitMSB == 1 ? "True" : "False") + "\n";
			
			/**Adaptation Field extension flag --- 1 bit*/
			bitMSB = bytes;
			adpFieldExF = bitMSB = (bitMSB & 1);
			adpField += "Adaptation Field extension flag: " + (bitMSB == 1 ? "True" : "False") + "\n";
			
			if(pcrFlag == 1){
				/**Program clock reference base --- 33 bits*/
				int aux;
				bitMSB = 0;
				for(int i = 0; i < 4; i++){
					bytes = buff.read();
					bitMSB = (bitMSB | bytes);
					bitMSB <<= 8;
				}
				bytes = buff.read();
				bitMSB <<=1;
				aux = bytes;
				aux >>>= 7;
				bitMSB = (bitMSB | aux);
				adpField += "Program clock reference base: " + bitMSB + "\n";
				
				/**Reserved --- 6 bits ---> Nao sera exibido*/
				/**Program clock reference base extension --- 9 bits*/
				bitMSB = bytes;
				bitMSB = (bitMSB & 1);
				bytes = buff.read();
				bitMSB <<=1;
				bitMSB = (bitMSB | bytes);
				adpField += "Program clock reference base extension: " + bitMSB + "\n";
								
			}
			
			if(oPcrFlag == 1){
				/**Original program clock reference base --- 33 bits*/
				int aux;
				bitMSB = 0;
				for(int i = 0; i < 4; i++){
					bytes = buff.read();
					bitMSB = (bitMSB | bytes);
					bitMSB <<= 8;
				}
				adpField += "Original Program clock reference base: " + bitMSB + "\n";
				
				/**Reserved --- 6 bits ---> Nao sera exibido*/
				/**Program clock reference --- 9 bits*/
				bitMSB = bytes;
				bitMSB = (bitMSB & 1);
				bytes = buff.read();
				bitMSB <<=1;
				bitMSB = (bitMSB | bytes);
				adpField += "Original Program clock reference: " + bitMSB + "\n";
			}
			
			if(splicingPF == 1){
				/**Splicing count down --- 1 byte*/
				bytes = buff.read();
				adpField += "Splicing countdown: " + bytes + "\n";
			}
			
			if(privDataF == 1){
				/**Transport private data lenght --- 1 byte*/
				bytes = buff.read();
				adpField += "Transport private data lenght: " + bytes + "\n";
				
				/**private data --- 1 byte*/
				bytes = buff.read();
				adpField += "Private data byte: " + bytes + "\n";
			}
			
			if(adpFieldExF == 1){
				int lwtF, pWiseRateF, sSpliceF;
				
				/**Adaptation Field Extension Lenght --- 1 byte*/
				bytes = buff.read();
				adpField += "Adaptation Field Extension Lenght: " + bytes + "\n";
				
				/**ltw flag --- 1 bit*/
				bytes = buff.read();
				bitMSB = bytes;
				lwtF = bitMSB >>>= 7;
				adpField += "lwt flag: " + (bitMSB == 1 ? "True" : "False") + "\n";
				
				/**Piecewise Rate flag --- 1 bit*/
				bitMSB = bytes;
				bitMSB >>>=6;
				pWiseRateF = bitMSB = (bitMSB & 1);
				adpField += "Piecewise rate flag: " + (bitMSB == 1 ? "True" : "False") + "\n";
				
				/**Seamless splice flag --- 1 bit*/
				bitMSB = bytes;
				bitMSB >>>=5;
				sSpliceF = bitMSB = (bitMSB & 1);
				adpField += "Seamless splice flag: " + (bitMSB == 1 ? "True" : "False") + "\n";
				/**Reserved --- 6 bits*/
				
				if(lwtF == 1){
					/**lwt valid flag --- 1 bit*/
					bytes = buff.read();
					bitMSB >>>= 7;
					adpField += "lwt valid flag: " + (bitMSB == 1 ? "True" : "False") + "\n";
					
					/**lwt offset --- 15 bits*/
					bitMSB = bytes;
					bitMSB = (bitMSB & 127);
					bitMSB <<= 8;
					bytes = buff.read();
					bitMSB = (bitMSB | bytes);
					adpField += "lwt offset: " + bitMSB + "\n";
				}
				
				if(pWiseRateF == 1){
					/**Reserved --- 2 bits*/
					/**Piecewise rate --- 22 bits*/
					bytes = buff.read();
					bitMSB = bytes;
					bitMSB = (bitMSB & 63);
					
					for(int i = 0; i < 2; i++){
						bitMSB <<= 8;
						bytes = buff.read();
						bitMSB = (bitMSB | bytes);
					}
					adpField += "Piecewise rate: " + bitMSB + "\n";
				}
				
				if(splicingPF == 1){
					/**Splice type --- 4 bits*/
					bytes = buff.read();
					bitMSB = bytes;
					bitMSB >>>= 4;
					adpField += "Splice type: " + bitMSB + "\n";
					
					/**DTS next AU --- 3 bits ---> nao sera exibido*/
					bitMSB = bytes;
					bitMSB >>>= 1;
					bitMSB = (bitMSB | 7);
					
					/**Os proximos 3 bytes nao serao exibidos*/
					for(int i = 0; i < 3; i++){
						bytes = buff.read();
					}
				}
				/**Reserved --- 1 byte ---> nao sera exibido*/
				bytes = buff.read();
			}
			
		}
		
		return adpField;
	}

}
