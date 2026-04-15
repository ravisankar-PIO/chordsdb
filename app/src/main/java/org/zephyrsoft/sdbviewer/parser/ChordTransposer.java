package org.zephyrsoft.sdbviewer.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transposes chord lines by a given number of semitones,
 * preserving original spacing and handling transition pairs,
 * slash chords, and suffixes.
 */
public class ChordTransposer {

    private static final String[] SHARPS = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final String[] FLATS  = {"C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"};

    private static final Pattern TOKEN_PATTERN = Pattern.compile("(\\s+|\\S+)");

    private ChordTransposer() {
        // static utility class
    }

    /**
     * Transpose all chord tokens in a chord line by the given number of semitones.
     * Spacing between tokens is preserved exactly.
     */
    public static String transpose(String chordLine, int semitones) {
        if (semitones == 0) {
            return chordLine;
        }
        StringBuilder result = new StringBuilder();
        Matcher matcher = TOKEN_PATTERN.matcher(chordLine);
        while (matcher.find()) {
            String token = matcher.group();
            if (token.trim().isEmpty()) {
                result.append(token);
            } else {
                result.append(transposeChord(token, semitones));
            }
        }
        return result.toString();
    }

    private static String transposeChord(String token, int semitones) {
        // a. Transition pairs — multiple chord roots concatenated (e.g. DmEm, AbBb)
        //    Split on uppercase-letter boundaries, filter empty strings,
        //    then verify every part starts with a chord root letter (A-G)
        //    and contains no "/" (which would indicate a slash chord instead).
        String[] rawParts = token.split("(?=[A-Z])");
        List<String> parts = new ArrayList<>();
        for (String p : rawParts) {
            if (!p.isEmpty()) {
                parts.add(p);
            }
        }
        if (parts.size() > 1) {
            boolean allChordRoots = true;
            for (String part : parts) {
                if (!isChordRoot(part.charAt(0)) || part.contains("/")) {
                    allChordRoots = false;
                    break;
                }
            }
            if (allChordRoots) {
                StringBuilder sb = new StringBuilder();
                for (String part : parts) {
                    sb.append(transposeChord(part, semitones));
                }
                return sb.toString();
            }
        }

        // b. Slash chords — e.g. Am/G, Fmaj7/C
        if (token.contains("/")) {
            String[] sides = token.split("/", 2);
            return transposeChord(sides[0], semitones) + "/" + transposeChord(sides[1], semitones);
        }

        // c. Single chord — extract root (1 or 2 chars) and suffix
        if (token.isEmpty() || !isChordRoot(token.charAt(0))) {
            return token;
        }
        int rootLen = 1;
        if (token.length() > 1 && (token.charAt(1) == '#' || token.charAt(1) == 'b')) {
            rootLen = 2;
        }
        String root = token.substring(0, rootLen);
        String suffix = token.substring(rootLen);

        String newRoot = transposeRoot(root, semitones);
        if (newRoot == null) {
            return token; // unrecognised root — return unchanged
        }
        return newRoot + suffix;
    }

    /**
     * Transpose a single root note (e.g. "C", "F#", "Bb") by semitones.
     * Returns null if the root is not found in either chromatic scale.
     */
    private static String transposeRoot(String root, int semitones) {
        int index = -1;
        for (int i = 0; i < SHARPS.length; i++) {
            if (SHARPS[i].equals(root)) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            for (int i = 0; i < FLATS.length; i++) {
                if (FLATS[i].equals(root)) {
                    index = i;
                    break;
                }
            }
        }
        if (index == -1) {
            return null;
        }
        int newIndex = ((index + semitones) % 12 + 12) % 12;
        return semitones > 0 ? SHARPS[newIndex] : FLATS[newIndex];
    }

    /** Returns true for the seven letter names A–G (uppercase). */
    private static boolean isChordRoot(char c) {
        return c >= 'A' && c <= 'G';
    }
}
