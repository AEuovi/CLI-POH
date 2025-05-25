

package com.aeuovi.poh;

// FasterXML imports
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

// JLine imports
import org.jline.reader.*;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;
import org.jline.terminal.Attributes;

// Java imports
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class POH {

    private static final String VERSION = "v1.3.0";
    private static final String PASSPORT_FILE = "LocalPassports.pof";
    private static final String SPOOFED_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome123.0.6312.86 Safari/537.36";

    public static volatile boolean downloadComplete = false;

    public static void main(String[] args) throws IOException, InterruptedException {
        Terminal terminal = TerminalBuilder.builder().system(true).build();
        LineReader stdin = LineReaderBuilder.builder().terminal(terminal).build();
        Attributes original = terminal.getAttributes();

        syncPassports();
        System.out.println(
                "░▒▓███████▓▒░   ░▒▓██████▓▒░  ░▒▓█▓▒░░▒▓█▓▒░ \n"
                + "░▒▓█▓▒░░▒▓█▓▒░ ░▒▓█▓▒░░▒▓█▓▒░ ░▒▓█▓▒░░▒▓█▓▒░ \n"
                + "░▒▓█▓▒░░▒▓█▓▒░ ░▒▓█▓▒░░▒▓█▓▒░ ░▒▓█▓▒░░▒▓█▓▒░ \n"
                + "░▒▓███████▓▒░  ░▒▓█▓▒░░▒▓█▓▒░ ░▒▓████████▓▒░ \n"
                + "░▒▓█▓▒░        ░▒▓█▓▒░░▒▓█▓▒░ ░▒▓█▓▒░░▒▓█▓▒░ \n"
                + "░▒▓█▓▒░        ░▒▓█▓▒░░▒▓█▓▒░ ░▒▓█▓▒░░▒▓█▓▒░ \n"
                + "░▒▓█▓▒░         ░▒▓██████▓▒░  ░▒▓█▓▒░░▒▓█▓▒░ \n"
                + "Welcome to the Passport Office Helper (POH)"
        );
        printHelp();

        boolean active = true;
        while (active) {
            String input = stdin.readLine("> ").toLowerCase().trim();
            terminal.enterRawMode(); // Disables line editing and input echo
            terminal.puts(InfoCmp.Capability.cursor_invisible);
            terminal.flush();
            String[] arguments = input.split(" ");

            String command = arguments[0];
            String username = arguments.length > 1 ? arguments[1] : null;
            boolean override = arguments.length > 2 && Boolean.parseBoolean(arguments[2]);

            switch (command) {
                case "help":
                    printHelp();
                    break;
                case "version":
                    System.out.println("Passport Office Helper (POH) " + VERSION);
                    break;
                case "sync":
                    syncPassports();
                    break;
                case "playerinfo":
                    playerInfo(username);
                    break;
                case "iseligible":
                    showEligibility(username);
                    break;
                case "isvalid":
                    if ("all".equals(username)) {
                        isValidAll();
                    } else {
                        System.out.println(isValid(username));
                    }
                    break;
                case "quit":
                    active = false;
                    System.out.println("Thank you for using POH, have a nice day!");
                    Thread.sleep(3000);
                    break;
                default:
                    System.out.println("Error: Unknown command");
            }
            terminal.setAttributes(original); // Restore original settings
            terminal.puts(InfoCmp.Capability.cursor_visible);
            terminal.flush();
        }
    }

    private static void printHelp() {
        System.out.println(
                "Valid commands:\n"
                + "    Help : Displays this list\n"
                + "    Version : Outputs the version\n"
                + "    PlayerInfo <Username> : Outputs all relevant info about a player\n"
                + "    IsEligible <Username> : Checks if the player is eligible for a passport\n"
                + "    IsValid <Username | all> : Checks if a passport is still valid\n"
                + "    Sync : Updates the local passport list\n"
                + "    Quit : Exits POH"
        );
    }

    private static String sendPostRequest(String jsonBody) throws Exception {
        URL url = new URL("https://api.earthmc.net/v3/aurora/players");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", SPOOFED_UA);
        con.setDoOutput(true);

        try (OutputStream os = con.getOutputStream()) {
            os.write(jsonBody.getBytes("UTF-8"));
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            return in.lines().collect(Collectors.joining());
        }
    }

    private static void playerInfo(String username) {
        if (isNullOrEmpty(username)) {
            System.out.println("Error: You must provide a username.");
            return;
        }
        try {
            User user = fetchUser(username, false);
            if (user != null) {
                System.out.println(user);
            } else {
                System.out.println("Unknown username.");
            }
        } catch (Exception e) {
            System.out.println("Error fetching player info: " + e.getMessage());
        }
    }

    private static boolean isEligible(String username) {
        try {
            User user = fetchUser(username, true);
            if (user == null) {
                return false;
            }
            long diff = System.currentTimeMillis() - user.timestamps.joinedTownAt;
            return diff >= 14L * 24 * 60 * 60 * 1000;
        } catch (Exception e) {
            return false;
        }
    }

    private static void showEligibility(String username) {
        if (isNullOrEmpty(username)) {
            System.out.println("Error: You must provide a username.");
            return;
        }
        try {
            User user = fetchUser(username, false);
            if (user == null) {
                System.out.println("Unknown username.");
                return;
            }
            System.out.println(user.name + (isEligible(username) ? " is eligible." : " is NOT eligible."));
        } catch (Exception e) {
            System.out.println("Error checking eligibility: " + e.getMessage());
        }
    }

    private static String isValid(String username) {
        if (isNullOrEmpty(username)) {
            return "Error: You must provide a username.";
        }
        try {
            User user = fetchUser(username, false);
            if (user == null) {
                return "Unknown username: " + username;
            }

            Optional<Passport> passportOpt = loadPassports().stream()
                    .filter(p -> p.getUsername().equalsIgnoreCase(username))
                    .findFirst();

            if (passportOpt.isEmpty()) {
                return username + ": No passport found";
            }

            Passport passport = passportOpt.get();
            if ("SPE".equalsIgnoreCase(passport.getType())) {
                return username + ": SPECIAL passport, always valid";
            }

            long lastOnline = user.timestamps.lastOnline;
            long now = System.currentTimeMillis();
            long millisSinceLastOnline = now - lastOnline;

            long validityPeriod = 14L * 24 * 60 * 60 * 1000;  // 14 days
            long gracePeriod = 30L * 24 * 60 * 60 * 1000;     // 30 days total (including validity)

            if (millisSinceLastOnline <= validityPeriod) {
                long millisLeft = validityPeriod - millisSinceLastOnline;
                return username + ": VALID (" + formatDuration(millisLeft) + " left)";
            } else if (millisSinceLastOnline <= gracePeriod) {
                long millisLeft = gracePeriod - millisSinceLastOnline;
                return username + ": IN GRACE PERIOD (" + formatDuration(millisLeft) + " left)";
            } else {
                return username + ": NOT VALID (expired)";
            }
        } catch (Exception e) {
            return "Error checking validity for " + username + ": " + e.getMessage();
        }
    }

    private static void isValidAll() throws IOException {
        List<Passport> passports = loadPassports();
        if (passports.isEmpty()) {
            System.out.println("No passports found.");
            return;
        }

        StringBuilder result = new StringBuilder();
        for (Passport passport : passports) {
            String status = isValid(passport.getUsername());
            result.append(String.format("(#%04d) %s\n", passport.getId(), status));
        }
        System.out.print(result);
    }

    private static List<Passport> loadPassports() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(PASSPORT_FILE));
        List<Passport> passports = new ArrayList<>();
        for (String line : lines) {
            String[] parts = line.split(",", 3);
            if (parts.length < 3) continue;

            try {
                int id = Integer.parseInt(parts[0].trim());
                String username = parts[1].trim();
                String type = parts[2].trim();
                passports.add(new Passport(id, username, type));
            } catch (NumberFormatException ignored) {
            }
        }
        return passports;
    }

    private static String formatDuration(long millis) {
        long totalSeconds = millis / 1000;
        long days = totalSeconds / (24 * 3600);
        long hours = (totalSeconds % (24 * 3600)) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        if (days > 0) {
            return days + " day(s) " + hours + " hour(s)";
        } else if (hours > 0) {
            return hours + " hour(s) " + minutes + " minute(s)";
        } else {
            return minutes + " minute(s)";
        }
    }

    private static User fetchUser(String username, boolean silent) throws Exception {
        String jsonBody = "{\"query\": [\"" + username + "\"]}";

        final String[] spinner = {"|", "/", "-", "\\"};
        final boolean[] done = {false};
        final String[] responseHolder = {null};

        // Run the request in a separate thread
        Thread requestThread = new Thread(() -> {
            try {
                responseHolder[0] = sendPostRequest(jsonBody);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                done[0] = true;
            }
        });

        requestThread.start();

        if (!silent) {
            // Spinner in main thread
            int i = 0;
            while (!done[0]) {
                System.out.print("\r[" + spinner[i++ % spinner.length] + "] Fetching user " + username);
                System.out.flush();
                Thread.sleep(100);
            }

            System.out.println("\r[OK] Fetching user " + username);
        }

        // Wait for the request thread to ensure it's really finished
        requestThread.join();

        String response = responseHolder[0];
        if (response == null || response.trim().equals("[]")) {
            return null;
        }

        ObjectMapper mapper = new ObjectMapper();
        List<User> users = mapper.readValue(response, new TypeReference<List<User>>() {
        });
        return users.get(0);
    }

    private static boolean isNullOrEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static void syncPassports() {
        downloadComplete = false;
        String rawUrl = "https://raw.githubusercontent.com/AEuovi/CLI-POH/main/LocalPassports.pof";
        String localPath = "LocalPassports.pof";

        Thread downloadThread = new Thread(() -> {
            try {
                downloadFile(rawUrl, localPath);
                downloadComplete = true;
                System.out.print("\r[OK] Syncing");
            } catch (IOException e) {
                System.out.print("\r[X] Syncing");
                System.err.println("\nFailed to sync file: " + e.getMessage());
                downloadComplete = true;
            }
        });

        downloadThread.start();
        showSpinner("Syncing");

        System.out.println("\nFile synced successfully.");
    }

    private static void downloadFile(String urlStr, String dest) throws IOException {
        URL url = new URL(urlStr);
        try (InputStream in = url.openStream()) {
            Files.copy(in, Paths.get(dest), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void showSpinner(String message) {
        final String[] spinner = {"|", "/", "-", "\\"};
        int i = 0;
        while (!downloadComplete) {
            System.out.print("\r[" + spinner[i++ % spinner.length] + "] " + message);
            System.out.flush();
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
        System.out.print("\r[OK] " + message + "\n");
    }
}