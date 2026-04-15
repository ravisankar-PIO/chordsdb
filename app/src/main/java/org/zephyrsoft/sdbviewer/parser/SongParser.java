package org.zephyrsoft.sdbviewer.parser;

import org.zephyrsoft.sdbviewer.model.Song;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a {@link Song} and thus prepares it for being shown in the editor or as presentation.
 */
public class SongParser {
	
	private static final String LABEL_MUSIC = "Music: ";
	private static final String LABEL_TEXT = "Text: ";
	private static final String LABEL_TRANSLATION = "Translation: ";
	private static final String LABEL_PUBLISHER = "Publisher: ";
	
	private static final Pattern TRANSLATION_PATTERN = Pattern.compile("^(.*)\\[(.*)](.*)$");
	private static final String NEWLINE_REGEX = "\r?+\n";
	
	private SongParser() {
		// this class should only be used statically
	}
	
	public static List<SongElement> parseLyrics(Song song, boolean includeChords, boolean includeTranslation) {
		List<SongElement> ret = new ArrayList<>();

		if (song.getLyrics() != null) {
			boolean isFirst = true;
			for (String line : song.getLyrics().split(NEWLINE_REGEX)) {
				Matcher translationMatcher = TRANSLATION_PATTERN.matcher(line);
				if (translationMatcher.matches()) {
					String prefix = translationMatcher.group(1);
					String translation = translationMatcher.group(2);
					String suffix = translationMatcher.group(3);
					if (notEmpty(prefix)
						|| (includeTranslation && notEmpty(translation))
						|| notEmpty(suffix)) {
						isFirst = addNewlineIfNotFirstLine(ret, isFirst);
					}
					if (notEmpty(prefix)) {
						ret.add(new SongElement(SongElementEnum.LYRICS, prefix));
					}
					if (includeTranslation && notEmpty(translation)) {
						ret.add(new SongElement(SongElementEnum.TRANSLATION, translation));
					}
					if (notEmpty(suffix)) {
						ret.add(new SongElement(SongElementEnum.LYRICS, suffix));
					}
				} else if (isChordsLine(line)) {
				    if (includeChords) {
                        isFirst = addNewlineIfNotFirstLine(ret, isFirst);
                        ret.add(new SongElement(SongElementEnum.CHORDS, line));
                    }
				} else {
					isFirst = addNewlineIfNotFirstLine(ret, isFirst);
					ret.add(new SongElement(SongElementEnum.LYRICS, line));
				}

			}
		}

		return ret;
	}

	public static List<SongElement> parseCopyright(Song song) {
		List<SongElement> ret = new ArrayList<>();

		if (notEmpty(song.getComposer())) {
			ret.add(new SongElement(SongElementEnum.COPYRIGHT, LABEL_MUSIC + song.getComposer()));
		}
		if (notEmpty(song.getAuthorText())) {
			ret.add(new SongElement(SongElementEnum.COPYRIGHT, LABEL_TEXT + song.getAuthorText()));
		}
		if (notEmpty(song.getAuthorTranslation())) {
			ret.add(new SongElement(SongElementEnum.COPYRIGHT, LABEL_TRANSLATION + song.getAuthorTranslation()));
		}
		if (notEmpty(song.getPublisher())) {
			ret.add(new SongElement(SongElementEnum.COPYRIGHT, LABEL_PUBLISHER + song.getPublisher()));
		}
		if (notEmpty(song.getAdditionalCopyrightNotes())) {
			ret.add(new SongElement(SongElementEnum.COPYRIGHT, song.getAdditionalCopyrightNotes()));
		}

		return ret;
	}

	private static boolean addNewlineIfNotFirstLine(List<SongElement> elementList, boolean isFirst) {
		if (!isFirst) {
			elementList.add(new SongElement(SongElementEnum.NEW_LINE, "\n"));
		}
		return false;
	}

	/**
	 * Extract the first lyrics-only line from a song.
	 */
	public static String getFirstLyricsLine(Song song) {
		if (song.getLyrics() != null) {
			for (String line : song.getLyrics().split(NEWLINE_REGEX)) {
				Matcher translationMatcher = TRANSLATION_PATTERN.matcher(line);
				if (!translationMatcher.matches() && !isChordsLine(line)) {
					return line;
				}
			}
		}
		return "";
	}
	
	private static boolean isChordsLine(String line) {
		// Normalise tabs to spaces so tab-aligned chord lines work correctly
		String normalised = line.replace("\t", " ");

		// Empty or blank lines are not chord lines
		if (normalised.trim().isEmpty()) return false;

		// Split into tokens on whitespace
		String[] tokens = normalised.trim().split("\\s+");
		if (tokens.length == 0) return false;

		int recognised = 0;
		for (String token : tokens) {
			if (looksLikeChordToken(token)) recognised++;
		}

		// At least 60% of tokens must look like chords or valid notation
		return (double) recognised / tokens.length >= 0.6;
	}

	private static boolean looksLikeChordToken(String token) {
		if (token == null || token.isEmpty()) return false;

		// Accept common musical notation symbols
		if (token.matches("[|]+|---+|X\\d+|\\|X\\d+")) return true;

		// Strip surrounding parentheses for Style B inline chords like (Dm)
		if (token.startsWith("(") && token.endsWith(")")) {
			token = token.substring(1, token.length() - 1);
		}

		// Must start with a valid note letter A-G uppercase only
		if (token.isEmpty()) return false;
		if ("ABCDEFG".indexOf(token.charAt(0)) < 0) return false;

		// Safety check: if the token contains any non-ASCII character
		// (e.g. Tamil script), it is definitely not a chord
		for (int i = 0; i < token.length(); i++) {
			if (token.charAt(i) > 127) return false;
		}

		// Match valid chord pattern including:
		// - Standard: C, Dm, G#m, F#m, Bb, Am7
		// - Suspended: Dsus2, Asus4
		// - Slash: Am/G, F/C
		// - Transition pairs: DmEm, EmF, FG
		return token.matches(
			"[A-G][#b]?" +
			"(m|maj|min|dim|aug|sus)?" +
			"\\d{0,2}" +
			"(sus\\d|maj\\d|add\\d)?" +
			"([A-G][#b]?(m|maj|min|dim|aug|sus)?\\d{0,2})?" +
			"(/[A-G][#b]?(m|maj|min|dim|aug|sus)?\\d{0,2})?"
		);
	}

	/**
	 * Indicates specific elements of a {@link Song}.
	 */
	public enum SongElementEnum {
		/** a lyrics element (not always a whole line, see NEW_LINE) */
		LYRICS,
		/** a chord element (not always a whole line, see NEW_LINE) */
		CHORDS,
		/** a translation element (not always a whole line, see NEW_LINE) */
		TRANSLATION,
		/** a copyright element (always a whole line) */
		COPYRIGHT,
		/** indicates a line break between LYRICS, CHORDS and TRANSLATION elements - this element is only used there! */
		NEW_LINE
	}

	/**
	 * Holds one element of a {@link Song}, e.g. one lyrics line or the title.
	 *
	 * @author Mathis Dirksen-Thedens
	 */
	public static class SongElement {

		private SongElementEnum type;
		private String element;

		SongElement(SongElementEnum type, String element) {
			this.type = type;
			this.element = element;
		}

		public SongElementEnum getType() {
			return type;
		}

		public String getElement() {
			return element;
		}

		@Override
		public String toString() {
			return type + (element != null ? "[" + element + "]" : "");
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof SongElement && type == ((SongElement) obj).getType()
				&& equals(element, ((SongElement) obj).getElement());
		}

		private static boolean equals(String one, String two) {
			return (one == null && two == null) || (one != null && one.equals(two));
		}

	}

	private static boolean notEmpty(String str) {
		return str != null && !str.isEmpty();
	}

}
