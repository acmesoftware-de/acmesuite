package de.acmesoftware.acmesuite.build.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.List;

/**
 * One shift of the weekly plan (EARLY/LATE/NIGHT). The six working days Mon–Sat are stored as a
 * comma-joined list of {@link ShiftCell} names in {@code cells} — a small, fixed 3×6 grid, so a
 * flat column is simpler than a child table.
 */
@Entity
@Table(name = "shift_plan_row")
public class ShiftPlanRow {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "shift", length = 8)
    private ShiftId shift;

    @Column(name = "ord", nullable = false)
    private int ord;

    @Column(name = "label", length = 32, nullable = false)
    private String label;

    @Column(name = "time_range", length = 16, nullable = false)
    private String timeRange;

    @Column(name = "cells", length = 64, nullable = false)
    private String cells;

    protected ShiftPlanRow() {
    }

    public ShiftPlanRow(ShiftId shift, int ord, String label, String timeRange, List<ShiftCell> cells) {
        this.shift = shift;
        this.ord = ord;
        this.label = label;
        this.timeRange = timeRange;
        this.cells = join(cells);
    }

    public void setCells(List<ShiftCell> cells) {
        this.cells = join(cells);
    }

    private static String join(List<ShiftCell> cells) {
        return cells.stream().map(Enum::name).reduce((a, b) -> a + "," + b).orElse("");
    }

    public ShiftId getShift() {
        return shift;
    }

    public int getOrd() {
        return ord;
    }

    public String getLabel() {
        return label;
    }

    public String getTimeRange() {
        return timeRange;
    }

    /** The six day cells Mon–Sat, decoded from storage. */
    public List<ShiftCell> getCells() {
        if (cells == null || cells.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(cells.split(",")).map(ShiftCell::valueOf).toList();
    }
}
