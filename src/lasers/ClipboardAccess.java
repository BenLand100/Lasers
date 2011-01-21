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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

/**
 * Contains static methods for base64 encoding/decoding, as well as instance
 * methods for getting and putting Strings from/to the System clipboard.
 *
 * @author benland100
 */
public class ClipboardAccess implements ClipboardOwner {

    private static char[] map1 = new char[64];
    static {
        int i = 0;
        for (char c = 'A'; c <= 'Z'; c++) map1[i++] = c;
        for (char c = 'a'; c <= 'z'; c++) map1[i++] = c;
        for (char c = '0'; c <= '9'; c++) map1[i++] = c;
        map1[i++] = '+';
        map1[i++] = '/';
    }

    private static byte[] map2 = new byte[128];
    static {
        for (int i = 0; i < map2.length; i++) map2[i] = -1;
        for (int i = 0; i < 64; i++) map2[map1[i]] = (byte) i;
    }

    public static String encode(byte[] in) {
        int size = in.length;
        int nopad = (size * 4 + 2) / 3;
        int len = ((size + 2) / 3) * 4;
        char[] out = new char[len];
        int op = 0;
        for (int i = 0; i < size; ) {
            int i0 = in[i++] & 0xff;
            int i1 = i < size ? in[i++] & 0xff : 0;
            int i2 = i < size ? in[i++] & 0xff : 0;
            int o0 = i0 >>> 2;
            int o1 = ((i0 & 3) << 4) | (i1 >>> 4);
            int o2 = ((i1 & 0xf) << 2) | (i2 >>> 6);
            int o3 = i2 & 0x3F;
            out[op++] = map1[o0];
            out[op++] = map1[o1];
            out[op] = op < nopad ? map1[o2] : '=';
            op++;
            out[op] = op < nopad ? map1[o3] : '=';
            op++;
        }
        return new String(out);
    }

    public static byte[] decode(String str) {
        char[] in = str.toCharArray();
        int len = in.length;
        if (len % 4 != 0) throw new IllegalArgumentException("Length of Base64 encoded input string is not a multiple of 4.");
        while (len > 0 && in[len - 1] == '=') len--;
        int size = (len * 3) / 4;
        byte[] out = new byte[size];
        int op = 0;
        for (int i = 0; i < len; ) {
            int a = in[i++];
            int b = in[i++];
            int c = i < len ? in[i++] : 'A';
            int d = i < len ? in[i++] : 'A';
            if (a > 127 || b > 127 || c > 127 || d > 127) {
                throw new IllegalArgumentException("Illegal character in Base64 encoded data.");
            }
            a = map2[a];
            b = map2[b];
            c = map2[c];
            d = map2[d];
            if (a < 0 || b < 0 || c < 0 || d < 0) {
                throw new IllegalArgumentException("Illegal character in Base64 encoded data.");
            }
            out[op++] = (byte) ((a << 2) | (b >>> 4));
            if (op < size) out[op++] = (byte) (((b & 0xf) << 4) | (c >>> 2));
            if (op < size) out[op++] = (byte) (((c & 3) << 6) | d);
        }
        return out;
    }

    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }

    public void put(String str) {
        StringSelection stringSelection = new StringSelection(str);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, this);
    }

    public String get() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                return (String) contents.getTransferData(DataFlavor.stringFlavor);
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }
        return "";
    }
}
