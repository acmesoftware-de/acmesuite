package de.acmesoftware.acmesuite.build.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A shop-floor machine in the digital-twin monitor: live status with OEE and its availability /
 * performance / quality components, plus the running order and its progress. Telemetry — seeded,
 * read-only over the API, not Envers-audited.
 */
@Entity
@Table(name = "machine")
public class Machine {

    @Id
    @Column(name = "id", length = 48)
    private String id;

    @Column(name = "name", length = 48, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private MachineStatus status;

    @Column(name = "oee", nullable = false)
    private int oee;

    @Column(name = "availability", nullable = false)
    private int availability;

    @Column(name = "performance", nullable = false)
    private int performance;

    @Column(name = "quality", nullable = false)
    private int quality;

    @Column(name = "progress", nullable = false)
    private int progress;

    @Column(name = "current_order", length = 120)
    private String currentOrder;

    protected Machine() {
    }

    public Machine(String id, String name, MachineStatus status, int oee, int availability, int performance,
                   int quality, int progress, String currentOrder) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.oee = oee;
        this.availability = availability;
        this.performance = performance;
        this.quality = quality;
        this.progress = progress;
        this.currentOrder = currentOrder;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public MachineStatus getStatus() {
        return status;
    }

    public int getOee() {
        return oee;
    }

    public int getAvailability() {
        return availability;
    }

    public int getPerformance() {
        return performance;
    }

    public int getQuality() {
        return quality;
    }

    public int getProgress() {
        return progress;
    }

    public String getCurrentOrder() {
        return currentOrder;
    }
}
