import java.io.*;
import java.net.*;
import java.util.*;

public class Servidor {
    
    // Delimitador usado para separar as informações enviadas/recebidas
    private static String delimitador = ">#<";
    
    // Lista de Nomes dos Clientes ativos
    static LinkedList<String> nomesList = new LinkedList<String>();
    
    // Lista das informações de conexão dos Clientes ativos
    static LinkedList<SocketAddress> socketAddressList = new LinkedList<SocketAddress>();
    
    public static void main(String[] args) throws SocketException, IOException {
        DatagramSocket s = new DatagramSocket(4545);
        System.out.println("Servidor online.......");
        
        // Chama o método para criação do arquivo de Log
        criarLog();
        
        while(true) {
            DatagramPacket recebe = new DatagramPacket(new byte[1024], 1024);
            s.receive(recebe);
            
            String res = new String(recebe.getData());

            // Splita a mensagem recebida
            String[] msgSplit = res.split(delimitador);
            
            // Verifica o tipo de mensagem recebida, a partir da primeira informação splitada
            switch (msgSplit[0]) {
                case "VERIFICAR_NOME":
                    // Chama o método para validação do nome que o Cliente escolheu
                    verificaNome(s, recebe, msgSplit[1]);
                    break;
                case "NOME":
                    // Adicina o Cliente na lista de Clientes ativos
                    addClient(recebe, msgSplit[1]);
                    
                    // Envia para todos, (menos para ele mesmo), que o novo Cliente entro no chat
                    sendToAll(s, recebe, "(Público) " + msgSplit[1] + " entrou no chat!", msgSplit[1]);
                    
                    // Envia para todos a lista de nomes dos Clientes ativos, para inserção no Box
                    atualizarListaClientes(s, recebe, nomesList.toString(), msgSplit[1]);
                    break;
                case "CONVERSA":
                    // Se a segunda parte da mensagem for para "Todos", chama o sendToAll
                    // Se não, chama o sendToOne
                    if (msgSplit[2].equals("Todos")) {
                        sendToAll(s, recebe, "(Público) " + msgSplit[1] + " disse: " + msgSplit[3], msgSplit[1]);
                    }else {
                        sendToOne(s, recebe, "(Privado) " + msgSplit[1] + " disse a você: " + msgSplit[3], msgSplit[1], msgSplit[2]);
                    }
                    break;
                case "SAIR":
                    // Remove o Cliente da lista de Clientes ativos
                    removeClient(recebe, msgSplit[1]);
                    
                    // Envia para todos, (menos para ele mesmo), que o Cliente saiu do chat
                    sendToAll(s, recebe, "(Público) " + msgSplit[1] + " saiu do chat!", msgSplit[1]);
                    
                    // Envia para todos a lista de nomes dos Clientes ativos, para inserção no Box
                    atualizarListaClientes(s, recebe, nomesList.toString(), msgSplit[1]);
                    break;
                default:
                    break;
            }
        }
    }
    
    public static void addClient(DatagramPacket recebe, String nome) {
        nomesList.add(nome);
        socketAddressList.add(recebe.getSocketAddress());
    }
    
    public static void removeClient(DatagramPacket recebe, String nome) {
        nomesList.remove(nome);
        socketAddressList.remove(recebe.getSocketAddress());
    }
    
    public static void sendToAll(DatagramSocket s, DatagramPacket recebe, String resposta, String nome) throws IOException {
        // Insere no Log o que foi dito
        atualizarLog(recebe, resposta, nome);
        
        for(SocketAddress str: socketAddressList) {
            // A partir do For, verifica cada elemento da lista e envia a resposta
            // Porém valida antes para não devolver a resposta para o Cliente que a enviou
            if (!str.equals(recebe.getSocketAddress())) {
                // Splita a entrada da lista, pois um socketAddress tem o padrão "/IP:Porta"
                // Ex: /127.0.0.1:57473
                // Retirando também a "/" no começo do IP
                String[] strSplit = str.toString().split(":");
                InetAddress address = InetAddress.getByName(strSplit[0].replace("/", ""));
                Integer port = Integer.valueOf(strSplit[1]);
                
                byte[] buffer;
                buffer = resposta.getBytes();
                
                DatagramPacket resp = new DatagramPacket(buffer, 
                                                         buffer.length, 
                                                         address, 
                                                         port);
                s.send(resp);
            }
        }
    }
    
    public static void sendToOne(DatagramSocket s, DatagramPacket recebe, String resposta, String nome, String paraQuem) throws IOException {
        // Insere no Log o que foi dito
        atualizarLog(recebe, resposta, nome);
        
        // Pega na lista de socketAddress o cliente escolhido usando o index retornado da lista de Nomes
        SocketAddress str = socketAddressList.get(nomesList.indexOf(paraQuem.trim()));
        
        // Splita a entrada da lista, pois um socketAddress tem o padrão "/IP:Porta"
        // Ex: /127.0.0.1:57473
        // Retirando também a "/" no começo do IP
        String[] strSplit = str.toString().split(":");
        InetAddress address = InetAddress.getByName(strSplit[0].replace("/", ""));
        Integer port = Integer.valueOf(strSplit[1]);

        byte[] buffer;
        buffer = resposta.getBytes();

        DatagramPacket resp = new DatagramPacket(buffer, 
                                                 buffer.length, 
                                                 address, 
                                                 port);
        s.send(resp);
    }
    
    private static void criarLog() {
        java.io.File arquivo = new java.io.File("src/", "log.txt");
        
        // Verifica se o arquvio de log já existe anteriormente, se não, o cria.
        if (!arquivo.exists()) {
            try {
                arquivo.createNewFile();
                System.out.print("Log criado com sucesso!\n");
            }catch (IOException e) {
            }
        }else {
            System.out.println("Log existente!");
        }
    }
    
    private static void atualizarLog(DatagramPacket recebe, String log, String nome) throws IOException {
        java.io.File arquivo = new java.io.File("src/", "log.txt");
        
        // Splita a entrada da lista, pois um socketAddress tem o padrão "/IP:Porta"
        // Ex: /127.0.0.1:57473
        // Retirando também a "/" no começo do IP
        String[] strSplit = recebe.getSocketAddress().toString().split(":");
        InetAddress address = InetAddress.getByName(strSplit[0].replace("/", ""));
        Integer port = Integer.valueOf(strSplit[1]);
        
        String logConexao = "";
        logConexao = logConexao + ("<" + nome + ">@");
        logConexao = logConexao + ("<" + address + ">@");
        logConexao = logConexao + ("<" + port + ">#");
        logConexao = logConexao + ("<" + log + ">");
        
        FileWriter fileWriter = new FileWriter(arquivo, true);
        try (PrintWriter printWriter = new PrintWriter(fileWriter)) {
            printWriter.println(logConexao);
            printWriter.flush();
            printWriter.close();
        }
    }
    
    public static void atualizarListaClientes(DatagramSocket s, DatagramPacket recebe, String resposta, String nome) throws IOException {
        for(SocketAddress str: socketAddressList) {
            String[] strSplit = str.toString().split(":");
            InetAddress address = InetAddress.getByName(strSplit[0].replace("/", ""));
            Integer port = Integer.valueOf(strSplit[1]);

            byte[] buffer;
            buffer = resposta.getBytes();

            DatagramPacket resp = new DatagramPacket(buffer, 
                                                     buffer.length, 
                                                     address, 
                                                     port);
            s.send(resp);
        }
    }
    
    // Verifica se o nome escolhido consta na lista de Nomes de Clientes ativos
    // Se SIM, retorna ao Cliente que o nomo é INVÁLIDO
    // Se NÃO, retorna ao Cliente que o nome é VÁLIDO, podendo o Cliente enviar a resposta com cabeçalho "NOME"
    public static void verificaNome(DatagramSocket s, DatagramPacket recebe, String nome) throws IOException {
        byte[] buffer;
        String resposta;
        
        if(nomesList.indexOf(nome.trim()) == -1) {
            resposta = delimitador + "VALIDO";
        }else {
            resposta = delimitador + "INVALIDO";
        }
        buffer = resposta.getBytes();

        DatagramPacket resp = new DatagramPacket(buffer, 
                                                 buffer.length, 
                                                 recebe.getAddress(), 
                                                 recebe.getPort());
        s.send(resp);
    }
}