package com.shyam026.buddyassistant;

import org.junit.Test;
import static org.junit.Assert.*;

public class CommandRouterTest {

    @Test public void normalizationAndWakePhrase(){
        assertEquals("youtube kholo", CommandRouter.normalize("YouTube KHOLo!!!"));
        assertTrue(CommandRouter.isWakePhrase("Hey Buddy"));
        assertTrue(CommandRouter.isWakePhrase("hey buddy youtube kholo"));
        assertEquals("youtube kholo", CommandRouter.removeWakePhrase("Hey Buddy, YouTube kholo"));
    }

    @Test public void commandParsing100Cases(){
        String[] wake={
            "Hey Buddy","hey buddy","Hey Buddy, YouTube kholo","hey buddy volume badhao",
            "HEY BUDDY whatsapp message hello to rahul","hey buddy search cats"
        };
        for(int i=0;i<100;i++){
            String w=wake[i%wake.length];
            assertTrue("wake case "+i, CommandRouter.isWakePhrase(w));
        }

        String[] yt={
            "youtube search cats","youtube pe search jee physics","youtube par search java",
            "youtube me search lo-fi","youtube pe coding tutorial"
        };
        for(int i=0;i<100;i++){
            String q=CommandRouter.youtubeQuery(yt[i%yt.length]);
            assertNotNull("youtube case "+i,q);
            assertFalse(q.isEmpty());
        }

        String[] google={
            "search weather","google pe search java","google par search jee",
            "google me search android","find calculator"
        };
        for(int i=0;i<100;i++){
            String q=CommandRouter.googleSearchQuery(google[i%google.length]);
            assertNotNull("google case "+i,q);
            assertFalse(q.isEmpty());
        }
    }

    @Test public void hindiAndEdgeCases(){
        assertEquals("youtube kholo", CommandRouter.normalize("YouTube   KHOLo!!!"));
        assertNull(CommandRouter.youtubeQuery("youtube kholo"));
        assertNull(CommandRouter.googleSearchQuery("volume badhao"));
        assertNull(CommandRouter.parseWhatsApp("whatsapp kholo"));
        assertEquals(Integer.valueOf(60), CommandRouter.extractNumber("brightness 60"));
        assertEquals(Integer.valueOf(100), CommandRouter.extractNumber("volume 100 percent"));
    }

    @Test public void repeatedParserStress(){
        String[] commands={
            "hey buddy","hey buddy youtube kholo","youtube pe search physics",
            "search jee maths","whatsapp message hello to rahul","volume 50",
            "brightness 70","call rahul","back jao","home screen"
        };
        for(int i=0;i<100;i++){
            String c=commands[i%commands.length];
            String n=CommandRouter.normalize(c);
            assertNotNull(n);
            assertFalse(n.isEmpty());
        }
    }

    @Test public void whatsappParsing(){
        CommandRouter.WhatsAppRequest r=CommandRouter.parseWhatsApp("whatsapp message hello bhai to rahul");
        assertNotNull(r);
        assertEquals("rahul",r.contact);
        assertEquals("hello bhai",r.message);

        r=CommandRouter.parseWhatsApp("whatsapp pe msg kya haal hai to manish");
        assertNotNull(r);
        assertEquals("manish",r.contact);
        assertEquals("kya haal hai",r.message);
    }

    @Test public void callParsing(){
        assertEquals("rahul", CommandRouter.parseCall("rahul ko call karo").target);
        assertEquals("rahul", CommandRouter.parseCall("call rahul").target);
        assertEquals("9876543210", CommandRouter.parseCall("dial 9876543210").target);
        assertEquals("mummy", CommandRouter.parseCall("phone mummy").target);
    }

    @Test public void numberParsing(){
        for(int i=0;i<100;i++){
            int n=i%101;
            assertEquals(Integer.valueOf(n),CommandRouter.extractNumber("volume "+n));
        }
    }
}
