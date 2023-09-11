package Part3;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

public class GamePanel extends JPanel implements ActionListener
{
    private Timer timer;
    Kart[] karts;
    private boolean showOutlines;

    private final int FPS = 30; // how many times should the game update per second

    // map boundaries and other things
    private Point startPoint = new Point(425, 500);
    private Rectangle innerBounds = new Rectangle(150, 200, 550, 300);
    private Rectangle midBounds = new Rectangle(100, 150, 650, 400);
    private Rectangle outerBounds = new Rectangle(50, 100, 750, 500);
    Rectangle finishLine = new Rectangle(startPoint.x, startPoint.y, 1, 100);
    private int trackWidth = 100;

    //kart image folders
    private final String KART1_PATH = "karts/kart1/";
    private final String KART2_PATH = "karts/kart2/";

    private int id; // client id

    // socket
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean connected;  // client connection status

    protected GamePanel() {
        setFocusable(true);
        init();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT -> karts[id].steerLeft();
                    case KeyEvent.VK_RIGHT -> karts[id].steerRight();
                    case KeyEvent.VK_UP -> karts[id].accelerate();
                    case KeyEvent.VK_DOWN -> karts[id].decelerate();
                }
            }
        });

        // to request focus whenever mouse hovers on the game panel
        // this is necessary when the window has other elements such as text fields
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                requestFocusInWindow();
            }
        });
    }

    protected void init() {
        // set both karts at starting line
        Point kart1Pos = new Point(startPoint.x + trackWidth / 4, startPoint.y + trackWidth / 4);
        Point kart2Pos = new Point(startPoint.x + trackWidth / 4, startPoint.y + trackWidth / 4 + trackWidth / 2);

        Kart kart1 = new Kart(kart1Pos, new Dimension(35, 25), 0, KART1_PATH);
        Kart kart2 = new Kart(kart2Pos, new Dimension(35, 25), 0, KART2_PATH);

        karts = new Kart[]{kart1, kart2};

        requestFocusInWindow();


        /* delay = (number of millis in 1s) / (frames per second)
         but not always guaranteed to give the specified fps (depends on many factors)*/
        if(timer != null)
            timer.stop();
        timer = new Timer(1000 / FPS, this);
        timer.start();
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        map(g);
        for (var kart : karts) {
            kart.render(g);
        }
        if (showOutlines) {
            drawBoundaries(g);
        }
    }

    // track
    private void map(Graphics g)
    {
        Color c1 = Color.green;
        g.setColor(c1);
        g.fillRect(innerBounds.x, innerBounds.y, innerBounds.width, innerBounds.height); // grass
        Color c2 = Color.black;
        g.setColor(c2);
        g.drawRect(outerBounds.x, outerBounds.y, outerBounds.width, outerBounds.height); // outer edge
        g.drawRect(innerBounds.x, innerBounds.y, innerBounds.width, innerBounds.height); // inner edge
        Color c3 = Color.yellow;
        g.setColor(c3);
        g.drawRect(midBounds.x, midBounds.y, midBounds.width, midBounds.height); // mid-lane marker
        Color c4 = Color.white;
        g.setColor(c4);
        g.drawLine(startPoint.x, startPoint.y, startPoint.x, startPoint.y + trackWidth); // start line
    }

    // draw map boundaries and kart hitboxes, only called when showOutlines is true
    private void drawBoundaries(Graphics g) {
        g.setColor(Color.red);
        g.drawRect(outerBounds.x, outerBounds.y, outerBounds.width, outerBounds.height); // outer edge
        g.drawRect(innerBounds.x, innerBounds.y, innerBounds.width, innerBounds.height); // inner edge

        for (Kart kart : karts) kart.drawHitbox(g);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        checkCollisions();
        karts[id].update(0.3);
        if (connected)
            requestNextFrame();
        repaint();
    }

    // send local kart data to server and request other kart data from server
    private synchronized void requestNextFrame()
    {
        //send local kart data
        out.println("P1#" + karts[id].encode());
        out.flush();

        try {   // server replies with number of players connected to the server, does not include local kart
            String line = in.readLine().trim();
            if (line.length() > 1 && line.substring(0, 2).equals("P0")) {
                String message = "Player " + line.substring(2) + " left." +
                        " You may continue to play or close the game.";
                JOptionPane.showMessageDialog(this, message);
            }
            int numKarts = Integer.parseInt(line);
            log("Players connected: " + numKarts + "\n");


            if (numKarts < 1) { // if there are no players connected, skip waiting for kart data from server
                log("No other players found");
                return;
            }

            // receive the karts data and update the karts
            for (int i = 1; i <= numKarts; i++) {
                String response = in.readLine();
                System.out.println(response);
                int idx = Integer.parseInt(response.substring(0, response.indexOf('?')));
                String kartData = response.substring(response.indexOf('?') + 1);
                karts[idx].decode(kartData);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // connect to server and port given
    protected boolean connect(String server, int port)
    {
        try {
            socket = new Socket(server, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream());
            id = Integer.parseInt(in.readLine().trim());

            log("Connected to server.\nID: " + id + '\n');
            connected = true;
            requestFocusInWindow();
            return true;
        } catch (Exception e) {
            showError("Error connecting: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    void gameOver()
    {
        timer.stop();
        log("Game Over!");
        disconnect();
        JOptionPane.showMessageDialog(null, "Game Over!");
    }

    protected void disconnect() {
        connected = false;
        try {
            // tells the server that player is disconnecting
            if(out == null)
                return;
            out.println("P0#" + karts[id].encode());
            out.flush();

            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // collision checker
    private void checkCollisions() {

        // if both karts crash into each other, game over
        for (int i = 0; i < karts.length; i++) {
            if (i == id)
                continue; // does not check its own id
            if (karts[id].getShape().intersects(karts[i].getShape().getBounds())) {
                crashSfx();
                gameOver();
                return;
            }
        }
        checkKartCollision(karts[id], innerBounds, outerBounds);
        checkWinCondition(karts[id]);
    }

    private void checkWinCondition(Kart kart) {
        Shape kartBounds = kart.getShape();

        if(kartBounds.intersects(finishLine))
        {
            finishSfx();
            kart.stop();
            gameOver();
        }
    }

    // check if kart crashed into the map boundaries
    private void checkKartCollision(Kart kart, Rectangle innerBounds, Rectangle outerBounds)
    {
        Shape kartBounds = kart.getShape();
        if (kartBounds.intersects(innerBounds)) {
            kart.stop();

            if (!kart.isStuck()) {
                crashSfx();
                System.out.println("Collision with inner boundaries");
                kart.setStuck(true);
            }
        } else if (!outerBounds.contains(kartBounds.getBounds())) {
            kart.stop();
            if (!kart.isStuck()) {
                crashSfx();
                System.out.println("Collision with outer boundaries");
                kart.setStuck(true);
            }
        } else {
            kart.setStuck(false);
        }
    }

    // crash sound effect
    private void crashSfx() {
        try {
            String soundPath = "crash.wav";
            Clip crashSound = AudioSystem.getClip();
            crashSound.open(AudioSystem.getAudioInputStream(getClass().getClassLoader().getResource(soundPath)));
            crashSound.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // race end sound effect
    private void finishSfx()
    {
        try {
            String soundPath = "end.wav";
            Clip endSound = AudioSystem.getClip();
            endSound.open(AudioSystem.getAudioInputStream(getClass().getClassLoader().getResource(soundPath)));
            endSound.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Part 3 Stuff */
    public boolean isConnected() {
        return connected;
    }

    protected void setOutlines(boolean showOutlines) {
        this.showOutlines = showOutlines;
    }

    private void showError(String s)    // prints any error on screen into a JOptionPane
    {
        JOptionPane.showMessageDialog(this, s, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void log(String message)    // prints system messages
    {
        System.out.print(message);
    }
}
