package com.aeuovi.poh;

// FasterXML imports
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

// JLine imports
import org.jline.reader.*;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

// Java imports
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class POH {

    private static final String VERSION = "v1.1.0";
    private static final String PASSPORT_FILE = "LocalPassports.pof";

    public static void main(String[] args) throws IOException, InterruptedException {
        Terminal terminal = TerminalBuilder.builder().system(true).build();
        LineReader stdin = LineReaderBuilder.builder().terminal(terminal).build();

        System.out.println("Welcome to the Passport Office Helper (POH)");
        printHelp();

        boolean active = true;
        while (active) {
            String input = stdin.readLine("> ").toLowerCase().trim();
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
                    if ("all".equals(username)) isValidAll();
                    else isValid(username);
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
        }
    }

    private static void printHelp() {
        System.out.println(
                "Valid commands:\n" +
                "    Help : Displays this list\n" +
                "    Version : Outputs the version\n" +
                "    PlayerInfo <Username> : Outputs all relevant info about a player\n" +
                "    IsEligable <Username> : Checks if the player is eligible for a passport\n" +
                "    IsValid <Username | all> : Checks if a passport is still valid\n" +
                "    IssuePassport <Username> <true|false> : Issues a passport, optionally bypassing checks\n" +
                "    RevokePassport <Username> : Revokes the player's passport\n" +
                "    Quit : Exits POH"
        );
    }

    private static String sendPostRequest(String jsonBody) throws Exception {
        URL url = new URL("https://api.earthmc.net/v3/aurora/players");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
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
            User user = fetchUser(username);
            if (user != null) System.out.println(user);
            else System.out.println("Unknown username.");
        } catch (Exception e) {
            System.out.println("Error fetching player info: " + e.getMessage());
        }
    }

    private static boolean isEligible(String username) {
        try {
            User user = fetchUser(username);
            if (user == null) return false;
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
            User user = fetchUser(username);
            if (user == null) {
                System.out.println("Unknown username.");
                return;
            }
            System.out.println(user.name + (isEligible(username) ? " is eligible." : " is NOT eligible."));
        } catch (Exception e) {
            System.out.println("Error checking eligibility: " + e.getMessage());
        }
    }

    private static void isValid(String username) {
        if (isNullOrEmpty(username)) {
            System.out.println("Error: You must provide a username.");
            return;
        }
        try {
            User user = fetchUser(username);
            if (user == null) {
                System.out.printf("Unknown username: %s%n", username);
                return;
            }
            long daysSince = (System.currentTimeMillis() - user.timestamps.lastOnline) / (1000 * 60 * 60 * 24);
            long days14 = 14 - daysSince;
            long days30 = 30 - daysSince;
            System.out.printf("%s: %s (%d days grace)%n", user.name,
                    days14 <= 0 ? "NOT VALID" : "VALID for " + days14 + " more days", Math.max(days30, 0));
        } catch (Exception e) {
            System.out.printf("Error checking %s: %s%n", username, e.getMessage());
        }
    }

    private static void isValidAll() throws IOException {
        List<String> usernames = Files.readAllLines(Paths.get(PASSPORT_FILE));
        if (usernames.isEmpty()) {
            System.out.println("No usernames found in local passport file.");
            return;
        }
        for (String username : usernames) {
            isValid(username.trim());
        }
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

    private static User fetchUser(String username) throws Exception {
        String jsonBody = "{\"query\": [\"" + username + "\"]}";
        String response = sendPostRequest(jsonBody);
        if (response.trim().equals("[]")) return null;
        ObjectMapper mapper = new ObjectMapper();
        List<User> users = mapper.readValue(response, new TypeReference<List<User>>() {});
        return users.get(0);
    }

    private static boolean isNullOrEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}