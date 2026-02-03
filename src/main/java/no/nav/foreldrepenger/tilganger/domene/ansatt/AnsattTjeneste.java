package no.nav.foreldrepenger.tilganger.domene.ansatt;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.tilganger.domene.cache.AnsattCache;
import no.nav.foreldrepenger.tilganger.integrasjoner.azure.AzureGraph;
import no.nav.foreldrepenger.tilganger.integrasjoner.azure.User;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;

@ApplicationScoped
public class AnsattTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(AnsattTjeneste.class);

    private AzureGraph azureGraph;
    private AnsattCache ansattCache;

    AnsattTjeneste() {
        // CDI proxy
    }

    @Inject
    public AnsattTjeneste(AnsattCache ansattCache, AzureGraph azureGraph) {
        this.ansattCache = ansattCache;
        this.azureGraph = azureGraph;
    }

    public Optional<Ansatt> finnAnsatt(String ident) {
        LOG.debug("Henter antatt: {}", ident);
        return hentAnsatt(ident, () -> azureGraph.finnUser(ident));
    }

    // Dersom man gjeninnfører OBO til fptilgang, så bruk azureGraph.me() i stedet for azureGraph.hentUser. Hent OID fra RequestKontekst
    public Optional<Ansatt> hentAnsatt(UUID uid) {
        LOG.debug("Henter antatt: {}", uid);
        return hentAnsatt(uid.toString(), () -> azureGraph.hentUser(uid));
    }

    public Optional<Ansatt> refreshAnsatt(String ident) {
        LOG.debug("Henter antatt: {}", ident);
        return refreshAnsatt(ident, () -> azureGraph.finnUser(ident));
    }

    // Dersom man gjeninnfører OBO til fptilgang, så bruk azureGraph.me() i stedet for azureGraph.hentUser. Hent OID fra RequestKontekst
    public Optional<Ansatt> refreshAnsatt(UUID uid) {
        LOG.debug("Henter antatt: {}", uid);
        return refreshAnsatt(uid.toString(), () -> azureGraph.hentUser(uid));
    }

    // Historisk metode for å hente ansattinformasjon basert på OBO / RequestKontekst. Pr brukes bare CC-tilgang, ikke OBO
    public Optional<Ansatt> hentAnsattFraKontekst() {
        var ansattref = getAnsattReferanseFraKontekst(); // OID eller ident
        LOG.debug("Henter antatt fra kontekst: {}", ansattref);
        return hentAnsatt(ansattref, () -> Optional.of(azureGraph.me()));
    }

    private Optional<Ansatt> hentAnsatt(String identifikator, Supplier<Optional<User>> ansattSupplier) {
        var ansatt = ansattCache.read(identifikator);
        if (ansatt.isEmpty()) {
            var før = System.currentTimeMillis();
            LOG.debug("Finner ikke ansatt eller grupper i cache {}", identifikator);
            var user = ansattSupplier.get();
            ansatt = user.map(AnsattTjeneste::mapUser);
            if (ansatt.isPresent()) {
                var navIdent = ansatt.get().ident();
                LOG.debug("Lagrer i cache {}", identifikator);
                ansattCache.store(navIdent, ansatt.get());
                var uid = ansatt.get().uid();
                LOG.debug("Lagrer i cache {}", uid);
                ansattCache.store(uid.toString(), ansatt.get());
            }
            LOG.info("[{} ms] Hent ansatt.", System.currentTimeMillis() - før);
        } else {
            LOG.debug("Fant ansatt i cache for {}", identifikator);
        }
        return ansatt;
    }

    private Optional<Ansatt> refreshAnsatt(String identifikator, Supplier<Optional<User>> ansattSupplier) {
        var før = System.currentTimeMillis();
        var user = ansattSupplier.get();
        var ansatt = user.map(AnsattTjeneste::mapUser);
        if (ansatt.isPresent()) {
            var navIdent = ansatt.get().ident();
            LOG.debug("Lagrer i cache {}", identifikator);
            ansattCache.store(navIdent, ansatt.get());
            var uid = ansatt.get().uid();
            LOG.debug("Lagrer i cache {}", uid);
            ansattCache.store(uid.toString(), ansatt.get());
        }
        LOG.info("[{} ms] Hent ansatt.", System.currentTimeMillis() - før);
        return ansatt;
    }

    private static Ansatt mapUser(User user) {
        var forEtternavn = Optional.ofNullable(user.givenName())
            .map(fornavn -> fornavn + Optional.ofNullable(user.surname()).map(etternavn -> " " + etternavn).orElse(""))
            .or(() -> Optional.ofNullable(user.surname())) // Bare etternavn
            .orElseGet(user::displayName); // Vanligvis på format vi vil unngå - Etternavn, Fornavn
        return new Ansatt(user.id(), user.onPremisesSamAccountName(), forEtternavn, user.streetAddress());
    }

    private static UUID getOidFraKontekst() {
        return KontekstHolder.getKontekst() instanceof RequestKontekst rk ? rk.getOid() : null;
    }

    private static String getUidFraKontekst() {
        return KontekstHolder.getKontekst().getUid();
    }

    private static String getAnsattReferanseFraKontekst() {
        return Optional.ofNullable(getOidFraKontekst()).map(UUID::toString).orElseGet(AnsattTjeneste::getUidFraKontekst);
    }
}
