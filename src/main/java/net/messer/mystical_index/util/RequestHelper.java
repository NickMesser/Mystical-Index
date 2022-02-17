package net.messer.mystical_index.util;

import net.minecraft.item.Item;

import java.util.Locale;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestHelper {
    private static final Pattern STACKS_MATCHER = Pattern.compile("(?<amount>\\d+) stacks?( of)? (?<item>.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COUNTS_MATCHER = Pattern.compile("(?<amount>\\d+)x? (?<item>.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SINGLE_MATCHER = Pattern.compile("(?<item>.+)", Pattern.CASE_INSENSITIVE);
    private static final String[] WILDCARD_STRINGS = { "...", "~", "+", "?" };

    private final LibraryIndex index;

    private RequestHelper(LibraryIndex index) {
        this.index = index;
    }

    public static ItemAmount processRequest(LibraryIndex index, String request) {
        RequestHelper helper = new RequestHelper(index);

        return helper.getItemAmount(request);
    }

    private ItemAmount getItemAmount(String request) {
        ItemAmount itemAmount = matchRequest(STACKS_MATCHER.matcher(request), (integer, item) -> integer * item.getMaxCount());
        if (itemAmount != null) return itemAmount;
        itemAmount = matchRequest(COUNTS_MATCHER.matcher(request), (integer, item) -> integer);
        if (itemAmount != null) return itemAmount;
        itemAmount = matchRequest(SINGLE_MATCHER.matcher(request), (integer, item) -> 1);
        return itemAmount;
    }

    private ItemAmount matchRequest(Matcher matcher, BiFunction<Integer, Item, Integer> amountModifier) {
        if (matcher.matches()) {
            Item item = getItem(matcher.group("item"));

            int amount;
            try {
                amount = Integer.parseInt(matcher.group("amount"));
            } catch (IllegalArgumentException e) {
                amount = 1;
            }

            return item == null ? null :
                    new ItemAmount(item, amountModifier.apply(amount, item));
        }
        return null;
    }

    private Item getItem(String request) {
        boolean wildcard = false;
        for (String wildcardString : WILDCARD_STRINGS) {
            if (request.endsWith(wildcardString)) {
                request = request.substring(0, request.length() - wildcardString.length());
                wildcard = true;
            }
        }

        String[] expression = (wildcard ? "*" + request + "*" : request).split("\\*+", -1);

        for (BigStack bigStack : index.getItems().getAll()) {
            String itemName = bigStack.getItem().toString().toLowerCase(Locale.ROOT).trim();

            if (matchGlob(expression, itemName)
                    || matchGlob(expression, itemName + "s")
                    || matchGlob(expression, itemName + "es")
                    || itemName.endsWith("y") && matchGlob(expression, itemName.substring(0, itemName.length() - 1) + "ies")
            ) {
                return bigStack.getItem();
            }
        }

        return null;
    }

    private static boolean matchGlob(String[] expression, String string) {
        if (expression.length == 1) {
            return expression[0].equals(string);
        }

        if (!string.startsWith(expression[0])) {
            return false;
        }

        int offset = expression[0].length();
        for (int i = 1; i < expression.length - 1; i++) {
            String section = expression[i];
            int found = string.indexOf(section, offset);
            if (found == -1) return false;
            offset = found + section.length();
        }
        return string.substring(offset).endsWith(expression[expression.length - 1]);
    }

    public record ItemAmount(Item item, int amount) {}
}
