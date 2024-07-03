package no.nav.foreldrepenger.tilganger.domene.cache;

import java.util.Optional;

public interface Cache<T> {
    void store(String key, T value);
    Optional<T> read(String key);
}
