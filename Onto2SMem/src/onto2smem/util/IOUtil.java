/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package onto2smem.util;

import java.io.Reader;
import java.io.Writer;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class IOUtil {
    /**
     * 
     * @param r
     */
    public static void safeClose(Reader r) {
        try {
            r.close();
        } catch (Exception e) { /* nope */ }
    }
    /**
     * 
     * @param w
     */
    public static void safeClose(Writer w) {
        try {
            w.flush();
        } catch (Exception e) { /* nope */ }
        try {
            w.close();
        } catch (Exception e) { /* nope */ }
    }
    
    
}
