package de.acmesoftware.acmesuite.build.domain;

/** Stage of a production order on the planning board (design: GEPLANT → FERTIG). */
public enum OrderStage {
    GEPLANT,
    RUESTEN,
    IN_ARBEIT,
    PRUEFUNG,
    FERTIG
}
