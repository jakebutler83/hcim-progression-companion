package com.hcimprogression.companion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Exact-name carried-item checks. Free-form route advice is never guessed at. */
final class PackingChecklist
{
    private static final Pattern COUNT = Pattern.compile("^(.+?)\\s+[x×]\\s*([0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)$", Pattern.CASE_INSENSITIVE);

    static String name(String text)
    {
        return text.replace('\u00a0', ' ').trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    static Result check(String list, Map<String, Long> carried, boolean available)
    {
        if (list != null && list.length() > 2400)
            return new Result(Collections.emptyList(), "Packing list too long · review manually");
        List<Row> rows = new ArrayList<>();
        Map<String, Long> remaining = new HashMap<>(carried);
        int missing = 0, manual = 0, ready = 0;
        for (String line : RoutineRoutePlan.lines(list))
        {
            if (rows.size() == 32) break;
            Matcher match = COUNT.matcher(line);
            String item = "";
            long required = 0;
            if (match.matches())
            {
                item = name(match.group(1));
                try { required = Long.parseLong(match.group(2).replace(",", "")); }
                catch (NumberFormatException ignored) { required = 0; }
            }
            // Existing simple tool lines also work; descriptive legacy lines stay manual.
            else if (line.matches("(?i)hammer|chisel|spade|rake|seed dibber"))
            {
                item = name(line); required = 1;
            }
            if (item.isEmpty() || required < 1 || required > Integer.MAX_VALUE)
            {
                rows.add(new Row(line, "Review manually", State.MANUAL)); manual++; continue;
            }
            if (!available)
            {
                rows.add(new Row(line, "Waiting for inventory", State.WAITING)); continue;
            }
            long have = Math.max(0, remaining.getOrDefault(item, 0L));
            long used = Math.min(have, required);
            remaining.put(item, have - used); // Do not count one item twice across duplicate entries.
            if (used >= required)
            {
                rows.add(new Row(line, "Ready", State.READY)); ready++;
            }
            else
            {
                rows.add(new Row(line, "Missing " + (required - used) + " (have " + used + ")", State.MISSING)); missing++;
            }
        }
        String summary = !available ? "Waiting for inventory"
            : rows.isEmpty() ? "No packing items listed"
            : missing == 0 && manual == 0 ? "Packing ready · " + ready + " checks"
            : missing + " missing · " + manual + " manual · " + ready + " ready";
        return new Result(rows, summary);
    }

    enum State { READY, MISSING, MANUAL, WAITING }
    static final class Row
    {
        final String item, status;
        final State state;
        Row(String item, String status, State state) { this.item = item; this.status = status; this.state = state; }
    }
    static final class Result
    {
        final List<Row> rows;
        final String summary;
        Result(List<Row> rows, String summary) { this.rows = Collections.unmodifiableList(rows); this.summary = summary; }
    }
}
