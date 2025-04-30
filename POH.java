package com.aeuovi.poh;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Scanner;

public class POH {

    static String version = "v1.0.0";
    static HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) {
        boolean active = true;
        Scanner stdin = new Scanner(System.in);

        System.out.println("Welcome to the Passport Office Helper (POH)");
        help();

        while (active) {
            System.out.print("> ");
            String input = stdin.nextLine().toLowerCase();
            String[] arguments = input.split(" ");

            switch (arguments[0]) {
                case "help" ->
                    help();
                case "version" ->
                    version();
                case "playerinfo" -> {
                    if (arguments.length < 2) {
                        System.out.println("Error: You must provide a username.");
                    } else {
                        System.out.println(playerInfo(arguments[1]));
                    }
                }
                case "iseligable" ->
                    isEligable(arguments.length > 1 ? arguments[1] : "");
                case "isvalid" ->
                    isValid(arguments[1]);
                case "quit" -> {
                    active = false;
                    System.out.println("Thank you for using POH, have a nice day!");
                }
                default ->
                    System.out.println("Error: Unknown command");
            }
        }

        stdin.close();
    }

    public static void help() {
        System.out.println("""
        Valid commands:
            Help : Displays this list
            Version : Outputs the version
            PlayerInfo <Username> : Outputs all of the relevant information of the given player
            IsEligable <Username> : Determines if the given player is eligible for the passport
            IsValid <Username> : Determines if the given player's passport is still valid
            Quit : exits POH""");
    }

    public static void version() {
        System.out.println("Passport Office Helper (POH) " + version);
    }

    public static String playerInfo(String username) {
        String jsonBody = String.format("{\"query\": [\"%s\"]}", username);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.earthmc.net/v3/aurora/players"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            if (body.trim().equals("[]")) {
                return "Unknown username.";
            }

            ObjectMapper mapper = new ObjectMapper();
            List<User> users = mapper.readValue(body, new TypeReference<List<User>>() {
            });
            return users.get(0).toString(); // Only one user expected
        } catch (Exception e) {
            return "An error occurred while fetching player info: " + e.getMessage();
        }
    }

    public static void isEligable(String username) {
        String jsonBody = String.format("{\"query\": [\"%s\"]}", username);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.earthmc.net/v3/aurora/players"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            if (body.trim().equals("[]")) {
                System.out.println("Unknown username.");
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            List<User> users = mapper.readValue(body, new TypeReference<List<User>>() {
            });
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
        if (username == null || username.isBlank()) {
            System.out.println("Error: You must provide a username.");
            return;
        }

        String jsonBody = String.format("{\"query\": [\"%s\"]}", username);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.earthmc.net/v3/aurora/players"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            if (body.trim().equals("[]")) {
                System.out.println("Unknown username.");
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            List<User> users = mapper.readValue(body, new TypeReference<List<User>>() {
            });
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
                System.out.printf("The passport is still valid for %d more days (%d with notice)%n", daysRemaining14, Math.max(daysRemaining30, 0));
            }

        } catch (Exception e) {
            System.out.println("An error occurred while checking passport validity: " + e.getMessage());
        }
    }
}
