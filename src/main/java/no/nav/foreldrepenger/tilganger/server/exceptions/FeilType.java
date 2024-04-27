package no.nav.foreldrepenger.tilganger.server.exceptions;

public enum FeilType {
    MANGLER_TILGANG_FEIL,
    TOMT_RESULTAT_FEIL,
    GENERELL_FEIL;

    @Override
    public String toString() {
        return name();
    }
}
