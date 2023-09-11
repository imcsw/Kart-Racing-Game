package Part1;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Graphics;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.Timer;

public class Kart extends JPanel
{
    protected ImageIcon images[], images2[];
    private final int TOTAL_IMAGES = 16; // number of images
    private int currentImage = 0; // current image index
    private final int ANIMATION_DELAY = 50; // millisecond delay
    private int width; // image width
    private int height; // image height
    private Timer animationTimer; // Timer drives animation
    // constructor initializes LogoAnimatorJPanel by loading images

    public Kart()
    {
        images = new ImageIcon[ TOTAL_IMAGES ];
        images2 = new ImageIcon[TOTAL_IMAGES];

        for (int count = 0; count < images.length; count++)
        {
            images[count] = new ImageIcon(getClass().getResource
                    ("/karts/kart1/" + count + ".png"));
        }

        for (int count = 0; count < images2.length; count++)
        {
            images2[count] = new ImageIcon(getClass().getResource
                    ("/karts/kart2/" + count + ".png"));
        }
        // this example assumes all images have the same width and height
        width = images[ 0 ].getIconWidth(); // get icon width
        height = images[ 0 ].getIconHeight(); // get icon height
    } // end LogoAnimatorJPanel constructor

    // display current image
    public void paintComponent( Graphics g )
    {
        super.paintComponent( g ); // call superclass paintComponent

        images[ currentImage ].paintIcon( this, g, 0, 0 );
        images2[ currentImage ].paintIcon( this, g, 55, 0 );

        // set next image to be drawn only if Timer is running
        if(animationTimer.isRunning())
        {
            currentImage = (currentImage + 1) % TOTAL_IMAGES;
        }
    } // end method paintComponent

    // start animation, or restart if window is redisplayed
    public void startAnimation()
    {
        if ( animationTimer == null )
        {
            currentImage = 0; // display first image
            // create timer
            animationTimer = new Timer( ANIMATION_DELAY, new TimerHandler());
            animationTimer.start();
        }
        else // animationTimer already exists, restart animation
        {
            if (!animationTimer.isRunning())
            {
                animationTimer.restart();
            }
        } // end else
    } // end method startAnimation

    // stop animation Timer
    public void stopAnimation()
    {
        animationTimer.stop();
    }

    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

    public Dimension getPreferredSize()
    {
        return new Dimension(width, height);
    }

    private class TimerHandler implements ActionListener
    {
        public void actionPerformed(ActionEvent actionEvent)
        {
            repaint();
        }
    }
}
