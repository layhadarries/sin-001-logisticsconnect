package co.wethinkcode.logisticsconnect;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.*;
import java.util.*;


public class CsvCleaner {
    private static final List<String> PROVINCES = List.of(
            "western cape", "gauteng",
            "free state", "eastern cape",
            "limpopo", "northern cape",
            "mpumalanga", "north west");
    private static final List<String> KZN_PROVINCE = List.of(
            "kwazulu natal",
            "kwa-zulu natal",
            "kwazulu-natal");
    private static final List<String> UNKNOWN_VAL = List.of("", "n/a", "unknown");
    private static final List<String> TRUE_VAL = List.of("y", "yes", "1", "true");
    private static final List<String> FALSE_VAL = List.of("n", "no", "0", "false");

    // main method, loads the csv and cleans it
    public static List<Hub> loadAndClean(InputStream csvStream) {
        // [1] read csv file
        List<String[]> rawCsvLines = new ArrayList<>();

        // [2] clean each row
        List<CleanedRow> cleaned = new ArrayList<>();

        for (String[] row : rawCsvLines) {
            cleaned.add(cleanRow(row));
        }

        // [3] fill in missing provinces
        inferMissingProvinces(cleaned);

        // [4] remove duplicates
        List<Hub> hubs = duplicateCheck(cleaned);

        return hubs;
    }


    // [1] Read CSV file
    private static List<String[]> readCsv(InputStream inputStream) {
        List<String[]> csvRows = new ArrayList<>();

        try (CSVReader reader = new CSVReader(
                new InputStreamReader(inputStream))) {
            String[] nextLine;

            // read row until the end of file
            while ((nextLine = reader.readNext()) != null) {
                csvRows.add(nextLine);
            }

            return csvRows.subList(1, csvRows.size()); // skip header row

        } catch (FileNotFoundException e) {
            throw new RuntimeException("Failed to find hubs-global.csv", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read hubs-global.csv", e);
        } catch (CsvValidationException e) {
            throw new RuntimeException("Failed to parse hubs-global.csv", e);
        }
    }


    // [2] Iterate through individual rows to clean/filter
    private record CleanedRow(
            String hubId,
            String province,
            String sortingCenter,
            Boolean active) {
    }

    private static CleanedRow cleanRow(String[] row) {
        // Column 0 = hub ID
        String hubId = trimWhitespace(row[0]).toUpperCase(Locale.ROOT);

        // Column 1 = province
        String rawProvince = row[1];
        String province = normalizeProvince(row[1]);

        // Column 2 = sorting centre
        String rawSortingCenter = trimWhitespace(row[2]);
        String sortingCenter = titleCase(rawSortingCenter);

        // Column 3 = active
        Boolean active = Boolean.parseBoolean(row[3]);

        // create cleaned row construct
        CleanedRow cleanedRow = new CleanedRow(hubId, province, sortingCenter, active);

        return cleanedRow;
    }


    // ------- helpers -------
    // trimWhitespace --- trim() <- removes side white spaces,
    //                    replaceAll("\\s+", " ") <- takes wide spaces into 1 space
    // normalize dates/booleans ---
    private static String trimWhitespace(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    // fix casing --- toLower, toUpper
    private static String titleCase(String value) {
        if (value.isBlank()) {
            return value;
        }
        String[] words = value.toLowerCase(Locale.ROOT).split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    // provinces cleaning + checks
    private static String normalizeProvince(String rawProvince) {
        if (rawProvince.isBlank()) {
            return null;
        }

        // lower, remove white spaces (inner and outer)
        String cleanedProvince = rawProvince.toLowerCase(Locale.ROOT).replaceAll("[\\s-]+", " ").trim();

        if (KZN_PROVINCE.contains(cleanedProvince)) {
            return "KwaZulu-Natal";
        }

        return titleCase(cleanedProvince);
    }

    // active checks
    private static Boolean parseActiveBoolean(String raw) {
        // trim + lowercase
        String value = trimWhitespace(raw).toLowerCase(Locale.ROOT);

        if (TRUE_VAL.contains(value)) {
            return Boolean.TRUE;
        }
        if (FALSE_VAL.contains(value)) {
            return Boolean.FALSE;
        }

        // N/A, unknown, "", etc
        return null;
    }


    // ------- extra checks -------
    // find missing provinces
    private static void inferMissingProvinces (List < CleanedRow > rows) {
        Map<String, String> provinceBySortingCenter = new HashMap<>();

        // First find sorting centres where we already know the province
        for (CleanedRow row : rows) {
            if (row.province != null) {

                if (!provinceBySortingCenter.containsKey(
                        row.sortingCenter)) {

                    provinceBySortingCenter.put(
                            row.sortingCenter,
                            row.province
                    );
                }
            }
        }

        // now look for rows with missing provinces
        for (CleanedRow row : rows) {
            if (row.province == null) {
                String province =
                        provinceBySortingCenter.get(row.sortingCenter);
                row.province = province;
            }
        }
    }


    // remove duplicate hubs
    private static List<Hub> duplicateCheck (List<CleanedRow> rows) {
        List<Hub> result = new ArrayList<>();

        Map<String, CleanedRow> uniqueHubs = new HashMap<>();

        for (CleanedRow row : rows) {
            String key =
                    row.sortingCenter + "|" + row.province;

            // add to dict if not in it already
            if (!uniqueHubs.containsKey(key)) {
                uniqueHubs.put(key, row);
            } else {
                CleanedRow existingRow = uniqueHubs.get(key);
                Boolean active = resolveActive(existingRow.active, row.active);

                existingRow.active = active;

                // Keep the smaller hub ID
                if (row.hubId.compareTo(existingRow.hubId) < 0) {
                    existingRow.hubId = row.hubId;
                }
            }
        }

        // convert the dict(map) values into Hub objects
        for (CleanedRow row : uniqueHubs.values()) {
            Hub hub = new Hub(
                    row.hubId,
                    row.province,
                    row.sortingCenter,
                    row.active
            );
            result.add(hub);
        }

        return result;
    }


    // decide whether a duplicate group is active
    private static Boolean resolveActive (
            Boolean firstValue,
            Boolean secondValue) {

        // if both values are unknown
        if (firstValue == null && secondValue == null) {
            return null;
        }

        // if 1st value is unknown, use 2nd value
        if (firstValue == null) {
            return secondValue;
        }

        // if 2nd value is unknown, use 1st value
        if (secondValue == null) {
            return firstValue;
        }

        // if both are true
        if (firstValue == true && secondValue == true) {
            return true;
        }

        // if both are false
        if (firstValue == false && secondValue == false) {
            return false;
        }

        // if one is true other is false, keep the first value
        return firstValue;
    }
}
