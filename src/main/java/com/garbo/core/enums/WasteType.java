package com.garbo.core.enums;

public enum WasteType {
    PLASTIC(false),
    GLASS(false),
    METAL(true),
    E_WASTE(true),
    PAPER(true),
    ORGANIC(true),
    TEXTILE(false),
    MIXED(false);

    private final boolean weightRequiredAtCompletion;

    WasteType(boolean weightRequiredAtCompletion) {
        this.weightRequiredAtCompletion = weightRequiredAtCompletion;
    }

    public boolean isWeightRequiredAtCompletion() {
        return weightRequiredAtCompletion;
    }
}
