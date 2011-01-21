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

package lasers;

import java.awt.Color;

/**
 *
 * Represents one (straight) segment of a beam. A reflection is considered a new
 * Beam object, and is stored in the `child` field. The origin WorldObject of
 * the beam is storred, but not a significant field in the current implementation
 *
 * @author benland100
 */
public class Beam {

    public final double angle;
    public final int org_x, org_y;
    public final Color c;
    public double distance;
    public WorldObject origin = null;
    public Beam child = null;

    public Beam(double angle, int org_x, int org_y, Color c) {
        this.angle = angle;
        this.org_x = org_x;
        this.org_y = org_y;
        this.c = c;
        distance = 0;
    }

}
