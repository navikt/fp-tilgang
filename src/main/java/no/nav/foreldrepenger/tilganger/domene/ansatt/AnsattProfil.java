package no.nav.foreldrepenger.tilganger.domene.ansatt;

import java.time.LocalDateTime;

public record AnsattProfil(String brukernavn,
                           String navn,
                           String fornavnEtternavn,
                           String ansattVedEnhetId,
                           boolean kanSaksbehandle,
                           boolean kanVeilede,
                           boolean kanBeslutte,
                           boolean kanOverstyre,
                           boolean kanOppgavestyre,
                           boolean kanBehandleKodeEgenAnsatt,
                           boolean kanBehandleKode6,
                           boolean kanBehandleKode7,
                           boolean kanDrifte,
                           LocalDateTime funksjonellTid) {

    private AnsattProfil(Builder builder) {
        this(builder.brukernavn,
            builder.navn,
            builder.fornavnEtternavn,
            builder.ansattVedEnhetId,
            builder.kanSaksbehandle,
            builder.kanVeilede,
            builder.kanBeslutte,
            builder.kanOverstyre,
            builder.kanOppgavestyre,
            builder.kanBehandleKodeEgenAnsatt,
            builder.kanBehandleKode6,
            builder.kanBehandleKode7,
            builder.kanDrifte,
            LocalDateTime.now());
    }

    @Override
    public String toString() {
        return "BrukerProfil{" + "kanSaksbehandle=" + kanSaksbehandle + ", kanVeilede=" + kanVeilede + ", kanBeslutte=" + kanBeslutte
            + ", kanOverstyre=" + kanOverstyre + ", kanOppgavestyre=" + kanOppgavestyre + ", kanDrifte=" + kanDrifte + ", funksjonellTid="
            + funksjonellTid + '}';
    }

    public static class Builder {
        private final String brukernavn;
        private final String navn;
        private final String fornavnEtternavn;
        private final String ansattVedEnhetId;
        private boolean kanSaksbehandle;
        private boolean kanVeilede;
        private boolean kanBeslutte;
        private boolean kanOverstyre;
        private boolean kanOppgavestyre;
        private boolean kanBehandleKodeEgenAnsatt;
        private boolean kanBehandleKode6;
        private boolean kanBehandleKode7;
        private boolean kanDrifte;

        public Builder(String brukernavn, String navn, String fornavnEtternavn, String ansattVedEnhetId) {
            this.brukernavn = brukernavn;
            this.navn = navn;
            this.fornavnEtternavn = fornavnEtternavn;
            this.ansattVedEnhetId = ansattVedEnhetId;
        }

        public Builder kanSaksbehandle(boolean kanSaksbehandle) {
            this.kanSaksbehandle = kanSaksbehandle;
            return this;
        }

        public Builder kanVeilede(boolean kanVeilede) {
            this.kanVeilede = kanVeilede;
            return this;
        }

        public Builder kanBeslutte(boolean kanBeslutte) {
            this.kanBeslutte = kanBeslutte;
            return this;
        }

        public Builder kanOverstyre(boolean kanOverstyre) {
            this.kanOverstyre = kanOverstyre;
            return this;
        }

        public Builder kanOppgavestyre(boolean kanOppgavestyre) {
            this.kanOppgavestyre = kanOppgavestyre;
            return this;
        }

        public Builder kanBehandleKodeEgenAnsatt(boolean kanBehandleKodeEgenAnsatt) {
            this.kanBehandleKodeEgenAnsatt = kanBehandleKodeEgenAnsatt;
            return this;
        }

        public Builder kanBehandleKode6(boolean kanBehandleKode6) {
            this.kanBehandleKode6 = kanBehandleKode6;
            return this;
        }

        public Builder kanBehandleKode7(boolean kanBehandleKode7) {
            this.kanBehandleKode7 = kanBehandleKode7;
            return this;
        }

        public Builder kanDrifte(boolean kanDrifte) {
            this.kanDrifte = kanDrifte;
            return this;
        }

        public AnsattProfil build() {
            return new AnsattProfil(this);
        }
    }


}
