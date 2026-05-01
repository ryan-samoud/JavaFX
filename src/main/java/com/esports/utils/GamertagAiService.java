package com.esports.utils;

import java.util.*;
import java.util.stream.Collectors;

public class GamertagAiService {

    // ── Game word banks ────────────────────────────────────────────────
    private static final Map<String, String[]> GAME_WORDS = new LinkedHashMap<>();
    static {
        GAME_WORDS.put("valorant",   new String[]{
            "Phantom","Vandal","Jett","Reyna","Sage","Cypher","Viper","Omen",
            "Spike","Ace","Clutch","Knife","Flash","Radiant","Sheriff","Ghost",
            "Operator","Sova","Raze","Breach","Brimstone","Phoenix","Neon","Harbor"});
        GAME_WORDS.put("lol",        new String[]{
            "Baron","Dragon","Nexus","Gank","Carry","Jungle","Rift","Thresh",
            "Yasuo","Zed","Jinx","Vayne","Ahri","Lux","Teemo","Malphite",
            "Penta","Elo","Smite","Tower","Buff","Garen","Ashe","Katarina"});
        GAME_WORDS.put("leagueoflegends", new String[]{
            "Baron","Dragon","Nexus","Gank","Carry","Jungle","Rift","Thresh",
            "Yasuo","Zed","Jinx","Vayne","Ahri","Lux","Penta","Elo","Smite"});
        GAME_WORDS.put("fortnite",   new String[]{
            "Storm","Build","Loot","Zone","Victory","Royale","Pump","Snipe",
            "Dub","Tilted","Scar","Turbo","Glider","Drop","Reboot","Pickaxe",
            "Chapter","Myth","Raven","Drift","Omega","Valor","Solo","Duo"});
        GAME_WORDS.put("minecraft",  new String[]{
            "Creeper","Diamond","Ender","Dragon","Nether","Redstone","Steve",
            "Herobrine","Craft","Mine","Block","Sword","Pickaxe","Ghast","Wither",
            "Notch","Chunk","Spawn","Portal","Anvil","Bedrock","Zombie","Golem"});
        GAME_WORDS.put("csgo",       new String[]{
            "AWP","Rush","Clutch","Ace","Flash","Smoke","Spray","Frag","Plant",
            "Defuse","Boost","Peek","CT","Deagle","Knife","Rank","Major",
            "Cache","Mirage","Inferno","Dust","Faceit","Retake","Push"});
        GAME_WORDS.put("cs2",        new String[]{
            "AWP","Rush","Clutch","Ace","Flash","Smoke","Spray","Frag","Plant",
            "Defuse","Boost","Peek","CT","Deagle","Knife","Rank","Major"});
        GAME_WORDS.put("apex",       new String[]{
            "Wraith","Caustic","Legend","Ring","Shield","Revive","Gibby",
            "Bloodhound","Lifeline","Pathfinder","Bangalore","Octane","Mirage",
            "Crypto","Loba","Rampart","Horizon","Ash","Newcastle","Mad"});
        GAME_WORDS.put("overwatch",  new String[]{
            "Hanzo","Reaper","Tracer","Mercy","Reinhardt","Genji","Ana","Zarya",
            "Bastion","Torbjorn","Widowmaker","Pharah","Winston","Lucio","Dva",
            "Orisa","Moira","Brigitte","Junkrat","McCree","Sombra","Echo"});
        GAME_WORDS.put("pubg",       new String[]{
            "Chicken","Dinner","Loot","Zone","Pan","Kar98","M416","Groza",
            "Miramar","Erangel","Vikendi","Parachute","Boost","Bridge","Solo","Squad"});
        GAME_WORDS.put("rocketleague", new String[]{
            "Boost","Aerial","Flip","Demo","Champ","Grand","Rocket","Goal",
            "Musty","Freeplay","Rank","Diamond","Plat","SSL","Ceiling","Pinch"});
        GAME_WORDS.put("fifa",       new String[]{
            "Skill","Dribble","Chip","FinessShot","Panenka","Ultimate","Team",
            "Rival","Icon","Rare","Gold","Clutch","Banger","Liga","Striker"});
        GAME_WORDS.put("cod",        new String[]{
            "Warzone","Nuke","Killstreak","Operator","Gulag","Loadout","Drop",
            "Squad","Snipe","Clutch","Respawn","Bounty","Crate","Ghost","Captain"});
        GAME_WORDS.put("callofduty", new String[]{
            "Warzone","Nuke","Killstreak","Operator","Gulag","Loadout","Drop",
            "Squad","Snipe","Clutch","Respawn","Bounty","Ghost","Soap"});
        GAME_WORDS.put("wow",        new String[]{
            "Raid","Guild","Dungeon","Paladin","Mage","Druid","Warlock","Rogue",
            "Titan","Myth","Heroic","Mythic","Azeroth","Horde","Alliance","Arena"});
        GAME_WORDS.put("dota",       new String[]{
            "Pudge","Invoker","Carry","Support","Roshan","Aegis","Rampage","Courier",
            "Mid","Radiant","Dire","Fountain","Jungle","Ward","Smoke","Gank"});
        GAME_WORDS.put("genshin",    new String[]{
            "Archon","Mora","Primogem","Abyss","Spiral","Traveler","Vision",
            "Paimon","Mondstadt","Liyue","Inazuma","Resin","Artifact","Wish"});
        GAME_WORDS.put("r6",         new String[]{
            "Siege","Breach","Frost","Ash","Kapkan","Glaz","Jager","Rook",
            "Flanked","Roam","Defender","Attacker","Bomb","Secure","Clutch"});
    }

    // ── Style prefixes ─────────────────────────────────────────────────
    private static final Map<String, String[]> STYLE_PREFIX = new HashMap<>();
    static {
        STYLE_PREFIX.put("agressif",   new String[]{"xX","Dark","Rage","Savage","Rogue","Blood","War","Fury","Brutal","Deadly"});
        STYLE_PREFIX.put("mysterieux", new String[]{"Void","Shadow","Phantom","Silent","Ghost","Cryptic","Shade","Lurk","Enigma","Null"});
        STYLE_PREFIX.put("legendaire", new String[]{"Pro","Elite","Grand","Ultra","God","King","Omega","Alpha","Supreme","Mythic"});
        STYLE_PREFIX.put("drole",      new String[]{"MrNoob","NotA","Lil","Big","Actual","FakeGod","IAmThe","Sus","Cursed","Potato"});
        STYLE_PREFIX.put("technique",  new String[]{"Neo","Sys","0x","Null","XOR","Hex","404","Byte","Core","Algo"});
        STYLE_PREFIX.put("libre",      new String[]{"","The","Neon","Hyper","Cyber","Nova","Turbo","Blaze","Storm","Iron"});
    }

    // ── Style suffixes ─────────────────────────────────────────────────
    private static final Map<String, String[]> STYLE_SUFFIX = new HashMap<>();
    static {
        STYLE_SUFFIX.put("agressif",   new String[]{"GG","Killer","Beast","99","666","Slayer","Hunter","PWR"});
        STYLE_SUFFIX.put("mysterieux", new String[]{"_","00","XIII","X","Zero","Void","Dark","NaN"});
        STYLE_SUFFIX.put("legendaire", new String[]{"1","GodTier","Master","IRL","TTV","HD","Prime","MVP"});
        STYLE_SUFFIX.put("drole",      new String[]{"Lmao","XD","Bruh","gg","NotGood","404","IDK","Yeet"});
        STYLE_SUFFIX.put("technique",  new String[]{"v2","_exe","_bot","32","64","0","1337","v1"});
        STYLE_SUFFIX.put("libre",      new String[]{"","7","99","X","Z","GG","Pro","HD","42","2K"});
    }

    // ── Default (no game matched) ──────────────────────────────────────
    private static final String[] DEFAULT_WORDS = {
        "Nexus","Nova","Blaze","Storm","Frost","Vortex","Echo","Cipher",
        "Phantom","Rogue","Neon","Steel","Apex","Titan","Omega","Raven",
        "Cobra","Wolf","Hawk","Reaper","Specter","Volt","Pyro","Cryo"
    };

    // ──────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ──────────────────────────────────────────────────────────────────
    public static List<String> generate(String game, String style, String keyword) {
        Random rng = new Random();
        String[] wordBank = resolveWordBank(game);
        String styleKey   = resolveStyle(style);
        String[] prefixes = STYLE_PREFIX.get(styleKey);
        String[] suffixes = STYLE_SUFFIX.get(styleKey);

        Set<String> results = new LinkedHashSet<>();
        int attempts = 0;

        while (results.size() < 6 && attempts < 80) {
            attempts++;
            String tag = buildTag(rng, wordBank, prefixes, suffixes, keyword);
            if (tag != null && tag.length() >= 3 && tag.length() <= 16) {
                results.add(tag);
            }
        }

        return new ArrayList<>(results);
    }

    // ──────────────────────────────────────────────────────────────────
    // BUILD ONE TAG
    // ──────────────────────────────────────────────────────────────────
    private static String buildTag(Random rng, String[] wordBank,
                                    String[] prefixes, String[] suffixes,
                                    String keyword) {
        String word   = pick(rng, wordBank);
        String prefix = pick(rng, prefixes);
        String suffix = pick(rng, suffixes);

        // 30% chance: use keyword as the word instead
        if (keyword != null && !keyword.isBlank() && rng.nextInt(10) < 3) {
            word = capitalise(keyword.replaceAll("[^a-zA-Z0-9]", ""));
        }

        // 4 assembly strategies chosen randomly
        String tag;
        switch (rng.nextInt(4)) {
            case 0 -> tag = prefix + word + suffix;
            case 1 -> tag = word + suffix;
            case 2 -> tag = prefix + word;
            default -> {
                // swap word for keyword combination
                if (keyword != null && !keyword.isBlank()) {
                    String kw = capitalise(keyword.replaceAll("[^a-zA-Z0-9]", ""));
                    tag = kw + word;
                } else {
                    tag = prefix + word + suffix;
                }
            }
        }

        tag = tag.replaceAll("[^a-zA-Z0-9_]", "");
        if (tag.length() > 16) tag = tag.substring(0, 16);
        return tag.isEmpty() ? null : tag;
    }

    // ──────────────────────────────────────────────────────────────────
    // HELPERS
    // ──────────────────────────────────────────────────────────────────
    private static String[] resolveWordBank(String game) {
        if (game == null || game.isBlank()) return DEFAULT_WORDS;
        String key = game.toLowerCase().replaceAll("[^a-z0-9]", "");
        // exact match
        if (GAME_WORDS.containsKey(key)) return GAME_WORDS.get(key);
        // partial match (e.g. "league of legends" → "lol" alias)
        for (Map.Entry<String, String[]> e : GAME_WORDS.entrySet()) {
            if (key.contains(e.getKey()) || e.getKey().contains(key)) {
                return e.getValue();
            }
        }
        return DEFAULT_WORDS;
    }

    private static String resolveStyle(String style) {
        if (style == null || style.isBlank()) return "libre";
        String s = style.toLowerCase().replaceAll("[^a-z]", "");
        for (String k : STYLE_PREFIX.keySet()) {
            if (s.contains(k) || k.contains(s)) return k;
        }
        return "libre";
    }

    private static String pick(Random rng, String[] arr) {
        return arr[rng.nextInt(arr.length)];
    }

    private static String capitalise(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}