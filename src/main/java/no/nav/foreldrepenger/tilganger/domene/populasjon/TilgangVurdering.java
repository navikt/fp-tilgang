package no.nav.foreldrepenger.tilganger.domene.populasjon;

import no.nav.vedtak.felles.integrasjon.populasjon.PopulasjonTilgangResultat;

public record TilgangVurdering(PopulasjonTilgangResultat tilgangResultat, String årsak) {

    public boolean fikkTilgang() {
        return tilgangResultat == PopulasjonTilgangResultat.GODKJENT;
    }

    public static TilgangVurdering godkjenn() {
        return new TilgangVurdering(PopulasjonTilgangResultat.GODKJENT, "");
    }

    public static TilgangVurdering avslå(PopulasjonTilgangResultat tilgangResultat) {
        return new TilgangVurdering(tilgangResultat, "");
    }

    public static TilgangVurdering avslåGenerell(String årsak) {
        return new TilgangVurdering(PopulasjonTilgangResultat.AVSLÅTT_ANNEN_ÅRSAK, årsak);
    }
}
