package Part1;

import javax.swing.JFrame;

public class KartMain
{
    public static void main(String[]args)
    {
        Kart kart = new Kart();

        JFrame window = new JFrame("Kart Spinning");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.add(kart);

        window.pack();
        window.setVisible(true);

        kart.startAnimation();
    }
}
