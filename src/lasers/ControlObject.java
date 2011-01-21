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

import java.util.List;

/**
 * Represents an object that can control other objects (e.g. Clocks and Detectors)
 * Classes implementing this interface should keep track of the controlled
 * objects and modify them as necessary. The World will invoke `control` and
 * `release` as nessary, and it is important that all currently controlled objects
 * be returned by `controlled` when it is invoked.
 *
 * @author benland100
 */
public interface ControlObject {

    /**
     * Called when an object is selected to be controlled by this ControlObject
     * @param obj The Object
     */
    public void control(ToggleObject obj);

    /**
     * Called when an object is selected to be relesed from this ControlObject
     * @param obj The Object
     */
    public void release(ToggleObject obj);

    /**
     * Returns a list of all the currently controlled objects
     * @return The list
     */
    public List<ToggleObject> controlled();
}
