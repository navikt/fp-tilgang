package no.nav.foreldrepenger.tilganger.domene.ansatt;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import no.nav.foreldrepenger.konfig.Environment;

public enum Gruppe {

    SAKSBEHNADLER(Environment.current().getProperty("gruppe.oid.saksbehandler")),
    VEILEDER(Environment.current().getProperty("gruppe.oid.veileder")),
    BESLUTTER(Environment.current().getProperty("gruppe.oid.beslutter")),
    OVERSTYRER(Environment.current().getProperty("gruppe.oid.overstyrer")),
    OPPGAVESTYRER(Environment.current().getProperty("gruppe.oid.oppgavestyrer")),
    DRIFTER(Environment.current().getProperty("gruppe.oid.drifter")),
    EGENANSATT(Environment.current().getProperty("gruppe.oid.egenansatt")),
    KODE6(Environment.current().getProperty("gruppe.oid.kode6")),
    KODE7(Environment.current().getProperty("gruppe.oid.kode7"))
    ;

    private static final List<UUID> ALLE_GRUPPER = new LinkedList<>();

    static {
        ALLE_GRUPPER.addAll(Arrays.stream(values()).map(Gruppe::getId).toList());
    }

    private UUID id;

    Gruppe(String id) {
        this.id = UUID.fromString(id);
    }

    public UUID getId() {
        return id;
    }

    public static List<UUID> getAlleGrupper() {
        return ALLE_GRUPPER;
    }
}
