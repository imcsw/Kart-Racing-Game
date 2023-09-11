package Part3;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

public class GameServer extends JFrame {
    private static final int DEFAULT_PORT = 3800;
    private int serverPort;

    public static void main(String[] args) {
        new GameServer();
    }

    // client threads, clients are handled in separate threads
    ArrayList<ClientThread> clientThreads;
    HashMap<String, String> karts;
    private ActionListener actionListener;

    JTextField portTextField;
    JButton start;
    JButton showDataButton;
    JButton end;
    JCheckBox finalizeCheckBox;
    JTextArea transcript;
    private boolean finalize;   // finalize server when last client disconnects
    ServerSocket serverSocket;
    int nClients;               // num of clients connected
    boolean running;            // return client status

    public GameServer()
    {
        initActionListener();
        clientThreads = new ArrayList<>();
        karts = new HashMap<>();
        JPanel mainPanel = new JPanel();

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); // panel to hold all buttons and text boxes

        JLabel portLabel = new JLabel("Port :");
        btnPanel.add(portLabel);
        portTextField = new JTextField(DEFAULT_PORT + "");
        btnPanel.add(portTextField);

        start = new JButton("Start");
        start.addActionListener(actionListener);
        btnPanel.add(start);

        showDataButton = new JButton("Show Karts Data");
        showDataButton.addActionListener(actionListener);
        showDataButton.setEnabled(false);
        btnPanel.add(showDataButton);

        finalizeCheckBox = new JCheckBox("Finalize");
        finalizeCheckBox.addActionListener(actionListener);
        btnPanel.add(finalizeCheckBox);

        end = new JButton("End");
        end.setEnabled(false);
        end.addActionListener(actionListener);
        btnPanel.add(end);

        mainPanel.add(btnPanel);

        transcript = new JTextArea();
        transcript = new JTextArea(7, 40);
        transcript.setLineWrap(true);
        transcript.setWrapStyleWord(true);
        transcript.setEditable(false);

        mainPanel.add(new JScrollPane(transcript));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Server");
        setContentPane(mainPanel);
        setSize(new Dimension(560, 350));
        setResizable(false);
        setVisible(true);
    }

    // starts server, initialize server socket, start listening to port provided in a new thread
    private void exec()
    {
        try {
            serverPort = Integer.parseInt(portTextField.getText().trim());
        } catch (NumberFormatException e) {
            showError("Invalid port number" + portTextField.getText());
            return;
        }
        try {
            serverSocket = new ServerSocket(serverPort);

        } catch (Exception e) {
            showError("Error initializing server socket");
        }
        running = true;
        new Thread(() -> {  // listen for new connections
            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    ClientThread clientThread = new ClientThread(socket);
                    clientThreads.add(clientThread);
                    clientThread.start();
                    showDataButton.setEnabled(true);
                } catch (Exception e) {
                    showError(e.getMessage());
                }
            }
        }).start();

        log("Listening on port " + serverPort);
        start.setEnabled(false);
        end.setEnabled(true);
    }

    private void showKartsData() {  // show client data
        log("\nAll clients data : ");
        log("\n");
        for (String key : karts.keySet()) {
            log(karts.get(key) + "\n");
        }
    }

    private void end() {    // disconnects all clients and closes thread
        for (ClientThread client : clientThreads) {
            try {
                log("Disconnecting player " + client.id);
                client.socket.close();
                client.running = false;
            } catch (Exception e) {
                showError("Cannot disconnect player " + client.id + ": " + e.getMessage());
            }
        }
        try {
            serverSocket.close();
        } catch (Exception e) {
            showError("Error closing server: " + e.getMessage());
        }
        log("Server closed");
        running = false;

        start.setEnabled(true);
        end.setEnabled(false);
    }

    private void initActionListener() {
        actionListener = e -> {
            if (e.getSource() == start) {
                exec();
            } else if (e.getSource() == showDataButton) {
                showKartsData();
            } else if (e.getSource() == end) {
                end();
            } else if (e.getSource() == finalizeCheckBox) {
                finalize = finalizeCheckBox.isSelected();
            }
        };
    }

    synchronized void log(String message) {
        transcript.append(message + "\n");
    }
    synchronized void showError(String errorMessage) {
        JOptionPane.showMessageDialog(this, errorMessage, "Error!", JOptionPane.ERROR_MESSAGE);
    }

    // client server interaction
    private class ClientThread extends Thread
    {
        boolean running;
        private final Socket socket;    // client's socket
        private BufferedReader in;
        private PrintWriter out;
        String id;  // player id

        private ClientThread(Socket socket)
        {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream());
                id = nClients++ + ""; // player id starts from 0
                log("New client detected. Client ID: " + id);
                out.println(id);
                out.flush();
                running = true;
            } catch (Exception e) {
                showError("Error: " + e.getMessage());
            }
        }

        // constantly listens to clients, store client data if stopped, finalize server if last client stops
        public void run()
        {
            String line;
            while (running) {
                try {
                    line = in.readLine();
                    handleClientRequest(line);

                } catch (Exception e) {
                    showError("Unable to load client request" + e.getMessage());
                    break;
                }
            }
            log("Player " + id + " left the game");

            if (--nClients == 0 && finalize) {
                log("Last client closed.");
                end();
            }
        }

        // handling client request
        private synchronized void handleClientRequest(String request)
        {
            if (request == null) {
                log("Client " + id + " closed");
                return;
            }
            log(request);
            StringTokenizer st = new StringTokenizer(request.trim(), "#");

            String protocol = st.nextToken();

            if (protocol.equals("P0")) {
                try {
                    socket.close();
                    log("Client closed : " + id);
                    log("Game Over !");
                    for(var client: clientThreads) {
                        if(client.id != id) {
                            client.out.println("P0#" + id);
                        }
                    }
                    running = false;
                } catch (Exception e) {
                    showError("Failed to close socket");
                }
            } else if (protocol.equals("P1")) {
                String data = st.nextToken();
                int numKarts = karts.size() - 1;
                karts.put(id, data.trim());
                log("Kart Data received from : " + id );
                log("Number of karts '" + (numKarts) + "' sent to client " + id);
                out.println(numKarts + "");
                out.flush();
                if (numKarts < 1)
                    return;
                for (var client : clientThreads) {
                    if (!client.id.equals(id)) {
                        out.println(client.id + "?" + karts.get(client.id));
                        out.flush();
                        log("Kart " + client.id + " Data " + karts.get(client.id) + " sent to : " + id + "\n");
                    }
                }
                log("----------------------");

            }
        }
    }
}
