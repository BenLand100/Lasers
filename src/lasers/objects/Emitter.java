/**
 *  Copyright 2010 by Benjamin J. Land (a.k.a. BenLand100)
 *
 *  This file is part of the Laser Logic Simulator
 *
 *  Laser Logic Simulator is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Laser Logic Simulator is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Laser Logic Simulator. If not, see <http://www.gnu.org/licenses/>.
 */

package lasers.objects;

import lasers.ToggleObject;
import lasers.ControlObject;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.swing.JColorChooser;
import javax.swing.JMenuItem;
import lasers.Beam;
import lasers.MethodAction;
import lasers.World;
import lasers.WorldObject;

/**
 * Implements a ToggleObject as a Beam emitter
 *
 * @author benland100
 */
public class Emitter extends WorldObject implements ToggleObject {

    protected ControlObject owner = null;
    protected boolean emitting = true;
    protected Color color = Color.RED;

    private static final int VERSION_0 = 0;
    private static final int VERSION_CURRENT = VERSION_0;

    public Emitter(World w) {
        super(w);
    }

    public static void write(DataOutputStream out, Emitter emittor) throws IOException {
        WorldObject.write(out,emittor);
        out.writeInt(VERSION_CURRENT);
        out.writeBoolean(emittor.emitting);
        out.writeInt(emittor.color.getRGB());
    }

    public static Emitter read(DataInputStream in, World w) throws IOException {
        Emitter res = new Emitter(w);
        WorldObject.read(in,res);
        int version = in.readInt();
        res.emitting = in.readBoolean();
        res.color = new Color(in.readInt());
        return res;
    }

    public static Emitter read_legacy(DataInputStream in, World w) throws IOException {
        Emitter res = new Emitter(w);
        WorldObject.read_legacy(in,res);
        res.emitting = in.readBoolean();
        return res;
    }

    protected WorldObject impl_duplicate() {
        Emitter e = new Emitter(world);
        e.emitting = emitting;
        return e;
    }

    public void cleanup() {
        if (owner != null) owner.release(this);
    }

    public void claim(ControlObject newowner) {
        if (owner != null) owner.release(this);
        owner = newowner;
    }

    public void setColor(Color c) {
        color = c;
        world.rebuildBeams();
        world.repaint();
    }

    public void setToggle(boolean on) {
        if (emitting != on) world.invalidate(this);
        emitting = on;
    }

    private void toggle() {
        emitting = !emitting;
        world.rebuildBeams();
        world.repaint();
    }

    private void setColor() {
        Color c = JColorChooser.showDialog(world, "Laser Color", color);
        if (c != null) {
            setColor(c);
        }
    }

    public JMenuItem[] getMenuItems() {
        return new JMenuItem[] {
            new JMenuItem(new MethodAction("Toggle Beam",this,"toggle",null)),
            new JMenuItem(new MethodAction("Choose Color",this,"setColor",null))
        };
    }

    @Override
    public void draw(Graphics2D g, double scale) {
        double cos = Math.cos(angle)*extent;
        double sin = Math.sin(angle)*extent;
        g.setColor(Color.GRAY);
        g.fillOval((int)((x-extent)*scale), (int)((y-extent)*scale), (int)(extent*2*scale),  (int)(extent*2*scale));
        g.setColor(color);
        g.drawLine(x, y, (int)(x+cos), (int)(y+sin));
    }

    @Override
    public Beam unsettled() {
        if (emitting) {
            return new Beam(angle,(int)(x+Math.cos(angle)*extent),(int)(y+Math.sin(angle)*extent),color);
        } else {
            return null;
        }
    }

    @Override
    public Beam strike(Beam beam) {
        beam.distance = Math.hypot(beam.org_x-x,beam.org_y-y);
        return null;
    }

}
