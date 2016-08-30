package org.insightcentre.nlp.saffron.domainmodelling.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SaffronMapUtils extends org.apache.commons.collections.MapUtils {

	public static <K, V extends Number> LinkedHashMap<String, V> readMapFromFile(File f, Integer maxResults)
			throws IOException, ParseException {
		BufferedReader br = new BufferedReader(new FileReader(f));

		LinkedHashMap<java.lang.String, V> ranks = new LinkedHashMap<>();

		java.lang.String line;
		for (int i = 0; (line = br.readLine()) != null; i++) {
			String[] split = line.split(",");

			@SuppressWarnings("unchecked")
			V d = (V) NumberFormat.getInstance().parse(split[1]);
			ranks.put(split[0], d);
			if (maxResults != null && i == maxResults - 1) {
				br.close();
				return ranks;
			}
		}
		br.close();
		return ranks;
	}

	public static <K, V extends Number> LinkedHashMap<java.lang.String, V> readMapFromFile(File f) throws IOException,
			ParseException {
		return readMapFromFile(f, null);
	}

	public static <K, V extends Number> void writeMapToFile(File f, Map<String, V> m) throws IOException {

		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		for (Map.Entry<String, V> s : m.entrySet()) {
			bw.write(s.getKey() + "," + s.getValue() + "\n");
		}
		bw.close();
	}

	// http://stackoverflow.com/questions/109383/how-to-sort-a-mapkey-value-on-the-values-in-java
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValues(Map<K, V> map) {
		Comparator<Map.Entry<K, V>> comparator = new Comparator<Map.Entry<K, V>>() {
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		};

		return sortMap(map, comparator);
	}

	public static <K extends Comparable<? super K>, V> Map<K, V> sortByKeys(Map<K, V> map) {
		Comparator<Map.Entry<K, V>> comparator = new Comparator<Map.Entry<K, V>>() {
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return (o1.getKey()).compareTo(o2.getKey());
			}
		};

		return sortMapByKeys(map, comparator);
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValuesReverse(Map<K, V> map) {
		Comparator<Map.Entry<K, V>> comparator = new Comparator<Map.Entry<K, V>>() {
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		};

		return sortMap(map, comparator);
	}

	public static <K extends Comparable<? super K>, V> Map<K, V> sortMapByKeys(Map<K, V> map, Comparator<Map.Entry<K, V>> comparator) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
		Collections.sort(list, comparator);

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortMap(Map<K, V> map,
			Comparator<Map.Entry<K, V>> comparator) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
		Collections.sort(list, comparator);

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	public static List<String> keysWithUniqueValues(Map<String, String> map) {
		List<String> stems = new ArrayList<String>();
		List<String> uniqueStemsKP = new ArrayList<String>();

		Set<String> keys = map.keySet();
		for (String key : keys) {
			String value = map.get(key);
			if (!stems.contains(value)) {
				stems.add(value);
			}
		}

		for (String stem : stems) {
			for (String key : keys) {
				if (stem.equals(map.get(key))) {
					uniqueStemsKP.add(key);
					break;
				}
			}
		}

		return uniqueStemsKP;
	}
}
