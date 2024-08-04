package no.nav.foreldrepenger.tilganger.domene.ansatt;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.tilganger.domene.cache.AnsattCache;
import no.nav.foreldrepenger.tilganger.domene.cache.GrupperCache;
import no.nav.foreldrepenger.tilganger.integrasjoner.azure.AzureGraph;
import no.nav.foreldrepenger.tilganger.integrasjoner.azure.Group;
import no.nav.foreldrepenger.tilganger.integrasjoner.azure.User;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@ApplicationScoped
public class AnsattTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(AnsattTjeneste.class);

    private AzureGraph azureGraph;
    private AnsattCache ansattCache;
    private GrupperCache grupperCache;

    AnsattTjeneste() {
        // CDI proxy
    }

    @Inject
    public AnsattTjeneste(AnsattCache ansattCache, GrupperCache grupperCache, AzureGraph azureGraph) {
        this.ansattCache = ansattCache;
        this.grupperCache = grupperCache;
        this.azureGraph = azureGraph;
    }

    public Optional<Ansatt> hentAnsattFraKontekst() {
        var uid = KontekstHolder.getKontekst().getUid();
        LOG.debug("Henter antatt fra kontekts: {}", uid);
        return hentAnsatt(uid, () -> Optional.of(azureGraph.me()));
    }

    public List<UUID> hentGrupperFraKontekst(List<UUID> gruppeFilter) {
        var uid = KontekstHolder.getKontekst().getUid();
        LOG.debug("Henter grupper fra kontekts: {}", uid);
        return hentGrupper(uid, () -> azureGraph.memberOf(new HashSet<>(gruppeFilter)));
    }

    public Optional<Ansatt> hentAnsatt(String ident) {
        LOG.debug("Henter antatt: {}", ident);
        return hentAnsatt(ident, () -> azureGraph.finnUser(ident));
    }

    public Optional<Ansatt> hentAnsatt(UUID uid) {
        LOG.debug("Henter antatt: {}", uid);
        return hentAnsatt(uid.toString(), () -> azureGraph.hentUser(uid));
    }

    public List<UUID> hentGrupper(Ansatt ansatt, List<UUID> gruppeFilter) {
        var identifikator = ansatt.uid().toString();
        LOG.debug("Henter ansatt grupper: {}", identifikator);
        return hentGrupper(identifikator, () -> azureGraph.hentGrupper(ansatt.uid(), new HashSet<>(gruppeFilter)));
    }

    private Optional<Ansatt> hentAnsatt(String identifikator, Supplier<Optional<User>> ansattSupplier) {
        var før = System.nanoTime();
        var ansatt = ansattCache.read(identifikator);
        if (ansatt.isEmpty()) {
            LOG.debug("Finner ikke ansatt eller grupper i cache {}", identifikator);
            var user = ansattSupplier.get();
            ansatt = user.map(AnsattTjeneste::mapUser);
            if (ansatt.isPresent()) {
                LOG.debug("Lagrer i cache {}", identifikator);
                ansattCache.store(identifikator, ansatt.get());
                var uid = ansatt.get().uid();
                LOG.debug("Lagrer i cache {}", uid);
                ansattCache.store(uid.toString(), ansatt.get());
            }
        } else {
            LOG.debug("Fant ansatt i cache for {}", identifikator);
        }
        LOG.info("[{} ms] Hent ansatt.", Duration.ofNanos(System.nanoTime() - før).toMillis());
        return ansatt;
    }

    private List<UUID> hentGrupper(String identifikator, Supplier<Set<Group>> grupperSupplier) {
        LOG.debug("Henter grupper for: {}", identifikator);
        var før = System.nanoTime();
        var grupper = grupperCache.read(identifikator);
        if (grupper.isEmpty()) {
            LOG.debug("Finner ikke grupper i cache for {}", identifikator);
            grupper = Optional.of(grupperSupplier.get().stream().map(Group::id).toList());
            if (!grupper.get().isEmpty()) {
                LOG.debug("Lagrer grupper i cache for {}", identifikator);
                grupperCache.store(identifikator, grupper.get());
            }
        } else {
            LOG.debug("Fant grupper i cache for {}", identifikator);
        }
        LOG.info("[{} ms] Hent grupper.", Duration.ofNanos(System.nanoTime() - før).toMillis());
        return grupper.get();
    }

    private static Ansatt mapUser(User user) {
        var forEtternavn = Optional.ofNullable(user.givenName())
            .map(fornavn -> fornavn + Optional.ofNullable(user.surname()).map(etternavn -> " " + etternavn).orElse(""))
            .or(() -> Optional.ofNullable(user.surname())) // Bare etternavn
            .orElse("");
        return new Ansatt(user.id(), user.onPremisesSamAccountName(), user.displayName(), forEtternavn, user.mail(), user.streetAddress());
    }
}
