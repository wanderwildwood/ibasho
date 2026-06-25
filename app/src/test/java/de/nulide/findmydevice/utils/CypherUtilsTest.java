package de.nulide.findmydevice.utils;

import org.junit.Assert;
import org.junit.Test;

public class CypherUtilsTest {

    @Test
    public void testToHex() {
        String actual = CypherUtils.toHex("AAAAZZZZ".getBytes());
        String expected = "414141415a5a5a5a";
        Assert.assertEquals(expected, actual);
    }

}
