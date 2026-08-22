package de.takulabs.smpshop;

enum CurrencyType {
    MONEY,
    POINTS;

    static CurrencyType from(String value) {
        if (value != null && value.equalsIgnoreCase("points")) {
            return POINTS;
        }
        return MONEY;
    }
}

