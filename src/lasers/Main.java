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

import javax.swing.JApplet;
import javax.swing.JFrame;

/**
 * Simple class that puts a world in a JFrame or an Applet, depeding on how the
 * program is loaded. The applet seems to be a bit buggy on some systems, so
 * use the JFrame if possible.
 *
 * @author benland100
 */
public class Main extends JApplet {

    @Override
    public void start() {
        add(new World());
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        World w = new World();
        JFrame frame = new JFrame("Laser Logic Simulator v2 by Benland100");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(w);
        frame.setSize(500,500);
        frame.setVisible(true);
    }

}
