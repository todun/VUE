 /*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package tufts.vue;

import java.io.File;
import java.util.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import javax.swing.*;
import javax.swing.border.*;

public class VueUtil extends tufts.Util
{
    private static String currentDirectoryPath = "";
    
    public static void openURL(String platformURL)
        throws java.io.IOException
    {
        // todo: spawn this in another thread just in case it hangs
        
        System.err.println("Opening URL [" + platformURL + "]");
        
        if (platformURL.endsWith(VueResources.getString("vue.extension"))) {
            if (platformURL.startsWith("resource:")) {
                java.net.URL url = VueResources.getURL(platformURL.substring(9));
                VUE.displayMap(tufts.vue.action.OpenAction.loadMap(url));
            }
            try {
                tufts.vue.VUE.displayMap(new File(new java.net.URL(platformURL).getFile()));
            } catch(Exception ex) {
                ex.printStackTrace();
                tufts.Util.openURL(platformURL);
            }
        } else {
            tufts.Util.openURL(platformURL);
        }
    }
    
    public static void  setCurrentDirectoryPath(String cdp) {
        currentDirectoryPath = cdp;
    }
    
    public static String getCurrentDirectoryPath() {
        return currentDirectoryPath;
    }    
    
    public static boolean isCurrentDirectoryPathSet() {
        if(currentDirectoryPath.equals("")) 
            return false;
        else
            return true;
    }

    public static File getDefaultUserFolder() {
        File userHome = new File(System.getProperty("user.home"));
        if(userHome == null) 
            userHome = new File(System.getProperty("java.io.tmpdir"));
        final String vueUserDir = isWindowsPlatform() ? "vue" : ".vue";
        File userFolder = new File(userHome.getPath() + File.separatorChar + vueUserDir);
        if(userFolder.isDirectory())
            return userFolder;
        if(!userFolder.mkdir())
            throw new RuntimeException(userFolder.getAbsolutePath()+":cannot be created");
        return userFolder;
    }
    
    public static void deleteDefaultUserFolder() {
        File userFolder = getDefaultUserFolder();
        File[] files = userFolder.listFiles();
        System.out.println("file count = "+files.length);
        for(int i = 0; i<files.length;i++) {
            if(files[i].isFile() && !files[i].delete()) 
                throw new RuntimeException(files[i].getAbsolutePath()+":cannot be created");
        }
        if(!userFolder.delete()) 
             throw new RuntimeException(userFolder.getAbsolutePath()+":cannot be deleted");
    }

    static final double sFactor = 0.9;
    public static Color darkerColor(Color c) {
        return factorColor(c, sFactor);
    }
    public static Color brighterColor(Color c) {
        return factorColor(c, 1/sFactor);
    }
    public static Color factorColor(Color c, double factor)
    {
	return new Color((int)(c.getRed()  *factor),
			 (int)(c.getGreen()*factor),
			 (int)(c.getBlue() *factor));
    }


    /**
     * Compute the intersection point of two lines, as defined
     * by two given points for each line.
     * This already assumes that we know they intersect somewhere (are not parallel), 
     */

    public static float[] computeLineIntersection
        (float s1x1, float s1y1, float s1x2, float s1y2,
         float s2x1, float s2y1, float s2x2, float s2y2, float[] result)
    {
        // We are defining a line here using the formula:
        // y = mx + b  -- m is slope, b is y-intercept (where crosses x-axis)
        
        boolean m1vertical = (s1x1 == s1x2);
        boolean m2vertical = (s2x1 == s2x2);
        float m1 = Float.NaN;
        float m2 = Float.NaN;
        if (!m1vertical)
            m1 = (s1y1 - s1y2) / (s1x1 - s1x2);
        if (!m2vertical)
            m2 = (s2y1 - s2y2) / (s2x1 - s2x2);
        
        // Solve for b using any two points from each line.
        // to solve for b:
        //      y = mx + b
        //      y + -b = mx
        //      -b = mx - y
        //      b = -(mx - y)
        // float b1 = -(m1 * s1x1 - s1y1);
        // float b2 = -(m2 * s2x1 - s2y1);
        // System.out.println("m1=" + m1 + " b1=" + b1);
        // System.out.println("m2=" + m2 + " b2=" + b2);

        // if EITHER line is vertical, the x value of the intersection
        // point will obviously have to be the x value of any point
        // on the vertical line.
        
        float x = 0;
        float y = 0;
        if (m1vertical) {   // first line is vertical
            //System.out.println("setting X to first vertical at " + s1x1);
            float b2 = -(m2 * s2x1 - s2y1);
            x = s1x1; // set x to any x point from the first line
            // using y=mx+b, compute y using second line
            y = m2 * x + b2;
        } else {
            float b1 = -(m1 * s1x1 - s1y1);
            if (m2vertical) { // second line is vertical (has no slope)
                //System.out.println("setting X to second vertical at " + s2x1);
                x = s2x1; // set x to any point from the second line
            } else {
                // second line has a slope (is not veritcal: m is valid)
                float b2 = -(m2 * s2x1 - s2y1);
                x = (b2 - b1) / (m1 - m2);
            }
            // using y=mx+b, compute y using first line
            y = m1 * x + b1;
        }
        //System.out.println("x=" + x + " y=" + y);

        result[0] = x;
        result[1] = y;
        return result;
    }

    public static final float[] NoIntersection = { Float.NaN, Float.NaN, Float.NaN, Float.NaN };
    private static final String[] SegTypes = { "MOVEto", "LINEto", "QUADto", "CUBICto", "CLOSE" }; // for debug
    
    public static float[] computeIntersection(float rayX1, float rayY1,
                                              float rayX2, float rayY2,
                                              java.awt.Shape shape)
    {
        return computeIntersection(rayX1,rayY1, rayX2,rayY2, shape, new float[2], 1);
    }
    
    /**
     * Compute the intersection of an arbitrary shape and a line segment
     * that is assumed to pass throught the shape.  Usually used
     * with an endpoint (rayX2,rayY2) that ends in the center of the
     * shape, tho that's not required.
     *
     * @param max - max number of intersections to compute. An x/y
     * pair of coords will put into result up to max times. Must be >= 1.
     *
     * @return float array of size 2: x & y values of intersection,
     * or ff no intersection, returns Float.NaN values for x/y.
     */
    public static float[] computeIntersection(float rayX1, float rayY1,
                                              float rayX2, float rayY2,
                                              java.awt.Shape shape,
                                              float[] result, int max)
    {
        java.awt.geom.PathIterator i = shape.getPathIterator(null);
        // todo performance: if this shape has no curves (CUBICTO or QUADTO)
        // this flattener is redundant.  Also, it would be faster to
        // actually do the math for arcs and compute the intersection
        // of the arc and the line, tho we can save that for another day.
        i = new java.awt.geom.FlatteningPathIterator(i, 0.5);
        
        float[] seg = new float[6];
        float firstX = 0f;
        float firstY = 0f;
        float lastX = 0f;
        float lastY = 0f;
        int cnt = 0;
        int hits = 0;
        while (!i.isDone()) {
            int segType = i.currentSegment(seg);
            if (cnt == 0) {
                firstX = seg[0];
                firstY = seg[1];
            } else if (segType == PathIterator.SEG_CLOSE) {
                seg[0] = firstX; 
                seg[1] = firstY; 
            }
            float endX = seg[0];
            float endY = seg[1];
                
            // at cnt == 0, we have only the first point from the path iterator, and so no line yet.
            if (cnt > 0 && Line2D.linesIntersect(rayX1, rayY1, rayX2, rayY2, lastX, lastY, seg[0], seg[1])) {
                //System.out.println("intersection at segment #" + cnt + " " + SegTypes[segType]);
                if (max <= 1) {
                    return computeLineIntersection(rayX1, rayY1, rayX2, rayY2, lastX, lastY, seg[0], seg[1], result);
                } else {
                    float[] tmp = computeLineIntersection(rayX1, rayY1, rayX2, rayY2, lastX, lastY, seg[0], seg[1], new float[2]);
                    result[hits*2 + 0] = tmp[0];
                    result[hits*2 + 1] = tmp[1];
                    if (++hits >= max)
                        return result;
                }
            }
            cnt++;
            lastX = endX;
            lastY = endY;
            i.next();
        }
        return NoIntersection;
    }

    /** compute the first two y value crossings of the given x_axis and shape */
    public static float[] computeYCrossings(float x_axis, Shape shape, float[] result) {
        return computeIntersection(x_axis, Integer.MIN_VALUE, x_axis, Integer.MAX_VALUE, shape, result, 2);
    }
    
    /** compute 2 y values for crossings of at x_axis, and store result in the given Line2D */
    public static Line2D computeYCrossings(float x_axis, Shape shape, Line2D result) {
        float[] coords = computeYCrossings(x_axis, shape, new float[4]);
        result.setLine(x_axis, coords[1], x_axis, coords[3]);
        return result;
    }
    
    /**
     * This will clip the given vertical line to the edges of the given shape.
     * Assumes line start is is min y (top), line end is max y (bottom).
     * @param line - line to clip y values if outside edge of given shape
     * @param shape - shape to clip line to
     * @param pad - padding: keep line endpoints at least this many units away from shape edge
     *
     * todo: presumes only 2 crossings: will only handle concave polygons
     * Should be relatively easy to extend this to work for non-vertical lines if the need arises.
     */
    public static Line2D clipToYCrossings(Line2D line, Shape shape, float pad)
    {
        float x_axis = (float) line.getX1();
        float[] coords = computeYCrossings(x_axis, shape, new float[4]);
        // coords[0] & coords[2], the x values, can be ignored, as they always == x_axis

        if (coords.length < 4) {
            // TODO FIX: if line is outside edge of shape, we're screwed (see d:/test-layout.vue)
            // TODO: we were getting this of NoIntersection being returned (which was only of size
            // 2, and thus give us array bounds exceptions below) -- do we need to do anything
            // here to make sure the NoIntersection case is handled more smoothly?
            System.err.println("clip error " + coords);
            new Throwable("CLIP ERROR shape=" + shape).printStackTrace();
            return null;
        }

        float upper; // y value at top
        float lower; // y value at bottom
        if (coords[1] < coords[3]) {
            // cross1 is min cross (top), cross2 is max cross (bottom)
            upper = coords[1];
            lower = coords[3];
        } else {
            // cross2 is min cross (top), cross1 is max cross (bottom)
            upper = coords[3];
            lower = coords[1];
        }
        upper += pad;
        lower -= pad;
        // clip line to upper & lower (top & bottom)
        float y1 = Math.max(upper, (float) line.getY1());
        float y2 = Math.min(lower, (float) line.getY2());
        line.setLine(x_axis, y1, x_axis, y2);
        return line;
    }
    

    /**
     * On a line drawn from the center of c1 to the center of c2, compute the the line segment
     * from the intersection at the edge of shape c1 to the intersection at the edge of shape c2.
     */
    
    public static Line2D.Float computeConnector(LWComponent c1, LWComponent c2, Line2D.Float result)
    {
        float segX1 = c1.getCenterX();
        float segY1 = c1.getCenterY();
        float segX2 = c2.getCenterX();
        float segY2 = c2.getCenterY();

        // compute intersection at shape 2 of ray from center of shape 1 to center of shape 2
        //float[] intersection_at_2 = computeIntersection(segX1, segY1, segX2, segY2, c2.getShape());
        // compute intersection at shape 1 of ray from center of shape 2 to center of shape 1
        //float[] intersection_at_1 = computeIntersection(segX2, segY2, segX1, segY1, c1.getShape());

        // compute intersection at shape 1 of ray from center of shape 1 to center of shape 2
        float[] intersection_at_1 = computeIntersection(segX1, segY1, segX2, segY2, c1.getShape());
        // compute intersection at shape 2 of ray from center of shape 2 to center of shape 1
        float[] intersection_at_2 = computeIntersection(segX2, segY2, segX1, segY1, c2.getShape());

        if (intersection_at_1 == NoIntersection) {
            // default to center of component 1
            result.x1 = segX1;
            result.y1 = segY1;
        } else {
            result.x1 = intersection_at_1[0];
            result.y1 = intersection_at_1[1];
        }
        
        if (intersection_at_2 == NoIntersection) {
            // default to center of component 2
            result.x2 = segX2;
            result.y2 = segY2;
        } else {
            result.x2 = intersection_at_2[0];
            result.y2 = intersection_at_2[1];
        }

        //System.out.println("connector: " + out(result));
        //System.out.println("\tfrom: " + c1);
        //System.out.println("\t  to: " + c2);
        
        return result;
    }
    
    public static void alert(javax.swing.JComponent component,String message,String title) {
        javax.swing.JOptionPane.showMessageDialog(component,message,title,javax.swing.JOptionPane.ERROR_MESSAGE,VueResources.getImageIcon("vueIcon32x32"));                                      
    }
   
    public static void alert(String message,String title) {
        javax.swing.JOptionPane.showMessageDialog(VUE.getRootParent(),message,title,javax.swing.JOptionPane.ERROR_MESSAGE,VueResources.getImageIcon("vueIcon32x32"));                                      
    }
   
    public static int confirm(String message,String title) {
       return JOptionPane.showConfirmDialog(VUE.getRootParent(),message,title,JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE,VueResources.getImageIcon("vueIcon32x32"));
    }
    
    public static int confirm(javax.swing.JComponent component, String message, String title) {
        return JOptionPane.showConfirmDialog(component,message,title,JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE,VueResources.getImageIcon("vueIcon32x32"));
    }
    
               
        
    
}
