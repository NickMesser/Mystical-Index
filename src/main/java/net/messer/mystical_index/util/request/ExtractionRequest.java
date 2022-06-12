package net.messer.mystical_index.util.request;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractionRequest extends Request {
    private static final Pattern STACKS_MATCHER = Pattern.compile("(?<amount>\\d+|all) stacks?( of)? (?<item>.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern STACK_MATCHER = Pattern.compile("stack( of)? (?<item>.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COUNTS_MATCHER = Pattern.compile("(?<amount>\\d+|all)x? (?<item>.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SINGLE_MATCHER = Pattern.compile("(?<item>.+)", Pattern.CASE_INSENSITIVE); // TODO rework this
    private static final String[] WILDCARD_STRINGS = { "...", "~", "+", "?" };

    private final String[] expression;
    private final BiFunction<Integer, Item, Integer> amountModifier;
    private Item match;
    private List<ItemStack> stacks = List.of();

    private ExtractionRequest(String itemQuery, int amount, BiFunction<Integer, Item, Integer> amountModifier) {
        super(amount);
        boolean wildcard = false;
        for (String wildcardString : WILDCARD_STRINGS) {
            if (itemQuery.endsWith(wildcardString)) {
                itemQuery = itemQuery.substring(0, itemQuery.length() - wildcardString.length());
                wildcard = true;
            }
        }
        this.expression = (wildcard ? "*" + itemQuery + "*" : itemQuery)
                .replace(' ', '_').split("\\*+", -1);

        this.amountModifier = amountModifier;
    }

    public static ExtractionRequest get(String query) {
        ExtractionRequest request = parseAmount(STACKS_MATCHER.matcher(query), (integer, item) -> integer * item.getMaxCount());
        if (request != null) return request;
        request = parseAmount(STACK_MATCHER.matcher(query), (integer, item) -> item.getMaxCount());
        if (request != null) return request;
        request = parseAmount(COUNTS_MATCHER.matcher(query), (integer, item) -> integer);
        if (request != null) return request;
        request = parseAmount(SINGLE_MATCHER.matcher(query), (integer, item) -> 1);
        return request;
    }

    private static ExtractionRequest parseAmount(Matcher matcher, BiFunction<Integer, Item, Integer> amountModifier) {
        if (matcher.matches()) {
            int amount;
            try {
                amount = Integer.parseInt(matcher.group("amount"));
            } catch (NumberFormatException e) {
                amount = Integer.MAX_VALUE;
            } catch (IllegalArgumentException e) {
                amount = 1;
            }

            return new ExtractionRequest(matcher.group("item"), amount, amountModifier);
        }
        return null;
    }

    public void apply(LibraryIndex index, boolean apply) {
        stacks = index.extractItems(this, apply);
    }

    public boolean hasMatched() {
        return match != null;
    }

    public boolean matches(Item matchItem) {
        if (hasMatched()) {
            return match == matchItem;
        } else {
            String itemName = matchItem.toString().toLowerCase(Locale.ROOT).trim();

            boolean matched = matchGlob(expression, itemName)
                    || matchGlob(expression, itemName + "s")
                    || matchGlob(expression, itemName + "es")
                    || itemName.endsWith("y") && matchGlob(expression, itemName.substring(0, itemName.length() - 1) + "ies");

            if (matched) {
                match = matchItem;
                amountUnsatisfied = amountModifier.apply(amountUnsatisfied, matchItem);
            }

            return matched;
        }
    }

    @Override
    public void satisfy(int amount) {
        if (!hasMatched()) throw new IllegalStateException("Can't satisfy before a match has been found.");
        super.satisfy(amount);
    }

    @Override
    public int getAmountUnsatisfied() {
        if (!hasMatched()) throw new IllegalStateException("Can't get amount before a match has been found.");
        return super.getAmountUnsatisfied();
    }

    public Item getMatchedItem() {
        if (!hasMatched()) throw new IllegalStateException("Can't get matched item before a match has been found.");

        return match;
    }

    public Text getMessage() {
        if (hasMatched())
            return new TranslatableText("chat.mystical_index.extracted", getTotalAmountAffected(), getMatchedItem().getName().getString());
        return new TranslatableText("chat.mystical_index.no_match");
    }

    public List<ItemStack> getAffectedStacks() {
        return stacks;
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
}
