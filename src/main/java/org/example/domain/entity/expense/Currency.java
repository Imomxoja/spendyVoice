package org.example.domain.entity.expense;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum Currency {
    DOLLAR("USD", "$", "dollar", "dollars"),
    EURO("EUR", "€", "euro", "euros"),
    POUND("GBP", "£", "pound", "pounds");

    private final String code;
    private final String symbol;
    private final String[] names;

    private static final Map<String, String> LOOKUP_MAP = new HashMap<>();

    static {
        for (Currency currency : Currency.values()) {
            LOOKUP_MAP.put(currency.symbol, currency.code);
            for (String name : currency.names) {
                LOOKUP_MAP.put(name.toLowerCase(), currency.code);
            }
        }
    }

    Currency(String code, String symbol, String... names) {
        this.code = code;
        this.symbol = symbol;
        this.names = names;
    }

    public static String fromString(String input) {
        return LOOKUP_MAP.get(input.toLowerCase());
    }
}
