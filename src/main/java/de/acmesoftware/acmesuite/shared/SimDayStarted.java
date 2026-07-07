package de.acmesoftware.acmesuite.shared;

/**
 * Event: a new day has started. Other modules (e.g. CRM/Supply) can hang autonomous daily demand
 * off this (customer orders, material replenishment) without knowing the originator's internal
 * services.
 */
public record SimDayStarted(long day) {
}
