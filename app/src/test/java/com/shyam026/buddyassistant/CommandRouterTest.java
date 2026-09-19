package com.shyam026.buddyassistant;

import org.junit.Test;
import static org.junit.Assert.*;

public class CommandRouterTest {
    @Test public void normalizationAndWakePhrase() {
        assertEquals("youtube kholo", CommandRouter.normalize("YouTube KHOLo!!!"));
        assertEquals("youtube kholo", CommandRouter.normalize("you tube   kholo"));
        assertEquals("wifi on", CommandRouter.normalize("Wi-Fi ON"));
        assertTrue(CommandRouter.isWakePhrase("Hey Buddy"));
        assertTrue(CommandRouter.isWakePhrase("hey buddy youtube kholo"));
        assertEquals("youtube kholo",
                CommandRouter.removeWakePhrase("Hey Buddy, YouTube kholo"));
    }

    @Test public void commandParsingStress() {
        String[] wake = {
                "Hey Buddy",
                "hey buddy",
                "Hey Buddy, YouTube kholo",
                "hey buddy volume badhao",
                "HEY BUDDY whatsapp message hello to rahul",
                "hey buddy search cats"
        };
        for (int i = 0; i < 150; i++) {
            assertTrue(CommandRouter.isWakePhrase(wake[i % wake.length]));
        }

        String[] yt = {
                "youtube search cats",
                "youtube pe search jee physics",
                "youtube par search java",
                "youtube me search lo-fi",
                "youtube pe coding tutorial",
                "youtube me jee maths chalao"
        };
        for (int i = 0; i < 150; i++) {
            String q = CommandRouter.youtubeQuery(yt[i % yt.length]);
            assertNotNull("youtube case " + i, q);
            assertFalse(q.isEmpty());
        }
        assertNull(CommandRouter.youtubeQuery("youtube kholo"));
        assertNull(CommandRouter.youtubeQuery("youtube open"));

        String[] google = {
                "search weather",
                "google pe search java",
                "google par search jee",
                "google me search android",
                "find calculator"
        };
        for (int i = 0; i < 150; i++) {
            String q = CommandRouter.googleSearchQuery(google[i % google.length]);
            assertNotNull("google case " + i, q);
            assertFalse(q.isEmpty());
        }
    }

    @Test public void hindiAndEdgeCases() {
        assertNull(CommandRouter.googleSearchQuery("volume badhao"));
        assertNull(CommandRouter.parseWhatsApp("whatsapp kholo"));
        assertEquals(Integer.valueOf(60), CommandRouter.extractNumber("brightness 60"));
        assertEquals(Integer.valueOf(100), CommandRouter.extractNumber("volume 100 percent"));
        assertEquals("rahul",
                CommandRouter.parseCall("rahul ko call karo").target);
        assertEquals("rahul",
                CommandRouter.parseCall("call rahul").target);
        assertEquals("9876543210",
                CommandRouter.parseCall("dial 9876543210").target);
    }

    @Test public void whatsappParsing() {
        CommandRouter.WhatsAppRequest r =
                CommandRouter.parseWhatsApp("whatsapp message hello bhai to rahul");
        assertNotNull(r);
        assertEquals("rahul", r.contact);
        assertEquals("hello bhai", r.message);

        r = CommandRouter.parseWhatsApp("whatsapp pe msg kya haal hai to manish");
        assertNotNull(r);
        assertEquals("manish", r.contact);
        assertEquals("kya haal hai", r.message);
    }

    @Test public void repeatedParserStress() {
        String[] commands = {
                "hey buddy",
                "hey buddy youtube kholo",
                "youtube pe search physics",
                "search jee maths",
                "whatsapp message hello to rahul",
                "volume 50",
                "brightness 70",
                "call rahul",
                "back jao",
                "home screen"
        };
        for (int i = 0; i < 250; i++) {
            String normalized = CommandRouter.normalize(commands[i % commands.length]);
            assertNotNull(normalized);
            assertFalse(normalized.isEmpty());
        }
    }
}
