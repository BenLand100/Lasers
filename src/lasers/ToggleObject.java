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

/**
 * Represents an object to be controlled by a ControlObject. Classes implementing
 * this interface should keep track of which object controlls them, and in the
 * event of being claimed by another ControlObject, should `release` itself from
 * the previous claimer, unless it can maintain multiple owners at once.
 *
 * @author benland100
 */
public interface ToggleObject {

    /**
     * Notifies this object it has a new owner
     * @param owner The new owner
     */
    public void claim(ControlObject owner);

    /**
     * Invoked by a ControlObject when the state should change
     * @param on
     */
    public void setToggle(boolean on);

}
