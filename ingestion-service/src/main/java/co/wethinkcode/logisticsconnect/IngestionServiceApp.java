package co.wethinkcode.logisticsconnect;

import co.wethinkcode.logisticsconnect.Hub;
import io.javalin.Javalin;

import java.io.InputStream;
import java.util.List;

public class IngestionServiceApp {

    public static void main(String[] args) {
        List<Hub> cleanedHubs = loadCleanedHubs();
        System.out.println("Loaded " + cleanedHubs.size() + " cleaned hub records.");

        // create web api server
        Javalin app = Javalin.create().start(7050);

        app.get("/health", ctx -> ctx.result("OK"));

        // cleaned hub/sorting-center records
        app.get("/hubs", ctx -> ctx.json(cleanedHubs));
    }

    private static List<Hub> loadCleanedHubs() {
        try (InputStream csvStream = IngestionServiceApp.class.getResourceAsStream("/hubs-global.csv")) {
            if (csvStream == null) {
                throw new IllegalStateException("hubs-global.csv not found on classpath");
            }
            return CsvCleaner.loadAndClean(csvStream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load and clean hubs-global.csv", e);
        }
    }
}