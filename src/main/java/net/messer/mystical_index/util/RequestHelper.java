package net.messer.mystical_index.util;

import java.util.regex.Pattern;

public class RequestHelper {
    private static final Pattern STACKS_MATCHER = Pattern.compile("(?<amount>\\d+) stacks?( of)? (?<item>.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COUNTS_MATCHER = Pattern.compile("(?<amount>\\d+)x? (?<item>.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SINGLE_MATCHER = Pattern.compile("(?<item>.+)", Pattern.CASE_INSENSITIVE);


}
