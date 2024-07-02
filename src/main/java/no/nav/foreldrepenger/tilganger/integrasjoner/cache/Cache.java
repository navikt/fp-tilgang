package no.nav.foreldrepenger.tilganger.integrasjoner.cache;

import java.util.Optional;

public interface Cache {
    void store(String key, String value);
    Optional<String> read(String key);
}
