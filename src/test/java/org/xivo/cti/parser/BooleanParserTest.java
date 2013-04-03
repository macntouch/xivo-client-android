package org.xivo.cti.parser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BooleanParserTest {

    @Test
    public void isTrue() {
        assertTrue("cannot parse ",BooleanParser.parse("yes"));
        assertTrue("cannot parse ",BooleanParser.parse("Yes"));
        assertTrue("cannot parse ",BooleanParser.parse("true"));
        assertTrue("cannot parse ",BooleanParser.parse("1"));
    }
    
    @Test
    public void isFalse() {
        assertFalse("cannot parse",BooleanParser.parse("UU"));
    }

}
