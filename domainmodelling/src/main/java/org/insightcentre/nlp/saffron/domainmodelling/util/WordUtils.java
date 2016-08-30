package org.insightcentre.nlp.saffron.domainmodelling.util;
public class WordUtils {

    public static Integer computeTokensNo(String s) {
        return s.split(" ").length;
    }

    public static Integer minimumWordLength(String[] words) {
        Integer minLength = Integer.MAX_VALUE;

        for (String word : words) {
            if (minLength > word.length()) {
                minLength = word.length();
            }
        }
        return minLength;
    }

    public static boolean isAlpha(final char c) {
    	return Character.isLetter(c);
    }

    public static boolean isAlphaNumeric(final char c) {
        return Character.isLetterOrDigit(c);
    }

}
