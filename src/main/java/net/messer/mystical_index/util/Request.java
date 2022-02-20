package net.messer.mystical_index.util;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Request {
    private static final Pattern STACKS_MATCHER = Pattern.compile("(?<amount>\\d+) stacks?( of)? (?<item>.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern STACK_MATCHER = Pattern.compile("stack( of)? (?<item>.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COUNTS_MATCHER = Pattern.compile("(?<amount>\\d+)x? (?<item>.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SINGLE_MATCHER = Pattern.compile("(?<item>.+)", Pattern.CASE_INSENSITIVE);
    private static final String[] WILDCARD_STRINGS = { "...", "~", "+", "?" };

    private final String[] expression;
    private int amountUnsatisfied;
    private int amountExtracted = 0;
    private int lastCheck = 0;
    private final BiFunction<Integer, Item, Integer> amountModifier;
    private BiConsumer<Request, BlockEntity> blockExtractedCallback;
    private Item match;
    private Vec3d sourcePosition;

    private Request(String itemQuery, int amount, BiFunction<Integer, Item, Integer> amountModifier) {
        boolean wildcard = false;
        for (String wildcardString : WILDCARD_STRINGS) {
            if (itemQuery.endsWith(wildcardString)) {
                itemQuery = itemQuery.substring(0, itemQuery.length() - wildcardString.length());
                wildcard = true;
            }
        }
        this.expression = (wildcard ? "*" + itemQuery + "*" : itemQuery)
                .replace(' ', '_').split("\\*+", -1);

        this.amountUnsatisfied = amount;
        this.amountModifier = amountModifier;
    }

    public static Request get(String query) {
        Request request = parseAmount(STACKS_MATCHER.matcher(query), (integer, item) -> integer * item.getMaxCount());
        if (request != null) return request;
        request = parseAmount(STACK_MATCHER.matcher(query), (integer, item) -> item.getMaxCount());
        if (request != null) return request;
        request = parseAmount(COUNTS_MATCHER.matcher(query), (integer, item) -> integer);
        if (request != null) return request;
        request = parseAmount(SINGLE_MATCHER.matcher(query), (integer, item) -> 1);
        return request;
    }

    private static Request parseAmount(Matcher matcher, BiFunction<Integer, Item, Integer> amountModifier) {
        if (matcher.matches()) {
            int amount;
            try {
                amount = Integer.parseInt(matcher.group("amount"));
            } catch (IllegalArgumentException e) {
                amount = 1;
            }

            return new Request(matcher.group("item"), amount, amountModifier);
        }
        return null;
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

    public void satisfy(int amount) {
        if (!hasMatched()) throw new IllegalStateException("Can't satisfy before a match has been found.");

        if (amountUnsatisfied != -1) {
            amountUnsatisfied -= amount;
        }
        amountExtracted += amount;
    }

    public boolean isSatisfied() {
        return amountUnsatisfied <= 0;
    }

    public int getAmountUnsatisfied() {
        if (!hasMatched()) throw new IllegalStateException("Can't get amount before a match has been found.");

        return amountUnsatisfied;
    }

    public Item getMatchedItem() {
        if (!hasMatched()) throw new IllegalStateException("Can't get matched item before a match has been found.");

        return match;
    }

    public int getTotalAmountExtracted() {
        return amountExtracted;
    }

    public int getAmountExtracted() {
        int amount = amountExtracted - lastCheck;
        lastCheck = amountExtracted;
        return amount;
    }

    public Request setSourcePosition(Vec3d position) {
        sourcePosition = position;
        return this;
    }

    public Vec3d getSourcePosition() {
        return sourcePosition;
    }

    public Request setBlockExtractedCallback(BiConsumer<Request, BlockEntity> callback) {
        this.blockExtractedCallback = callback;
        return this;
    }

    public void runBlockExtractedCallback(BlockEntity blockEntity) {
        if (sourcePosition == null || blockExtractedCallback == null) return;
        this.blockExtractedCallback.accept(this, blockEntity);
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
