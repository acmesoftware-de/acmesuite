package de.acmesoftware.acmesuite.crm.domain;

/** Ordered sales stage; win probability is derived from it (see api/acme-crm.yaml). */
public enum PipelineStage {
    NEU(15),
    QUALIFIZIERT(35),
    ANGEBOT(60),
    VERHANDLUNG(80),
    GEWONNEN(100);

    private final int probability;

    PipelineStage(int probability) {
        this.probability = probability;
    }

    public int probability() {
        return probability;
    }
}
