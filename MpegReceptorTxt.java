package projetolavid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

/**Esta classe � essencialmente a mesma do MpegReceptor, s� que � apenas para escrita em arquivo .txt e n�o possui nenhuma estrutura de dados*/

public class MpegReceptorTxt {
	
	public static void mpegTS(BufferedReader buff, PrintWriter pw) throws IOException{
	/** Sync Byte do cabecalho da Transport Packet --- o Sync Byte possui um valor fixo de 0x47 H(71 D)*/
		int bytes;
		String data;
		do{
			bytes = buff.read();
			while(bytes == 255){ /**Depois de obter os dados do Service Information do pacote de transporte, os valores seguintes s�o "FF" Hexa (255 Dec)*/
				bytes = buff.read();
			}
			if(bytes == 71){ /**Valor definido do Sync Byte (0x47)*/
				
				data = "Sync_Byte: " + String.format("%X", bytes) + "\n"; /** Exibe em Hexadecimal*/
				transportPacket(buff, pw, data);
			}

		}while(buff.ready());
		
	}
	
	public static void transportPacket(BufferedReader buff, PrintWriter pw, String syncByte) throws IOException{
		pw.println("Transport Stream Packet Layer\n\n" + syncByte);
		pw.println(); /**Espa�amento*/
		int bytes = 0, bitMSB = 0; //MSB => Most Significant bit
		int adpFieldC, PID;
		
		/**Transport error indicator --- 1 bit*/
		bytes = buff.read();
		bitMSB = bytes;
		bitMSB >>>= 7;
		pw.println("Transport error indicator: " + bitMSB + "\n"); /** Ocupa o bit mais a esquerda (MSB) do byte lido, entao desloca-se o byte 7 casas a direita sem sinal (unsigned right shift)*/
		
		/**Payload unit start indicator --- 1 bit*/
		bitMSB = bytes;
		bitMSB >>>=6;
		bitMSB = (bitMSB & 1);
		pw.println("Playload unit start indicator: " + bitMSB + "\n"); /** Ocupa o segundo bit do byte lido, deve-se deslocar 6 casas a direita e utilizar o operando & com o numero 1 para manter apenas o bit mais a direita*/
		
		/**Transport Priority --- 1 bit*/
		bitMSB = bytes;
		bitMSB >>>=5;
		bitMSB = (bitMSB & 1);
		pw.println("Transport Priority: " + bitMSB + "\n"); /** Ocupa o terceiro bit do byte lido, deve-se deslocar 5 casas a direita e utilizar o operando & com o numero 1 para manter apenas o bit mais a direita*/
		
		/**PID --- 13 bits*/
		bitMSB = bytes;
		bitMSB = (bitMSB & 31); /** Faz-se a operacao de bitwise com o operando & com os 5 bits mais a esquerda, por isso utiliza-se o numero 31(0001 1111)*/
		bytes = buff.read();
		bitMSB <<= 8;
		PID = bitMSB = (bitMSB | bytes);
		pw.print("PID: " + bitMSB); /** PID Ocupa os 5 bits restantes mais o byte seguinte*/
		
		if(PID == 0){
			pw.println(" - Program Association Table (PAT)\n");
		}else if(PID == 1){
			pw.println(" - Conditional Acess Table (CAT)\n");
		}else if(PID == 2){
			pw.println(" - Transport Stream Description Table (TSDT)\n");
		}else if(PID >= 3 && bitMSB <= 15){
			pw.println(" - Reserved\n");
		}else if(PID >= 16 && bitMSB <= 8190){
			pw.println(" - Other porpuses\n");
		}else{
			pw.println(" - Null packet\n");
		}
		
		/**Transport Scrambling control --- 2 bits*/
		bytes = buff.read();
		bitMSB = bytes;
		bitMSB >>>=6;
		pw.println("Transport Scrambling control: " + bitMSB + "\n"); /** Ocupa dois bits do byte lido, entao desloca-se 6 bits para a direita*/
		
		/**Adaptation field control --- 2 bits*/
		bitMSB = bytes;
		bitMSB >>>= 4;
		adpFieldC = bitMSB = (bitMSB & 3);
		pw.println("Adaptation field control: " + bitMSB + "\n"); /**Ocupa dois bits do byte lido, desloca-se 4 bits para a direita e entao utiliza-se operacao &(and) com o numero 3 para considerar apenas os dois bits mais a direita*/
		
		if(adpFieldC == 0){
			pw.println("Reserved for future use by ISO/IEC\n");
		}else if(adpFieldC == 1){
			pw.println("No adaptation field, payload only\n");
		}else if(adpFieldC == 2){
			pw.println("Adaptation field only, no payload\n");
		}else{
			pw.println("Adaptation field followed by payload\n");
		}
		
		/**Continuity counter --- 4 bits*/
		bitMSB = bytes;
		bitMSB = (bitMSB & 15);
		pw.println("Continuity counter: " + bitMSB + "\n"); /**Ocupa os 4 bits restantes do byte, entao utiliza-se a operacao and para manter os 4 bits mais a direita*/
		
		if(adpFieldC == 2 || adpFieldC == 3){
			adaptationField(buff, pw);
		}
		
		/**Data byte --- 1 byte*/
		if(adpFieldC == 1 || adpFieldC == 3){
			bytes = buff.read();
			pw.println("Data byte: " + bytes + "\n");
			
			if(PID == 0){
				programAssociationSection(buff, pw);
			}else if(PID >= 16 && PID <= 8190){
				programMapSection(buff, pw);
			}
		}
		
		pw.println("-----------------------------------------------------");
		pw.println();
	}
	
	public static void programAssociationSection(BufferedReader buff, PrintWriter pw) throws IOException{
		pw.println("\nProgram Association Section\n\n");
		pw.println(); /**Espa�amento*/
		int bitMSB = 0;
		int bytes;
		
		/**Table ID --- 1 byte ---> 0x00*/
		bytes = buff.read();
		pw.println("Table ID: " + bytes + "\n");
		
		/**Section syntax indicator --- 1 bit*/
		bytes = buff.read();
		bitMSB = bytes;
		bitMSB >>>= 7;
		pw.println("Section syntax indicator: " + bitMSB + "\n");
		
		/**'0' e reserved nao serao exibidos --- 3 bits*/
		/**Section Lenght --- 12 bits*/
		bitMSB = bytes;
		bitMSB = (bitMSB & 15);
		bitMSB <<= 8;
		bytes = buff.read();
		bitMSB = (bitMSB | bytes);
		pw.println("Section Lenght: " + bitMSB + "\n");
		
		/**Transport Stream ID --- 2 bytes*/
		bytes = buff.read();
		bitMSB = bytes;
		bitMSB <<= 8;
		bytes = buff.read();
		bitMSB = (bitMSB | bytes);
		pw.println("Transport Stream ID: " + bitMSB + "\n");
		
		/**Reserved --- 2 bits -> nao sera exibido*/
		/**Version number --- 5 bits*/
		bytes = buff.read();
		bitMSB >>>= 1;
		bitMSB = (bitMSB & 31);
		pw.println("Version number: " + bitMSB + "\n");
		
		/**Current next indicator --- 1 bit*/
		bitMSB = bytes;
		bitMSB = (bitMSB & 1);
		pw.println("Current next indicator: " + ((bitMSB == 1) ? "True" : "False") + "\n");
		
		/**Section number --- 1 byte*/
		bytes = buff.read();
		pw.println("Section number: " + bytes + "\n");
		
		/**Last section number --- 1 byte*/
		bytes = buff.read();
		pw.println("Last Section number: " + bytes + "\n");
		
		/**Program number --- 2 bytes*/
		bytes = buff.read();
		bitMSB = bytes;
		bitMSB <<= 8;
		bytes = buff.read();
		bitMSB = (bitMSB | bytes);
		pw.println("Program Number: " + bitMSB + "\n");
		
		/**Reserved --- 3 bits --- Nao sera exibido*/
		/**Program Map PID || Program Network PID --- 13 bits*/
		bytes = buff.read();
		bitMSB = bytes;
		bitMSB = (bitMSB & 31);
		bitMSB <<= 8;
		bytes = buff.read();
		bitMSB = (bitMSB | bytes);
		if(bitMSB == 0){
			pw.println("Program Network PID: " + bitMSB + "\n");
		}else{
			pw.println("Program Map PID: " + bitMSB + "\n");
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
		pw.println("CRC Section: " + String.format("%X\n", bitMSB));
		
	}
	
	public static void programMapSection(BufferedReader buff, PrintWriter pw) throws IOException{
		pw.println("\nProgram Map Section\n\n");
		pw.println();
		int bitMSB = 0;
		int bytes;
		
		/**Table ID --- 1 byte*/
		bytes = buff.read();
		pw.println("Table ID: " + bytes + "\n");
		
		/**Section syntax indicator --- 1 bit*/
		bytes = buff.read();
		bitMSB = bytes;
		bitMSB >>>= 7;
		pw.println("Section syntax indicator: " + bitMSB + "\n");
		
		/**'0' e reserved nao serao exibidos --- 3 bits*/
		/**Section Lenght --- 12 bits*/
		bitMSB = bytes;
		bitMSB = (bitMSB & 15);
		bitMSB <<= 8;
		bytes = buff.read();
		bitMSB = (bitMSB | bytes);
		pw.println("Section Lenght: " + bitMSB + "\n");
		
		/**Program Number --- 2 bytes*/
		bytes = buff.read();
		bitMSB = bytes;
		bitMSB <<= 8;
		bytes = buff.read();
		bitMSB = (bitMSB | bytes);
		pw.println("Program number: " + bitMSB + "\n");
		
		/**Reserved --- 2 bits -> nao sera exibido*/
		/**Version number --- 5 bits*/
		bytes = buff.read();
		bitMSB >>>= 1;
		bitMSB = (bitMSB & 31);
		pw.println("Version number: " + bitMSB + "\n");
		
		/**Current next indicator --- 1 bit*/
		bitMSB = bytes;
		bitMSB = (bitMSB & 1);
		pw.println("Current next indicator: " + ((bitMSB == 1) ? "True" : "False") + "\n");
		
		/**Section number --- 1 byte*/
		bytes = buff.read();
		pw.println("Section number: " + bytes + "\n");
		
		/**Last section number --- 1 byte*/
		bytes = buff.read();
		pw.println("Last Section number: " + bytes + "\n");
		
		/**Reserved --- 3 bits --- Nao sera exibido*/
		/**PCR PID --- 13 bits*/
		bytes = buff.read();
		bitMSB = bytes;
		bitMSB = (bitMSB & 31);
		bitMSB <<= 8;
		bytes = buff.read();
		bitMSB = (bitMSB | bytes);
		pw.println("PCR PID: " + bitMSB + "\n");
		
		/**Reserved --- 4 bits --- Nao sera exibido*/
		/**Program info lenght --- 12 bits*/
		bytes = buff.read();
		bitMSB = bytes;
		bitMSB = (bitMSB & 15);
		bitMSB <<= 8;
		bytes = buff.read();
		bitMSB = (bitMSB | bytes);
		pw.println("Program info lenght: " + bitMSB + "\n");
		
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
		pw.println("CRC Section: " + String.format("%X\n", bitMSB));
		
	}
	
	
	public static void adaptationField(BufferedReader buff, PrintWriter pw) throws IOException{
		pw.println("\nAdaptation Field\n\n");
		pw.println();
		int bytes, bitMSB, pcrFlag, oPcrFlag, splicingPF, privDataF, adpFieldExF;
		
		/**Adaptation Field Lenght --- 1 byte*/
		bytes = buff.read();
		pw.println("Adptation Field Lenght: " + bytes + "\n");
		if(bytes > 0){
			/**Discontinuity indicator --- 1 bit*/
			bytes = buff.read();
			bitMSB = bytes;
			bitMSB >>>= 7;
			pw.println("Discontinuity indicator: " + (bitMSB == 1 ? "True" : "False") + "\n");
			
			/**Random Acess Indicator --- 1 bit*/
			bitMSB = bytes;
			bitMSB >>>= 6;
			bitMSB = (bitMSB & 1);
			pw.println("Random Acess Indicator" + (bitMSB == 1 ? "True" : "False") + "\n");
			
			/**Elementary Stream Priority indicator --- 1 bit*/
			bitMSB = bytes;
			bitMSB >>>=5;
			bitMSB = (bitMSB & 1);
			pw.println("Elementary Stream Priority indicator: " + (bitMSB == 1 ? "True" : "False") + "\n");
			
			/**PCR flag --- 1 bit*/
			bitMSB = bytes;
			bitMSB >>>= 4;
			pcrFlag = bitMSB = (bitMSB & 1);
			pw.println("PCR flag: " + (bitMSB == 1 ? "True" : "False") + "\n");
			
			/**OPCR flag --- 1 bit*/
			bitMSB = bytes;
			bitMSB >>>= 3;
			oPcrFlag = bitMSB = (bitMSB & 1);
			pw.println("OPCR flag: " + (bitMSB == 1 ? "True" : "False") + "\n");
			
			/**splicing point flag --- 1 bit*/
			bitMSB = bytes;
			bitMSB >>>= 2;
			splicingPF = bitMSB = (bitMSB & 1);
			pw.println("Splicing point flag: " + (bitMSB == 1 ? "True" : "False") + "\n");
			
			/**Transport private data flag --- 1 bit*/
			bitMSB = bytes;
			bitMSB >>>= 1;
			privDataF = bitMSB = (bitMSB & 1);
			pw.println("Transport private data flag: " + (bitMSB == 1 ? "True" : "False") + "\n");
			
			/**Adaptation Field extension flag --- 1 bit*/
			bitMSB = bytes;
			adpFieldExF = bitMSB = (bitMSB & 1);
			pw.println("Adaptation Field extension flag: " + (bitMSB == 1 ? "True" : "False") + "\n");
			
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
				pw.println("Program clock reference base: " + bitMSB + "\n");
				
				/**Reserved --- 6 bits ---> Nao sera exibido*/
				/**Program clock reference base extension --- 9 bits*/
				bitMSB = bytes;
				bitMSB = (bitMSB & 1);
				bytes = buff.read();
				bitMSB <<=1;
				bitMSB = (bitMSB | bytes);
				pw.println("Program clock reference base extension: " + bitMSB + "\n");
								
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
				pw.println("Original Program clock reference base: " + bitMSB + "\n");
				
				/**Reserved --- 6 bits ---> Nao sera exibido*/
				/**Program clock reference --- 9 bits*/
				bitMSB = bytes;
				bitMSB = (bitMSB & 1);
				bytes = buff.read();
				bitMSB <<=1;
				bitMSB = (bitMSB | bytes);
				pw.println("Original Program clock reference: " + bitMSB + "\n");
			}
			
			if(splicingPF == 1){
				/**Splicing count down --- 1 byte*/
				bytes = buff.read();
				pw.println("Splicing countdown: " + bytes + "\n");
			}
			
			if(privDataF == 1){
				/**Transport private data lenght --- 1 byte*/
				bytes = buff.read();
				pw.println("Transport private data lenght: " + bytes + "\n");
				
				/**private data --- 1 byte*/
				bytes = buff.read();
				pw.println("Private data byte: " + bytes + "\n");
			}
			
			if(adpFieldExF == 1){
				int lwtF, pWiseRateF, sSpliceF;
				
				/**Adaptation Field Extension Lenght --- 1 byte*/
				bytes = buff.read();
				pw.println("Adaptation Field Extension Lenght: " + bytes + "\n");
				
				/**ltw flag --- 1 bit*/
				bytes = buff.read();
				bitMSB = bytes;
				lwtF = bitMSB >>>= 7;
				pw.println("lwt flag: " + (bitMSB == 1 ? "True" : "False") + "\n");
				
				/**Piecewise Rate flag --- 1 bit*/
				bitMSB = bytes;
				bitMSB >>>=6;
				pWiseRateF = bitMSB = (bitMSB & 1);
				pw.println("Piecewise rate flag: " + (bitMSB == 1 ? "True" : "False") + "\n");
				
				/**Seamless splice flag --- 1 bit*/
				bitMSB = bytes;
				bitMSB >>>=5;
				sSpliceF = bitMSB = (bitMSB & 1);
				pw.println("Seamless splice flag: " + (bitMSB == 1 ? "True" : "False") + "\n");
				/**Reserved --- 6 bits*/
				
				if(lwtF == 1){
					/**lwt valid flag --- 1 bit*/
					bytes = buff.read();
					bitMSB >>>= 7;
					pw.println("lwt valid flag: " + (bitMSB == 1 ? "True" : "False") + "\n");
					
					/**lwt offset --- 15 bits*/
					bitMSB = bytes;
					bitMSB = (bitMSB & 127);
					bitMSB <<= 8;
					bytes = buff.read();
					bitMSB = (bitMSB | bytes);
					pw.println("lwt offset: " + bitMSB + "\n");
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
					pw.println("Piecewise rate: " + bitMSB + "\n");
				}
				
				if(splicingPF == 1){
					/**Splice type --- 4 bits*/
					bytes = buff.read();
					bitMSB = bytes;
					bitMSB >>>= 4;
						pw.println("Splice type: " + bitMSB + "\n");
					
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
	}

}