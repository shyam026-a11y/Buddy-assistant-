package com.shyam026.buddyassistant;

import org.junit.Test;

import static org.junit.Assert.*;

public class CommandRouterTest {
    @Test public void wakePhraseAcceptsBuddyAndHeyBuddy() {
        assertTrue(CommandRouter.isWakePhrase("Buddy"));
        assertTrue(CommandRouter.isWakePhrase("Hey Buddy"));
        assertTrue(CommandRouter.isWakePhrase("Buddy open YouTube"));
        assertTrue(CommandRouter.isWakePhrase("Hey Buddy open YouTube"));
    }

    @Test public void wakePhraseRemainderIsCorrect() {
        assertEquals("", CommandRouter.removeWakePhrase("Buddy"));
        assertEquals("", CommandRouter.removeWakePhrase("Hey Buddy"));
        assertEquals("open youtube",
                CommandRouter.removeWakePhrase("Buddy open YouTube"));
        assertEquals("open youtube",
                CommandRouter.removeWakePhrase("Hey Buddy open YouTube"));
        assertNull(CommandRouter.removeWakePhrase("open youtube"));
    }

    @Test public void youtubeAndSearchParsing() {
        assertEquals(
                "arijit singh",
                CommandRouter.youtubeQuery(
                        "youtube search arijit singh"));

        assertEquals(
                "jee physics",
                CommandRouter.googleSearchQuery(
                        "search JEE physics"));
    }

    @Test public void memoryParsing() {
        assertEquals(
                "my study time is 7 pm",
                CommandRouter.memoryText(
                        "remember my study time is 7 pm"));

        assertNull(
                CommandRouter.memoryText(
                        "open youtube"));
    }

    @Test public void wakeRoutingRemainsStableAcross1000Repetitions() {
        for (int i = 0; i < 1000; i++) {
            assertEquals(
                    "open youtube",
                    CommandRouter.removeWakePhrase(
                            "Hey Buddy open YouTube"));

            assertEquals(
                    "search jee physics",
                    CommandRouter.removeWakePhrase(
                            "Buddy search JEE physics"));

            assertNull(
                    CommandRouter.removeWakePhrase(
                            "open youtube"));
        }
    }
}
