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
import java.util.LinkedList;
import java.util.List;
import lasers.Beam;
import lasers.World;
import lasers.WorldObject;

/**
 * Implements a ControlObject that reacts to a Beam striking it
 *
 * @author benland100
 */
public class Detector extends WorldObject implements ControlObject {

    protected boolean struck;
    protected LinkedList<ToggleObject> toggle = new LinkedList<ToggleObject>();

    private static final int VERSION_0 = 0;
    private static final int VERSION_CURRENT = VERSION_0;

    public static void write(DataOutputStream out, Detector detector) throws IOException {
        WorldObject.write(out,detector);
        out.writeInt(VERSION_CURRENT);
        out.writeBoolean(detector.struck);
    }

    public static Detector read(DataInputStream in, World w) throws IOException {
        Detector res = new Detector(w);
        WorldObject.read(in,res);
        int version = in.readInt();
        res.struck = in.readBoolean();
        return res;
    }

    public static Detector read_legacy(DataInputStream in, World w) throws IOException {
        Detector res = new Detector(w);
        WorldObject.read_legacy(in,res);
        res.struck = in.readBoolean();
        return res;
    }

    public Detector(World w) {
        super(w);
    }

    protected WorldObject impl_duplicate() {
        Detector d = new Detector(world);
        d.struck = struck;
        return d;
    }

    @Override
    public void draw(Graphics2D g, double scale) {
        g.setColor(Color.GRAY);
        g.fillOval((int)((x-extent)*scale), (int)((y-extent)*scale), (int)(extent*2*scale),  (int)(extent*2*scale));
        g.setColor(struck ? Color.RED : Color.WHITE);
        g.drawOval((int)((x-extent)*scale), (int)((y-extent)*scale), (int)(extent*2*scale),  (int)(extent*2*scale));
    }

    @Override
    public Beam unsettled() {
        struck = false;
        return null;
    }

    @Override
    public Beam strike(Beam beam) {
        beam.distance = Math.hypot(beam.org_x-x,beam.org_y-y);
        this.struck = true;
        return null;
    }
    
    @Override
    public void settled() {
        for (ToggleObject obj : toggle) {
            obj.setToggle(struck);
        }
    }

    public void control(ToggleObject obj) {
        obj.claim(this);
        toggle.add(obj);
    }

    public void release(ToggleObject obj) {
        toggle.remove(obj);
    }

    public List<ToggleObject> controlled() {
        return (List<ToggleObject>)toggle.clone();
    }

}
