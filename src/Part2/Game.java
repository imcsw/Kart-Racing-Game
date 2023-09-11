package Part2;

import javax.swing.*;
import java.awt.*;

public class Game extends JFrame
{
    public static void main(String[] args) {
        new Game();
    }

    Game()
    {
        setTitle("Part 2");
        GamePanel game = new GamePanel();
        game.showOutlines = true; // draw kart hitboxes and map boundaries
        setContentPane(game);

        setSize(new Dimension(850, 700));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }
}
