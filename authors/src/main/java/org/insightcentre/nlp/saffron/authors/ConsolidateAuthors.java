package org.insightcentre.nlp.saffron.authors;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import me.xuender.unidecode.Unidecode;
import org.bouncycastle.util.Arrays;
import org.insightcentre.nlp.saffron.DefaultSaffronListener;
import org.insightcentre.nlp.saffron.SaffronListener;
import org.insightcentre.nlp.saffron.data.Author;

/**
 *
 * @author John McCrae
 */
public class ConsolidateAuthors {

    private final HashSet<String> asianNames;
    private final HashMap<String, NameTag> nameDB;
    private final HashMap<String, Set<String>> nicknames;

    private final HashSet<String> highFreqSurnames = new HashSet<String>() {
        {
            add("van");
            add("de");
            add("der");
            add("von");
            add("la");
            add("le");
            add("den");
            add("di");
            add("o");
            add("mac");
            add("del");
            add("du");
            add("ben");
            add("da");
            add("a");
            add("ten");
            add("lo");
            add("al");

        }
    };

    /**
     * Create a consolidate authors object
     * @throws IOException If resources could not be loaded. They should be available in the JAR, so this will not happen if compiled normally.
     */
    public ConsolidateAuthors() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        this.asianNames = mapper.readValue(ConsolidateAuthors.class.getResource("/asian-surnames.json"), mapper.getTypeFactory().constructCollectionType(HashSet.class, String.class));
        this.nameDB = mapper.readValue(new GZIPInputStream(ConsolidateAuthors.class.getResource("/namedb.json.gz").openStream()), mapper.getTypeFactory().constructMapType(HashMap.class, String.class, NameTag.class));
        this.nicknames = new HashMap<>();
        Map<String, Set<String>> names = mapper.readValue(ConsolidateAuthors.class.getResource("/names.json"), mapper.getTypeFactory().constructMapType(HashMap.class, String.class, Set.class));
        for (Map.Entry<String, Set<String>> e : names.entrySet()) {
            for (String t : e.getValue()) {
                if(!nicknames.containsKey(t)) {
                    nicknames.put(t, new HashSet<>());
                }
                nicknames.get(t).add(e.getKey());
            }
        }
    }

    /**
     * Is there are match between the ith and jth words in the name
     * @param name1 The first name
     * @param name2 The second name
     * @param i Index in the first name
     * @param j Index in the second name
     * @return true if they match, e.g., "James", "Jimmy" and "J." all match
     */
    private boolean matchAt(TaggedName name1, TaggedName name2, int i, int j) {
        if ((name1.tag[i] == NameTag.initial || name1.tag[i] == NameTag.maybe_initial)
                && name2.normalized[j].startsWith(name1.normalized[i].substring(0, 1))) {
            return name2.tag[j] != NameTag.surname;
        }

        if ((name2.tag[j] == NameTag.initial || name2.tag[j] == NameTag.maybe_initial)
                && name1.normalized[i].startsWith(name2.normalized[j].substring(0, 1))) {
            return name1.tag[i] != NameTag.surname;
        }

        Set<String> n1 = new HashSet<>();
        n1.add(name1.normalized[i]);
        if (nicknames.containsKey(name1.normalized[i])) {
            n1.addAll(nicknames.get(name1.normalized[i]));
        }
        Set<String> n2 = new HashSet<>();
        n2.add(name2.normalized[j]);
        if (nicknames.containsKey(name2.normalized[j])) {
            n2.addAll(nicknames.get(name2.normalized[j]));
        }

        n1.retainAll(n2);
        if (!n1.isEmpty()) {
            switch (name1.tag[i]) {
                case given:
                    return name2.tag[j] != NameTag.surname;
                case surname:
                    return name2.tag[j] != NameTag.given && name2.tag[j] != NameTag.initial;
                case maybe_initial:
                case either:
                    return true;
                default:
                    System.err.println("Unreachable... I think!");
                    return false;
            }
        } else {
            return false;
        }
    }

    /**
     * The tags for words in a name
     */
    private enum NameTag {
        given,
        surname,
        either, // A name that can be either a given or a surname
        initial,
        maybe_initial // 'a' and 'o', because J O Neill may be John Oliver Neill
    }

    /**
     * A name with its tags and a reference to the original author object
     */
    private class TaggedName {

        String[] normalized;
        NameTag[] tag;
        Author author;
    }

    /**
     * Consolidate a list of authors into a (smaller) set of authors
     * @param authors The list of authors
     * @return A maps whose keys are the list of consolidated authors and values
     *  are all the other author objects mapped to this author
     */
    public Map<Author, Set<Author>> consolidate(Collection<Author> authors) {
        return consolidate(authors, new DefaultSaffronListener());
    }

    /**
     * Consolidate a list of authors into a (smaller) set of authors
     * @param authors The list of authors
     * @param log An object for giving log messages
     * @return A maps whose keys are the list of consolidated authors and values
     *  are all the other author objects mapped to this author
     */
    public Map<Author, Set<Author>> consolidate(Collection<Author> authors, SaffronListener log) {
        Map<String, List<TaggedName>> bySurname = new HashMap<>();
        for (Author author : authors) {
            TaggedName taggedName = tag(author);
            for (int i = 0; i < taggedName.normalized.length; i++) {
                if (taggedName.tag[i] == NameTag.surname || taggedName.tag[i] == NameTag.either) {
                    if (!highFreqSurnames.contains(taggedName.normalized[i]) || i == taggedName.normalized.length - 1) { 
                        if (!bySurname.containsKey(taggedName.normalized[i])) {
                            bySurname.put(taggedName.normalized[i], new ArrayList<>());
                        }
                        bySurname.get(taggedName.normalized[i]).add(taggedName);
                    }
                }
            }
        }
        Set<Author> marked = new HashSet<>();
        Map<Author, Set<Author>> consolidated = new HashMap<>();
        for (List<TaggedName> names : bySurname.values()) {
            for (TaggedName name1 : names) {
                if(marked.contains(name1.author))
                    continue;
                boolean nonSymmetricMatch = false;
                for (TaggedName name2 : names) {
                    if(marked.contains(name2.author))
                        continue;
                    if(name1 == name2) {
                        nonSymmetricMatch = true;
                    } else if (nonSymmetricMatch) {
                        int s = isSimilar(name1, name2);
                        if (s == -1) { // name1 is more canonical
                            if (!consolidated.containsKey(name1.author)) {
                                consolidated.put(name1.author, new HashSet<>());
                            }
                            consolidated.get(name1.author).add(name2.author);
                            marked.add(name2.author);
                        } else if (s == +1) { // name2 is more canonical

                            if (!consolidated.containsKey(name2.author)) {
                                consolidated.put(name2.author, new HashSet<>());
                            }
                            consolidated.get(name2.author).add(name1.author);
                            marked.add(name1.author);
                        }

                    }
                }
            }
        }
        
        for(Author a : authors) {
            if(!marked.contains(a)) {
                consolidated.put(a, Collections.singleton(a));
            }
        }
                
        return consolidated;
    }

    /**
     * Check if two authors could have the same name
     * @param author The first author
     * @param author2 The second author
     * @return True if they have the same name
     */
    public boolean isSimilar(Author author, Author author2) {
        return isSimilar(tag(author), tag(author2)) != 0;
    }

    private int isSimilar(TaggedName name1, TaggedName name2) {
        int[] match1 = new int[name1.normalized.length];
        int[] match2 = new int[name2.normalized.length];
        Arrays.fill(match1, -1);
        Arrays.fill(match2, -1);

        // First match each element in the names
        boolean givenMatch = false, surnameMatch = false;
        for (int i = 0; i < name1.normalized.length; i++) {
            for (int j = 0; j < name2.normalized.length; j++) {
                if (match1[i] < 0 && match2[j] < 0) {
                    if (matchAt(name1, name2, i, j)) {
                        match1[i] = j;
                        match2[j] = i;
                        switch (name1.tag[i]) {
                            case given:
                            case initial:
                                givenMatch = true;
                                break;
                            case surname:
                                surnameMatch = true;
                                break;
                            case either:
                            case maybe_initial:
                                if (name2.tag[j] == NameTag.given || name2.tag[j] == NameTag.initial) {
                                    givenMatch = true;
                                } else if (name2.tag[j] == NameTag.surname) {
                                    surnameMatch = true;
                                } else if (givenMatch) {
                                    surnameMatch = true;
                                } else {
                                    givenMatch = true;
                                }
                        }
                    }
                }
            }
        }

        // If no given name or no surname matches, then they are not similar
        if (!givenMatch || !surnameMatch) {
            return 0;
        }

        // We allow some names to be dropped and the name to still match
        boolean givenDropLeft1 = false, givenDropLeft2 = false,
                givenDropRight1 = false, givenDropRight2 = false,
                surnameDropLeft1 = false, surnameDropLeft2 = false,
                surnameDropRight1 = false, surnameDropRight2 = false;

        // Check how many unaligned elements of name 1 we are matching
        int state = 0; // 0 = before first matching given name, 1 = after first matching given name, 2 = before first matching surname, 3 = after first matching surname
        for (int i = 0; i < name1.normalized.length; i++) {
            if (match1[i] < 0) {
                switch (state) {
                    case 0:
                        givenDropLeft1 = true;
                        break;
                    case 1:
                        givenDropRight1 = true;
                        if (name1.tag[i] == NameTag.surname) {
                            state = 2;
                        }
                        break;
                    case 2:
                        givenDropLeft1 = true;
                        break;
                    case 3:
                        givenDropRight1 = true;
                }
            } else {
                switch (state) {
                    case 0:
                        state = 1;
                        break;
                    case 1:
                        if (name1.tag[i] == NameTag.surname) {
                            state = 3;
                        } else if(givenDropRight1)
                            // We fail here as we do not allow unaligned names in
                            // between aligned given names
                            return 0;
                        break;
                    case 2:
                        state = 3;
                        break;
                    case 3:
                        if(surnameDropRight1)
                            return 0;
                        break;
                }
            }
        }

        // Repeat for name2
        state = 0;
        for (int j = 0; j < name2.normalized.length; j++) {
            if (match2[j] < 0) {
                switch (state) {
                    case 0:
                        givenDropLeft2 = true;
                        break;
                    case 1:
                        givenDropRight2 = true;
                        if (name2.tag[j] == NameTag.surname) {
                            state = 2;
                        }
                        break;
                    case 2:
                        givenDropLeft2 = true;
                        break;
                    case 3:
                        givenDropRight2 = true;
                }
            } else {
                switch (state) {
                    case 0:
                        state = 1;
                        break;
                    case 1:
                        if (name2.tag[j] == NameTag.surname) {
                            state = 3;
                        } else if (givenDropRight2)
                            return 0;
                        break;
                    case 2:
                        state = 3;
                        break;
                    case 3:
                        if(surnameDropRight2)
                            return 0;
                        break;
                }
            }
        }

        // Don't allow contradicting drops, e.g., John Peter != John Paul
        if (givenDropLeft1 && givenDropLeft2) {
            return 0;
        }

        if (givenDropRight1 && givenDropRight2) {
            return 0;
        }

        if (surnameDropLeft1 && surnameDropLeft1) {
            return 0;
        }

        if (surnameDropRight1 && surnameDropRight2) {
            return 0;
        }

        if (givenDropLeft1 && givenDropRight1) {
            return 0;
        }

        if (givenDropLeft2 && givenDropRight2) {
            return 0;
        }

        if (surnameDropLeft1 && surnameDropRight1) {
            return 0;
        }

        if (surnameDropLeft2 && surnameDropRight2) {
            return 0;
        }

        // If we are only dropping from 2 then we prefer the longer name
        if (givenDropLeft2 || givenDropRight2 || surnameDropLeft2 || surnameDropRight2) {
            return 1;
        }

        return -1;
    }

    /**
     * Tag a name
     * @param author The author
     * @return The name, with the words normalized and tagged as given/surname
     */
    private TaggedName tag(Author author) {
        // Step 1. Normalize the name by lowercasing and removing accents
        // Gómez => gomez
        String name = Unidecode.decode(author.name.toLowerCase());

        // Step 2. Check for a comma
        // McCrae, J.P. => J.P. McCrae
        String[] commaSplit = name.split(",\\s*");
        if (commaSplit.length == 2) {
            name = commaSplit[1] + " " + commaSplit[0];
        }

        // Step 3. Check for multi initials
        // k.d. lang => k. d. lang
        name = name.replaceAll("([a-z]\\.)([a-z]\\.)", "$1 $2");

        // Step 4. Tokenize the name
        TaggedName taggedName = new TaggedName();
        taggedName.normalized = name.split("\\s+");
        taggedName.tag = new NameTag[taggedName.normalized.length];
        taggedName.author = author;

        // Step 5. Check for inverted Asian names
        // Tanaka Taro => Taro Tanaka
        // Tanaka T. => T. Tanaka
        if (taggedName.normalized.length == 2 && asianNames.contains(taggedName.normalized[0])) {
            String s = taggedName.normalized[0];
            taggedName.normalized[0] = taggedName.normalized[1];
            taggedName.normalized[1] = s;
            taggedName.tag[0] = taggedName.normalized[0].matches("[a-z]\\.?") ? NameTag.initial : NameTag.given;
            taggedName.tag[1] = NameTag.surname;
            return taggedName;
        }

        // Step 6. Check initialisms
        for (int i = 0; i < taggedName.normalized.length; i++) {
            switch (taggedName.normalized[i].length()) {
                case 1:
                    if (taggedName.normalized[i].equals("a") || taggedName.normalized[i].equals("o") && i > 0) {
                        taggedName.tag[i] = NameTag.maybe_initial;
                    } else {
                        taggedName.tag[i] = NameTag.initial;
                    }
                    break;
                case 2:
                    if (taggedName.normalized[i].charAt(1) == '.') {
                        taggedName.tag[i] = NameTag.initial;
                    }
                    break;
                default:
            }
        }

        // Step 7. Invert initialisms
        // McCrae J. P. => J. P. McCrae
        for (int i = taggedName.normalized.length - 1; i > 0; i--) {
            if (taggedName.tag[i] != NameTag.initial && taggedName.tag[i] != NameTag.maybe_initial) {
                if (i != taggedName.normalized.length - 1) {
                    String[] newNormalized = new String[taggedName.normalized.length];
                    NameTag[] newTags = new NameTag[taggedName.tag.length];
                    System.arraycopy(taggedName.normalized, i, newNormalized, 0, taggedName.normalized.length - i);
                    System.arraycopy(taggedName.tag, i, newTags, 0, taggedName.tag.length - i);
                    System.arraycopy(taggedName.normalized, 0, newNormalized, taggedName.normalized.length - i, i);
                    System.arraycopy(taggedName.tag, 0, newTags, taggedName.tag.length - i, i);
                    taggedName.normalized = newNormalized;
                    taggedName.tag = newTags;
                }
                break;
            }
        }

        if (taggedName.normalized.length == 0) {
            return taggedName; // Empty name string!
        }
        if (taggedName.normalized.length == 1) {
            taggedName.tag[0] = NameTag.surname;
            return taggedName;
        }

        // Main tagging. We assume there is at least one given name and one 
        // surname (this is false), and then we use the database to infer which
        // element is which. Given names must all precede surnames.
        if (taggedName.tag[0] == null) {
            taggedName.tag[0] = NameTag.given;
        }
        taggedName.tag[taggedName.tag.length - 1] = NameTag.surname;
        boolean surnameFound = false;
        for (int i = 1; i < taggedName.normalized.length - 1; i++) {
            if (taggedName.tag[i] == null) {
                if (surnameFound) {
                    taggedName.tag[i] = NameTag.surname;
                } else {
                    taggedName.tag[i] = nameDB.getOrDefault(taggedName.normalized[i], NameTag.either);
                    surnameFound = taggedName.tag[i] == NameTag.surname;
                }
            }
        }

        return taggedName;
    }

}
