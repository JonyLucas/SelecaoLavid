# SelecaoLavid
Código fonte do projeto: MpegReceptor.java

Descrição: Projeto que exibe informações em janela dos metadados obtidos a partir dos pacotes de Stream do arquivo video.ts (Essa é a versão final do projeto, ela exibe apenas as tabelas PAT e PMT, há uma versão mais avançada no branche (jonyLucas-patch-2).

Desenvolvido por: João Lucas Fabião Amorim

OBS: Utilizei o arquivo "MPEG-2 TS packet analyser.exe" como base para entender o fluxo do arquivo .ts e verificar se os valores apresentados no programa estavam certos.


A classe MpegRecptor possui 5 metodos:

  public static void main(String args[]);
  
  public static void mpegTS(BufferedReader buff) throws IOException;
  
  public static String programAssociationSection(BufferedReader buff) throws IOException;
  
  public static String programMapSection(BufferedReader buff) throws IOException;
  
  public static String adaptationField(BufferedReader buff) throws IOException; (Não está presente na versão final)
  

Main:

  O método main possui um FileReader e BufferedReader para a leitura do arquivo .ts;
  
  Na main trata-se as exceções do pacote java.io;
  
  Na main fazemos a chamada do método mpegTS passando como parâmetro o BufferedReader;
  
mpegTS:

  O método mpegTS começa a leitura dos pacotes de stream do arquivo em um laço do-while, que loopa até encontrar o Sync Byte de um pacote de transporte, ele lê o primeiro byte (o Sync Byte do ST packet) do pacote e a partir dele chama-se a função transportPacket.
  
transportPacket:
  
  O método transportPacket exibe as informações do cabeçalho do pacote, como PID, se possui adaptation field, entre outras informações, nesse método verificamos o PID, caso seja igual a 0 então ele será pacote que carrega a tabela PAT - Program Association Table - (Valor do PID da tabela PAT foi definido como zero, por padrão), a partir disso, chamamos a função programAssociationSection() para exibir as informações da tabela PAT, dentre essas informações, obtemos o PID para a tabela PMT - Program Map Table -, que tem informações sobre as Elementary Streams (ES) e os dados carregados no pacote.
  
  O programa então pecorre o arquivo.ts, até encontrar a tabela PMT, e assim que encontra-la, chamará a função programMapSection(), em que será exibido informações sobre a tabela PMT.
  
  Depois de obter as informações das tabelas, o programa exibe em janela as informações adquiridas.
