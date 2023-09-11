package Part2;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class GamePanel extends JPanel implements ActionListener {

    private final Kart kart1;
    private final Kart kart2;
    private final Timer timer;

    private final Point startPoint = new Point(425, 500);
    private final int trackWidth = 100;

    Rectangle innerBounds = new Rectangle(150, 200, 550, 300);
    Rectangle outerBounds = new Rectangle(50, 100, 750, 500);
    Rectangle midBounds = new Rectangle(100, 150, 650, 400);
    Rectangle finishLine = new Rectangle(startPoint.x, startPoint.y, 1, 100);
    Dimension kartDim = new Dimension(35, 25);


    // karts
    private final String imgRoot1 = "karts/kart1/";
    private final String imgRoot2 = "karts/kart2/";

    // displays all hitboxes (kart, map) if set to true
    protected boolean showOutlines;

    public GamePanel()
    {
        setFocusable(true);

        Point kart1Pos = new Point(startPoint.x + 25, startPoint.y + 25);
        Point kart2Pos = new Point(startPoint.x + 25, startPoint.y + 25 + 50);
        kart1 = new Kart(kart1Pos, kartDim, 0, imgRoot1);
        kart2 = new Kart(kart2Pos, kartDim, 0, imgRoot2);
        addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                switch (e.getKeyCode())
                {
                    case KeyEvent.VK_LEFT -> kart1.steerLeft();
                    case KeyEvent.VK_RIGHT -> kart1.steerRight();
                    case KeyEvent.VK_UP -> kart1.accelerate();
                    case KeyEvent.VK_DOWN -> kart1.decelerate();

                    case KeyEvent.VK_A -> kart2.steerLeft();
                    case KeyEvent.VK_D -> kart2.steerRight();
                    case KeyEvent.VK_W -> kart2.accelerate();
                    case KeyEvent.VK_S -> kart2.decelerate();

                }
            }
        });
        requestFocusInWindow();
        timer = new Timer(30, this);
        timer.start();
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        map(g);
        kart1.render(g);
        kart2.render(g);

        if (showOutlines)
        {
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
        g.drawLine(startPoint.x, startPoint.y, startPoint.x, startPoint.y + trackWidth); // start/finish line
    }

    private void drawBoundaries(Graphics g) {
        g.setColor(Color.red);
        g.drawRect(outerBounds.x, outerBounds.y, outerBounds.width, outerBounds.height); // outer edge
        g.drawRect(innerBounds.x, innerBounds.y, innerBounds.width, innerBounds.height); // inner edge
        g.drawRect(finishLine.x, finishLine.y, finishLine.width, finishLine.height);

        kart1.drawHitbox(g);
        kart2.drawHitbox(g);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        checkCollisions();
        kart1.update(0.3);
        kart2.update(0.3);
        repaint();
    }

    // collision checker
    private void checkCollisions() {

        // if both karts crash into each other, game over
        if (kart1.getShape().intersects(kart2.getShape().getBounds())) {
            crashSfx();
            gameOver();
            return;
        }

        checkKartCollision(kart1, innerBounds, outerBounds);
        checkKartCollision(kart2, innerBounds, outerBounds);

        checkWinCondition(kart1);
        checkWinCondition(kart2);
    }

    private void gameOver() {
        timer.stop();
        JOptionPane.showMessageDialog(this, "Game Over !");
    }

    // check if kart crashed into the map boundaries
    private void checkKartCollision(Kart kart, Rectangle innerBounds, Rectangle outerBounds) {
        Shape kartBounds = kart.getShape();
        if (kartBounds.intersects(innerBounds)) {
            kart.stop();

            if (!kart.isStuck()) {
                crashSfx();
                System.out.println("Inner collision");
                kart.setStuck(true);
            }
        } else if (!outerBounds.contains(kartBounds.getBounds())) {
            kart.stop();
            if (!kart.isStuck()) {
                crashSfx();
                System.out.println("Outer collision");
                kart.setStuck(true);
            }
        } else {
            kart.setStuck(false);
        }
    }

    private void checkWinCondition(Kart kart)
    {
        Shape kartBounds = kart.getShape();

        if(kartBounds.intersects(finishLine))
        {
            finishSfx();
            kart.stop();
            gameOver();
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
}
