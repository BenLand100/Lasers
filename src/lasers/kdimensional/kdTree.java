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

import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import static java.lang.Math.*;

/**
 * An arbitrarty dimensional sorted tree. The 1D version functions the same as a
 * binary tree. E.G., this class abstracts a binary tree to higher dimensions.
 *
 * @author Benjamin J. Land
 */
public class kdTree<T> {

    private TreeNode<T> root;
    private final int dimension;

    public kdTree(kdPoint[] points, T[] data) {
        if (points.length != data.length)
            throw new RuntimeException("Array size mismatch");
        Entry<T>[] res = new Entry[points.length];
        for (int i = 0; i < points.length; i++) {
            res[i] = new Entry(new kdPoint(points[i]),data[i]);
        }
        dimension = (points.length > 0) ? points[0].dimension() : 0;
        root = dimension > 0 ? tree(res, 0, points.length - 1, 0) : null;
    }

    public kdTree(Point[] points, T[] data) {
        if (points.length != data.length)
            throw new RuntimeException("Array size mismatch");
        Entry<T>[] res = new Entry[points.length];
        for (int i = 0; i < points.length; i++) {
            res[i] = new Entry(new kdPoint(points[i]),data[i]);
        }
        dimension = 2;
        root = tree(res, 0, points.length - 1, 0);
    }

    public kdTree(Point2D[] points, T[] data) {
        if (points.length != data.length)
            throw new RuntimeException("Array size mismatch");
        Entry<T>[] res = new Entry[points.length];
        for (int i = 0; i < points.length; i++) {
            res[i] = new Entry(new kdPoint(points[i]),data[i]);
        }
        dimension = 2;
        root = tree(res, 0, points.length - 1, 0);
    }

    public kdTree(kdPoint[] points) {
        Entry<T>[] res = new Entry[points.length];
        for (int i = 0; i < points.length; i++) {
            res[i] = new Entry(new kdPoint(points[i]),null);
        }
        dimension = (points.length > 0) ? points[0].dimension() : 0;
        root = dimension > 0 ? tree(res, 0, points.length - 1, 0) : null;
    }

    public kdTree(Point[] points) {
        Entry<T>[] res = new Entry[points.length];
        for (int i = 0; i < points.length; i++) {
            res[i] = new Entry(new kdPoint(points[i]),null);
        }
        dimension = 2;
        root = tree(res, 0, points.length - 1, 0);
    }

    public kdTree(Point2D[] points) {
        Entry<T>[] res = new Entry[points.length];
        for (int i = 0; i < points.length; i++) {
            res[i] = new Entry(new kdPoint(points[i]),null);
        }
        dimension = 2;
        root = tree(res, 0, points.length - 1, 0);
    }

    private static class nnSearchData<T> {
        //k is the number of points to find
        int n;
        //maxDist is the maximum distance a point can be to be a NN
        double maxDist;
        //map keeps an ordered map of distances vs kdPoints
        TreeMap<Double, Entry<T>> map = new TreeMap<Double, Entry<T>>();
    }

    private static class rangeSearchData<T> {
        //range is the range to search in
        double range;
        //map keeps an ordered map of distances vs kdPoints
        TreeMap<Double, Entry<T>> map = new TreeMap<Double, Entry<T>>();
    }

    public static class Entry<T> {
        public final kdPoint point;
        public final T data;
        public Entry(kdPoint point, T data) {
            this.point = new kdPoint(point);
            this.data = data;
        }
        public Entry(Entry<T> entry) {
            this(entry.point, entry.data);
        }
    }

    public static class TreeNode<T> {
        //for fast back propogation
        public TreeNode<T> parent = null;
        //the entry at this node
        public Entry<T> data = null;
        //left is the lesser or equal side of the tree
        public TreeNode<T> left = null;
        //right is the greater side of the tree
        public TreeNode<T> right = null;
    }

    public int getDimension() {
        return dimension;
    }

    public TreeNode<T> getTreeRoot() {
        return root;
    }

    public TreeMap<Double, Entry<T>> rangeSearch(double r, kdPoint pt) {
        if (pt.dimension() != dimension) throw new RuntimeException("Dimensions of Point does not equal dimension of Tree");
        rangeSearchData d = new rangeSearchData();
        d.range = r;
        rangeSearch(d, root, null, pt, 0);
        return d.map;
    }

    public TreeMap<Double, Entry<T>> rangeSearch(double r, double... mag) {
        return rangeSearch(r,new kdPoint(mag));
    }

    private void rangePropUp(rangeSearchData<T> data, TreeNode<T> n, TreeNode<T> top, kdPoint s, int depth) {
        if (n == top) return;
        double dist = s.dist(n.data.point);
        if (dist <= data.range) {
            data.map.put(dist, new Entry(n.data));
        }
        int dim = depth % dimension;
        if (abs(n.data.point.mag[dim] - s.mag[dim]) <= data.range) {
            if (s.mag[dim] <= n.data.point.mag[dim]) {
                if (n.right != null) {
                    rangeSearch(data, n.right, n, s, depth + 1);
                }
            } else {
                if (n.left != null) {
                    rangeSearch(data, n.left, n, s, depth + 1);
                }
            }
        }
        rangePropUp(data, n.parent, top, s, depth - 1);
    }

    private void rangeSearch(rangeSearchData data, TreeNode<T> n, TreeNode<T> top, kdPoint s, int depth) {
        int dim = depth % dimension;
        if (n.left != null && s.mag[dim] <= n.data.point.mag[dim]) {
            rangeSearch(data, n.left, top, s, depth + 1);
        } else if (n.right != null && s.mag[dim] > n.data.point.mag[dim]) {
            rangeSearch(data, n.right, top, s, depth + 1);
        } else {
            rangePropUp(data, n, top, s, depth);
        }
    }

    public TreeMap<Double, Entry<T>> nnSearch(int n, kdPoint pt) {
        if (pt.dimension() != dimension) throw new RuntimeException("Dimensions of Point does not equal dimension of Tree");
        nnSearchData d = new nnSearchData();
        d.n = n;
        nnSearch(d, root, null, pt, 0);
        return d.map;
    }

    public TreeMap<Double, Entry<T>> nnSearch(int n, double... mag) {
        return nnSearch(n,new kdPoint(mag));
    }

    private void nnPropUp(nnSearchData<T> data, TreeNode<T> n, TreeNode<T> top, kdPoint s, int depth) {
        if (n == top) return;
        double dist = s.dist(n.data.point);
        if (data.map.size() < data.n) {
            data.map.put(dist, new Entry(n.data));
            data.maxDist = max(data.maxDist, dist);
        } else if (dist < data.maxDist) {
            data.map.put(dist, new Entry(n.data));
            data.map.remove(data.map.lastKey());
            data.maxDist = data.map.lastKey();
        }
        int dim = depth % dimension;
        if (data.map.size() < data.n || abs(n.data.point.mag[dim] - s.mag[dim]) < data.maxDist) {
            if (s.mag[dim] <= n.data.point.mag[dim]) {
                if (n.right != null) {
                    nnSearch(data, n.right, n, s, depth + 1);
                }
            } else {
                if (n.left != null) {
                    nnSearch(data, n.left, n, s, depth + 1);
                }
            }
        }
        nnPropUp(data, n.parent, top, s, depth - 1);
    }

    private void nnSearch(nnSearchData data, TreeNode<T> n, TreeNode<T> top, kdPoint s, int depth) {
        int dim = depth % dimension;
        if (n.left != null && s.mag[dim] <= n.data.point.mag[dim]) {
            nnSearch(data, n.left, top, s, depth + 1);
        } else if (n.right != null && s.mag[dim] > n.data.point.mag[dim]) {
            nnSearch(data, n.right, top, s, depth + 1);
        } else {
            nnPropUp(data, n, top, s, depth);
        }
    }

    private void sortDim(Entry<T>[] sort, int left, int right, int pivot, int dim) {
        if (right > left) {
            Entry<T> temp = sort[pivot];
            sort[pivot] = sort[right];
            sort[right] = temp;
            double mid = temp.point.mag[dim];
            int store = left;
            for (int i = left; i < right; i++) {
                if (sort[i].point.mag[dim] <= mid) {
                    temp = sort[i];
                    sort[i] = sort[store];
                    sort[store] = temp;
                    store++;
                }
            }
            temp = sort[store];
            sort[store] = sort[right];
            sort[right] = temp;
            sortDim(sort, left, store - 1, (left + store - 1) / 2, dim);
            sortDim(sort, store + 1, right, (right + store + 1) / 2, dim);
        }
    }

    private TreeNode<T> tree(Entry<T>[] data) {
        return tree(data,0,data.length-1,0);
    }

    private TreeNode<T> tree(Entry<T>[] data, int left, int right, int depth) {
        if (left == right) {
            TreeNode<T> t = new TreeNode<T>();
            t.data = data[left];
            return t;
        }
        int dim =  depth % dimension;
        int mid = (left + right) / 2;
        sortDim(data, left, right, mid, dim);
        TreeNode<T> t = new TreeNode<T>();
        if (right - left == 1) {
            t.data = data[right];
            TreeNode<T> r = new TreeNode<T>();
            t.left = r;
            t.left.data = data[left];
            t.left.parent = t;
        } else {
            while (mid < right-1 && data[mid].point.mag[dim] >= data[mid+1].point.mag[dim]) mid++;
            t.data = data[mid];
            t.left = tree(data, left, mid - 1, depth + 1);
            if (t.left != null) {
                t.left.parent = t;
            }
            t.right = tree(data, mid + 1, right, depth + 1);
            if (t.right != null) {
                t.right.parent = t;
            }
        }
        return t;
    }
}