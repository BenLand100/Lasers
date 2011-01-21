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
import lasers.Beam;
import lasers.World;
import lasers.WorldObject;

/**
 * Represents an object that can slectively block or not block a Beam as an
 * implementation of ToggleObject
 *
 * @author benland100
 */
public class Blocker extends WorldObject implements ToggleObject {

    protected ControlObject owner = null;
    protected boolean onIsOpaque;
    protected boolean opaque;

    private static final int VERSION_0 = 0;
    private static final int VERSION_CURRENT = VERSION_0;

    public static void write(DataOutputStream out, Blocker blocker) throws IOException {
        WorldObject.write(out,blocker);
        out.writeInt(VERSION_CURRENT);
        out.writeBoolean(blocker.onIsOpaque);
        out.writeBoolean(blocker.opaque);
    }

    public static Blocker read(DataInputStream in, World w) throws IOException {
        Blocker res = new Blocker(w,true);
        WorldObject.read(in,res);
        int version = in.readInt();
        res.onIsOpaque = in.readBoolean();
        res.opaque = in.readBoolean();
        return res;
    }

    public static Blocker read_legacy(DataInputStream in, World w) throws IOException {
        Blocker res = new Blocker(w,true);
        WorldObject.read_legacy(in,res);
        res.onIsOpaque = in.readBoolean();
        res.opaque = in.readBoolean();
        return res;
    }

    public Blocker(World w, boolean opaque) {
        super(w);
        onIsOpaque = opaque;
        this.opaque = !opaque;
    }

    protected WorldObject impl_duplicate() {
        Blocker b = new Blocker(world,opaque);
        b.onIsOpaque = onIsOpaque;
        b.opaque = opaque;
        return b;
    }

    @Override
    public void cleanup() {
        if (owner != null) owner.release(this);
    }

    public void claim(ControlObject newowner) {
        if (owner != null) owner.release(this);
        owner = newowner;
    }

    public void setToggle(boolean on) {
        if (onIsOpaque) {
            if (opaque != on) world.invalidate(this);
            opaque = on;
        } else {
            if (opaque == on) world.invalidate(this);
            opaque = !on;
        }
    }

    @Override
    public void draw(Graphics2D g, double scale) {
        if (opaque) {
            g.setColor(Color.GRAY);
            g.fillOval((int)((x-extent)*scale), (int)((y-extent)*scale), (int)(extent*2*scale),  (int)(extent*2*scale));
        } else {
            g.setColor(Color.WHITE);
            g.drawOval((int)((x-extent)*scale), (int)((y-extent)*scale), (int)(extent*2*scale),  (int)(extent*2*scale));
        }
    }

    @Override
    public Beam unsettled() {
        return null;
    }

    @Override
    public Beam strike(Beam beam) {
        if (opaque) {
            beam.distance = Math.hypot(beam.org_x-x,beam.org_y-y);
            return null;
        } else {
            return null;
        }
    }

}
