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

package lasers.kdimensional;

import java.awt.Point;
import java.awt.geom.Point2D;
import static java.lang.Math.sqrt;

/**
 * An arbitrary dimensional point/vector
 *
 * @author benland100
 */
public class kdPoint {

    public final double[] mag;

    public kdPoint(kdPoint pt) {
        this(pt.mag);
    }

    public kdPoint(Point pt) {
        mag = new double[] {pt.x, pt.y};
    }

    public kdPoint(Point2D pt) {
        mag = new double[] {pt.getX(), pt.getY()};
    }

    public kdPoint(double... mag) {
        this.mag = mag;
    }

    public void set(kdPoint pt) {
        if (pt.mag.length != mag.length) throw new RuntimeException("Invalid dimension");
        for (int i = 0; i < mag.length; i++) {
            mag[i] = pt.mag[i];
        }
    }

    public kdPoint cross(kdPoint pt) {
        if (pt.mag.length != mag.length || mag.length != 3) throw new RuntimeException("Invalid cross product");
        double[] cross = new double[3];
        cross[0] = mag[2]*pt.mag[1] - mag[1]*pt.mag[2];
        cross[1] = mag[2]*pt.mag[0] - mag[0]*pt.mag[2];
        cross[2] = mag[0]*pt.mag[1] - mag[1]*pt.mag[0];
        return new kdPoint(cross);
    }

    public kdPoint mul(double scaler) {
        double[] res = new double[mag.length];
        for (int i = 0; i < mag.length; i++) {
            res[i] = mag[i] * scaler;
        }
        return new kdPoint(res);
    }

    public double len() {
        double sq = 0;
        for (int i = 0; i < mag.length; i++) {
            sq += mag[i]*mag[i];
        }
        return sqrt(sq);
    }

    public kdPoint norm() {
        return mul(1D/len());
    }

    public double dot(kdPoint pt) {
        if (pt.mag.length != mag.length) throw new RuntimeException("Diminsion mismatch");
        double dot = 0;
        for (int i = 0; i < mag.length; i++) {
            dot += pt.mag[i]*mag[i];
        }
        return dot;
    }

    public double dist(kdPoint pt) {
        if (pt.mag.length != mag.length) throw new RuntimeException("Diminsion mismatch");
        double sq = 0;
        for (int i = 0; i < mag.length; i++) {
            double c = pt.mag[i] - mag[i];
            sq += c*c;
        }
        return sqrt(sq);
    }

    public kdPoint sub(kdPoint pt) {
        if (pt.mag.length != mag.length) throw new RuntimeException("Diminsion mismatch");
        double[] res = new double[mag.length];
        for (int i = 0; i < mag.length; i++) {
            res[i] = mag[i] - pt.mag[i];
        }
        return new kdPoint(res);
    }

    public kdPoint add(kdPoint pt) {
        if (pt.mag.length != mag.length) throw new RuntimeException("Diminsion mismatch");
        double[] res = new double[mag.length];
        for (int i = 0; i < mag.length; i++) {
            res[i] = mag[i] + pt.mag[i];
        }
        return new kdPoint(res);
    }

    public int dimension() {
        return mag.length;
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder("(");
        for (int i = 0; i < mag.length; i++) {
            buff.append(mag[i]).append(',');
        }
        buff.delete(buff.length()-1, buff.length());
        buff.append(')');
        return buff.toString();
    }

    @Override
    public int hashCode() {
        int code = new Integer(mag.length).hashCode();
        for (int i = 0; i < mag.length; i++) {
           code ^= new Double(mag[i]).hashCode() >>> i;
        }
        return code;
    }

    @Override
    public boolean equals(Object pt) {
        if (pt instanceof kdPoint && ((kdPoint)pt).mag.length == mag.length) {
            double[] cmp = ((kdPoint)pt).mag;
            for (int i = 0; i < mag.length; i++) {
                if (mag[i] != cmp[i]) return false;
            }
            return true;
        }
        return false;
    }


}