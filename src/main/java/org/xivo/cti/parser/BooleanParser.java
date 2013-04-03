package org.xivo.cti.parser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BooleanParser {

    private static final Set<String> yesSet = new HashSet<String>( Arrays.asList( new String[] {
            "yes","true","1"
          } ) );
    
    public static boolean parse(String condition) {
        return yesSet.contains(condition.toLowerCase());
    }

}
