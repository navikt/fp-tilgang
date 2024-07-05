package no.nav.foreldrepenger.tilganger.utils;

import java.util.regex.Pattern;

public class RegexUtils {
    private static final String NAVIDENT_REGEX = "^[a-zA-Z]\\d{6}$";
    public static final Pattern NAVIDENT_PATTERN = Pattern.compile(NAVIDENT_REGEX);

    private RegexUtils() {
    }
}
