package com.hcimprogression.companion;

import java.util.Locale;
import net.runelite.api.ChatMessageType;
import net.runelite.client.util.Text;

/** Only a server-supplied completion message is evidence; player/console chat is not. */
final class QuestCompletionCapture
{
    private static final String PREFIX = "Congratulations, you've completed a quest:";

    private QuestCompletionCapture() { }

    static String chatName(ChatMessageType type, String message)
    {
        if (type != ChatMessageType.GAMEMESSAGE && type != ChatMessageType.SPAM) return "";
        if (message == null || message.length() > 512 || message.matches("(?s).*[\\p{Cntrl}].*")) return "";
        String text = Text.removeTags(message).replace('\u00a0', ' ').trim();
        if (!text.startsWith(PREFIX)) return "";
        String title = text.substring(PREFIX.length()).trim();
        if (title.endsWith(".") || title.endsWith("!")) title = title.substring(0, title.length() - 1).trim();
        if (title.isEmpty() || title.length() > 100 || title.contains("<") || title.contains(">")) return "";
        return title.replaceAll("\\s+", " ");
    }

    static String key(String title)
    {
        return ProgressionFeedback.clean(title).replace('\u00a0', ' ').replace('\u2019', '\'')
            .replace('\u2018', '\'').replace('\u2013', '-').replace('\u2014', '-')
            .replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }
}
