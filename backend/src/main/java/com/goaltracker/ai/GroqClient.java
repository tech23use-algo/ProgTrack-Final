package com.goaltracker.ai;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GroqClient {

    private static final String SYSTEM = """
You are ProgTrack AI, an elite progressive learning coach and curriculum architect.
Your mission is to transform any goal or learnable skill into a rigorous, day-by-day, step-by-step masterclass curriculum.

CORE PEDAGOGICAL PRINCIPLES:
1. EVERY SINGLE DAY MUST DELIVER A BRAND NEW, DISTINCT SKILL OUTCOME:
   - NEVER create generic or repetitive practice days (e.g. avoid 'Practice dribbling again', 'Work on goal', 'Continue practice').
   - Every day focuses on ONE specific, dedicated sub-skill, drill, or technique that systematically builds on the previous day.
   - Example (Football/Soccer as Striker):
     * Day 1: Striker Stance, Body Shape & First-Touch Cushioning (absorbing driven passes with inside of foot).
     * Day 2: Close-Control Dribbling & Cone Slalom Drills (using laces, inside/outside foot cuts at pace).
     * Day 3: 1v1 Skill Feints: The Step-Over & Scissors Move to Beat Defenders (dropping shoulder to unbalance markers).
     * Day 4: Striker Power Shooting: Laces Drive & Low Corner Placement (plant foot positioning, locking ankle, striking through ball).
     * Day 5: Striker Off-The-Ball Runs: Curved Runs & Beating the Offside Trap (timing blind-side runs, near-post darts).
     * Day 6: Hold-Up Play, Shielding & Layoffs (pinning defenders with body leverage, first-time link-up play).
     * Day 7: Aerial Finishing, Heading Mechanics & Near-Post Attacks (timing leaps, downward forehead contact).
     * Day 8: Weak-Foot Finishing Drills (non-dominant foot power and precision).
     * Day 9: 1v1 Breakaways vs Goalkeeper: Composure & Rounding the Keeper (feinting shot, delicate chip/dink finish).
     * Day 10: Volleys & Half-Volleys off Crosses (striking out of the air, keeping chest over ball).
     * Day 11: Poacher Instincts: Rebound Anticipation & 1-Touch Box Finishing (reading deflections, rapid reaction taps).
     * Day 12: Striker Defensive Pressing & Cutting Passing Lanes (curving pressure runs to isolate fullbacks).
     * Day 13: High-Intensity Match Finishing Under Pressure & Fatigue (rapid sprints into contested shots).
     * Day 14: Match Simulation Showcase & Performance Evaluation.
   - Example (Piano):
     * Day 1: Proper Seated Posture, Curved Hand Shape & Middle C Landmarks.
     * Day 2: Right-Hand 5-Finger Legato Touch (C-D-E-F-G smooth connections).
     * Day 3: Left-Hand Bass Clef Finger Independence.
     * Day 4: Rhythm Fundamentals & Metronome Timing at 60 BPM.
     * Day 5: Two-Hand Parallel Motion Coordination.
     * Day 6: Reading Staff Intervals (2nds and 3rds).
     * Day 7: First Melodic Song Piece (Ode to Joy melody).
     * Day 8: Triad Harmony & C Major / G7 Chord Transitions.
     * Day 9: Left-Hand Chords with Right-Hand Melody.
     * Day 10: Dynamics & Expression (Piano vs Forte Control).
     * Day 11: Thumb-Under Tuck & One-Octave C Major Scale.
     * Day 12: Rolling Arpeggios & Wrist Flexibility Drills.
     * Day 13: Hands-Together Song Fluidity & Tricky Bar Isolation.
     * Day 14: Full Performance Recording & Self-Evaluation.

2. HIGHLY DETAILED ACTIONABLE DESCRIPTIONS:
   Every milestone description must be rich and structured with:
   - Technique Mechanics: Clear physical/mental execution cues (e.g. body angle, posture, foot placement, hand positioning).
   - Drills (with time breakdown): Concrete timed drills fitting the daily minutes budget (e.g. Warm-up, Core Technique Drills, Scenario Application).
   - Daily Outcome: A measurable benchmark to verify today's skill was mastered (e.g. 'Daily Outcome: Score 8 out of 10 laces strikes into bottom corners from 18 yards').

3. DAILY TIME BUDGET:
   - Convert the user's daily hours budget into exact minutes (estimatedMinutes = hoursPerDay * 60).
   - Set each milestone's estimatedMinutes to this exact value.
   - Ensure the drill time breakdown in the description sums up to this exact daily minutes budget.

4. SEQUENTIAL DAY NUMBERING:
   - Number dayNumber from 1 up to the total days requested (or until deadline).
   - Format each milestone title as: 'Day X: <Specific Technique or Skill Outcome>'.

JSON SCHEMA:
Return ONLY a valid JSON object matching:
{
  "status": "OK",
  "reason": "Structured daily progressive curriculum generated",
  "milestones": [
    {
      "dayNumber": 1,
      "title": "Day 1: Striker Stance, Body Shape & First-Touch Cushioning",
      "description": "Technique Mechanics: Keep knees slightly bent on balls of feet with an open body shape facing the pitch. Cushion oncoming passes using the inside arch of your foot, absorbing ball momentum upon contact.\\n\\nDrills (60m):\\n- Warm-up (10m): Dynamic stretches, ankle mobility & 150 ball taps.\\n- Core Drill (30m): Wall-pass first-touch cushioning (50 reps right foot, 50 reps left foot).\\n- Application (15m): Receive driven ball on the half-turn and accelerate 5 yards into open space.\\n- Review (5m): Cool-down and touch consistency log.\\n\\nDaily Outcome: 90% of first touches settle cleanly within 1 yard of your body ready for your next move.",
      "estimatedMinutes": 60
    }
  ]
}

If the user's input is not a genuine goal or learnable skill, return:
{
  "status": "REJECTED",
  "reason": "Input is not a genuine goal or learnable skill."
}
Never output markdown code blocks or commentary outside the JSON object.
""";

    private final GroqProperties properties;
    private final ObjectMapper mapper;
    private final RestClient client;

    public GroqClient(GroqProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
        String key = properties.apiKey() == null ? "" : properties.apiKey();
        this.client = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public AiDtos.Result breakdown(String input) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            return fallback(input);
        }

        String model = properties.model() != null && !properties.model().isBlank()
                ? properties.model()
                : "llama-3.3-70b-versatile";

        var req = new AiDtos.ChatRequest(
                List.of(
                        new AiDtos.Message("system", SYSTEM),
                        new AiDtos.Message("user", input)
                ),
                model,
                properties.temperature(),
                Map.of("type", "json_object")
        );

        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                String body = client.post()
                        .uri("/chat/completions")
                        .body(req)
                        .retrieve()
                        .body(String.class);

                if (body != null && !body.isBlank()) {
                    JsonNode envelope = mapper.readTree(body);
                    String content = envelope.path("choices").path(0).path("message").path("content").asText(null);
                    if (content != null && !content.isBlank()) {
                        String cleaned = cleanJsonContent(content);
                        JsonNode parsed = mapper.readTree(cleaned);
                        return normalize(parsed, input);
                    }
                }
            } catch (Exception ex) {
                if (attempt == 1) {
                    return fallback(input);
                }
            }
        }
        return fallback(input);
    }

    private String cleanJsonContent(String content) {
        if (content == null) return "{}";
        String s = content.trim();
        if (s.startsWith("```json")) {
            s = s.substring(7);
        } else if (s.startsWith("```")) {
            s = s.substring(3);
        }
        if (s.endsWith("```")) {
            s = s.substring(0, s.length() - 3);
        }
        return s.trim();
    }

    private AiDtos.Result normalize(JsonNode json, String input) {
        String status = json.path("status").asText(null);
        if ("REJECTED".equalsIgnoreCase(status)) {
            return new AiDtos.Result("REJECTED", json.path("reason").asText("Input was not recognized as a goal or skill."), List.of());
        }

        if (json.has("milestones") && json.get("milestones").isArray()) {
            return new AiDtos.Result("OK", json.path("reason").asText("Plan created"), readMilestones(json.get("milestones"), input));
        }
        if (json.has("plan") && json.get("plan").isArray()) {
            return new AiDtos.Result("OK", json.path("reason").asText("Plan created"), readPlan(json.get("plan"), input));
        }
        if (json.has("tasks") && json.get("tasks").isArray()) {
            return new AiDtos.Result("OK", json.path("reason").asText("Plan created"), readMilestones(json.get("tasks"), input));
        }
        if (json.has("days") && json.get("days").isArray()) {
            return new AiDtos.Result("OK", json.path("reason").asText("Plan created"), readPlan(json.get("days"), input));
        }
        if (json.isArray()) {
            return new AiDtos.Result("OK", "Plan created", readMilestones(json, input));
        }
        if ("OK".equalsIgnoreCase(status) && json.has("milestones")) {
            return new AiDtos.Result("OK", json.path("reason").asText("Plan created"), readMilestones(json.get("milestones"), input));
        }
        throw new IllegalArgumentException("Unsupported planner schema");
    }

    private List<AiDtos.Milestone> readMilestones(JsonNode nodes, String input) {
        int defaultMinutes = parseDailyMinutes(input);
        var result = new ArrayList<AiDtos.Milestone>();
        int dayCounter = 1;
        for (JsonNode item : nodes) {
            int day = item.path("dayNumber").asInt(dayCounter);
            String rawTitle = item.path("title").asText("").trim();
            String desc = item.path("description").asText("").trim();
            if (rawTitle.isEmpty()) {
                rawTitle = desc.isEmpty() ? "Skill technique session" : desc;
            }
            String title = rawTitle;
            if (!title.toLowerCase(Locale.ROOT).startsWith("day ")) {
                title = "Day " + day + ": " + title;
            }
            int minutes = item.path("estimatedMinutes").asInt(defaultMinutes);
            if (minutes <= 0) minutes = defaultMinutes;
            result.add(new AiDtos.Milestone(title, desc.isEmpty() ? title : desc, minutes, Math.max(1, day)));
            dayCounter++;
        }
        if (result.isEmpty()) throw new IllegalArgumentException("No milestones");
        return result;
    }

    private List<AiDtos.Milestone> readPlan(JsonNode days, String input) {
        int defaultMinutes = parseDailyMinutes(input);
        var result = new ArrayList<AiDtos.Milestone>();
        int dayCounter = 1;
        for (JsonNode day : days) {
            int number = day.path("dayNumber").asInt(dayCounter++);
            JsonNode tasks = day.has("tasks") ? day.get("tasks") : (day.has("milestones") ? day.get("milestones") : null);
            if (tasks != null && tasks.isArray() && !tasks.isEmpty()) {
                for (JsonNode task : tasks) {
                    String rawTitle = task.path("title").asText("").trim();
                    String desc = task.path("description").asText("").trim();
                    if (rawTitle.isEmpty()) rawTitle = desc.isEmpty() ? "Skill technique session" : desc;
                    String title = rawTitle;
                    if (!title.toLowerCase(Locale.ROOT).startsWith("day ")) {
                        title = "Day " + number + ": " + title;
                    }
                    int minutes = task.path("estimatedMinutes").asInt(defaultMinutes);
                    if (minutes <= 0) minutes = defaultMinutes;
                    result.add(new AiDtos.Milestone(title, desc.isEmpty() ? title : desc, minutes, Math.max(1, number)));
                }
            } else {
                String rawTitle = day.path("title").asText("").trim();
                String desc = day.path("description").asText("").trim();
                if (rawTitle.isEmpty()) rawTitle = desc.isEmpty() ? "Technique & Practice" : desc;
                String title = rawTitle;
                if (!title.toLowerCase(Locale.ROOT).startsWith("day ")) {
                    title = "Day " + number + ": " + title;
                }
                int minutes = day.path("estimatedMinutes").asInt(defaultMinutes);
                if (minutes <= 0) minutes = defaultMinutes;
                result.add(new AiDtos.Milestone(title, desc.isEmpty() ? title : desc, minutes, Math.max(1, number)));
            }
        }
        if (result.isEmpty()) throw new IllegalArgumentException("No daily tasks");
        return result;
    }

    private int parseDailyMinutes(String input) {
        if (input == null) return 60;
        String lower = input.toLowerCase(Locale.ROOT);
        if (lower.contains("hours available per day:")) {
            try {
                String hStr = input.replaceAll("(?si).*hours available per day:\\s*([0-9.]+).*", "$1").trim();
                double hours = Double.parseDouble(hStr);
                return Math.max(15, (int) Math.round(hours * 60));
            } catch (Exception ignored) {}
        }
        return 60;
    }

    private AiDtos.Result fallback(String input) {
        if (input == null) input = "";
        String lower = input.toLowerCase(Locale.ROOT);

        String title = "Skill Practice";
        if (lower.contains("skill:")) {
            title = input.replaceAll("(?si).*skill:\\s*([^\\n]+).*", "$1").trim();
        } else if (lower.contains("goal:")) {
            title = input.replaceAll("(?si).*goal:\\s*([^\\n]+).*", "$1").trim();
        } else {
            String first = input.split("\\n", 2)[0].trim();
            if (!first.isEmpty()) title = first;
        }

        int dailyMinutes = parseDailyMinutes(input);

        int totalDays = 14;
        if (lower.contains("duration:")) {
            try {
                String dStr = input.replaceAll("(?si).*duration:\\s*([0-9]+).*", "$1").trim();
                totalDays = Integer.parseInt(dStr);
            } catch (Exception ignored) {}
        } else if (lower.contains("deadline:")) {
            try {
                String dlStr = input.replaceAll("(?si).*deadline:\\s*([0-9]{4}-[0-9]{2}-[0-9]{2}).*", "$1").trim();
                LocalDate dl = LocalDate.parse(dlStr);
                totalDays = (int) Math.max(1, ChronoUnit.DAYS.between(LocalDate.now(), dl) + 1);
            } catch (Exception ignored) {}
        }
        totalDays = Math.max(1, Math.min(365, totalDays));

        List<AiDtos.Milestone> tasks;
        if (lower.contains("cricket") || lower.contains("criket") || lower.contains("batting") || lower.contains("bowling")) {
            tasks = generateCricketCurriculum(totalDays, dailyMinutes);
        } else if (lower.contains("football") || lower.contains("soccer") || lower.contains("striker") || lower.contains("dribbl")) {
            tasks = generateFootballStrikerCurriculum(totalDays, dailyMinutes);
        } else if (lower.contains("piano") || lower.contains("keyboard")) {
            tasks = generatePianoCurriculum(totalDays, dailyMinutes);
        } else if (lower.contains("guitar")) {
            tasks = generateGuitarCurriculum(totalDays, dailyMinutes);
        } else if (lower.contains("code") || lower.contains("coding") || lower.contains("python") || lower.contains("java") || lower.contains("react") || lower.contains("javascript") || lower.contains("programming")) {
            tasks = generateCodingCurriculum(title, totalDays, dailyMinutes);
        } else {
            tasks = generateGeneralCurriculum(title, totalDays, dailyMinutes);
        }
        return new AiDtos.Result("OK", "Progressive plan created", tasks);
    }

    private List<AiDtos.Milestone> generateFootballStrikerCurriculum(int totalDays, int dailyMinutes) {
        int mWarm = Math.max(5, (int) Math.round(dailyMinutes * 0.15));
        int mCore = Math.max(10, (int) Math.round(dailyMinutes * 0.50));
        int mScen = Math.max(5, (int) Math.round(dailyMinutes * 0.25));
        int mRev = Math.max(3, dailyMinutes - mWarm - mCore - mScen);

        List<String[]> strikerDays = List.of(
            new String[]{
                "Striker Stance, Body Shape & First-Touch Cushioning",
                "Technique Mechanics: Stand on the balls of your feet with knees flexed and an open body shape facing the pitch. Cushion driven passes using the inside arch of your foot, absorbing ball momentum upon contact.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Ankle mobility, dynamic stretches & 150 ball taps.\n- Core Drill (" + mCore + "m): Wall-pass first-touch cushioning: 50 reps right foot, 50 reps left foot.\n- Scenario (" + mScen + "m): Receive driven pass on the half-turn and take an immediate directional touch into open space.\n- Review (" + mRev + "m): Cool-down and touch consistency log.\n\nDaily Outcome: 90% of first touches settle cleanly within 1 yard of your body ready for your next move."
            },
            new String[]{
                "Close-Control Dribbling & Cone Slalom Drills",
                "Technique Mechanics: Dribble using small, rapid touches with the laces and outside of your foot. Keep center of gravity low and lift eyes up between touches to scan defenders.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Sole rolls, tick-tocks & fast footwork ladders.\n- Core Drill (" + mCore + "m): 6-cone slalom course: inside/outside foot cuts at progressive speeds (10 sets right foot, 10 sets left foot).\n- Scenario (" + mScen + "m): 15-yard sprint dribble with a sudden 90-degree cut and acceleration burst.\n- Review (" + mRev + "m): Cool-down and agility check.\n\nDaily Outcome: Navigate the 6-cone slalom at full speed without touching a cone in under 12 seconds."
            },
            new String[]{
                "1v1 Attacking Moves: The Step-Over & Scissors Feint",
                "Technique Mechanics: Plant non-faking foot firmly while whipping faking foot over the ball from inside to outside. Drop your shoulder to sell the direction before exploding the opposite way with the outside of your other foot.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Stationary step-overs and scissors drills.\n- Core Drill (" + mCore + "m): Approach cone defender at pace, execute a crisp step-over feint, and accelerate away (20 reps right, 20 reps left).\n- Scenario (" + mScen + "m): 1v1 confrontation: attack a dummy defender, execute a scissors move, and break away toward the box.\n- Review (" + mRev + "m): Movement smoothness and deception review.\n\nDaily Outcome: Fluidly unbalance defenders with convincing shoulder drops and sharp explosive exits."
            },
            new String[]{
                "Striker Power Shooting: Laces Drive & Low Corner Placement",
                "Technique Mechanics: Plant non-kicking foot 6 inches beside the ball pointing directly at target. Lock ankle down with toes pointed toward the turf. Strike dead-center of the ball with the laces, keeping chest and knee over the ball, landing on your kicking foot.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Stationary form shooting from 12 yards.\n- Core Drill (" + mCore + "m): 30 laces power drives from the edge of the box (18 yards) aiming for the bottom corners.\n- Scenario (" + mScen + "m): Roll ball forward, take one prep touch, and release a quick laces shot within 2 steps.\n- Review (" + mRev + "m): Shot power and target accuracy evaluation.\n\nDaily Outcome: Place at least 70% of laces power strikes firmly into bottom corners without skying the ball."
            },
            new String[]{
                "Striker Running Practice: Curved Runs & Beating the Offside Trap",
                "Technique Mechanics: Maintain an onside position by curving your run along the backline rather than running straight. Time your explosive forward acceleration the instant the passer winds up to strike the ball.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Sprint mechanics, high knees, and deceleration brakes.\n- Core Drill (" + mCore + "m): 15 curved run repetitions timing sprints past offside mannequin poles to collect through-balls.\n- Scenario (" + mScen + "m): Blind-side dart: check towards midfielder, suddenly spin off defender's blind shoulder, and sprint into the channel.\n- Review (" + mRev + "m): Run timing and burst speed check.\n\nDaily Outcome: Consistently burst behind defensive lines without being caught offside across 15 run trials."
            },
            new String[]{
                "Hold-Up Play, Shielding & Midfield Link-Up Layoffs",
                "Technique Mechanics: Back into defending center back with a wide, low stance and outstretched arm for feel. Pin defender using your hips, keep the ball on your far foot away from pressure, and play crisp one-touch layoffs to advancing midfielders.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Core stability, low stance holds & shielding balance.\n- Core Drill (" + mCore + "m): Receive driven ball under physical contact, hold for 3 seconds, and execute a firm 1-touch layoff (25 reps each side).\n- Scenario (" + mScen + "m): Receive with back to goal, roll defender on the half-turn, and take an immediate shot.\n- Review (" + mRev + "m): Physical hold-up efficiency assessment.\n\nDaily Outcome: Successfully shield the ball from aggressive pressure and lay off 10 clean balls to supporting runners."
            },
            new String[]{
                "Aerial Finishing, Heading Mechanics & Near-Post Attacks",
                "Technique Mechanics: Time leap off one foot, arch back slightly in the air, and strike through the equator of the ball using the hard center of your forehead with eyes wide open and mouth closed. Direct headers downwards into corners.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Neck activation, jump timing & self-tossed header mechanics.\n- Core Drill (" + mCore + "m): 30 near-post darting headers from crossing deliveries (15 left side, 15 right side).\n- Scenario (" + mScen + "m): Attacking crosses between two defensive markers with downward finishing.\n- Review (" + mRev + "m): Contact point and downward trajectory check.\n\nDaily Outcome: Direct 75% of attacking headers downwards towards target corners without flinching."
            },
            new String[]{
                "Weak-Foot Finishing & Non-Dominant Foot Power Drills",
                "Technique Mechanics: Replicate dominant foot mechanics exactly: solid non-kicking plant foot, locked weak-foot ankle, and committed follow-through. Practice both low driven shots and instep curled finishes with weak foot.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Weak-foot passing against a wall (100 reps).\n- Core Drill (" + mCore + "m): 35 weak-foot strikes from 16 yards out targeting both bottom corners.\n- Scenario (" + mScen + "m): Receive cross across body and take a first-time shot with weak foot without a prep touch.\n- Review (" + mRev + "m): Weak-foot confidence and striking clean evaluation.\n\nDaily Outcome: Achieve clean laces contact and target accuracy with your non-dominant foot on 70% of attempts."
            },
            new String[]{
                "1v1 Breakaways vs Goalkeeper: Composure & Finishing Options",
                "Technique Mechanics: Slow down slightly as you enter the box to regain balance. Read goalkeeper stance: if they stay back, finish low into corners; if they rush out, round them with a sudden directional push or execute a subtle dink over their diving body.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Dribbling at speed into deceleration zone.\n- Core Drill (" + mCore + "m): 20 full breakaway simulations from halfway line finishing within 3 seconds of entering the box.\n- Scenario (" + mScen + "m): Alternate between rounding the goalkeeper and early near-post snapshot finishes.\n- Review (" + mRev + "m): Composure and decision-making speed review.\n\nDaily Outcome: Convert 80% of 1v1 breakaway opportunities with calm, calculated finishes."
            },
            new String[]{
                "Volleys & Half-Volleys off Crosses: Striking Out of the Air",
                "Technique Mechanics: Keep chest and knee firmly over the ball as it drops. Lock ankle and point toes down for side volleys or laces drives. Strike through the center-top of the ball to prevent ballooning over the crossbar.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Self-tossed volley drills and hip flexibility.\n- Core Drill (" + mCore + "m): 30 half-volley and full-volley strikes off bouncing service feeds from wide angles.\n- Scenario (" + mScen + "m): Attacking a driven cutback cross on the volley from 12 yards out.\n- Review (" + mRev + "m): Clean contact consistency and ball flight control.\n\nDaily Outcome: Keep 80% of airborne strikes on target without skying over the bar."
            },
            new String[]{
                "Penalty Box Poacher Instincts: Rebound Anticipation & 1-Touch Finishes",
                "Technique Mechanics: Never watch the shot; anticipate the goalkeeper spill by taking 2 aggressive steps toward goal the second a teammate shoots. Use any legal body part (toe poke, side foot, knee) to steer loose balls across the line.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Rapid agility ladder and reactionary sprint bursts.\n- Core Drill (" + mCore + "m): 25 random rebound reaction drills: deflect ball off wall or post, striker reacts and finishes with 1 touch in under 1 second.\n- Scenario (" + mScen + "m): Crowded 6-yard box chaos drill: react to deflections amidst traffic.\n- Review (" + mRev + "m): Reaction speed and penalty box sharpness.\n\nDaily Outcome: Score 90% of loose ball rebounds within the 6-yard box with instantaneous 1-touch finishes."
            },
            new String[]{
                "Striker Defensive Pressing & Blocking Passing Lanes",
                "Technique Mechanics: Initiate defensive press with a curved sprint angle that cuts off the passing lane to the fullback, forcing the center back onto their weaker foot. Close down rapidly until 3 yards away, then decelerate and match their hips.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Curved sprint acceleration and deceleration stops.\n- Core Drill (" + mCore + "m): 15 high-press sprint triggers: read center back body cue, sprint on curved line, and intercept or force error.\n- Scenario (" + mScen + "m): Win ball high up pitch from press and immediately transition into a 2-touch shot.\n- Review (" + mRev + "m): Pressing discipline and turnover conversion.\n\nDaily Outcome: Execute 10 clean pressing triggers that force an errant pass or direct turnover."
            },
            new String[]{
                "High-Intensity Match Finishing Under Pressure & Fatigue",
                "Technique Mechanics: Maintain technical composure and biomechanical precision even when heart rate is near maximum. Keep ankles locked and head still regardless of physical exhaustion.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): High-intensity interval sprints and recovery breaths.\n- Core Drill (" + mCore + "m): 15 sprint-and-finish circuits: 30-yard recovery sprint, receive ball, battle defender shoulder-to-shoulder, and finish cleanly.\n- Scenario (" + mScen + "m): 90th-minute game scenario: 2 physical challenges followed by a game-winning shot attempt.\n- Review (" + mRev + "m): Stamina and technical resilience check.\n\nDaily Outcome: Maintain a 75%+ shooting accuracy rate during the final high-fatigue repetitions."
            },
            new String[]{
                "Striker Showcase Match Simulation & Technical Evaluation",
                "Technique Mechanics: Synthesize all 13 core striker competencies: movement, first-touch, dribbling, 1v1 feints, hold-up play, laces shooting, and aerial finishing in a full match simulation.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Complete pre-match striker warm-up routine.\n- Core Drill (" + mCore + "m): 6-stage Striker Skills Combine: 1v1 dribble -> hold-up layoff -> curved run -> weak foot strike -> header -> 1v1 breakaway.\n- Scenario (" + mScen + "m): Scrimmage match play or target combine testing with video recording.\n- Review (" + mRev + "m): Video review, skill scorecard benchmark, and next training cycle plan.\n\nDaily Outcome: Benchmark your completed striker scorecard and achieve mastery in all core forward categories."
            }
        );

        var list = new ArrayList<AiDtos.Milestone>();
        for (int i = 0; i < totalDays; i++) {
            int dayNum = i + 1;
            String title;
            String desc;
            if (i < strikerDays.size()) {
                title = "Day " + dayNum + ": " + strikerDays.get(i)[0];
                desc = strikerDays.get(i)[1];
            } else {
                title = "Day " + dayNum + ": Advanced Striker Tactics & Repertoire";
                desc = "Technique Mechanics: Refine high-level forward play: penalty kick psychological routines, free-kick finishing, counter-attacking burst lines, and defensive leadership.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Agility ladders & fast footwork.\n- Core Drill (" + mCore + "m): Match situation set-piece and rapid finishing drills.\n- Scenario (" + mScen + "m): Contested game scenarios with active pressure.\n- Review (" + mRev + "m): Performance evaluation.\n\nDaily Outcome: Execute match-winning plays with calmness and complete technical precision.";
            }
            list.add(new AiDtos.Milestone(title, desc, dailyMinutes, dayNum));
        }
        return list;
    }

    private List<AiDtos.Milestone> generatePianoCurriculum(int totalDays, int dailyMinutes) {
        int mWarm = Math.max(5, (int) Math.round(dailyMinutes * 0.15));
        int mCore = Math.max(10, (int) Math.round(dailyMinutes * 0.50));
        int mScen = Math.max(5, (int) Math.round(dailyMinutes * 0.25));
        int mRev = Math.max(3, dailyMinutes - mWarm - mCore - mScen);

        List<String[]> pianoDays = List.of(
            new String[]{
                "Piano Posture, Hand Shape & Finding Middle C",
                "Technique Mechanics: Sit with upright posture and bench adjusted so forearms rest parallel to keys. Form curved fingers naturally as if holding a bubble or tennis ball (avoid flat fingers or collapsed knuckles). Learn finger numbering 1 to 5 for both hands. Locate Middle C and navigate keyboard using 2-black-key vs 3-black-key landmarks.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Posture alignment, bench height check & flat-surface finger tapping.\n- Core Drill (" + mCore + "m): Locating all C keys across octaves with curved fingertips without looking down.\n- Application (" + mScen + "m): Alternating hands on Middle C with relaxed wrists and clean keystrokes.\n- Review (" + mRev + "m): Knuckle alignment and shoulder tension release.\n\nDaily Outcome: Flawlessly find Middle C and all octave C notes across the keyboard with relaxed curved hands."
            },
            new String[]{
                "Five-Finger Position & Legato Touch",
                "Technique Mechanics: Position right-hand fingers 1-5 on C-D-E-F-G. Practice pressing each key individually while keeping remaining fingers gently resting on key surfaces without tension. Master legato touch (smoothly connecting each tone without overlap).\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Finger lift and release drills on tabletop.\n- Core Drill (" + mCore + "m): C-D-E-F-G ascending and descending 5-finger patterns at 50 BPM.\n- Application (" + mScen + "m): 3-note stepping drills (C-D-E, D-E-F, E-F-G) with seamless legato connections.\n- Review (" + mRev + "m): Finger independence and wrist flexibility check.\n\nDaily Outcome: Play C-D-E-F-G ascending and descending 10 times with smooth legato and zero wrist stiffness."
            },
            new String[]{
                "Left-Hand Bass Clef & Finger Independence",
                "Technique Mechanics: Place left-hand finger 5 on Bass C up to thumb (finger 1) on G. Practice ascending and descending 5-finger patterns. Focus on equal strike velocity and relaxed wrists.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Left hand wrist circles & finger stretches.\n- Core Drill (" + mCore + "m): Left-hand Bass C-D-E-F-G patterns maintaining equal sound volume across all fingers.\n- Application (" + mScen + "m): Alternating right hand and left hand 5-finger sequences at 60 BPM.\n- Review (" + mRev + "m): Left-hand pinky (finger 5) strength and curvature check.\n\nDaily Outcome: Match right-hand dexterity and sound consistency with the left hand."
            },
            new String[]{
                "Rhythm Fundamentals & Metronome Timing at 60 BPM",
                "Technique Mechanics: Master quarter notes (1 beat), half notes (2 beats), and whole notes (4 beats) in 4/4 time. Tap rhythms before playing with a metronome at 60 BPM.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Clapping rhythm patterns and counting aloud (1-2-3-4).\n- Core Drill (" + mCore + "m): Playing 5-finger patterns holding notes for full durations in time with metronome.\n- Application (" + mScen + "m): Switching between quarter notes and half notes seamlessly.\n- Review (" + mRev + "m): Tempo consistency and internal clock check.\n\nDaily Outcome: Play 5-finger patterns in sync with a 60 BPM metronome without rushing or dragging."
            },
            new String[]{
                "Two-Hand Parallel Motion Coordination",
                "Technique Mechanics: Combine both hands in C position playing parallel motion (C-D-E-F-G) slowly at 50 BPM. Coordinate simultaneous keystrokes without hesitation.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Hands-separate warm-up run.\n- Core Drill (" + mCore + "m): Both hands playing together in slow motion with synchronized finger strikes.\n- Application (" + mScen + "m): Two-hand contrary motion drill (moving outward and inward).\n- Review (" + mRev + "m): Hand balance and volume symmetry.\n\nDaily Outcome: Coordinate simultaneous keystrokes across both hands in parallel motion without hesitating."
            },
            new String[]{
                "Reading Sheet Music (Treble & Bass Staffs)",
                "Technique Mechanics: Learn line and space notes: E-G-B-D-F and F-A-C-E for Treble clef, G-B-D-F-A and A-C-E-G for Bass clef. Sight-read simple intervals (2nds and 3rds).\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Flashcard note naming drill.\n- Core Drill (" + mCore + "m): Sight-reading 4-bar simple sheet music exercises without writing letter names.\n- Application (" + mScen + "m): Playing 2nd (step) and 3rd (skip) intervals directly from notation.\n- Review (" + mRev + "m): Reading speed and eye-hand coordination.\n\nDaily Outcome: Read and play an unfamiliar 4-bar line of music with 100% pitch accuracy."
            },
            new String[]{
                "First Melodic Piece (Beethoven's Ode to Joy)",
                "Technique Mechanics: Learn right-hand melody of Beethoven's Ode to Joy in C Major. Focus on accurate fingering, consistent timing, and musical phrasing.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Review melody notes and rhythm.\n- Core Drill (" + mCore + "m): Practice measures 1-8 slowly, looping tricky finger transitions.\n- Application (" + mScen + "m): Play through the complete piece from start to finish at target tempo.\n- Review (" + mRev + "m): Phrasing and musical expression review.\n\nDaily Outcome: Perform Ode to Joy melody completely from beginning to end without stopping."
            },
            new String[]{
                "Introduction to Chords (C Major & G7 Triads)",
                "Technique Mechanics: Form C Major triad (C-E-G) with fingers 1-3-5 and G7 two-finger chord (B-F). Practice dropping relaxed wrist weight into keys simultaneously.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): 3-finger shape formation drills in the air.\n- Core Drill (" + mCore + "m): Alternating between C Major chord and G7 chord with clean 1-3-5 key strikes.\n- Application (" + mScen + "m): 4-beat chord progression rhythm backing.\n- Review (" + mRev + "m): Clean chord sound and lack of finger buzzing.\n\nDaily Outcome: Transition between C Major and G7 chords smoothly without pausing between measures."
            },
            new String[]{
                "Left-Hand Harmony with Right-Hand Melody",
                "Technique Mechanics: Play sustained left-hand chords while right hand plays melody phrases. Dedicate practice to volume balance between hands (melody louder than accompaniment).\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Left hand chord sequence alone.\n- Core Drill (" + mCore + "m): Combine left-hand chords with right-hand Ode to Joy melody line measure-by-measure.\n- Application (" + mScen + "m): Complete two-hand song run-through.\n- Review (" + mRev + "m): Balance between dominant right-hand and supporting left-hand chords.\n\nDaily Outcome: Play complete song hands-together with clean chord accompaniment and singing melody."
            },
            new String[]{
                "Dynamics & Expression (Piano vs Forte Control)",
                "Technique Mechanics: Practice playing soft (piano) and loud (forte) by controlling arm weight and key depression speed rather than finger tension.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Gradual crescendo and decrescendo drills on 5-finger pattern.\n- Core Drill (" + mCore + "m): Contrasting soft and loud phrases within the learned song.\n- Application (" + mScen + "m): Expressive performance with intentional dynamic shaping.\n- Review (" + mRev + "m): Emotional nuance and relaxation check.\n\nDaily Outcome: Clearly demonstrate audible contrast between piano (soft) and forte (loud) without losing tempo."
            },
            new String[]{
                "One-Octave C Major Scale & Thumb-Under Tuck",
                "Technique Mechanics: Learn thumb-under technique: tucking thumb (finger 1) under finger 3 after playing E to reach F smoothly. Drill smooth scale transitions.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Thumb flexibility exercises tucking under palm.\n- Core Drill (" + mCore + "m): 1-2-3-thumb-tuck drill (C-D-E-F-G-A-B-C) ascending and descending.\n- Application (" + mScen + "m): One-octave scale runs with both hands separate at 60 BPM.\n- Review (" + mRev + "m): Thumb elbow stability and horizontal smoothness.\n\nDaily Outcome: Play a 1-octave C Major scale ascending and descending fluidly with correct fingering."
            },
            new String[]{
                "Arpeggios & Wrist Flexibility Exercises",
                "Technique Mechanics: Practice rolling C Major broken chords (arpeggios) across octaves. Maintain flexible, circular wrist motions throughout.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Wrist breathing exercises and arm drops.\n- Core Drill (" + mCore + "m): Broken chords (C-E-G-C) ascending with smooth wrist movement.\n- Application (" + mScen + "m): Rolling arpeggio accompaniment patterns.\n- Review (" + mRev + "m): Suppleness and freedom from tension.\n\nDaily Outcome: Execute flowing 2-octave C Major arpeggios with relaxed, circular wrist motion."
            },
            new String[]{
                "Two-Hand Song Polishing & Difficult Bar Isolation",
                "Technique Mechanics: Combine hands on chosen repertoire piece with steady tempo at target BPM. Work on tricky measures and transition zones.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Play through at half speed.\n- Core Drill (" + mCore + "m): Loop difficult measures 15 times until error-free.\n- Application (" + mScen + "m): Full tempo run-through with expression and dynamics.\n- Review (" + mRev + "m): Memorization and confidence benchmark.\n\nDaily Outcome: Perform complete repertoire piece at full speed with 0 pauses or hesitation."
            },
            new String[]{
                "Full Performance Recording & Self-Evaluation",
                "Technique Mechanics: Record a full run-through of your piece. Review posture, curved hand shape, finger dexterity, and tempo stability.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Pre-performance calming breath & scale warm-up.\n- Core Drill (" + mCore + "m): 3 complete recorded takes of your learned song.\n- Application (" + mScen + "m): Self-critique listening to audio/video recordings.\n- Review (" + mRev + "m): Benchmark achievements and set next milestone targets.\n\nDaily Outcome: Complete a clean performance recording and establish your Phase 2 piano development goals."
            }
        );

        var list = new ArrayList<AiDtos.Milestone>();
        for (int i = 0; i < totalDays; i++) {
            int dayNum = i + 1;
            String title;
            String desc;
            if (i < pianoDays.size()) {
                title = "Day " + dayNum + ": " + pianoDays.get(i)[0];
                desc = pianoDays.get(i)[1];
            } else {
                title = "Day " + dayNum + ": Advanced Piano Repertoire & Technique";
                desc = "Technique Mechanics: Deepen musical mastery: polish complex repertoire measures, drill technical agility exercises, and refine dynamic phrasing.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Scales and arpeggios.\n- Core Drill (" + mCore + "m): Hanon dexterity drills and complex piece rehearsal.\n- Application (" + mScen + "m): Expressive interpretation and recital polish.\n- Review (" + mRev + "m): Technique log and goal progress.\n\nDaily Outcome: Master new musical measures with nuanced articulation and effortless execution.";
            }
            list.add(new AiDtos.Milestone(title, desc, dailyMinutes, dayNum));
        }
        return list;
    }

    private List<AiDtos.Milestone> generateGuitarCurriculum(int totalDays, int dailyMinutes) {
        int mWarm = Math.max(5, (int) Math.round(dailyMinutes * 0.15));
        int mCore = Math.max(10, (int) Math.round(dailyMinutes * 0.50));
        int mScen = Math.max(5, (int) Math.round(dailyMinutes * 0.25));
        int mRev = Math.max(3, dailyMinutes - mWarm - mCore - mScen);

        List<String[]> guitarDays = List.of(
            new String[]{
                "Guitar Posture, Pick Grip & Tuning",
                "Technique Mechanics: Sit upright with guitar body resting comfortably on your thigh. Hold pick firmly but relaxed between thumb and side of index finger. Tune strings to E-A-D-G-B-E.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Posture alignment & electronic tuner checks.\n- Core Drill (" + mCore + "m): Clean open string alternate picking (down-up on each string).\n- Application (" + mScen + "m): Open string rhythm tapping with metronome at 60 BPM.\n- Review (" + mRev + "m): Pick angle and shoulder relaxation.\n\nDaily Outcome: Tune the guitar accurately and pick each open string cleanly with zero accidental muting."
            },
            new String[]{
                "Fretting Hand Technique & Fingertip Placement",
                "Technique Mechanics: Place fingertips directly behind frets (not on top of fretwire or far back) with thumb centered behind the neck. Keep fingers arched to avoid muting neighboring strings.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Finger spider stretches on the fretboard.\n- Core Drill (" + mCore + "m): 1-2-3-4 chromatic warm-up across the high E and B strings.\n- Application (" + mScen + "m): Clean single-note melody lines with metronome.\n- Review (" + mRev + "m): Fingertip callus check and wrist angle.\n\nDaily Outcome: Sound 4 consecutive fretted notes on each string with 100% clarity and zero buzzing."
            },
            new String[]{
                "First Open Chords: E Minor & A Minor",
                "Technique Mechanics: Form 2-finger E minor and 3-finger A minor chords. Check every string individually to ensure notes ring out cleanly without buzz.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Chord shape placement drills without strumming.\n- Core Drill (" + mCore + "m): Strum-and-check method: strum, pick each string, adjust fingers (25 reps each).\n- Application (" + mScen + "m): Alternating between Em and Am to a slow 50 BPM click.\n- Review (" + mRev + "m): Finger arching and palm clearance.\n\nDaily Outcome: Switch between Em and Am chords cleanly in rhythm without pausing."
            },
            new String[]{
                "Rhythm Strumming Patterns & Meter",
                "Technique Mechanics: Strum with a loose, relaxed wrist motion originating from the forearm. Keep strumming arm moving constantly in down-up rhythm.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Muted string strumming rhythm practice.\n- Core Drill (" + mCore + "m): 4/4 down-down-up-up-down strumming pattern on Em.\n- Application (" + mScen + "m): Strumming through Em and Am chord progression for 3 minutes continuously.\n- Review (" + mRev + "m): Wrist fluidity and tempo consistency.\n\nDaily Outcome: Maintain steady down-up strumming for 3 full minutes without dropping tempo."
            },
            new String[]{
                "C Major & G Major Open Chords",
                "Technique Mechanics: Master the foundational C Major and G Major open chords. Keep fingers arched high over strings.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Finger dexterity exercises.\n- Core Drill (" + mCore + "m): 30 transitions between C Major and G Major chords.\n- Application (" + mScen + "m): 4-chord progression: G - Em - C - D.\n- Review (" + mRev + "m): Hand muscle memory check.\n\nDaily Outcome: Form C and G chords on the first attempt without manual finger adjustment."
            },
            new String[]{
                "First Complete Song Play-Through",
                "Technique Mechanics: Combine your learned chords (G, Em, C, D) with consistent strumming into a complete song performance.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Progression review at slow tempo.\n- Core Drill (" + mCore + "m): Practice full song verse and chorus transitions.\n- Application (" + mScen + "m): Play along with original recording or metronome.\n- Review (" + mRev + "m): Overall musicality and flow.\n\nDaily Outcome: Perform complete 4-chord song from beginning to end with steady rhythm."
            },
            new String[]{
                "Performance Review & Technique Showcase",
                "Technique Mechanics: Record your complete performance. Assess chord clarity, strumming smoothness, and finger placement.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Pre-recording warm-up.\n- Core Drill (" + mCore + "m): Record 2 complete song takes.\n- Application (" + mScen + "m): Self-evaluation and identifying next skills (barre chords, fingerpicking).\n- Review (" + mRev + "m): Goal retrospective.\n\nDaily Outcome: Benchmark your completed guitar evaluation and plan your intermediate guitar path."
            }
        );

        var list = new ArrayList<AiDtos.Milestone>();
        for (int i = 0; i < totalDays; i++) {
            int dayNum = i + 1;
            String title;
            String desc;
            if (i < guitarDays.size()) {
                title = "Day " + dayNum + ": " + guitarDays.get(i)[0];
                desc = guitarDays.get(i)[1];
            } else {
                title = "Day " + dayNum + ": Guitar Technique & Song Repertoire";
                desc = "Technique Mechanics: Advance guitar skills: practice barre chords, pentatonic scales, fingerpicking patterns, and song fluency.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Finger stretches & chromatic drills.\n- Core Drill (" + mCore + "m): Scales and chord transition speed runs.\n- Application (" + mScen + "m): Full song repertoire practice.\n- Review (" + mRev + "m): Technique check.\n\nDaily Outcome: Play new chord progressions and scale patterns with effortless finger coordination.";
            }
            list.add(new AiDtos.Milestone(title, desc, dailyMinutes, dayNum));
        }
        return list;
    }

    private List<AiDtos.Milestone> generateCodingCurriculum(String title, int totalDays, int dailyMinutes) {
        int mWarm = Math.max(5, (int) Math.round(dailyMinutes * 0.15));
        int mCore = Math.max(10, (int) Math.round(dailyMinutes * 0.50));
        int mScen = Math.max(5, (int) Math.round(dailyMinutes * 0.25));
        int mRev = Math.max(3, dailyMinutes - mWarm - mCore - mScen);

        List<String[]> codingDays = List.of(
            new String[]{
                "Environment Setup, CLI Basics & First Code Execution",
                "Technique Mechanics: Install language runtime/compiler and configure VS Code. Practice basic terminal commands and execute your first 'Hello World' program.\n\nDrills (" + dailyMinutes + "m):\n- Concept (" + mWarm + "m): How compilers, runtimes, and the CLI operate.\n- Core Drill (" + mCore + "m): Installing tools, setting environment variables, and writing your first 3 script variations.\n- Project (" + mScen + "m): Interactive CLI greeting script taking user input and printing formatted response.\n- Review (" + mRev + "m): Git init, initial commit, and verification.\n\nDaily Outcome: Successfully run code via the terminal and commit your working environment to version control."
            },
            new String[]{
                "Variables, Primitive Types & Memory Models",
                "Technique Mechanics: Understand variables, primitive types (integers, floats, strings, booleans), and memory assignment.\n\nDrills (" + dailyMinutes + "m):\n- Concept (" + mWarm + "m): Type systems, variable mutability, and operators.\n- Core Drill (" + mCore + "m): 10 guided coding drills creating calculators and string manipulation scripts.\n- Project (" + mScen + "m): Build a terminal-based unit converter (Celsius to Fahrenheit, Km to Miles).\n- Review (" + mRev + "m): Code cleanup and variable naming review.\n\nDaily Outcome: Write clean programs correctly manipulating numbers and strings without type errors."
            },
            new String[]{
                "Conditional Logic & Control Flow",
                "Technique Mechanics: Master if-else branching, boolean logic (AND, OR, NOT), and comparison operators.\n\nDrills (" + dailyMinutes + "m):\n- Concept (" + mWarm + "m): Truth tables and conditional execution trees.\n- Core Drill (" + mCore + "m): 8 branching challenges (leap year calculation, grade scoring, authentication check).\n- Project (" + mScen + "m): Build an interactive text-based adventure decision tree game.\n- Review (" + mRev + "m): Eliminating nested conditionals using guard clauses.\n\nDaily Outcome: Construct complex multi-path logic that handles all edge cases predictably."
            },
            new String[]{
                "Loops & Iteration Patterns",
                "Technique Mechanics: Learn for and while loops, iteration accumulators, break/continue statements, and avoiding infinite loops.\n\nDrills (" + dailyMinutes + "m):\n- Concept (" + mWarm + "m): Loop counters, termination conditions, and index pointers.\n- Core Drill (" + mCore + "m): 10 iteration exercises (summing ranges, printing multiplication tables, filtering values).\n- Project (" + mScen + "m): Build a prime number finder and password strength validator.\n- Review (" + mRev + "m): Loop efficiency and performance inspection.\n\nDaily Outcome: Execute iterative algorithms with clean termination conditions and zero off-by-one errors."
            },
            new String[]{
                "Functions, Scope & Modular Architecture",
                "Technique Mechanics: Write modular functions with parameters, default arguments, return types, and understand local vs global scope.\n\nDrills (" + dailyMinutes + "m):\n- Concept (" + mWarm + "m): Pure functions, parameters, return statements, and the call stack.\n- Core Drill (" + mCore + "m): Refactor previous script challenges into clean, testable reusable functions.\n- Project (" + mScen + "m): Build a financial budget calculator module.\n- Review (" + mRev + "m): Function docstrings and argument validation.\n\nDaily Outcome: Write modular, reusable functions that follow single responsibility principles."
            },
            new String[]{
                "Data Structures: Lists, Arrays & Key-Value Maps",
                "Technique Mechanics: Store and query grouped data using lists/arrays, tuples, sets, and dictionaries/hashmaps.\n\nDrills (" + dailyMinutes + "m):\n- Concept (" + mWarm + "m): Array indexing, map key lookups, and algorithmic time complexity.\n- Core Drill (" + mCore + "m): Manipulating lists, sorting, searching, and dictionary aggregations.\n- Project (" + mScen + "m): Build a contact manager CLI or inventory tracking system.\n- Review (" + mRev + "m): Data structure selection best practices.\n\nDaily Outcome: Cleanly structure and query collections of records using appropriate data structures."
            },
            new String[]{
                "Mini-Project Integration, Testing & Refactoring",
                "Technique Mechanics: Combine variables, conditionals, loops, functions, and collections into a complete, polished software application.\n\nDrills (" + dailyMinutes + "m):\n- Planning (" + mWarm + "m): Application architecture and feature specification.\n- Core Drill (" + mCore + "m): End-to-end coding of a complete CLI application with error handling.\n- Testing (" + mScen + "m): Write automated test assertions for core application functions.\n- Review (" + mRev + "m): Code linting, git commit, and milestone retrospective.\n\nDaily Outcome: Ship a fully working, tested application that runs reliably from the command line."
            }
        );

        var list = new ArrayList<AiDtos.Milestone>();
        for (int i = 0; i < totalDays; i++) {
            int dayNum = i + 1;
            String t;
            String d;
            if (i < codingDays.size()) {
                t = "Day " + dayNum + ": " + codingDays.get(i)[0];
                d = codingDays.get(i)[1];
            } else {
                t = "Day " + dayNum + ": Advanced Coding Concepts & Architecture";
                d = "Technique Mechanics: Implement object-oriented design, error handling, API integrations, and clean code principles.\n\nDrills (" + dailyMinutes + "m):\n- Concept (" + mWarm + "m): Architectural design patterns.\n- Core Drill (" + mCore + "m): Writing robust modules and error handling.\n- Project (" + mScen + "m): Extending software features.\n- Review (" + mRev + "m): Code review.\n\nDaily Outcome: Architect resilient code modules with clean error handling and test coverage.";
            }
            list.add(new AiDtos.Milestone(t, d, dailyMinutes, dayNum));
        }
        return list;
    }

    private List<AiDtos.Milestone> generateGeneralCurriculum(String title, int totalDays, int dailyMinutes) {
        int mWarm = Math.max(5, (int) Math.round(dailyMinutes * 0.15));
        int mCore = Math.max(10, (int) Math.round(dailyMinutes * 0.50));
        int mScen = Math.max(5, (int) Math.round(dailyMinutes * 0.25));
        int mRev = Math.max(3, dailyMinutes - mWarm - mCore - mScen);

        List<String[]> generalPhases = List.of(
            new String[]{
                "Fundamentals, Posture & Baseline Setup for " + title,
                "Technique Mechanics: Set up your workspace, ergonomic positioning, equipment, safety standards, and foundational terminology.\n\nDrills (" + dailyMinutes + "m):\n- Preparation (" + mWarm + "m): Workspace ergonomic check & tool inspection.\n- Core Drill (" + mCore + "m): Baseline foundational drill and physical mechanics.\n- Application (" + mScen + "m): Initial guided practice trial under optimal conditions.\n- Review (" + mRev + "m): Posture check and progress logging.\n\nDaily Outcome: Establish proper posture, physical positioning, and baseline execution mechanics."
            },
            new String[]{
                "Core Mechanical Movement & Controlled Repetition",
                "Technique Mechanics: Focus on the primary foundational mechanical movement. Execute slow, deliberate repetitions to internalize proper form.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Joint mobility and muscle activation.\n- Core Drill (" + mCore + "m): Slow deliberate repetitions focusing on strict form integrity.\n- Application (" + mScen + "m): 3 consecutive sets with consistent execution tempo.\n- Review (" + mRev + "m): Form consistency review.\n\nDaily Outcome: Execute the core fundamental movement with strict form across all repetitions."
            },
            new String[]{
                "Precision Technique & Eliminating Form Flaws",
                "Technique Mechanics: Isolate awkward micro-movements, identify early compensatory habits, and refine precision control.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Targeted micro-movement warm-up.\n- Core Drill (" + mCore + "m): Isolated technique drills addressing common beginner friction points.\n- Application (" + mScen + "m): Smooth execution under conscious self-correction.\n- Review (" + mRev + "m): Video or mirror self-critique.\n\nDaily Outcome: Eliminate unnecessary tension and execute smooth, uninterrupted repetitions."
            },
            new String[]{
                "Power, Speed & Acceleration Dynamics",
                "Technique Mechanics: Build explosive power and speed while preserving technical integrity.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Dynamic explosive prep drills.\n- Core Drill (" + mCore + "m): Interval sets alternating between standard tempo and accelerated pace.\n- Application (" + mScen + "m): Timed speed trials maintaining strict form.\n- Review (" + mRev + "m): Energy conservation check.\n\nDaily Outcome: Increase execution speed by 15% without compromising mechanical form."
            },
            new String[]{
                "Pacing, Movement Footwork & Spatial Awareness",
                "Technique Mechanics: Master transition footwork, spatial positioning, rhythm, and continuous movement flow.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Footwork agility and spatial drills.\n- Core Drill (" + mCore + "m): Multi-directional movement circuits integrating yesterday's techniques.\n- Application (" + mScen + "m): Continuous flow execution over sustained intervals.\n- Review (" + mRev + "m): Balance and footwork recovery review.\n\nDaily Outcome: Transition seamlessly between movements with balanced center of gravity."
            },
            new String[]{
                "Pressure Situations & Handling Complex Scenarios",
                "Technique Mechanics: Apply your skills under artificial constraints, resistance, or reactive cues.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Reactive reflex drills.\n- Core Drill (" + mCore + "m): Simulated high-pressure scenario drills with strict time limits.\n- Application (" + mScen + "m): Stress-testing execution against unpredictable variables.\n- Review (" + mRev + "m): Decision-making speed audit.\n\nDaily Outcome: Successfully execute technique under simulated high-pressure constraints."
            },
            new String[]{
                "Practical Integration & First Complete Exercise",
                "Technique Mechanics: Synthesize Week 1 skills into a complete end-to-end performance or mini-project.\n\nDrills (" + dailyMinutes + "m):\n- Preparation (" + mWarm + "m): Warm-up review of Days 1-6.\n- Core Drill (" + mCore + "m): End-to-end execution of a complete project or routine.\n- Application (" + mScen + "m): Live trial run or timed demonstration.\n- Review (" + mRev + "m): Benchmark evaluation and Week 1 retrospective.\n\nDaily Outcome: Complete a full real-world trial integrating all foundational competencies."
            },
            new String[]{
                "Weak-Side & Non-Dominant Skill Development",
                "Technique Mechanics: Strengthen non-dominant hand/foot/side and address bilateral imbalances.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Bilateral activation drills.\n- Core Drill (" + mCore + "m): Non-dominant side repetitions focusing on mechanics replication.\n- Application (" + mScen + "m): Alternating dominant and non-dominant sets.\n- Review (" + mRev + "m): Symmetry and confidence evaluation.\n\nDaily Outcome: Achieve 80% comparable competence on your non-dominant side."
            },
            new String[]{
                "Complex Combinations & Step Sequences",
                "Technique Mechanics: Chain multiple sub-skills into fluid, continuous combinations.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Isolated sequence drills.\n- Core Drill (" + mCore + "m): Multi-step combination sequences with rapid transitions.\n- Application (" + mScen + "m): Combinations executed in real-time scenarios.\n- Review (" + mRev + "m): Continuity and cadence check.\n\nDaily Outcome: Execute multi-step combinations with zero hesitation between phases."
            },
            new String[]{
                "Dynamic Decision-Making & Reactive Agility",
                "Technique Mechanics: React to visual and auditory cues without pre-planning moves.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Rapid cognitive reaction drills.\n- Core Drill (" + mCore + "m): Randomized cue drills requiring immediate adaptive execution.\n- Application (" + mScen + "m): Dynamic situational challenge.\n- Review (" + mRev + "m): Reaction time benchmarking.\n\nDaily Outcome: Cut reaction and execution latency by 20% in unpredictable scenarios."
            },
            new String[]{
                "Instinct & Anticipation Training",
                "Technique Mechanics: Read subtle situational cues and anticipate next actions before they occur.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Pattern recognition warm-up.\n- Core Drill (" + mCore + "m): Anticipation drills and preemptive positioning.\n- Application (" + mScen + "m): Reading opponent or environmental signals.\n- Review (" + mRev + "m): Anticipation accuracy check.\n\nDaily Outcome: Successfully anticipate and initiate movements one step ahead of the cue."
            },
            new String[]{
                "Tactical Strategy & Situational Problem Solving",
                "Technique Mechanics: Apply tactical intelligence, game theory, and strategic planning.\n\nDrills (" + dailyMinutes + "m):\n- Strategy (" + mWarm + "m): Tactical theory and scenario analysis.\n- Core Drill (" + mCore + "m): Executing specific tactical game plans against varied challenges.\n- Application (" + mScen + "m): Case study simulation or scrimmage testing.\n- Review (" + mRev + "m): Tactical effectiveness audit.\n\nDaily Outcome: Execute tactical game plans effectively against diverse challenges."
            },
            new String[]{
                "High-Intensity Simulation Under Pressure & Fatigue",
                "Technique Mechanics: Maintain peak technical accuracy even under severe physical and mental fatigue.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): High-intensity interval warm-up.\n- Core Drill (" + mCore + "m): Repeated high-intensity execution circuits with minimal rest.\n- Application (" + mScen + "m): End-game crisis simulation.\n- Review (" + mRev + "m): Stamina and error rate tracking.\n\nDaily Outcome: Maintain over 80% accuracy during high-fatigue execution intervals."
            },
            new String[]{
                "Comprehensive Performance Showcase & Benchmark",
                "Technique Mechanics: Full synthesis of all learned skills in a complete benchmark demonstration.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Full competition warm-up.\n- Core Drill (" + mCore + "m): Comprehensive skills showcase trial.\n- Application (" + mScen + "m): Video recording and performance grading against standards.\n- Review (" + mRev + "m): Long-term mastery roadmap and celebratory retrospective.\n\nDaily Outcome: Benchmark your completed mastery evaluation and chart your next progression milestone."
            }
        );

        var list = new ArrayList<AiDtos.Milestone>();
        for (int i = 0; i < totalDays; i++) {
            int dayNum = i + 1;
            String t;
            String d;
            if (i < generalPhases.size()) {
                t = "Day " + dayNum + ": " + generalPhases.get(i)[0];
                d = generalPhases.get(i)[1];
            } else {
                t = "Day " + dayNum + ": Advanced Technique Mastery & Performance Polish";
                d = "Technique Mechanics: Refine advanced nuances of " + title + ": optimize speed, eliminate inefficiencies, and practice elite execution under realistic conditions.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Movement prep & mental focus.\n- Core Drill (" + mCore + "m): Advanced execution drills.\n- Application (" + mScen + "m): Real-world mastery trials.\n- Review (" + mRev + "m): Performance audit.\n\nDaily Outcome: Execute complex technique with effortless fluency and precision.";
            }
            list.add(new AiDtos.Milestone(t, d, dailyMinutes, dayNum));
        }
        return list;
    }

    private List<AiDtos.Milestone> generateCricketCurriculum(int totalDays, int dailyMinutes) {
        int mWarm = Math.max(5, (int) Math.round(dailyMinutes * 0.15));
        int mCore = Math.max(10, (int) Math.round(dailyMinutes * 0.50));
        int mScen = Math.max(5, (int) Math.round(dailyMinutes * 0.25));
        int mRev = Math.max(3, dailyMinutes - mWarm - mCore - mScen);

        List<String[]> cricketDays = List.of(
            new String[]{
                "What to Learn First: The Batting Grip, Stance & Head Alignment",
                "Technique Mechanics: The foundational skill every cricketer must master first is the neutral 'V-Grip' and a balanced stance. Form a 'V' with thumb and index finger of both hands aligned between the bat spine and the outside edge. Place feet shoulder-width apart, knees slightly flexed, and keep your head completely still with eyes level directly over the popping crease.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Neck mobility, rotational stretches & 50 stationary grip checks.\n- Core Drill (" + mCore + "m): Stance alignment and shadow batting in front of a mirror (50 repetitions).\n- Scenario (" + mScen + "m): Tapping the bat lightly and setting a relaxed backlift towards second slip.\n- Review (" + mRev + "m): Checking head stillness and loose bottom-hand pressure.\n\nDaily Outcome: Master the neutral V-grip and balanced stance with head perfectly level over off-stump without falling over."
            },
            new String[]{
                "Forward Defensive Stroke: Smothering Pace & Spin",
                "Technique Mechanics: Step your front foot right next to the pitch of the ball, bend your front knee, and present the full face of the bat with soft hands directly beneath your eyes.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Front foot lunges and balance holds.\n- Core Drill (" + mCore + "m): 40 stationary drop-ball forward defensive blocks into the ground.\n- Scenario (" + mScen + "m): Facing gentle throw-downs on a good length and defending under your nose.\n- Review (" + mRev + "m): Ensuring soft hands so the ball drops dead at your feet.\n\nDaily Outcome: Dead-bat 20 consecutive good-length deliveries with zero edges or pops in the air."
            },
            new String[]{
                "Straight Drive: High Front Elbow & Driving Through the V",
                "Technique Mechanics: Lead with your head and front shoulder towards the pitch of a full delivery. Keep your front elbow pointing high towards mid-off, swinging through the line of the ball between mid-on and mid-off.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): High elbow shadow swings.\n- Core Drill (" + mCore + "m): 30 straight drive tee-drills or drop-ball feeds along the carpet.\n- Scenario (" + mScen + "m): Driving overpitched half-volleys through the 'V'.\n- Review (" + mRev + "m): High elbow check and carpet-hugging ball trajectory.\n\nDaily Outcome: Strike 15 consecutive straight drives along the ground with a towering front elbow."
            },
            new String[]{
                "Cover Drive: Foot to the Pitch & Bending the Front Knee",
                "Technique Mechanics: Step your front foot across towards cover right to the pitch of the ball. Bend your front knee deeply, keep your head over the contact point, and drive through extra cover with an open face.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Side-lunge weight transfers.\n- Core Drill (" + mCore + "m): 35 cover drive shadow and throw-down drills targeting the cover boundary.\n- Scenario (" + mScen + "m): Driving balls delivered outside off-stump along the ground.\n- Review (" + mRev + "m): Preventing head from falling towards off-side.\n\nDaily Outcome: Execute clean, powerful cover drives along the ground on 80% of full deliveries outside off-stump."
            },
            new String[]{
                "Back-Foot Defense & The Square Cut",
                "Technique Mechanics: On short deliveries outside off-stump, push off your front foot and rock back and across. Extend arms with a high-to-low blade angle, rolling wrists over the ball to hit the cut down into the turf.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Back-foot transfer drills.\n- Core Drill (" + mCore + "m): 30 square cut drills off high bouncing drop-feeds.\n- Scenario (" + mScen + "m): Alternating between back-foot defense and aggressive square cut.\n- Review (" + mRev + "m): Rolling wrists over the bounce.\n\nDaily Outcome: Hit 10 consecutive square cuts firmly into the ground behind point."
            },
            new String[]{
                "The Pull Shot: Swivelling on the Back Foot",
                "Technique Mechanics: Move your back foot back and across in line with the stumps. Swivel on the ball of your back foot, extending arms fully, and roll wrists over the top of the ball to pull it down behind square leg.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Hip swivels and rotational torso stretches.\n- Core Drill (" + mCore + "m): 25 short-ball pull shot drills targeting the mid-wicket and square leg zones.\n- Scenario (" + mScen + "m): Pulling waist-high deliveries safely downward.\n- Review (" + mRev + "m): Keeping head forward over the bounce.\n\nDaily Outcome: Safely pull 15 short-pitched deliveries along the ground without top-edging."
            },
            new String[]{
                "Fast Bowling Basics: 2-Finger Seam Grip & Run-Up Rhythm",
                "Technique Mechanics: Place index and middle fingers on top of the seam with thumb underneath on the smooth leather. Establish a smooth, repeatable run-up rhythm accelerating into a balanced gather.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Shoulder rotations and 50-meter build-up strides.\n- Core Drill (" + mCore + "m): 20 run-up marker measurements and seam release check throws.\n- Scenario (" + mScen + "m): Bowling full deliveries with an upright seam down a target line.\n- Review (" + mRev + "m): Wrist lock and upright seam wobble test.\n\nDaily Outcome: Deliver 15 balls with an upright, wobble-free seam hitting the target zone."
            },
            new String[]{
                "Swing Bowling: Outswing & Inswing Seam Angles",
                "Technique Mechanics: For outswing, angle seam toward first slip with shiny side on the leg side. For inswing, angle seam toward fine leg with shiny side on the off side. Cock wrist and pull down hard behind the ball.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Wrist snapping drills with a two-tone practice ball.\n- Core Drill (" + mCore + "m): 30 deliveries practicing conventional swing release.\n- Scenario (" + mScen + "m): Bowling the 'corridor of uncertainty' 4th-stump line.\n- Review (" + mRev + "m): Ball movement and lateral drift.\n\nDaily Outcome: Achieve visible late swing through the air on at least 10 deliveries."
            },
            new String[]{
                "Catching & Slip Fielding: Soft Hands & Pinky-to-Pinky",
                "Technique Mechanics: Keep hands relaxed with pinkies touching to create a soft cup. Watch the ball all the way into your palms and 'give' with the impact like catching an egg.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Reaction ball wall bounces.\n- Core Drill (" + mCore + "m): 50 slip cradle and high catching drills.\n- Scenario (" + mScen + "m): Flat rapid catches off the bat edge.\n- Review (" + mRev + "m): Hand softness and head stillness.\n\nDaily Outcome: Take 20 consecutive slip and outfield catches with zero drops using soft hands."
            },
            new String[]{
                "Running Between the Wickets & Calling Communication",
                "Technique Mechanics: Run with bat in inside hand. Call loud, decisive calls ('Yes', 'No', 'Wait'). Slide bat grounded behind the crease and turn sharply facing the ball.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): 22-yard shuttle sprints with bat sliding.\n- Core Drill (" + mCore + "m): 15 quick single simulations with partner calling.\n- Scenario (" + mScen + "m): Turning for a second run on the arm of the boundary fielder.\n- Review (" + mRev + "m): Crease sliding technique.\n\nDaily Outcome: Complete 10 two-run shuttles in under 6.5 seconds each with proper bat sliding."
            },
            new String[]{
                "Spin Bowling Basics: Finger Spin vs Wrist Spin Revolutions",
                "Technique Mechanics: Grip the ball across the seam. For off-spin, spin using the index finger knuckle. For leg-spin, cock wrist and spin off the third finger ring finger.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Finger flick and wrist rotation warm-ups.\n- Core Drill (" + mCore + "m): 30 deliveries targeting a 2-foot landing square on a good length.\n- Scenario (" + mScen + "m): Varying flight and dip to deceive the batter.\n- Review (" + mRev + "m): Revolution count and turn off the surface.\n\nDaily Outcome: Land 15 deliveries inside the target landing square with sharp turn."
            },
            new String[]{
                "Lofted Power Hitting & Clearing the Infield",
                "Technique Mechanics: Clear front leg slightly to create swing path. Keep eyes on the ball, extend arms fully through the line, and generate power from hip rotation rather than pure arm tension.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Rotational medicine ball or bat swings.\n- Core Drill (" + mCore + "m): 25 lofted drives over mid-on and mid-off off full tosses and half-volleys.\n- Scenario (" + mScen + "m): Death-overs boundary hitting simulation.\n- Review (" + mRev + "m): Clean contact and bat speed.\n\nDaily Outcome: Consistently clear the inner ring along high trajectory without losing balance."
            },
            new String[]{
                "Playing Pace & Spin in High-Pressure Match Scenarios",
                "Technique Mechanics: Quick footwork: fully forward or fully back. Pick lengths early out of the bowler's hand.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Rapid eye-tracking drills.\n- Core Drill (" + mCore + "m): Match situation net session: 6-over chase scenario with targets.\n- Scenario (" + mScen + "m): Facing mixed pace and spin under run-rate pressure.\n- Review (" + mRev + "m): Strike rotation and dismissal analysis.\n\nDaily Outcome: Successfully chase target score in simulated net pressure scenario."
            },
            new String[]{
                "Full Match Showcase & Technical Performance Audit",
                "Technique Mechanics: Synthesize batting, bowling, and fielding into a complete competitive trial.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Full team match warm-up routine.\n- Core Drill (" + mCore + "m): 3-phase match trial (Batting assessment, Bowling spell, Fielding drills).\n- Scenario (" + mScen + "m): Competitive scrimmage.\n- Review (" + mRev + "m): Video review, technical scorecard grading, and next cycle goals.\n\nDaily Outcome: Benchmark your completed cricket scorecard with mastery in all fundamental disciplines."
            },
            new String[]{
                "Slower Ball Variations: The Knuckleball & Off-Cutter",
                "Technique Mechanics: Grip the ball with knuckles or slide fingers down the side of the seam at release to drop the pace by 15-20 km/h without changing arm action.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Finger dexterity drills.\n- Core Drill (" + mCore + "m): 25 slower ball releases aiming for target yorker cones.\n- Scenario (" + mScen + "m): Deceiving the batter in death overs.\n- Review (" + mRev + "m): Arm speed deception check.\n\nDaily Outcome: Maintain identical arm speed while dropping release pace by at least 15 km/h."
            },
            new String[]{
                "The Sweep & Reverse Sweep against Spin",
                "Technique Mechanics: Drop low on your back knee, extend arms early, and keep your head over the front knee to sweep across the line safely along the ground.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Hip mobility and knee flexion.\n- Core Drill (" + mCore + "m): 30 sweep shots off slow spinning throw-downs.\n- Scenario (" + mScen + "m): Manipulating fielders by sweeping through square leg.\n- Review (" + mRev + "m): Contact point and downward trajectory.\n\nDaily Outcome: Sweep 15 deliveries cleanly along the ground without top-edging."
            },
            new String[]{
                "Upper Cut & Ramp Shot to Fast Bouncers",
                "Technique Mechanics: Use the bowler's pace: arch back, guide the ball with an open face over the slip cordon or fine leg.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Reaction drills with tennis balls.\n- Core Drill (" + mCore + "m): 20 upper cut and ramp touch drills.\n- Scenario (" + mScen + "m): Scoring off short-pitched bouncers.\n- Review (" + mRev + "m): Touch control and soft grip.\n\nDaily Outcome: Safely guide short bouncers over the keeper's head on target."
            },
            new String[]{
                "Yorkers & Death Bowling Execution",
                "Technique Mechanics: Target the base of the stumps or the batter's toes. Drive through the crease with a late, hard wrist snap.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Target line strides.\n- Core Drill (" + mCore + "m): 30 bowling deliveries aiming at a shoe or cone placed on the crease line.\n- Scenario (" + mScen + "m): Death over simulation: defending 12 runs with yorkers.\n- Review (" + mRev + "m): Length accuracy.\n\nDaily Outcome: Land 15 yorkers in the 6-inch toe-zone at full match pace."
            },
            new String[]{
                "Direct Hit Run-Outs from 30 Yards",
                "Technique Mechanics: Attack the ball at speed, stay low, pick up with one hand beside your lead foot, and release off balanced steps aiming at a single stump.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Ground fielding pickups and shoulder activation.\n- Core Drill (" + mCore + "m): 30 pickup-and-throw drills targeting a single stump from 30 yards.\n- Scenario (" + mScen + "m): Rapid direct-hit run-out simulation.\n- Review (" + mRev + "m): Pick-up cleanliness and throwing accuracy.\n\nDaily Outcome: Strike the target stump directly on at least 10 throws from 30 yards."
            },
            new String[]{
                "Building Long Innings: Strike Rotation Tactics",
                "Technique Mechanics: Nudge the ball into gaps with soft hands, drop and run for quick singles, keeping the scoreboard moving without taking high risks.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Placement and direction drills.\n- Core Drill (" + mCore + "m): 30 drop-and-run placement drills into pre-set ring cones.\n- Scenario (" + mScen + "m): Middle-overs simulation: rotate strike every second ball.\n- Review (" + mRev + "m): Dot ball minimization check.\n\nDaily Outcome: Convert 80% of defensive shots into aggressive strike-rotation singles."
            },
            new String[]{
                "Tactical Field Setting & Bowling to a Plan",
                "Technique Mechanics: Set fielders according to the batter's weaknesses and bowl strictly to your field (e.g. 7-2 off-side field bowling 5th-stump channel).\n\nDrills (" + dailyMinutes + "m):\n- Planning (" + mWarm + "m): Field setting chalkboard strategy.\n- Core Drill (" + mCore + "m): 24 consecutive deliveries bowling strictly to a set field without bowling on the wrong side.\n- Scenario (" + mScen + "m): Adjusting field after boundaries.\n- Review (" + mRev + "m): Bowling discipline review.\n\nDaily Outcome: Bowl an entire 4-over spell without conceding a single ball on the wrong side of your field."
            },
            new String[]{
                "Mystery Spin Variations: The Carrom Ball & Arm Ball",
                "Technique Mechanics: For carrom ball, flick the ball out with the middle finger. For arm ball, release with an upright seam drifting into the right-hander.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Finger flick drills.\n- Core Drill (" + mCore + "m): 30 variation releases targeting good length cones.\n- Scenario (" + mScen + "m): Deceiving the batter with disguised variation.\n- Review (" + mRev + "m): Seam rotation and drift check.\n\nDaily Outcome: Cleanly execute 15 variation deliveries with consistent length and deception."
            },
            new String[]{
                "Power Play Batting & Piercing the Infield",
                "Technique Mechanics: Exploit the 2-fielder restriction by using the bowler's pace, lofting over mid-off, and placing into vacant cow-corner.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Fast-hands bat speed warm-ups.\n- Core Drill (" + mCore + "m): Power play simulated 6-over batting challenge.\n- Scenario (" + mScen + "m): Target scoring: 50 runs in 36 balls.\n- Review (" + mRev + "m): Risk vs reward execution.\n\nDaily Outcome: Score at least 45 runs across 36 power play simulated balls with minimal dismissals."
            },
            new String[]{
                "Reverse Swing Mechanics with the Older Ball",
                "Technique Mechanics: Keep one side rough and heavily shine the opposite side. Bowl with high arm speed at 135+ km/h into the rough side to generate late reverse movement.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Ball maintenance and shiny-side preservation.\n- Core Drill (" + mCore + "m): 25 full-length reverse swing deliveries aimed at leg-stump base.\n- Scenario (" + mScen + "m): Late tailender cleanup simulation.\n- Review (" + mRev + "m): Late deviation point check.\n\nDaily Outcome: Achieve noticeable reverse swing into the stumps on older ball deliveries."
            }
        );

        var list = new ArrayList<AiDtos.Milestone>();
        for (int i = 0; i < totalDays; i++) {
            int dayNum = i + 1;
            String title;
            String desc;
            if (i < cricketDays.size()) {
                title = "Day " + dayNum + ": " + cricketDays.get(i)[0];
                desc = cricketDays.get(i)[1];
            } else {
                title = "Day " + dayNum + ": Elite Match Strategy & Skills Showcase";
                desc = "Technique Mechanics: Synthesize all cricket competencies: stroke play, bowling discipline, pressure handling, and match leadership.\n\nDrills (" + dailyMinutes + "m):\n- Warm-up (" + mWarm + "m): Full physical activation.\n- Core Drill (" + mCore + "m): Comprehensive match trial.\n- Scenario (" + mScen + "m): High-pressure game finish.\n- Review (" + mRev + "m): Technical scorecard audit.\n\nDaily Outcome: Execute match-winning plays with calmness and complete technical precision.";
            }
            list.add(new AiDtos.Milestone(title, desc, dailyMinutes, dayNum));
        }
        return list;
    }
}

