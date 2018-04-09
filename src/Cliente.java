import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.logging.*;
import javax.swing.*;

public class Cliente {
    
    // Variáveis para criação da tela
    private static JFrame applicationFrame;
    private static JTextField campoMsg;
    private static JLabel labelNome;
    private static JLabel labelTexto;
    private static JLabel labelContadorTexto;
    private static JTextArea areaConversa;
    private static JComboBox boxClientes;
    
    // Variaveis de controle de conexão
    private static Boolean entrou = false;
    private static Boolean saiu = false;
    
    // Delimitador usado para separar as informações enviadas/recebidas
    private static String delimitador = ">#<";
    
    // String para armazenamento do nome do Cliente
    private static String nomeCliente = "";
    
    // Variavel usada como controle da altura nas labels
    private static int contadorHeightLabel = -15;
    
    public static void main(String[] args) throws SocketException, UnknownHostException, IOException {
        DatagramSocket s = new DatagramSocket();
        InetAddress dest = InetAddress.getByName("localhost");
        
        // Chama o método para criação da tela
        criarTela();
        
        // Adiciona o Listener para capturar o fechamento da janela
        applicationFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                // Se o Cliente não saiu do chat, exibe a caixa de diálogo para confirmação
                // Se o Cliente já saiu do chat, apenas fecha a janela
                if (!saiu) {
                    if (JOptionPane.showConfirmDialog(null,"Deseja sair do Chat?")==JOptionPane.OK_OPTION){
                        try {
                            // Se o cliente ainda não entrou, apenas fecha a janela
                            // Se o cliente já entrou, manda a mensagem de SAIR e fecha a janela
                            if (entrou) {
                                mandarMsg(s, dest, "SAIR");
                            }
                            System.exit(0);
                        } catch (IOException ex) {
                            Logger.getLogger(Cliente.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }else {
                    System.exit(0);
                }
            }
        });
        
        // Adiciona o Listener para capturar o Enter do campoMsg
        campoMsg.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // Verifica se o cliente entrou no chat
                    if (entrou) {
                        // Verifica se a mensagem enviada é vazia
                        // Se não, segue com o código
                        // Se sim, manda a mensagem de SAIR
                        if (!campoMsg.getText().trim().equals("")) {
                            // Verifica se na mensagem contém o DELIMITADOR, ou se ela começa com o caracter proibido [
                            if(campoMsg.getText().contains(delimitador) || campoMsg.getText().startsWith("[")) {
                                JOptionPane.showMessageDialog(null,"Erro! Sua mensagem não pode conter os caracteres:\n[\n" + delimitador);
                            }else {
                                mandarMsg(s, dest, "CONVERSA");
                            }
                        }else {
                            mandarMsg(s, dest, "SAIR");
                        }
                    }else {
                        // Verifica se a mensagem enviada é vazia
                        // Se sim, apenas fecha a janela
                        // Se não, chama o método para verificação do nome escolhido
                        if (campoMsg.getText().trim().equals("")) {
                            System.exit(0);
                        }else {
                            verificarNome(s, dest);
                        }
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Cliente.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        
        // Adiciona o Listener para capturar a quantidade de caracteres digitados
        campoMsg.addKeyListener(new KeyAdapter() {  
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                // Executa o código apenas se a tecla digitada não é o ENTER
                if(e.getKeyCode() != java.awt.event.KeyEvent.VK_ENTER) {
                    if (entrou) {
                        // Verifica se o tamanho da mensagem é menor ou igual a 400 caracteres
                        // Se sim, pinta o contador de AZUL e atualiza seu texto
                        // Se não, pinta o contador de VERMELHO e atualiza o texto para 0
                        if(campoMsg.getText().length() <= 400) {
                            labelContadorTexto.setForeground(Color.BLUE);
                            labelContadorTexto.setText(String.valueOf(400-campoMsg.getText().length()));
                        }else {
                            labelContadorTexto.setForeground(Color.RED);
                            labelContadorTexto.setText("0");
                        }
                        
                        // Chama o método para centralizar o texto do contador
                        centralizarContador();
                    }
                }
            };  
        });
        
        // Começa a janela com o campoMsg selecionado
        campoMsg.requestFocus();
        
        // PULO DO GATO
        // Enquanto a variavel de controle SAIU não ser TRUE, continua a receber mensagem
        // Caso ela altere para TRUE, (significando que o cliente saiu), fecha o DatagramSocket
        while(!saiu) {
            receberMsg(s, dest);
        }
        s.close();
    }
    
    public static void criarTela() {
        applicationFrame = new JFrame();
        
        // Label para colocar o nome
        labelNome = new JLabel();
        labelNome.setVisible(false);
        labelNome.setText("");
        labelNome.setForeground(Color.BLUE);
        applicationFrame.add(labelNome, BorderLayout.PAGE_START);
        
        // Box inicial para seleção do sendToOne
        boxClientes = new JComboBox(new String[]{"Todos"});
        boxClientes.setBounds(235,0,183,17);
        boxClientes.setVisible(false);
        applicationFrame.add(boxClientes, BorderLayout.CENTER);
        
        // Area aonde todas as mensagens são visualizadas
        areaConversa = new JTextArea();
        areaConversa.setEditable(false);
        areaConversa.setFont(new Font("Arial", Font.BOLD, 12));
        areaConversa.setText("Digite seu nome");
        areaConversa.setForeground(Color.BLACK);
        areaConversa.setBackground(new java.awt.Color(255, 255, 215));
        applicationFrame.add(new JScrollPane(areaConversa), BorderLayout.CENTER);
        
        // Campo de texto para digitar a mensagem
        campoMsg = new JTextField();
        campoMsg.setEnabled(true);
        applicationFrame.add(campoMsg, BorderLayout.PAGE_END);
        
        // Label para contar/mostrar quantidade de caracteres digitados
        labelContadorTexto = new JLabel();
        labelContadorTexto.setVisible(true);
        labelContadorTexto.setFont(new Font("Arial", Font.ITALIC, 10));
        labelContadorTexto.setText("400");
        labelContadorTexto.setForeground(Color.BLUE);
        labelContadorTexto.setBounds(424,2,18,13);
        applicationFrame.add(labelContadorTexto, BorderLayout.NORTH);

        // Altera o comportamento da janela para travar o fechamento
        applicationFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        applicationFrame.setSize(455, 453);
        applicationFrame.setLocationRelativeTo(null);
        applicationFrame.setResizable(false);
        applicationFrame.setVisible(true);
        campoMsg.requestFocus();
    }
    
    public static void mandarMsg (DatagramSocket socket, InetAddress destino, String tipoMsg) throws IOException {
        byte[] buffer;
        DatagramPacket msg;
        String envio = "";
        
        // Padrão da mensagem:
        // TIPO_MENSAGEM + DELIMITADOR + NOME_REMETENTE + DELIMITADOR + NOME_DESTINATARIO + DELIMITADOR + MENSAGEM
        // "CONVERSA"    + ">#<"       + "MURILLO"      + ">#<"       + "ELAINE"          + ">#<"       + "BOM DIA"
        // CONVERSA>#<MURILLO>#<ELAINE>#<BOM DIA
        switch (tipoMsg) {
            case "SAIR":
                envio = "SAIR" + delimitador + 
                        nomeCliente + delimitador + 
                        boxClientes.getSelectedItem() + delimitador + 
                        "";
                labelNome.setForeground(Color.RED);
                labelNome.setText(labelNome.getText().replace("Online", "Offline"));
                boxClientes.setSelectedIndex(0);
                boxClientes.setEnabled(false);
                areaConversa.setText(areaConversa.getText() + "(Público) Você saiu do chat!");
                inserirLabel("PUBLICO","(Público) Você saiu do chat!");
                campoMsg.setEnabled(false);
                labelContadorTexto.setText("");
                entrou = false;
                saiu = true;
            break;
            
            case "VERIFICAR_NOME":
                envio = "VERIFICAR_NOME" + delimitador + 
                        campoMsg.getText() + delimitador + 
                        boxClientes.getSelectedItem() + delimitador + 
                        "";
            break;
            
            case "NOME":
                envio = "NOME" + delimitador + 
                        campoMsg.getText() + delimitador + 
                        boxClientes.getSelectedItem() + delimitador + 
                        "";
                nomeCliente = campoMsg.getText();
                labelNome.setText(nomeCliente + " (Online)");
                labelNome.setVisible(true);
                boxClientes.setVisible(true);
                areaConversa.setText("");
                areaConversa.setForeground(new java.awt.Color(255, 255, 215));
                campoMsg.setText("");
                entrou = true;
            break;
            
            case "CONVERSA":
                // Pega o tamanho da mensagem sendo enviada
                int tamanhoMsg = campoMsg.getText().trim().length();
                
                // Copia para a variavel temporaria a mensagem sendo enviada
                String subString = campoMsg.getText().trim();
                
                // Se o tamanho da mensagem for maior do que o limite (400), exclui o excedente da mesma
                if(tamanhoMsg > 400) {
                    subString = campoMsg.getText().trim().substring(0, 400);
                }
                
                envio = "CONVERSA" + delimitador + 
                        nomeCliente + delimitador + 
                        boxClientes.getSelectedItem() + delimitador + 
                        subString;
                
                // Se na Box estiver selecionado "Todos", escreve a mensagem em modo PÚBLICO
                // Se não, escreve a mensagem em modo PRIVADO
                if (boxClientes.getSelectedItem().equals("Todos")) {
                    areaConversa.setText(areaConversa.getText() + "(Público) Você disse: " + subString + "\n");
                    inserirLabel("PUBLICO","(Público) Você disse: " + subString);
                }else {
                    areaConversa.setText(areaConversa.getText() + "(Privado) Você disse para " + boxClientes.getSelectedItem().toString().trim() + ": " + subString + "\n");
                    inserirLabel("PRIVADO","(Privado) Você disse para " + boxClientes.getSelectedItem().toString().trim() + ": " + subString);
                }
                campoMsg.setText("");
                labelContadorTexto.setText("400");
                centralizarContador();
                labelContadorTexto.setForeground(Color.BLUE);
            break;
        }
        
        buffer = envio.getBytes();
        msg = new DatagramPacket(buffer, buffer.length, destino, 4545);
        socket.send(msg);
    }
    
    // Método que fica em loop, garantindo que o cliente sempre possa tanto enviar quanto receber mensagens
    public static void receberMsg (DatagramSocket socket, InetAddress destino) throws IOException {
        DatagramPacket resposta = new DatagramPacket(new byte[512], 512);
        socket.receive(resposta);
        String res = new String(resposta.getData());
        
        // Se a mensagem recebida começar com o caracter [, chama o método para atualizar a lista de Clientes ativos
        // Se for igual a "DELIMITADOR" + VALIDO, reenvia o nome desejado para desbloqueio do chat
        // Se for igual a "DELIMITADOR" + INVALIDO, exibe a mensagem de erro, nome repetido
        // Se não for nenhuma dessas (significando que o Cliente está ativo no chat), segue com o código ELSE
        if (res.startsWith("[")) {
            atualizarListaClientes(res, boxClientes, labelNome);
        }else if(res.trim().equals(delimitador + "VALIDO")) {
            mandarMsg(socket, destino, "NOME");
        }else if(res.trim().equals(delimitador + "INVALIDO")) {
            JOptionPane.showMessageDialog(null,"Erro! Nome já escolhido. Tente outro.");
            campoMsg.requestFocus();
        }else {
            // Escreve na area de conversa a mensagem recebida
            areaConversa.setText(areaConversa.getText() + res.trim() + "\n");
            
            // Se a mensagem recebida começar com "(Público)", chama o método em modo público, ou não.
            if(res.trim().startsWith("(Público)")) {
                inserirLabel("PUBLICO",res.trim());
            }else {
                inserirLabel("PRIVADO",res.trim());
            }
        }
        
        // Seta o foco no campoMsg, tendo efeito secundário a exibição de um "alerta" laranja nas outras janelas de clientes
        campoMsg.requestFocus();
    }
    
    public static void verificarNome(DatagramSocket s, InetAddress dest) throws IOException {
        String nome = campoMsg.getText().trim();
        // Verifica se no nome escolhido contém algum dos caracteres proibidos
        // Se não, verifica se o nome escolhido é "TODOS" (IgnoreCase)
        // Se não, verifica se o nome é maior do que 15 caracteres
        // Se não, envia a mensagem para VERIFICAR_NOME
        if (nome.matches(".*[<>#,\\]\\[].*")) {
            JOptionPane.showMessageDialog(null,"Erro! Nome não pode conter os caracteres:\n<\n>\n#\n[\n]\n,");
            campoMsg.requestFocus();
        }else if (nome.equalsIgnoreCase("Todos")) {
            JOptionPane.showMessageDialog(null,"Erro! Nome inválido. Tente outro.");
        }else if(nome.length() > 15) {
            JOptionPane.showMessageDialog(null,"Erro! Nome muito grande, máximo de 15 caracteres). Tente outro.");
        }else {
            mandarMsg(s, dest, "VERIFICAR_NOME");
        }
    }
    
    public static void atualizarListaClientes (String linha, JComboBox boxClientes, JLabel labelNome) {
        // Limpa String recebida e a transforma em vetor
        linha = linha.replace("[", "");
        linha = linha.replace("]", "");
        linha = linha.replaceAll(", ", ",");
        String[] nomesSplit = linha.split(",");
        
        // Remove todos os itens da Box e inclui primeiramente o "Todos"
        Object clienteSelecionado = boxClientes.getSelectedItem();
        boxClientes.removeAllItems();
        boxClientes.addItem("Todos");
        
        boolean controleClienteSelecionado = false;
        // Percorre todo o vetor de Clientes adicionando cada item a Box
        for (String nomesSplit1 : nomesSplit) {
            if (!nomeCliente.equals(nomesSplit1.trim())) {
                boxClientes.addItem(nomesSplit1);
            }
            // Verifica se o cliente do for é o mesmo do cliente selecionado
            if (clienteSelecionado.toString().trim().equals(nomesSplit1.trim())) {
                controleClienteSelecionado = true;
            }
        }
        
        // Se a variavel de controle tiver sido alterada para TRUE, altera na Box p/ o cliente selecionado
        // Se não, altera na Box para o DEFAULT = "Todos"
        if (controleClienteSelecionado) {
            boxClientes.setSelectedItem(clienteSelecionado);
        }else {
            boxClientes.setSelectedIndex(0);
        }
    }
    
    // Método responsável para "colorir" as mensagem enviadas/recebidas
    // Vai inserindo Label's, uma embaixo da outra, com cada mensagem enviada/recebida
    // Usa a variavel de controle contadorHeightLabel para que cada Label fique logo embaixo da última
    public static void inserirLabel(String tipo, String texto) {
        labelTexto = new JLabel();
        labelTexto.setVisible(true);
        labelTexto.setFont(new Font("Arial", Font.BOLD, 12));
        labelTexto.setText(texto);
        if(tipo.equals("PUBLICO")) {
            labelTexto.setForeground(new java.awt.Color(0, 0, 0));
        }else {
            labelTexto.setForeground(new java.awt.Color(115, 0, 227));
        }
        contadorHeightLabel = contadorHeightLabel + 15;
        labelTexto.setBounds(0,contadorHeightLabel,10001,13);
        areaConversa.add(labelTexto, BorderLayout.CENTER);
    }
    
    // Centraliza o contador de caracteres dependendo da quantidade restante
    public static void centralizarContador() {
        int length = Integer.parseInt(labelContadorTexto.getText());
        if((length >= 0) && (length <= 9)) {
            labelContadorTexto.setBounds(430,2,18,13);
        }else if((length >= 10) && (length <= 99)) {
            labelContadorTexto.setBounds(427,2,18,13);
        }else {
            labelContadorTexto.setBounds(424,2,18,13);
        }
    }
}