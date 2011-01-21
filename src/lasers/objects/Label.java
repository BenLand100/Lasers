package lasers.objects;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.swing.JColorChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import lasers.MethodAction;
import lasers.World;
import lasers.WorldObject;

/**
 * Lets a user make notes in a World that do not obstruct Beams
 *
 * @author benland100
 */
public class Label extends WorldObject {

    Color color;
    String str;

    private static final int VERSION_0 = 0;
    private static final int VERSION_CURRENT = VERSION_0;

    public static void write(DataOutputStream out, Label l) throws IOException {
        WorldObject.write(out,l);
        out.writeInt(VERSION_CURRENT);
        out.writeUTF(l.str);
        out.writeInt(l.color.getRGB());
    }

    public static Label read(DataInputStream in, World w) throws IOException {
        Label res = new Label(w);
        WorldObject.read(in,res);
        int version = in.readInt();
        res.str = in.readUTF();
        res.color = new Color(in.readInt());
        return res;
    }

    public static Label read_legacy(DataInputStream in, World w) throws IOException {
        Label res = new Label(w);
        WorldObject.read_legacy(in,res);
        res.str = in.readUTF();
        res.color = new Color(in.readInt());
        return res;
    }

    public Label(World w) {
        super(w);
        extent = 5;
        str = "";
        color = Color.CYAN;
    }

    public void setText(String text) {
        str = text;
        world.repaint();
    }

    public void setColor(Color c) {
        color = c;
        world.repaint();
    }

    @Override
    public void draw(Graphics2D g, double scale) {
        g.setColor(color);
        g.drawOval(x-extent, y-extent, extent*2, extent*2);
        g.drawLine(x-extent, y, x+extent, y);
        g.drawLine(x, y-extent, x, y+extent);
        g.drawString(str, x+extent*2, y+extent);
    }

    @Override
    protected WorldObject impl_duplicate() {
        Label l = new Label(world);
        l.str = str;
        l.color = color;
        return l;
    }

    private void setText() {
        String wat = JOptionPane.showInputDialog(world, "Change the text", str);
        if (wat != null) {
            str = wat;
        }
        world.repaint();
    }

    private void setColor() {
        Color c = JColorChooser.showDialog(world, "Text Color", color);
        if (c != null) {
            setColor(c);
        }
    }

    @Override
    public JMenuItem[] getMenuItems() {
        return new JMenuItem[] {
            new JMenuItem(new MethodAction("Change Text", this, "setText", null)),
            new JMenuItem(new MethodAction("Change Color", this, "setColor", null))
        };
    }

}
