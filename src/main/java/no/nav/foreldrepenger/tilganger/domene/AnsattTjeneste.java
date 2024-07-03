package no.nav.foreldrepenger.tilganger.domene;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.tilganger.domene.cache.CacheAnsatt;
import no.nav.foreldrepenger.tilganger.domene.cache.CacheGrupper;
import no.nav.foreldrepenger.tilganger.integrasjoner.azure.Group;

import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.tilganger.domene.cache.Cache;
import no.nav.foreldrepenger.tilganger.integrasjoner.azure.AzureGraph;
import no.nav.foreldrepenger.tilganger.integrasjoner.azure.User;

@ApplicationScoped
public class AnsattTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(AnsattTjeneste.class);

    private AzureGraph azureGraph;
    private Cache<Ansatt> ansattCache;
    private Cache<Set<UUID>> grupperCache;

    AnsattTjeneste() {
        // CDI proxy
    }

    @Inject
    public AnsattTjeneste(@CacheAnsatt Cache<Ansatt> ansattCache,
                          @CacheGrupper Cache<Set<UUID>> grupperCache,
                          AzureGraph azureGraph) {
        this.ansattCache = ansattCache;
        this.grupperCache = grupperCache;
        this.azureGraph = azureGraph;
    }

    public Optional<Ansatt> hentAnsattFraKontekst() {
        var uid = KontekstHolder.getKontekst().getUid();
        LOG.debug("Henter antatt fra kontekts: {}", uid);
        return hentAnsatt(uid, () -> Optional.of(azureGraph.me()));
    }

    public Set<UUID> hentGrupperFraKontekst() {
        var uid = KontekstHolder.getKontekst().getUid();
        LOG.debug("Henter grupper fra kontekts: {}", uid);
        return hentGrupper(uid, () -> azureGraph.memberOf());
    }

    public Optional<Ansatt> hentAnsatt(String ident) {
        LOG.debug("Henter antatt: {}", ident);
        return hentAnsatt(ident, () -> azureGraph.finnUser(ident));
    }

    public Optional<Ansatt> hentAnsatt(UUID uid) {
        LOG.debug("Henter antatt: {}", uid);
        return hentAnsatt(uid.toString(), () -> azureGraph.finnUser(uid));
    }

    public Set<UUID> hentGrupper(Ansatt ansatt) {
        var identifikator = ansatt.uid().toString();
        LOG.debug("Henter ansatt grupper: {}", identifikator);
        return hentGrupper(identifikator, () -> azureGraph.hentGrupper(ansatt.uid()));
    }

    private Optional<Ansatt> hentAnsatt(String identifikator, Supplier<Optional<User>> ansattSupplier) {
        var ansatt = ansattCache.read(identifikator);
        if (ansatt.isEmpty()) {
            LOG.debug("Finner ikke ansatt i cache {}", identifikator);
            ansatt = ansattSupplier.get().map(AnsattTjeneste::mapUser);
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
        return ansatt;
    }

    private Set<UUID> hentGrupper(String identifikator, Supplier<Set<Group>> grupperSupplier) {
        LOG.debug("Henter grupper for: {}", identifikator);
        var grupper = grupperCache.read(identifikator);
        if (grupper.isEmpty()) {
            LOG.debug("Finner ikke grupper i cache for {}", identifikator);
            grupper = Optional.of(grupperSupplier.get().stream().map(Group::id).collect(Collectors.toSet()));
            if (!grupper.get().isEmpty()) {
                LOG.debug("Lagrer grupper i cache for {}", identifikator);
                grupperCache.store(identifikator, grupper.get());
            }
        } else {
            LOG.debug("Fant grupper i cache for {}", identifikator);
        }
        return grupper.get();
    }

    private static Ansatt mapUser(User user) {
        return new Ansatt(user.id(), user.onPremisesSamAccountName(), user.displayName(), user.mail());
    }
}
