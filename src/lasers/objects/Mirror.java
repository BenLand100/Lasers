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

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import lasers.Beam;
import lasers.World;
import lasers.WorldObject;

/**
 * Reflects a Beam
 *
 * @author benland100
 */
public class Mirror extends WorldObject {

    public Mirror(World w) {
        super(w);
        extent = 20;
    }

    private static final int VERSION_0 = 0;
    private static final int VERSION_CURRENT = VERSION_0;

    public static void write(DataOutputStream out, Mirror mirror) throws IOException {
        WorldObject.write(out,mirror);
        out.writeInt(VERSION_CURRENT);
    }

    public static Mirror read(DataInputStream in, World w) throws IOException {
        Mirror res = new Mirror(w);
        WorldObject.read(in,res);
        int version = in.readInt();
        return res;
    }

    public static Mirror read_legacy(DataInputStream in, World w) throws IOException {
        Mirror res = new Mirror(w);
        WorldObject.read_legacy(in,res);
        return res;
    }

    protected WorldObject impl_duplicate() {
        return new Mirror(world);
    }

    @Override
    public void draw(Graphics2D g, double scale) {
        double cos = Math.cos(angle)*extent;
        double sin = Math.sin(angle)*extent;
        g.setColor(Color.GRAY);
        g.drawLine((int)((x - cos)*scale), (int)((y - sin)*scale), (int)((x + cos)*scale), (int)((y + sin)*scale));
    }

    @Override
    public Beam unsettled() {
        return null;
    }

    @Override
    public Beam strike(Beam beam) {
        double ourslope = Math.tan(angle);
        double itsslope = Math.tan(beam.angle);
        double xint = (beam.org_y - y - itsslope*beam.org_x + ourslope*x) / (ourslope-itsslope);
        double yint = itsslope == 0 ? beam.org_y : ourslope*(xint-x) + y;
        if (Math.hypot(x-xint,y-yint) > extent) return null;
        beam.distance = Math.hypot(beam.org_x-xint,beam.org_y-yint);
        double newangle = Math.PI*2 + angle*2 - beam.angle;
        return new Beam(newangle,(int)xint,(int)yint,beam.c);
    }

}
