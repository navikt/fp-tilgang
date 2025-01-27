package no.nav.foreldrepenger.tilganger.domene.populasjon;

public record TilgangVurdering(TilgangResultat tilgangResultat, String årsak) {

    public boolean fikkTilgang() {
        return tilgangResultat == TilgangResultat.GODKJENT;
    }

    public static TilgangVurdering godkjenn() {
        return new TilgangVurdering(TilgangResultat.GODKJENT, "");
    }

    public static TilgangVurdering avslå(TilgangResultat tilgangResultat) {
        return new TilgangVurdering(tilgangResultat, "");
    }

    public static TilgangVurdering avslåGenerell(String årsak) {
        return new TilgangVurdering(TilgangResultat.AVSLÅTT_ANNEN_ÅRSAK, årsak);
    }
}
