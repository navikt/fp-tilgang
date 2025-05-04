package no.nav.foreldrepenger.tilganger.domene.ansatt;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.tilganger.domene.cache.GrupperCache;
import no.nav.foreldrepenger.tilganger.integrasjoner.azure.AzureGraph;
import no.nav.foreldrepenger.tilganger.integrasjoner.azure.Group;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppe;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@ApplicationScoped
public class GrupperTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(GrupperTjeneste.class);

    private static final AnsattGruppeProvider PROVIDER = AnsattGruppeProvider.instance();
    private static final Set<UUID> ALLE_ANSATTGRUPPE_OIDS = Arrays.stream(AnsattGruppe.values())
        .map(PROVIDER::getAnsattGruppeOid)
        .collect(Collectors.toSet());

    private AzureGraph azureGraph;
    private GrupperCache grupperCache;

    public GrupperTjeneste() {
        // CDI proxy
    }

    @Inject
    public GrupperTjeneste(GrupperCache grupperCache, AzureGraph azureGraph) {
        this.grupperCache = grupperCache;
        this.azureGraph = azureGraph;
    }

    public Set<AnsattGruppe> filtrertGrupperforAnsatt(UUID ansattOid, Set<AnsattGruppe> ansattGrupper) {
        var grupperForAnsatt = alleGrupperForAnsatt(ansattOid);
        return grupperForAnsatt.stream().filter(ansattGrupper::contains).collect(Collectors.toSet());
    }

    // Dersom man gjeninnfører OBO til fptilgang, så bruk azureGraph.memberOf i stedet for azureGraph.hentGrupper
    public Set<AnsattGruppe> alleGrupperForAnsatt(UUID ansattOid) {
        var gruppeOidsForAnsatt = hentGrupper(ansattOid.toString(),
                () -> azureGraph.hentGrupper(ansattOid, new HashSet<>(GrupperTjeneste.ALLE_ANSATTGRUPPE_OIDS)));
        return PROVIDER.getAnsattGrupperFra(gruppeOidsForAnsatt);
    }

    private List<UUID> hentGrupper(String identifikator, Supplier<Set<Group>> grupperSupplier) {
        var grupper = grupperCache.read(identifikator);
        if (grupper.isEmpty()) {
            var før = System.currentTimeMillis();
            grupper = grupperSupplier.get().stream().map(Group::id).toList();
            if (!grupper.isEmpty()) {
                grupperCache.store(identifikator, grupper);
            }
            LOG.info("[{} ms] Hent grupper.", System.currentTimeMillis() - før);
        }
        return grupper;
    }
}
