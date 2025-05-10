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

    private static final String VERSION = "v1.2.1";
    private static final String PASSPORT_FILE = "LocalPassports.pof";
    private static final String SPOOFED_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome123.0.6312.86 Safari/537.36";

    public static void main(String[] args) throws IOException, InterruptedException {
        Terminal terminal = TerminalBuilder.builder().system(true).build();
        LineReader stdin = LineReaderBuilder.builder().terminal(terminal).build();
        Attributes original = terminal.getAttributes();

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
                case "playerinfo":
                    playerInfo(username);
                    break;
                case "iseligable":
                    showEligibility(username);
                    break;
                case "isvalid":
                    if ("all".equals(username)) {
                        isValidAll();
                    } else {
                        System.out.println(isValid(username));
                    }
                    break;
                case "issuepassport":
                    issuePassport(username, override);
                    break;
                case "revokepassport":
                    revokePassport(username);
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
                + "    IsEligable <Username> : Checks if the player is eligible for a passport\n"
                + "    IsValid <Username | all> : Checks if a passport is still valid\n"
                + "    IssuePassport <Username> <true|false> : Issues a passport, optionally bypassing checks\n"
                + "    RevokePassport <Username> : Revokes the player's passport\n"
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
            long daysSince = (System.currentTimeMillis() - user.timestamps.lastOnline) / (1000 * 60 * 60 * 24);
            long days14 = 14 - daysSince;
            long days30 = 30 - daysSince;
            return user.name + ": " + (days14 <= 0 ? "NOT VALID (" : "VALID for " + days14 + " more days (") + Math.max(days30, 0) + " days grace)";
        } catch (Exception e) {
            return "Error chacking " + username + ": " + e.getMessage();
        }
    }

    private static void isValidAll() throws IOException {
        List<String> usernames = Files.readAllLines(Paths.get(PASSPORT_FILE));
        if (usernames.isEmpty()) {
            System.out.println("No usernames found in local passport file.");
            return;
        }
        String validity = "";
        for (String username : usernames) {
            validity += isValid(username.trim()) + "\n";
        }
        System.out.print(validity);
    }

    private static void issuePassport(String username, boolean force) throws IOException {
        if (isNullOrEmpty(username)) {
            System.out.println("Error: You must provide a username.");
            return;
        }
        if (!force && !isEligible(username)) {
            System.out.println("Player is not eligible. Use override to bypass.");
            return;
        }
        try (FileWriter fw = new FileWriter(PASSPORT_FILE, true)) {
            fw.write(username + System.lineSeparator());
            System.out.println("Passport issued.");
        }
    }

    private static void revokePassport(String username) throws IOException {
        if (isNullOrEmpty(username)) {
            System.out.println("Error: You must provide a username.");
            return;
        }
        Path path = Paths.get(PASSPORT_FILE);
        List<String> lines = Files.readAllLines(path);
        List<String> updated = lines.stream()
                .filter(line -> !line.trim().equalsIgnoreCase(username.trim()))
                .collect(Collectors.toList());
        Files.write(path, updated);
        System.out.println(updated.size() == lines.size() ? "No entry found." : "Passport revoked.");
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
}
