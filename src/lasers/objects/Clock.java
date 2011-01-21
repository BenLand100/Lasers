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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.JMenuItem;
import lasers.Beam;
import lasers.MethodAction;
import lasers.World;
import lasers.WorldObject;

/**
 * Contains all the logic for a ticking Clock that implements ControlObject
 *
 * @author benland100
 */
public class Clock extends WorldObject implements ControlObject {

    private final static Map<World,Integer> worlds = Collections.synchronizedMap(new HashMap<World,Integer>());
    private final static List<Clock> slow = Collections.synchronizedList(new LinkedList<Clock>());
    private final static List<Clock> regular = Collections.synchronizedList(new LinkedList<Clock>());
    private final static List<Clock> fast = Collections.synchronizedList(new LinkedList<Clock>());
    private final static List<Clock> extreme = Collections.synchronizedList(new LinkedList<Clock>());
    private final static Thread timer = new Thread("laser-clocks") {
        @Override
        public void run() {
            int i = 0;
            boolean slow_on = true, regular_on = true, fast_on = true, extreme_on = true;
            while (true) {
                try {
                    Thread.sleep(250);
                    if (i % 4 == 0){
                        slow_on = !slow_on;
                        synchronized (slow) {
                            for (Clock c : slow) {
                                c.tick(slow_on);
                            }
                        }
                    }
                    if (i % 3 == 0) {
                        regular_on = !regular_on;
                        synchronized (regular) {
                            for (Clock c : regular) {
                                c.tick(regular_on);
                            }
                        }
                    }
                    if (i % 2 == 0) {
                        fast_on = !fast_on;
                        synchronized (fast) {
                            for (Clock c : fast) {
                                c.tick(fast_on);
                            }
                        }
                    }
                    synchronized (extreme) {
                        extreme_on = !extreme_on;
                        for (Clock c : extreme) {
                            c.tick(extreme_on);
                        }
                    }
                    synchronized (worlds) {
                        for (World w : worlds.keySet()) {
                            w.rebuildBeams();
                            w.repaint();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                i++;
            }
        }
    };
    static {
        timer.start();
    }

    public static final int NONE = -1;
    public static final int SLOW = 0;
    public static final int REGULAR = 1;
    public static final int FAST = 2;
    public static final int EXTREME = 3;

    protected int type;
    protected boolean active;
    protected LinkedList<ToggleObject> toggle = new LinkedList<ToggleObject>();

    private static final int VERSION_0 = 0;
    private static final int VERSION_CURRENT = VERSION_0;

    public static void write(DataOutputStream out, Clock clock) throws IOException {
        WorldObject.write(out,clock);
        out.writeInt(VERSION_CURRENT);
        out.writeInt(clock.type);
        out.writeBoolean(clock.active);
    }

    public static Clock read(DataInputStream in, World w) throws IOException {
        Clock res = new Clock(w);
        WorldObject.read(in,res);
        int version = in.readInt();
        res.retype(in.readInt());
        res.active = in.readBoolean();
        return res;
    }

    public static Clock read_legacy(DataInputStream in, World w) throws IOException {
        Clock res = new Clock(w);
        WorldObject.read_legacy(in,res);
        res.retype(in.readInt());
        res.active = in.readBoolean();
        return res;
    }


    public Clock(World w) {
        super(w);
        if (worlds.containsKey(w)) {
            worlds.put(w,worlds.get(w)+1);
        } else {
            worlds.put(w,1);
        }
        retype(REGULAR);
    }

    protected WorldObject impl_duplicate() {
        Clock c = new Clock(world);
        c.active = active;
        c.retype(type);
        return c;
    }

    public void tick(boolean high) {
        active = high;
    }

    public void retype(int i) {
        switch (type) {
            case SLOW:
                slow.remove(this);
                break;
            case REGULAR:
                regular.remove(this);
                break;
            case FAST:
                fast.remove(this);
                break;
            case EXTREME:
                extreme.remove(this);
                break;
        }
        type = i;
        switch (type) {
            case SLOW:
                slow.add(this);
                break;
            case REGULAR:
                regular.add(this);
                break;
            case FAST:
                fast.add(this);
                break;
            case EXTREME:
                extreme.add(this);
                break;
        }
    }

    @Override
    public void cleanup() {
        int count = worlds.get(world)-1;
        worlds.put(world,count);
        if (count < 1) {
            worlds.remove(world);
        }
        retype(NONE);
    }

    @Override
    public void draw(Graphics2D g, double scale) {
        g.setColor(Color.LIGHT_GRAY);
        g.fillOval((int)((x-extent)*scale), (int)((y-extent)*scale), (int)(extent*2*scale),  (int)(extent*2*scale));
        g.setColor(active ? Color.RED : Color.BLUE);
        g.drawOval((int)((x-extent)*scale), (int)((y-extent)*scale), (int)(extent*2*scale),  (int)(extent*2*scale));
    }

    @Override
    public Beam unsettled() {
        return null;
    }

    @Override
    public Beam strike(Beam beam) {
        beam.distance = Math.hypot(beam.org_x-x,beam.org_y-y);
        return null;
    }

    @Override
    public void settled() {
        for (ToggleObject obj : toggle) {
            obj.setToggle(active);
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

    @Override
    public JMenuItem[] getMenuItems() {
        return new JMenuItem[] {
            new JMenuItem(new MethodAction("No Tick",this,"retype",new Class[] {Integer.TYPE},NONE)),
            new JMenuItem(new MethodAction("Slow Tick",this,"retype",new Class[] {Integer.TYPE},SLOW)),
            new JMenuItem(new MethodAction("Regular Tick",this,"retype",new Class[] {Integer.TYPE},REGULAR)),
            new JMenuItem(new MethodAction("Fast Tick",this,"retype",new Class[] {Integer.TYPE},FAST)),
            new JMenuItem(new MethodAction("Extreme Tick",this,"retype",new Class[] {Integer.TYPE},EXTREME))
        };
    }

}
