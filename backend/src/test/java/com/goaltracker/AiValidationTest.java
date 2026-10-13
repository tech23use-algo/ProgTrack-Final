package com.goaltracker;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goaltracker.ai.AiDtos;
import com.goaltracker.ai.GroqClient;
import com.goaltracker.ai.GroqProperties;

class AiValidationTest {

    @Test
    void responseDtoParses() {
        var r = new AiDtos.Result("OK", null, List.of(new AiDtos.Milestone("Read", "Basics", 30, 1)));
        assertEquals("OK", r.status());
        assertEquals(1, r.milestones().size());
    }

    @Test
    void footballStrikerCurriculumGeneratesDetailedEverydayOutcomes() {
        var properties = new GroqProperties("", "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile", 0.2, 30);
        var client = new GroqClient(properties, new ObjectMapper());

        var result = client.breakdown("Skill: play football as striker\nDuration: 14 days\nHours available per day: 1.0\nDeadline: 2026-10-27");
        assertEquals("OK", result.status());
        assertEquals(14, result.milestones().size());

        // Verify all 14 days have distinct titles
        var titles = new HashSet<String>();
        for (var m : result.milestones()) {
            titles.add(m.title());
            assertTrue(m.description().contains("Daily Outcome:"), "Each day must have a Daily Outcome checkpoint");
            assertTrue(m.description().contains("Drills"), "Each day must have a timed Drills breakdown");
            assertTrue(m.description().contains("Technique Mechanics:"), "Each day must include mechanics");
            assertEquals(60, m.estimatedMinutes());
        }
        assertEquals(14, titles.size(), "All 14 days must have unique titles");

        // Day 1: Stance & first-touch
        var day1 = result.milestones().get(0);
        assertTrue(day1.title().toLowerCase().contains("first-touch") || day1.title().toLowerCase().contains("stance"));

        // Day 2: Dribbling
        var day2 = result.milestones().get(1);
        assertTrue(day2.title().toLowerCase().contains("dribbling") || day2.description().toLowerCase().contains("dribble"));

        // Day 3: Particular 1v1 skill / feint
        var day3 = result.milestones().get(2);
        assertTrue(day3.title().toLowerCase().contains("step-over") || day3.title().toLowerCase().contains("scissors") || day3.description().toLowerCase().contains("feint"));

        // Day 4: Shooting practice
        var day4 = result.milestones().get(3);
        assertTrue(day4.title().toLowerCase().contains("shooting") || day4.description().toLowerCase().contains("shooting") || day4.description().toLowerCase().contains("laces"));

        // Day 5: Running practice & movement
        var day5 = result.milestones().get(4);
        assertTrue(day5.title().toLowerCase().contains("running") || day5.title().toLowerCase().contains("runs") || day5.description().toLowerCase().contains("offside"));
    }

    @Test
    void pianoCurriculumGeneratesDayToDayTechniqueProgression() {
        var properties = new GroqProperties("", "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile", 0.2, 30);
        var client = new GroqClient(properties, new ObjectMapper());

        var result = client.breakdown("Skill: piano\nDuration: 14 days\nHours available per day: 1.0\nDeadline: 2026-10-26");
        assertEquals("OK", result.status());
        assertEquals(14, result.milestones().size());

        var day1 = result.milestones().get(0);
        assertEquals(1, day1.dayNumber());
        assertTrue(day1.title().startsWith("Day 1:"));
        assertTrue(day1.description().toLowerCase().contains("posture") || day1.title().toLowerCase().contains("posture"));
        assertTrue(day1.description().toLowerCase().contains("curved") || day1.description().toLowerCase().contains("holding") || day1.description().toLowerCase().contains("middle c"));
        assertTrue(day1.description().contains("Daily Outcome:"));
        assertEquals(60, day1.estimatedMinutes());

        var day2 = result.milestones().get(1);
        assertEquals(2, day2.dayNumber());
        assertTrue(day2.title().startsWith("Day 2:"));
        assertTrue(day2.description().toLowerCase().contains("legato") || day2.description().toLowerCase().contains("finger") || day2.title().toLowerCase().contains("five-finger"));
        assertEquals(60, day2.estimatedMinutes());
    }

    @Test
    void dailyMinutesAdjustsToHoursPerDay() {
        var properties = new GroqProperties("", "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile", 0.2, 30);
        var client = new GroqClient(properties, new ObjectMapper());

        var halfHourResult = client.breakdown("Skill: play football as striker\nDuration: 7 days\nHours available per day: 0.5\nDeadline: 2026-10-19");
        assertEquals(7, halfHourResult.milestones().size());
        assertEquals(30, halfHourResult.milestones().get(0).estimatedMinutes());

        var twoHourResult = client.breakdown("Skill: play football as striker\nDuration: 7 days\nHours available per day: 2.0\nDeadline: 2026-10-19");
        assertEquals(7, twoHourResult.milestones().size());
        assertEquals(120, twoHourResult.milestones().get(0).estimatedMinutes());
    }

    @Test
    void guitarCurriculumGeneratesPostureAndProgression() {
        var properties = new GroqProperties("", "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile", 0.2, 30);
        var client = new GroqClient(properties, new ObjectMapper());

        var result = client.breakdown("Skill: guitar\nDuration: 7 days\nHours available per day: 1.0\nDeadline: 2026-10-19");
        assertEquals("OK", result.status());
        assertEquals(7, result.milestones().size());

        var day1 = result.milestones().get(0);
        assertEquals(1, day1.dayNumber());
        assertTrue(day1.title().contains("Day 1:"));
        assertTrue(day1.description().toLowerCase().contains("posture") || day1.description().toLowerCase().contains("pick"));
        assertTrue(day1.description().contains("Daily Outcome:"));
        assertEquals(60, day1.estimatedMinutes());
    }

    @Test
    void cricketCurriculumGenerates24DaysWithFirstSkillSpecified() {
        var properties = new GroqProperties("", "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile", 0.2, 30);
        var client = new GroqClient(properties, new ObjectMapper());

        var result = client.breakdown("Skill: criket\nDuration: 24 days\nHours available per day: 1.0\nDeadline: 2026-11-06");
        assertEquals("OK", result.status());
        assertEquals(24, result.milestones().size());

        // Day 1 specifies what skill to learn first: grip and stance
        var day1 = result.milestones().get(0);
        assertEquals(1, day1.dayNumber());
        assertTrue(day1.title().toLowerCase().contains("what to learn first") || day1.title().toLowerCase().contains("grip"));
        assertTrue(day1.description().toLowerCase().contains("v-grip") || day1.description().toLowerCase().contains("stance"));

        // Day 2 specifies forward defense
        var day2 = result.milestones().get(1);
        assertEquals(2, day2.dayNumber());
        assertTrue(day2.title().toLowerCase().contains("forward defense") || day2.title().toLowerCase().contains("defensive"));

        // Day 3 specifies straight drive
        var day3 = result.milestones().get(2);
        assertTrue(day3.title().toLowerCase().contains("straight drive"));

        // All 24 days have distinct titles and daily outcomes
        var titles = new HashSet<String>();
        for (var m : result.milestones()) {
            titles.add(m.title());
            assertTrue(m.description().contains("Daily Outcome:"));
            assertEquals(60, m.estimatedMinutes());
        }
        assertEquals(24, titles.size());
    }
}
