package de.acmesoftware.acmesuite.process;

/**
 * Contract type and the color of the signature folder in which it circulates through the office
 * (blue=purchasing, yellow=sales, green=HR hire, red=HR termination).
 */
public enum ContractType {

    PURCHASE("Purchase contract", FolderColor.BLUE),
    SALES("Supply contract", FolderColor.YELLOW),
    HR_HIRE("Hire", FolderColor.GREEN),
    HR_TERMINATION("Termination", FolderColor.RED);

    private final String displayName;
    private final FolderColor folderColor;

    ContractType(String displayName, FolderColor folderColor) {
        this.displayName = displayName;
        this.folderColor = folderColor;
    }

    public String displayName() {
        return displayName;
    }

    public FolderColor folderColor() {
        return folderColor;
    }
}
