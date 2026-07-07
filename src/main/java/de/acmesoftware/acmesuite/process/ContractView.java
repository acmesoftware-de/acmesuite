package de.acmesoftware.acmesuite.process;

/** Read view of a contract in circulation (API + client). */
public record ContractView(String id, String type, String color, String subject, String status,
                           String creatorUnitKey, String carrierKey, String currentApproverKey,
                           int step, int totalSteps, String stage, int reworkCount,
                           long valueEur, boolean restricted, java.util.List<String> restrictions,
                           String reason) {

    public static ContractView of(Contract c) {
        return new ContractView(c.id(), c.type().name(), c.color().name(), c.subject(),
                c.status().name(), c.creatorUnitKey(), c.carrierKey(), c.currentApproverKey(),
                c.step(), c.totalSteps(), c.stage().name(), c.reworkCount(),
                c.valueEur(), c.restricted(), c.restrictionLabels(), c.reason());
    }
}
