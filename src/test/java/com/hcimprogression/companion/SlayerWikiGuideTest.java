package com.hcimprogression.companion;

import org.junit.Test;
import static org.junit.Assert.*;

/** Offline factual-field fixtures; no network or game access. */
public class SlayerWikiGuideTest
{
    @Test public void keepsVariantStatsSeparateAndReadsCurrentWeaknessKeys()
    {
        SlayerWikiGuide guide = SlayerWikiGuide.parse("Dust devil", 123, "{{Infobox Monster\n"
            + "|version1 = Smoke Dungeon\n|version2 = Catacombs of Kourend\n|name = Dust devil\n"
            + "|combat1 = 93\n|combat2 = 110\n|hitpoints1 = 105\n|hitpoints2 = 130\n|def1 = 80\n"
            + "|elementalweaknesstype = Air\n|elementalweaknesspercent = 35\n|dlight = 20\n}}\n"
            + "{{LocLine\n|location = [[Kourend|Catacombs of Kourend]]\n|levels=110\n}}\n");
        assertEquals(2, guide.variants.size());
        assertTrue(guide.variants.get(0).stats.contains("HP: 105"));
        assertFalse(guide.variants.get(1).stats.contains("105"));
        assertFalse(guide.variants.get(1).stats.contains("Defence: 80"));
        assertTrue(guide.variants.get(1).stats.contains("Elemental weakness: Air"));
        assertTrue(guide.variants.get(1).stats.contains("bonus (%): 35"));
        assertTrue(guide.variants.get(1).stats.contains("Light ranged defence: 20"));
        assertEquals("Catacombs of Kourend", guide.locations); assertEquals(123, guide.revision);
    }
    @Test public void readsNestedTemplatesWithoutAccidentallyUsingOtherInfoboxes()
    {
        SlayerWikiGuide guide = SlayerWikiGuide.parse("Bloodveld", 1,
            "{{Infobox Monster\n|name = Bloodveld\n|hitpoints = 120\n|max hit = {{maxhit|5}}\n}}\n"
            + "{{Infobox Monster\n|name = unrelated\n|hitpoints = 999\n}}\n");
        assertEquals(1, guide.variants.size()); assertTrue(guide.variants.get(0).stats.contains("HP: 120"));
        assertFalse(guide.variants.get(0).stats.contains("999"));
        assertTrue(guide.variants.get(0).stats.contains("Weakness: not listed"));
        assertTrue(guide.locations.contains("No structured locations"));
    }
    @Test public void numberedElementalWeaknessOverridesCommonButDoesNotLeak()
    {
        SlayerWikiGuide guide = SlayerWikiGuide.parse("Example", 1,
            "{{Infobox Monster\n|version1=A\n|version2=B\n|elementalweaknesstype1=Water\n|elementalweaknesspercent1=50\n}}\n");
        assertTrue(guide.variants.get(0).stats.contains("Water"));
        assertFalse(guide.variants.get(1).stats.contains("Water"));
        assertTrue(guide.variants.get(1).stats.contains("not listed"));
    }
    @Test public void rejectsUrlsControlCharactersAndOversizedTitles()
    {
        for (String bad : new String[]{"https://evil.example", "../Foo", "<img>", "a\nb", "a@b", "", "x".repeat(101)})
            try { SlayerWikiGuide.validTitle(bad); fail(bad); } catch (IllegalArgumentException expected) { }
        assertEquals("https://oldschool.runescape.wiki/w/Dust_devil", SlayerWikiGuide.url("Dust_devil"));
    }
    @Test public void malformedOrTooLargePagesFailWithNoInventedMonster()
    {
        for (String bad : new String[]{"No monster here", "{{Infobox Monster\n|hitpoints = 1", "x".repeat(500_001)})
            try { SlayerWikiGuide.parse("Example", 1, bad); fail("invalid input"); } catch (IllegalArgumentException expected) { }
    }
    @Test public void rendersPlainBoundedTextInsteadOfDownloadedHtml()
    {
        assertEquals("Dust devil & desert", SlayerWikiGuide.clean("'''[[Dust devil]]''' &amp; <b>desert</b><ref>footnote</ref>"));
        assertTrue(SlayerWikiGuide.clean("a".repeat(1000)).length() < 400);
    }
    @Test public void taskLabelsAreMappedOnlyWhenUnambiguous()
    {
        assertEquals("Dust devil", SlayerWikiGuide.monsterTitle("DUST DEVILS"));
        assertEquals("Blue dragon", SlayerWikiGuide.monsterTitle("Blue dragons"));
        assertEquals("Metal dragons", SlayerWikiGuide.monsterTitle("Metal dragons"));
    }
    @Test public void locationsAreUniqueAndBounded()
    {
        StringBuilder source = new StringBuilder("{{Infobox Monster\n|name = Example\n}}\n");
        for (int i = 0; i < 20; i++) source.append("{{LocLine\n|location = [[Area ").append(i / 2).append("]]\n}}\n");
        SlayerWikiGuide guide = SlayerWikiGuide.parse("Example", 1, source.toString());
        assertEquals(10, guide.locations.split("\n").length);
    }
}
