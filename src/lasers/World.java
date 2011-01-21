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

import lasers.kdimensional.kdTree;
import lasers.kdimensional.kdPoint;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import lasers.objects.Blocker;
import lasers.objects.Clock;
import lasers.objects.Detector;
import lasers.objects.Emitter;
import lasers.objects.Label;
import lasers.objects.Mirror;

/**
 * The bulk of the program logic is here. This class represents a Laser Simulator
 * environment with all the GUI bells and whistles for editing such an
 * environment. This also has methods for saving/loading the state, as well as
 * implementations for copying data from one instance to another (or another
 * program) by using the clipboard.
 *
 * Basic rundown of the UI:
 *      Right click for more options
 *      Click and drag an object to rotate it
 *      Double click and drag an object to move it
 *      Click and drag empty space to select objects
 *      Selections can be moved by double clicking and dragging space in it
 *      Selections can be coppied, pasted, and deleted
 *      Pasted objects are automatically selections
 *
 * @author benland100
 */
public class World extends JPanel {

    //Objects that can be added must be defined here
    public static enum ObjectType {
        EMITTER, DETECTOR, OPAQUE_BLOCK, TRANSPARENT_BLOCK, MIRROR, CLOCK, LABEL
    }
    
    //The list of WorldObjects in this World. Not all indexes are valid, but the
    //first `count` indexes are.
    private WorldObject[] objs;
    private int count;
    
    //The scale of the world (currently 1.0 always) and the origin position.
    //The origin is rendered at the center of the window, always, and is modified
    //by draging the background around
    private double scale,  org_x, org_y;

    //The current list of beams (only the starting segments) that is rebuilt by
    //calling `rebuildBeams`
    private final LinkedList<Beam> beams = new LinkedList<Beam>();

    //List that keeps track of what objects need to be retraced while updating
    //the beams list, e.g. objects that were modified by a striking beam that
    //could change the function of another Beam
    private final LinkedList<WorldObject> invalid = new LinkedList<WorldObject>();

    //A kdTree used in calculating Beam interactions with objects and the objects
    //in the world being clicked. Makes finding objects closest to a point very
    //easy and fast
    private kdTree<WorldObject> tree;

    //The bounds (in world coordinates) of the World as reported by `rebuildTree`
    private Rectangle bounds;

    //The maximum necessary length to trace a Beam before assuming it hits nothing
    //which is calculated to be the diagonal length of the bounds intersected
    //with the world bounds of the viewscreen so that Beams never appear to stop
    //nor miss an object
    private double maxd;

    //Keeps track of objects that have been selected for linking to, and the
    //last object clicked.
    private WorldObject linkingObj = null, clickedObj = null;

    //As of the last mousepress, the number of clicks (as reported by the OS)
    //associated with that mouse event
    private int clickcount;

    //Marks whether the last Drag invalidated the kdTree, or made a new selection
    //and the appropriate actions are taken on the next mouserelease
    private boolean treeInvalid, selectionMade;

    //The current selection rectangle, or null if there is no selection
    private Rectangle selectRect;

    //Once a selection is made, the objects in the box are added to this list
    //and cleared when the selection is terminated
    private LinkedList<WorldObject> selectGroup = new LinkedList<WorldObject>();

    //Speaks for itself, but its used for calculating offsets in dragging mostly
    private Point lastRelevantMousePos;

    public World() {
        scale = 1.0;
        count = 0;
        org_x = 0;
        org_y = 0;
        objs = new WorldObject[100];
        rebuildTree();

        enableEvents(AWTEvent.MOUSE_EVENT_MASK);
        enableEvents(AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    /**
     * Converts a position on the JPanel to a position in the World
     * @param x ScreenX
     * @param y ScreenY
     * @return WorldPoint
     */
    public Point toWorld(int x, int y) {
        int w = getWidth();
        int h = getHeight();
        x -= w / 2;
        y -= h / 2;
        x /= scale;
        y /= scale;
        x -= org_x;
        y -= org_y;
        return new Point(x, y);
    }

    /**
     * Converts a position in the World to a position on the JPanel
     * @param x WorldX
     * @param y WorldY
     * @return ScreenPoint
     */
    public Point toScreen(int x, int y) {
        int w = getWidth();
        int h = getHeight();
        x += org_x;
        y += org_y;
        x *= scale;
        y *= scale;
        x += w / 2;
        y += h / 2;
        return new Point(x, y);
    }

    /**
     * Gets the WorldObject at the specified screen location, or null if no
     * object is there.
     * @param x ScreenX
     * @param y ScreenY
     * @return The detected object, or null
     */
    public WorldObject objectFromPoint(int x, int y) {
        try {
            TreeMap<Double, kdTree.Entry<WorldObject>> res = tree.nnSearch(1, new kdPoint(toWorld(x, y)));
            if (res.size() > 0) {
                Double key = res.firstKey();
                WorldObject obj = res.get(key).data;
                if (key <= obj.getExtent()) {
                    return obj;
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Follows a beam, hitting objects, creating child beams, and continuing via
     * a loop until a beam reaches `maxd`. A beam is said to have hit an object
     * when it crosses an object's extent.
     *
     * @param beam A beam to follow
     */
    private synchronized void traceBeam(Beam beam) {
        Beam child = null;
        beam.distance = 0;
        do {
            double cos = Math.cos(beam.angle);
            double sin = Math.sin(beam.angle);
            //If anything has an extent smaller than 3, this could be an issue
            //but larger increments make it harder to ensure the object is
            //interacted with, while smaller increments make it significantly
            //slower, for obvious reasons
            for (int d = 0; d < maxd; d += 3) {
                WorldObject nearest = null;
                try {
                    TreeMap<Double, kdTree.Entry<WorldObject>> res = tree.nnSearch(1, new kdPoint(beam.org_x + d * cos, beam.org_y + d * sin));
                    if (res.size() > 0) {
                        Double key = res.firstKey();
                        nearest = res.get(key).data;
                        if (nearest != beam.origin && key <= nearest.getExtent()) {
                            child = nearest.strike(beam);
                            if (child != null) {
                                child.origin = nearest;
                                beam.child = child;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (beam.distance != 0) {
                    break;
                }
            }
            if (beam.distance == 0) {
                beam.distance = maxd;
            }
            beam = child;
            child = null;
        } while (beam != null);
    }

    /**
     * Notifies the world that durring a Beam trace, an object's state became
     * invalid and must be recalculated.
     * @param obj
     */
    public synchronized void invalidate(WorldObject obj) {
        invalid.add(obj);
    }

    /**
     * Recalculates the entire state of the laser beams for the client. Currently,
     * if the calculation takes more than 1000 cycles to complete, e.g. during
     * each calculation cycle objects continue to be invalidated, it is assumed
     * that a race condition has been encountered in the logic, and the beams
     * are cleared.
     */
    public synchronized void rebuildBeams() {
        Point tl = toWorld(0,0);
        Point br = toWorld(getWidth(),getHeight());
        double width = Math.max(br.x,bounds.getMaxX()) - Math.min(tl.x, bounds.getMinX());
        double height = Math.max(br.x,bounds.getMaxY()) - Math.min(tl.y, bounds.getMinY());
        maxd = Math.hypot(width,height);

        int cycles = 0;
        synchronized (beams) {
            do {
                beams.clear();
                invalid.clear();
                for (int i = 0; i < count; i++) {
                    WorldObject obj = objs[i];
                    Beam beam = obj.unsettled();
                    if (beam != null) {
                        beam.origin = obj;
                        beams.add(beam);
                    }
                }
                for (Beam beam : beams) {
                    traceBeam(beam);
                }
                for (int i = 0; i < count; i++) {
                    objs[i].settled();
                }
                cycles++;
            } while (invalid.size() > 0 && cycles < 1000);
            if (invalid.size() > 0) {
                invalid.clear();
                beams.clear();
                System.err.println("Race condition probably encountered... fix it.");
            }
        }
    }

    /**
     * Rebuilds the kdTree after modifications to objects positions have been
     * made. If I took the time to write an add and remove feature to my kdTree,
     * this method could be a lot faster by only changing the objects modified.
     */
    public synchronized void rebuildTree() {
        WorldObject[] dat = new WorldObject[count];
        kdPoint[] pts = new kdPoint[count];
        bounds = new Rectangle(count < 1 ? new Point(0,0) : objs[0].getPos());
        for (int i = 0; i < count; i++) {
            bounds.add(objs[i].getPos());
            dat[i] = objs[i];
            pts[i] = new kdPoint(objs[i].getPos());
        }
        tree = new kdTree<WorldObject>(pts, dat);
    }

    /**
     * Removes a single object from the World and fires all the necessary
     * recalculation and updating.
     * Functions as a GUI callback
     * @param obj Object to remove
     */
    public void removeObject(WorldObject obj) {
        synchronized (objs) {
            for (int i = 0; i < count; i++) {
                if (objs[i] == obj) {
                    obj.cleanup();
                    if (count - 1 > 0) {
                        objs[i] = objs[count - 1];
                    }
                    count--;
                    rebuildTree();
                    rebuildBeams();
                    break;
                }
            }
        }
        repaint();
    }

    /**
     * Creates and adds the specified ObjectType of object at the specified
     * coordinates in the World, then returs that object.
     * Functions as a GUI callback
     * @param type Type of object to create
     * @param local Where to place the object
     * @return The object created
     */
    public WorldObject addObject(ObjectType type, Point local) {
        WorldObject object = null;
        switch (type) {
            case EMITTER:
                object = new Emitter(this);
                break;
            case DETECTOR:
                object = new Detector(this);
                break;
            case OPAQUE_BLOCK:
                object = new Blocker(this, true);
                break;
            case TRANSPARENT_BLOCK:
                object = new Blocker(this, false);
                break;
            case MIRROR:
                object = new Mirror(this);
                break;
            case CLOCK:
                object = new Clock(this);
                break;
            case LABEL:
                object = new Label(this);
                break;
        }
        synchronized (objs) {
            if (object != null) {
                object.setPos(local);
                if (count + 1 > objs.length) {
                    WorldObject[] temp = new WorldObject[objs.length + 100];
                    System.arraycopy(objs, 0, temp, 0, count);
                    objs = temp;
                }
                objs[count++] = object;
                rebuildTree();
                rebuildBeams();
            }
        }
        repaint();
        return object;
    }

    /**
     * Signifies an object as a ControlObject waiting to be linked to an object.
     * Functions as a GUI callback
     * @param obj ControlObject to be linked to
     */
    private void linkObject(WorldObject obj) {
        linkingObj = obj;
    }

    /**
     * Saves the current state by letting the user choose a file to save to.
     * Functions as a GUI callback
     */
    public void save() {
        JFileChooser choose = new JFileChooser();
        int res = choose.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            try {
                FileOutputStream fout = new FileOutputStream(choose.getSelectedFile());
                synchronized (objs) {
                    WorldObject[] copy = new WorldObject[count];
                    System.arraycopy(objs, 0, copy, 0, count);
                    write(fout,copy);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Loads a state by letting the user choose a file to load.
     * Functions as a GUI callback
     */
    public void load() {
        JFileChooser choose = new JFileChooser();
        int res = choose.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            try {
                FileInputStream fin = new FileInputStream(choose.getSelectedFile());
                objs = read(fin);
                count = objs.length;
                rebuildTree();
                rebuildBeams();
                repaint();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Deletes the current selection.
     * Functions as a GUI callback
     */
    private void delete() {
        if (selectRect != null) {
            synchronized (objs) {
                for (int i = 0; i < count; i++) {
                    if (selectGroup.contains(objs[i])) {
                        objs[i].cleanup();
                        objs[i] = objs[i+1];
                        objs[i+1] = objs[count-1];
                        count--;
                        i--;
                    }
                }
            }
            selectRect = null;
            selectGroup.clear();
            rebuildTree();
            rebuildBeams();
            repaint();
        }
    }

    /**
     * Coppies or Pastes the current selection relative to the `world` location.
     * Functions as a GUI callback
     * @param copy True for copy, False for paste
     * @param world Position to calculate relative positions from
     */
    private void copypaste(boolean copy, Point world) {
        synchronized (objs) {
            ClipboardAccess clipboard = new ClipboardAccess();
            if (copy && selectRect != null) {
                ArrayList<WorldObject> tocopy = new ArrayList<WorldObject>(selectGroup);
                WorldObject[] copyData = new WorldObject[tocopy.size()];
                ControlObject[] controllers = new ControlObject[tocopy.size()];
                for (int i = 0; i < copyData.length; i++) {
                    copyData[i] = tocopy.get(i).duplicate();
                    Point pos = copyData[i].getPos();
                    pos.translate(-world.x, -world.y);
                    copyData[i].setPos(pos);
                    if (tocopy.get(i) instanceof ControlObject) controllers[i] = (ControlObject)tocopy.get(i);
                }
                for (int c = 0; c < controllers.length; c++) {
                    if (controllers[c] == null) continue;
                    for (ToggleObject obj : controllers[c].controlled()) {
                        for (int i = 0; i < tocopy.size(); i++) {
                            if (obj == tocopy.get(i)) {
                                ((ControlObject)copyData[c]).control((ToggleObject)copyData[i]);
                                break;
                            }
                        }
                    }
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                write(out,copyData);
                clipboard.put(new String(out.toByteArray()));
            } else if (!copy) {
                WorldObject[] copyData = read(new ByteArrayInputStream(clipboard.get().getBytes()));
                if (count + copyData.length > objs.length) {
                    WorldObject[] temp = new WorldObject[objs.length + copyData.length*2];
                    System.arraycopy(objs, 0, temp, 0, count);
                    objs = temp;
                }
                selectGroup.clear();
                if (copyData.length < 1) {
                    selectRect = new Rectangle();
                } else {
                    Point pos = copyData[0].getPos();
                    pos.translate(world.x, world.y);
                    selectRect = new Rectangle(pos);
                }
                for (int i = 0; i < copyData.length; i++) {
                    Point pos = copyData[i].getPos();
                    pos.translate(world.x, world.y);
                    selectRect.add(pos);
                    copyData[i].setPos(pos);
                    selectGroup.add(copyData[i]);
                    objs[count++] = copyData[i];
                }
                selectRect.grow(20,20);
                rebuildTree();
                rebuildBeams();
                repaint();
                copyData = null;
            }
        }
    }

    /**
     * Selects every WorldObject in the World
     * Functions as a GUI callback
     */
    public void selectall() {
        selectRect = (Rectangle)bounds.clone();
        selectRect.grow(20,20);
        repaint();
    }

    /**
     * Gets the menu for the specified screen position using the MenuItems from
     * selected, if not null.
     * @param selected WorldObject to get MenuItems from
     * @param x ScreenX
     * @param y ScreenY
     * @return A PopupMenu for the location
     */
    public JPopupMenu getMenu(WorldObject selected, int x, int y) {
        JPopupMenu result = new JPopupMenu();
        if (selected == null) {
            Point pos = toWorld(x, y);
            result.add(new JMenuItem(new MethodAction("Add Emitter", this, "addObject", null, ObjectType.EMITTER, pos)));
            result.add(new JMenuItem(new MethodAction("Add Detector", this, "addObject", null, ObjectType.DETECTOR, pos)));
            result.add(new JMenuItem(new MethodAction("Add Mirror", this, "addObject", null, ObjectType.MIRROR, pos)));
            result.add(new JMenuItem(new MethodAction("Add Clock", this, "addObject", null, ObjectType.CLOCK, pos)));
            result.add(new JMenuItem(new MethodAction("Add Opaque Switch", this, "addObject", null, ObjectType.OPAQUE_BLOCK, pos)));
            result.add(new JMenuItem(new MethodAction("Add Transparent Switch", this, "addObject", null, ObjectType.TRANSPARENT_BLOCK, pos)));
            result.add(new JMenuItem(new MethodAction("Add Label", this, "addObject", null, ObjectType.LABEL, pos)));
            result.add(new JSeparator());
            result.add(new JMenuItem(new MethodAction("Select All",this,"selectall",null)));
            if (selectRect != null && selectRect.contains(toWorld(x,y))) {
                result.add(new JMenuItem(new MethodAction("Copy",this,"copypaste",new Class[]{Boolean.TYPE,Point.class},true,toWorld(x,y))));
                result.add(new JMenuItem(new MethodAction("Delete",this,"delete",null)));
            }
            result.add(new JMenuItem(new MethodAction("Paste",this,"copypaste",new Class[]{Boolean.TYPE,Point.class},false,toWorld(x,y))));
            result.add(new JSeparator());
            result.add(new JMenuItem(new MethodAction("Save State",this,"save",null)));
            result.add(new JMenuItem(new MethodAction("Load State",this,"load",null)));
        } else {
            result.add(new JMenuItem(new MethodAction("Remove", this, "removeObject", new Class[]{WorldObject.class}, selected)));
            if (selectRect != null && selectRect.contains(toWorld(x,y))) {
                result.add(new JMenuItem(new MethodAction("Copy",this,"copypaste",new Class[]{Boolean.TYPE,Point.class},true,toWorld(x,y))));
                result.add(new JMenuItem(new MethodAction("Delete",this,"delete",null)));
            }
            if (selected instanceof ControlObject) {
                result.add(new JMenuItem(new MethodAction("Toggle Link", this, "linkObject", new Class[]{WorldObject.class}, selected)));
            }
            JMenuItem[] items = selected.getMenuItems();
            if (items != null) {
                result.add(new JSeparator());
                for (int i = 0; i < items.length; i++) {
                    result.add(items[i]);
                }
            }
        }
        return result;
    }

    @Override
    public void processMouseMotionEvent(MouseEvent event) {
        if (linkingObj != null) {
            repaint();
        }
        if (event.getID() == MouseEvent.MOUSE_DRAGGED) {
            switch (clickcount) {
                case 1:
                    if (clickedObj != null) {
                        Point pos = clickedObj.getPos();
                        Point loc = toWorld(event.getX(), event.getY());
                        clickedObj.setAngle(Math.atan2(loc.y - pos.y, loc.x - pos.x));
                        rebuildBeams();
                        repaint();
                    } else {
                        Point tl = toWorld(lastRelevantMousePos.x,lastRelevantMousePos.y);
                        Point br = toWorld(event.getX(), event.getY());
                        selectRect = new Rectangle(Math.min(tl.x,br.x), Math.min(tl.y,br.y), Math.abs(br.x-tl.x), Math.abs(br.y-tl.y));
                        selectionMade = true;
                        repaint();
                    }
                    break;
                case 2:
                    if (clickedObj != null) {
                        clickedObj.setPos(toWorld(event.getX(), event.getY()));
                        rebuildBeams();
                        repaint();
                        treeInvalid = true;
                    } else {
                        Point cur = event.getPoint();
                        if (selectRect != null && selectRect.contains(toWorld(lastRelevantMousePos.x,lastRelevantMousePos.y))) {
                            int dx = (int)Math.round((cur.x - lastRelevantMousePos.x) / scale);
                            int dy = (int)Math.round((cur.y - lastRelevantMousePos.y) / scale);
                            selectRect.translate(dx,dy);
                            for (WorldObject o : selectGroup) {
                                Point p = o.getPos();
                                p.translate(dx, dy);
                                o.setPos(p.x,p.y);
                            }
                            rebuildTree();
                        } else {
                            org_x += (cur.x - lastRelevantMousePos.x) / scale;
                            org_y += (cur.y - lastRelevantMousePos.y) / scale;
                        }
                        lastRelevantMousePos = cur;
                        rebuildBeams();
                        repaint();
                    }
                    break;
            }
        }
    }

    @Override
    public void processMouseEvent(MouseEvent event) {
        switch (event.getID()) {
            case MouseEvent.MOUSE_PRESSED:
                lastRelevantMousePos = event.getPoint();
                clickedObj = objectFromPoint(event.getX(), event.getY());
                clickcount = event.getClickCount();
                repaint();
                break;
            case MouseEvent.MOUSE_CLICKED:
                switch (event.getButton()) {
                    case MouseEvent.BUTTON1:
                        if (selectRect != null && selectRect.contains(toWorld(event.getX(),event.getY()))) {
                            //Clicked inside the selectRect
                        } else {
                            selectGroup.clear();
                            selectRect = null;
                        }
                        clickedObj = objectFromPoint(event.getX(), event.getY());
                        if (linkingObj != null && clickedObj instanceof ToggleObject) {
                            if (((ControlObject)linkingObj).controlled().contains((ToggleObject) clickedObj)) {
                                ((ControlObject)linkingObj).release((ToggleObject) clickedObj);
                            } else {
                                ((ControlObject)linkingObj).control((ToggleObject) clickedObj);
                            }
                            rebuildBeams();
                            repaint();
                        }
                        linkingObj = null;
                        break;
                    case MouseEvent.BUTTON3:
                        clickedObj = objectFromPoint(event.getX(), event.getY());
                        JPopupMenu menu = getMenu(clickedObj, event.getX(), event.getY());
                        if (menu != null) {
                            menu.show(this, event.getX(), event.getY());
                        }
                        break;
                }
                break;
            case MouseEvent.MOUSE_RELEASED:
                if (selectionMade) {
                    selectGroup.clear();
                    for (int i = 0; i < count; i++) {
                        if (selectRect.contains(objs[i].getPos())) {
                            selectGroup.add(objs[i]);
                        }
                    }
                    selectionMade = false;
                }
                if (treeInvalid) {
                    rebuildTree();
                    rebuildBeams();
                    treeInvalid = false;
                }
                repaint();
        }
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        int w = getWidth();
        int h = getHeight();
        g2d.setBackground(Color.BLACK);
        g2d.clearRect(0, 0, w, h);
        g2d.translate(w / 2, h / 2);
        g2d.translate(org_x * scale, org_y * scale);
        if (selectRect != null) {
            g2d.setColor(selectionMade ? Color.LIGHT_GRAY : Color.BLUE);
            g2d.drawRect((int)selectRect.getX(), (int)selectRect.getY(), (int)selectRect.getWidth(), (int)selectRect.getHeight());
        }
        if (linkingObj != null) {
            g2d.setColor(Color.BLUE);
            Point a = ((WorldObject) linkingObj).getPos();
            Point b = getMousePosition();
            b = toWorld(b.x, b.y);
            g2d.drawLine((int) (a.x * scale), (int) (a.y * scale), (int) (b.x * scale), (int) (b.y * scale));
        }
        for (int i = 0; i < count; i++) {
            if (objs[i] instanceof ControlObject) {
                g2d.setColor(Color.DARK_GRAY);
                Point a = objs[i].getPos();
                for (ToggleObject obj : ((ControlObject) objs[i]).controlled()) {
                    Point b = ((WorldObject) obj).getPos();
                    g2d.drawLine((int) (a.x * scale), (int) (a.y * scale), (int) (b.x * scale), (int) (b.y * scale));
                }
            }
        }
        synchronized (beams) {
            for (Beam b : beams) {
                while (b != null) {
                    g2d.setColor(b.c);
                    g2d.drawLine((int) (b.org_x * scale), (int) (b.org_y), (int) ((b.org_x + b.distance * Math.cos(b.angle)) * scale), (int) ((b.org_y + b.distance * Math.sin(b.angle)) * scale));
                    b = b.child;
                }
            }
        }
        for (int i = 0; i < count; i++) {
            objs[i].draw(g2d, scale);
        }
    }

    //Basically, the different methods of reading/writing supported
    private static enum SavingStyles {
        Legacy_Blocker, Legacy_Detector, Legacy_Emitter, Legacy_Mirror, Legacy_Clock, Legacy_Label,
        Blocker, Detector, Emitter, Mirror, Clock, Label
    };
    //Maps the Class of an object to a String to be written to the file to
    //identify the data that follows, and then maps the String back to a 
    //SavingStyle so this version can decide how to read the data again.
    private static final HashMap<Class,String> write_map = new HashMap<Class,String>();
    private static final HashMap<String,SavingStyles> read_map = new HashMap<String,SavingStyles>();
    static {
        //Legacy styles that can be read
        read_map.put("B",SavingStyles.Legacy_Blocker);
        read_map.put(Blocker.class.getName(),SavingStyles.Legacy_Blocker);
        read_map.put("D",SavingStyles.Legacy_Detector);
        read_map.put(Detector.class.getName(),SavingStyles.Legacy_Detector);
        read_map.put("E",SavingStyles.Legacy_Emitter);
        read_map.put(Emitter.class.getName(),SavingStyles.Legacy_Emitter);
        read_map.put("M",SavingStyles.Legacy_Mirror);
        read_map.put(Mirror.class.getName(),SavingStyles.Legacy_Mirror);
        read_map.put("C",SavingStyles.Legacy_Clock);
        read_map.put(Clock.class.getName(),SavingStyles.Legacy_Clock);
        read_map.put("L",SavingStyles.Legacy_Label);

        //Current styles to read
        read_map.put("b",SavingStyles.Blocker);
        read_map.put("d",SavingStyles.Detector);
        read_map.put("e",SavingStyles.Emitter);
        read_map.put("m",SavingStyles.Mirror);
        read_map.put("c",SavingStyles.Clock);
        read_map.put("l",SavingStyles.Label);

        //Current styles to write
        write_map.put(Blocker.class,"b");
        write_map.put(Detector.class,"d");
        write_map.put(Emitter.class,"e");
        write_map.put(Mirror.class,"m");
        write_map.put(Clock.class,"c");
        write_map.put(Label.class,"l");
    }

    /**
     * Reads a Stream to a WorldObject array. Currently supports all previous
     * version's save states, including raw ones.
     * @param in Stream to read
     * @return A new WorldObject[] array derrived from the Stream, or null
     */
    public WorldObject[] read(InputStream in) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[0xFFFF];
            int len;
            while ((len = in.read(buffer, 0, 0xFFFF)) > 0)
                out.write(buffer,0,len);
            byte[] data = out.toByteArray();
            DataInputStream din;
            if (new String(data,0,9).equals("LASERSv2:")) {
                data = ClipboardAccess.decode(new String(data,9,data.length-9));
                din = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(data)));
            } else if (new String(data,0,9).equals("LASERSv1:")) {
                data = ClipboardAccess.decode(new String(data,9,data.length-9));
                din = new DataInputStream(new ByteArrayInputStream(data));
            } else {
                din = new DataInputStream(new ByteArrayInputStream(data));
            }
            int num = din.readInt();
            WorldObject[] array = new WorldObject[num];
            for (int i = 0; i < num; i++) {
                String type = din.readUTF();
                switch (read_map.get(type)) {
                    case Blocker:
                        array[i] = Blocker.read(din,this);
                        break;
                    case Detector:
                        array[i] = Detector.read(din,this);
                        break;
                    case Emitter:
                        array[i] = Emitter.read(din,this);
                        break;
                    case Mirror:
                        array[i] = Mirror.read(din,this);
                        break;
                    case Clock:
                        array[i] = Clock.read(din,this);
                        break;
                    case Label:
                        array[i] = Label.read(din,this);
                        break;
                    case Legacy_Blocker:
                        array[i] = Blocker.read_legacy(din,this);
                        break;
                    case Legacy_Detector:
                        array[i] = Detector.read_legacy(din,this);
                        break;
                    case Legacy_Emitter:
                        array[i] = Emitter.read_legacy(din,this);
                        break;
                    case Legacy_Mirror:
                        array[i] = Mirror.read_legacy(din,this);
                        break;
                    case Legacy_Clock:
                        array[i] = Clock.read_legacy(din,this);
                        break;
                    case Legacy_Label:
                        array[i] = Label.read_legacy(din,this);
                        break;
                }
            }
            int numcontrol = din.readInt();
            for (int i = 0; i < numcontrol; i++) {
                ControlObject obj = (ControlObject)array[din.readInt()];
                int numtoggle = din.readInt();
                for (int c = 0; c < numtoggle; c++) {
                    obj.control((ToggleObject)array[din.readInt()]);
                }
            }
            din.close();
            return array;
        } catch (Exception e) {
            e.printStackTrace();
            return new WorldObject[0];
        } finally {
            try {
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Writes a WorldObject array to a Stream as a GZipped and Base64 encoded
     * representation of the array. Currently, this method handles ControlObject
     * links on its own, so ControlObjects should not attempt to save their
     * controlled states, it will all be done here
     * @param out Stream to write to
     * @param array Array to export
     */
    public void write(OutputStream out, WorldObject[] array) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream dout = new DataOutputStream(new GZIPOutputStream(bytes));
            dout.writeInt(array.length);
            for (int i = 0; i < array.length; i++) {
                String name = write_map.get(array[i].getClass());
                dout.writeUTF(name);
                switch (read_map.get(name)) {
                    case Blocker:
                        Blocker.write(dout,(Blocker)array[i]);
                        break;
                    case Detector:
                        Detector.write(dout,(Detector)array[i]);
                        break;
                    case Emitter:
                        Emitter.write(dout,(Emitter)array[i]);
                        break;
                    case Mirror:
                        Mirror.write(dout,(Mirror)array[i]);
                        break;
                    case Clock:
                        Clock.write(dout,(Clock)array[i]);
                        break;
                    case Label:
                        Label.write(dout,(Label)array[i]);
                        break;
                }
            }
            HashMap<Integer,LinkedList<Integer>> control_map = new HashMap<Integer,LinkedList<Integer>>();
            for (int i = 0; i < array.length; i++) {
                if (array[i] instanceof ControlObject) {
                    LinkedList<Integer> toggles = new LinkedList<Integer>();
                    for (ToggleObject obj : ((ControlObject)array[i]).controlled()) {
                        for (int c = 0; c < array.length; c++) {
                            if (array[c] == obj) {
                                toggles.add(c);
                            }
                        }
                    }
                    control_map.put(i,toggles);
                }
            }
            dout.writeInt(control_map.size());
            for (Map.Entry<Integer,LinkedList<Integer>> entry : control_map.entrySet()) {
                dout.writeInt(entry.getKey());
                LinkedList<Integer> elems = entry.getValue();
                dout.writeInt(elems.size());
                for (Integer i : elems) {
                    dout.writeInt(i);
                }
            }
            dout.close();
            byte[] data = bytes.toByteArray();
            out.write("LASERSv2:".getBytes());
            out.write(new String(ClipboardAccess.encode(data)).getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
