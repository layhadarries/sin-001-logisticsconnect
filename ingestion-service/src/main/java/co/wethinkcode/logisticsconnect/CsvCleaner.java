package co.wethinkcode.logisticsconnect;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


// Y, YES, yes, TRUE, true, FALSE, no, N/A, 0, 1, active, unknown <- active column
// true, yes, y, 1
// false, no, n, 0
// n/a, unknown
public class CsvCleaner {
    private static final Set<String> UNKNOWN_VAL = Set.of("", "n/a", "unknown");
    private static final Set<String> TRUE_VAL = Set.of("y", "yes", "1", "true");
    private static final Set<String> FALSE_VAL = Set.of("n", "no", "0", "false");

    
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
    




}
