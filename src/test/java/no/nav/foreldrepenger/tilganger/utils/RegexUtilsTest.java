package no.nav.foreldrepenger.tilganger.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RegexUtilsTest {

    @Test
    void navidentMatchTest() {
        assertTrue(RegexUtils.NAVIDENT_PATTERN.matcher("X123456").matches());
        assertTrue(RegexUtils.NAVIDENT_PATTERN.matcher("x123456").matches());
        assertTrue(RegexUtils.NAVIDENT_PATTERN.matcher("w000000").matches());
        assertTrue(RegexUtils.NAVIDENT_PATTERN.matcher("y999999").matches());
    }

    @Test
    void navidentNoMatchTest() {
        assertFalse(RegexUtils.NAVIDENT_PATTERN.matcher("X1234561").matches());
        assertFalse(RegexUtils.NAVIDENT_PATTERN.matcher("å123456").matches());
        assertFalse(RegexUtils.NAVIDENT_PATTERN.matcher("1111111").matches());
        assertFalse(RegexUtils.NAVIDENT_PATTERN.matcher("aaaaaaa").matches());
        assertFalse(RegexUtils.NAVIDENT_PATTERN.matcher("X12345").matches());
    }
}
