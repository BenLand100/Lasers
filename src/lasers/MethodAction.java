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

import java.lang.reflect.Method;
import javax.swing.AbstractAction;

/**
 * A staple of mine from the distant past. MethodAction just makes GUIs easier
 * by letting a "clickable" object have an action associated with it that invokes
 * some method from some object/class, with some variable arguments be created
 * with only one line of code. Takes the hassel out of GUIs.
 *
 * @author benland100
 */
public class MethodAction extends AbstractAction {

    private Method method;
    private Object parent;
    private Object[] args;

    public MethodAction(String name, Object parent, String method, Class[] types, Object... args) {
        this(name,parent.getClass(),method,types,args);
        this.parent = parent;
    }

    public MethodAction(String name, Class parentClass, String method, Class[] types, Object... args) {
        super(name);
        parent = null;
        this.args = args;
        try {
            if (types == null) {
                types = new Class[args.length];
                for (int i = 0; i < args.length; i++) {
                    types[i] = args[i].getClass();
                }
            }
            this.method = parentClass.getDeclaredMethod(method,types);
            this.method.setAccessible(true);
        } catch(java.lang.Exception e) {
            e.printStackTrace();
        }
    }

    public void actionPerformed(java.awt.event.ActionEvent e) {
        try {
            method.invoke(parent,args);
        } catch(java.lang.Exception x) {
            x.printStackTrace();
        }
    }

}