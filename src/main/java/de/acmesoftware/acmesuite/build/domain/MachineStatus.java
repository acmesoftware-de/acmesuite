package de.acmesoftware.acmesuite.build.domain;

/** Live machine state driving the monitor tile colour. */
public enum MachineStatus {
    RUNNING,
    SETUP,
    FAULT,
    MAINTENANCE,
    IDLE
}
