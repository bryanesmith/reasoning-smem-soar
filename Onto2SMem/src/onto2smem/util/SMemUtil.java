/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package onto2smem.util;

import java.util.regex.Pattern;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class SMemUtil {
    
    /**
     * 
     */
    public static final String LOCAL_NAME_PREDICATE = "^localname";
    
    /**
     * 
     */
    public static final String NAME_PREDICATE = "^name";
    
    /**
     * 
     * @param val
     * @return
     */
    public static String chomp(String val) {
        if (val != null) {
            return val.replaceAll(Pattern.quote("\n"), " ").replaceAll("\\s+", " ");
        }
        return val;
    }
    
    /**
     * <p>An uninformed estimate of the SMem string literal character limit.</p>
     */
    final static int STR_LEN_LIMIT = 90;
    
     /**
     * <p>Escapes and quotes a string literal for SMem.</p>
     * @param object
     * @return
     */
    public static String safeQuoteString(String object) {

        if (object == null || object.length() == 0) {
            return "|[EMPTY]|";
        }
        
        if (object.contains("\"")) {
            object = object.replaceAll("\"", "[QUOTE]");
        }
        if (object.contains("|")) {
            object = object.replaceAll("|", "[VBAR]");
        }
        if (object.contains("(")) {
            object = object.replaceAll(Pattern.quote("("), "[LPAREN]");
        }
        if (object.contains(")")) {
            object = object.replaceAll(Pattern.quote(")"), "[RPAREN]");
        }
        if (object.contains("\n")) {
            object = object.replaceAll(Pattern.quote("\n"), "[NEWLINE]");
        }
        
        // What is the limit? Long comments cause problems
        if (object.length() > SMemUtil.STR_LEN_LIMIT) {
            object = object.substring(0, SMemUtil.STR_LEN_LIMIT - 3) + "...";
        }
        
        if (object.charAt(0) != '|' && object.charAt(object.length() - 1) != '|') {
            object = "|" + object + "|";
        }

        return object;
    }
    
    public static String safeQuoteLiteral(String object) {
        
        if (object != null) {
            // Remove type information
            final String typePrefix = "^^xsd";
            if (object.contains(typePrefix)) {
                object = object.substring(0, object.indexOf(typePrefix));
            }
            
            final String enLang = "@en";
            if (object.endsWith(enLang)) {
                object = object.substring(0, object.length()-enLang.length());
            }
        }
        
        return safeQuoteString(object);
    }

    /**
     * 
     * @param uri
     * @return
     */
    public static String parseNameFromURI(String uri) {

        if (!uri.contains("#")) {
            return uri;
        }

        return uri.substring(uri.lastIndexOf("#")+1);
    }
    
    /**
     * 
     * @param predicateStr
     * @return
     */
    public static String safeQuotePredicate(String predicateStr) {
        // Check if starts with file name
        final String fileExtension = ".owl";
        if (predicateStr.contains(fileExtension)) {
            int startingIndex = predicateStr.indexOf(fileExtension) + fileExtension.length();
            return predicateStr.substring(startingIndex, predicateStr.length());
        }
        
        return predicateStr;
    }
}
