package Part2;

import javax.imageio.ImageIO;

import java.awt.*;
import java.awt.geom.AffineTransform;

public class Kart
{
    private int centerX, centerY;
    private int sizeX, sizeY;
    private int speed;
    private int direction;
    private boolean stuck;

    public boolean isStuck() {
        return stuck;
    }
    public void setStuck(boolean stuck) {
        this.stuck = stuck;
    }
    private Image[] images;

    public Kart(Point center, Dimension dim, int direction, String imgRootPath)
    {
        images = new Image[16];
        for (int i = 0; i < images.length; i++)
        {
            try
            {
                images[i] = ImageIO.read(getClass().getClassLoader().getResource(imgRootPath + i + ".png"));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        this.centerX = center.x;
        this.centerY = center.y;
        this.sizeX = dim.width;
        this.sizeY = dim.height;
        this.direction = direction;
    }

    public void render(Graphics g)
    {
        if (images != null && images[direction] != null)
            g.drawImage(images[direction], centerX - 25, centerY - 22, null);
        else
            System.out.println("Kart image not loaded");
    }

    // left turn
    public void steerLeft()
    {
        direction += 1;

        if (direction > 15)
            direction = 0;
    }

    // right turn
    public void steerRight()
    {
        direction -= 1;
        if (direction < 0)
            direction = 15;
    }

    // speeds up kart, speed cap to 100
    public void accelerate()
    {
        speed += 10;
        if (speed > 100)
            speed = 100;
    }

    // slows down kart, does not go -ve speed
    public void decelerate()
    {
        speed -= 10;
        if (speed < 0)
            speed = 0;
    }

    // update kart position using time interval
    public void update(double dt) {
        centerX += speed * Math.cos(direction * Math.PI / 8) * dt;
        centerY -= speed * Math.sin(direction * Math.PI / 8) * dt;
    }

    // stop kart
    public void stop() {
        speed = 0;
    }

    // used for collision detection
    public Shape getShape() {
        Rectangle bounds = new Rectangle(centerX - sizeX / 2, centerY - sizeY / 2, sizeX, sizeY);
        AffineTransform tx = new AffineTransform();
        // rotate the shape using AffineTransform for accuracy
        tx.rotate(-Math.PI / 8 * direction, centerX, centerY);
        return tx.createTransformedShape(bounds);
    }

    public void drawHitbox(Graphics g) {
        Shape bounds = getShape();
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.GREEN);
        g2d.draw(bounds);
    }
}
