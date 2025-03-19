package org.example.domain.entity.expense;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum Currency {
    DOLLAR("usd", "$", "dollar", "dollars"),
    EURO("eur", "€", "euro", "euros"),
    POUND("gbp", "£", "pound", "pounds");

    private final String code;
    private final String symbol;
    private final String[] names;

    private static final Map<String, Currency> LOOKUP_MAP = new HashMap<>();

    static {
        for (Currency currency : Currency.values()) {
            LOOKUP_MAP.put(currency.symbol, currency);
            for (String name : currency.names) {
                LOOKUP_MAP.put(name.toLowerCase(), currency);
            }
        }
    }

    Currency(String code, String symbol, String... names) {
        this.code = code;
        this.symbol = symbol;
        this.names = names;
    }

    public static Currency fromString(String input) {
        return LOOKUP_MAP.get(input.toLowerCase());
    }
}
