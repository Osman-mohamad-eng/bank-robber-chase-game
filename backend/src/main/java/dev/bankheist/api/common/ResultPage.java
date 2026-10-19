package dev.bankheist.api.common;

import java.util.List;

public record ResultPage<T>(List<T> items, int limit, int offset, long total) {
    public ResultPage {
        items = List.copyOf(items);
    }
}
