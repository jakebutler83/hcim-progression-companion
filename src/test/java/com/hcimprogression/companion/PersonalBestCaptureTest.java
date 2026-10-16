package com.hcimprogression.companion;

import org.junit.Test;
import net.runelite.client.config.RuneScapeProfile;
import net.runelite.client.config.RuneScapeProfileType;
import static org.junit.Assert.*;

public class PersonalBestCaptureTest
{
    @Test public void secondsKeepGamePrecisionWithoutClockEstimates()
    {
        assertEquals(62400L,PersonalBestCapture.milliseconds("62.4"));
        assertEquals(62405L,PersonalBestCapture.milliseconds("62.405"));
        assertEquals(0L,PersonalBestCapture.milliseconds(null));
        for(String value:new String[]{"NaN","Infinity","-1","0","86401","999999999999999999999"})assertEquals(value,0L,PersonalBestCapture.milliseconds(value));
    }
    @Test public void resultScreensAcceptMinutesHoursAndFractions()
    {
        assertEquals(73200L,PersonalBestCapture.parseTime("Personal best: <col=ff0000>1:13.20</col>"));
        assertEquals(3723450L,PersonalBestCapture.parseTime("1:02:03.450"));
        assertEquals(7200000L,PersonalBestCapture.parseTime("120:00"));
        for(String value:new String[]{"No personal best","0:00","1:60","1:60:00","1000:00","24:00:01"})assertEquals(value,0L,PersonalBestCapture.parseTime(value));
    }
    @Test public void modesAndTeamSizeKeysRemainDistinctAndBounded()
    {
        assertTrue(PersonalBestCapture.validKey("tombs of amascut expert mode solo"));
        assertTrue(PersonalBestCapture.validKey("chambers of xeric 24+ players"));
        assertTrue(PersonalBestCapture.validKey("quest cook's assistant"));
        for(String key:new String[]{"","__proto__","constructor","<img>","bad\nkey"})assertFalse(PersonalBestCapture.validKey(key));
    }
    @Test public void sameNamesCannotMixDifferentHashesProfilesOrWorldModes()
    {
        java.util.List<RuneScapeProfile> profiles=java.util.Arrays.asList(
            new RuneScapeProfile("Same Name",RuneScapeProfileType.STANDARD,123L,"rsprofile.one"),
            new RuneScapeProfile("Same Name",RuneScapeProfileType.QUEST_SPEEDRUNNING,123L,"rsprofile.speed"));
        assertEquals("STANDARD",PersonalBestCapture.modeForProfile("rsprofile.one",123L,RuneScapeProfileType.STANDARD,profiles));
        assertEquals("",PersonalBestCapture.modeForProfile("rsprofile.one",456L,RuneScapeProfileType.STANDARD,profiles));
        assertEquals("",PersonalBestCapture.modeForProfile("rsprofile.one",123L,RuneScapeProfileType.QUEST_SPEEDRUNNING,profiles));
        assertEquals("QUEST_SPEEDRUNNING",PersonalBestCapture.modeForProfile("rsprofile.speed",123L,RuneScapeProfileType.QUEST_SPEEDRUNNING,profiles));
        assertEquals("",PersonalBestCapture.modeForProfile(null,123L,RuneScapeProfileType.STANDARD,profiles));
    }
}
