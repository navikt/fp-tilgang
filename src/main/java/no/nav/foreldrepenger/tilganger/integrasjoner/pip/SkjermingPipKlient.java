package no.nav.foreldrepenger.tilganger.integrasjoner.pip;

import jakarta.enterprise.context.Dependent;

import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.felles.integrasjon.skjermingpip.AbstractSkjermetPersonPipKlient;

@Dependent
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC,
    endpointProperty = "skjermet.person.pip.base.url", endpointDefault = "https://skjermede-personer-pip.intern.nav.no",
    scopesProperty = "skjermet.person.pip.scope", scopesDefault = "api://prod-gcp.nom.skjermede-personer-pip/.default")
public class SkjermingPipKlient extends AbstractSkjermetPersonPipKlient {

    public SkjermingPipKlient() {
        super();
    }
}
