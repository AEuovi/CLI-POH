package com.aeuovi.poh;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class POH {

    static String version = "v1.0.0";

    public static void main(String[] args) {
        boolean active = true;
        Scanner stdin = new Scanner(System.in);

        System.out.println("Welcome to the Passport Office Helper (POH)");
        help();

        while (active) {
            System.out.print("> ");
            String input = stdin.nextLine().toLowerCase();
            String[] arguments = input.trim().split(" ");

            String command = arguments[0];
            String username = arguments.length > 1 ? arguments[1] : null;

            if ("help".equals(command)) {
                help();
            } else if ("version".equals(command)) {
                version();
            } else if ("playerinfo".equals(command)) {
                playerInfo(username);
            } else if ("iseligable".equals(command)) {
                isEligable(username);
            } else if ("isvalid".equals(command)) {
                isValid(username);
            } else if ("quit".equals(command)) {
                active = false;
                System.out.println("Thank you for using POH, have a nice day!");
            } else {
                System.out.println("Error: Unknown command");
            }
        }

        stdin.close();
    }

    public static void help() {
        System.out.println(
            "Valid commands:\n" +
            "    Help : Displays this list\n" +
            "    Version : Outputs the version\n" +
            "    PlayerInfo <Username> : Outputs all of the relevant information of the given player\n" +
            "    IsEligable <Username> : Determines if the given player is eligible for the passport\n" +
            "    IsValid <Username> : Determines if the given player's passport is still valid\n" +
            "    Quit : exits POH"
        );
    }

    public static void version() {
        System.out.println("Passport Office Helper (POH) " + version);
    }

    private static String sendPostRequest(String jsonBody) throws Exception {
        URL url = new URL("https://api.earthmc.net/v3/aurora/players");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);

        OutputStream os = con.getOutputStream();
        os.write(jsonBody.getBytes("UTF-8"));
        os.close();

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String response = in.lines().collect(Collectors.joining());
        in.close();

        return response;
    }

    public static void playerInfo(String username) {
        if (username == null || username.isEmpty()) {
            System.out.println("Error: You must provide a username.");
            return;
        }

        String jsonBody = "{\"query\": [\"" + username + "\"]}";

        try {
            String body = sendPostRequest(jsonBody);
            if (body.trim().equals("[]")) {
                System.out.println("Unknown username.");
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            List<User> users = mapper.readValue(body, new TypeReference<List<User>>() {});
            System.out.println(users.get(0).toString());

        } catch (Exception e) {
            System.out.println("An error occurred while fetching player info: " + e.getMessage());
        }
    }

    public static void isEligable(String username) {
        if (username == null || username.isEmpty()) {
            System.out.println("Error: You must provide a username.");
            return;
        }

        String jsonBody = "{\"query\": [\"" + username + "\"]}";

        try {
            String body = sendPostRequest(jsonBody);
            if (body.trim().equals("[]")) {
                System.out.println("Unknown username.");
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            List<User> users = mapper.readValue(body, new TypeReference<List<User>>() {});
            User user = users.get(0);

            long registeredAt = user.timestamps.registered;
            long now = System.currentTimeMillis();
            long twoWeeksMillis = 14L * 24 * 60 * 60 * 1000;

            if (now - registeredAt >= twoWeeksMillis) {
                System.out.println(user.name + " is eligible for a passport.");
            } else {
                System.out.println(user.name + " is NOT eligible for a passport.");
            }

        } catch (Exception e) {
            System.out.println("An error occurred while checking eligibility: " + e.getMessage());
        }
    }

    public static void isValid(String username) {
        if (username == null || username.isEmpty()) {
            System.out.println("Error: You must provide a username.");
            return;
        }

        String jsonBody = "{\"query\": [\"" + username + "\"]}";

        try {
            String body = sendPostRequest(jsonBody);
            if (body.trim().equals("[]")) {
                System.out.println("Unknown username.");
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            List<User> users = mapper.readValue(body, new TypeReference<List<User>>() {});
            User user = users.get(0);

            long lastOnline = user.timestamps.lastOnline;
            long now = System.currentTimeMillis();
            long elapsedMillis = now - lastOnline;

            long daysSinceLastOnline = elapsedMillis / (1000 * 60 * 60 * 24);
            long daysRemaining14 = 14 - daysSinceLastOnline;
            long daysRemaining30 = 30 - daysSinceLastOnline;

            if (daysRemaining14 <= 0) {
                System.out.printf("The passport is no longer valid (is valid for %d more days)%n", Math.max(daysRemaining30, 0));
            } else {
                System.out.printf("The passport is still valid for %d more days (%d with notice)%n",
                        daysRemaining14, Math.max(daysRemaining30, 0));
            }

        } catch (Exception e) {
            System.out.println("An error occurred while checking passport validity: " + e.getMessage());
        }
    }
}
