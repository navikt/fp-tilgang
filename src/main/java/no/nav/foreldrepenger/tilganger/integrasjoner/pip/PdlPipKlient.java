package no.nav.foreldrepenger.tilganger.integrasjoner.pip;

import jakarta.enterprise.context.Dependent;

import no.nav.vedtak.felles.integrasjon.pdlpip.AbstractPersondataPipKlient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@Dependent
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC,
    endpointProperty = "pdl.pip.base.url", endpointDefault = "http://pdl-pip-api.pdl/api/v1",
    scopesProperty = "pdl.pip.scope", scopesDefault = "api://prod-fss:pdl:pdl-pip-api/.default")
public class PdlPipKlient extends AbstractPersondataPipKlient {

    public PdlPipKlient() {
        super();
    }
}
