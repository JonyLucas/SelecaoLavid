# SelecaoLavid
Código fonte do projeto: MpegReceptor.java
Descrição: Projeto que exibe informações em janela dos metadados obtidos a partir dos pacotes de Stream do arquivo video.ts.
Desenvolvido por: João Lucas Fabião Amorim

OBS: Utilizei o arquivo "MPEG-2 TS packet analyser.exe" como base para entender o fluxo do arquivo .ts e verificar se os valores apresentados no programa estavam certos.

A classe MpegRecptor possui 5 metodos:
  public static void main(String args[]);
  public static void mpegTS(BufferedReader buff) throws IOException;
  public static String programAssociationSection(BufferedReader buff) throws IOException;
  public static String programMapSection(BufferedReader buff) throws IOException;
  public static String adaptationField(BufferedReader buff) throws IOException;

Main:
  O método main possui um FileReader e BufferedReader para a leitura do arquivo .ts.
  Na main trata-se as exceções do pacote java.io;
  Na main fazemos a chamada do método mpegTS passando como parâmetro o BufferedReader.
  
mpegTS:
  O método mpegTS começa a leitura dos pacotes de stream do arquivo em um laço do-while, ele lê o primeiro byte (o Sync Byte do ST packet) e chama a função transportPacket.
  
transportPacket:
  
