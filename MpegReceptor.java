package projetolavid;

import java.util.Scanner;

import javax.swing.JOptionPane;

import java.io.*;

public class MpegReceptor {

	public static void main(String[] args) {
		
		try{
			FileReader file = new FileReader("d:\\Programação\\Atividade Lavid\\video.ts"); /**Localizacao do arquivo .ts*/
			BufferedReader arq = new BufferedReader(file);
			
			mpegTS(arq);
			//programMapSection(arq);
			
			arq.close();
			file.close();
			
		}catch(FileNotFoundException fnf){
			System.out.println("Arquivo nao encontrado");
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
		

	}
	
	public static void mpegTS(BufferedReader buff) throws IOException{
	/** Sync Byte do cabecalho da Transport Packet --- o Sync Byte possui um valor fixo de 0x47 H(71 D)*/
		int bytes;
		String data;
		do{
			bytes = buff.read();
			while(bytes == 255){
				bytes = buff.read();
			}
			if(bytes == 71){
				data = "Sync_Byte: " + String.format("%X", bytes) + "\n"; /** Exibe em Hexadecimal*/
				transportPacket(buff, data);
			}
		}while(buff.ready());
		
	}
	
	public static void transportPacket(BufferedReader buff, String syncByte) throws IOException{
		String data = "Transport Stream Packet Layer\n\n" + syncByte;
		int bytes = 0;
		int bitMSB = 0;
		int adpFieldC;
		
		/**Sync Byte --- 1 byte*/
		//bytes = buff.read();
		//data += "Sync_Byte: " + String.format("%X", bytes) + "\n"; /** Exibe em Hexadecimal*/
		
		/**Transport error indicator --- 1 bit*/
		bytes = buff.read();
		bitMSB = bytes;
		bitMSB >>>= 7;
		data += "Transport error indicator: " + bitMSB + "\n"; /** Ocupa o bit mais a esquerda (MSB) do byte lido, entao desloca-se o byte 7 casas a direita sem sinal (unsigned right shift)*/
		
		/**Playload unit start indicator --- 1 bit*/
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
		bitMSB = (bitMSB | bytes);
		data += "PID: " + bitMSB; /** PID Ocupa os 5 bits restantes mais o byte seguinte*/
		
		if(bitMSB == 0){
			data += " - Program Association Table (PAT)\n";
		}else if(bitMSB == 1){
			data += " - Conditional Acess Table (CAT)\n";
		}else if(bitMSB == 2){
			data += " - Transport Stream Description Table (TSDT)\n";
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
		
		/**Continuity counter --- 4 bits*/
		bitMSB = bytes;
		bitMSB = (bitMSB & 15);
		data += "Continuity counter: " + bitMSB + "\n"; /**Ocupa os 4 bits restantes do byte, entao utiliza-se a operacao and para manter os 4 bits mais a direita*/
		
		if(adpFieldC == 2 || adpFieldC == 3){
			//buff.read();
		}
		
		/**Data byte??? --- 1 byte*/
		if(adpFieldC == 1 || adpFieldC == 3){
			
			buff.read();
			/*for(int i = 0 ; i < n ; i++){
				
			}*/
		}
	
		JOptionPane.showMessageDialog(null, data);
		programMapSection(buff);
	}
	
	public static void programMapSection(BufferedReader buff) throws IOException{
		String mapSection = "Program Map Section\n\n";
		int bitMSB = 0;
		int bytes;
		
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
		mapSection += "Current next indicator: " + bitMSB + "\n";
		
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
		mapSection += "CRC Section: " + bitMSB + "\n";
		
		JOptionPane.showMessageDialog(null, mapSection);
		
	}

}
