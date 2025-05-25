package com.aeuovi.poh;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Scanner;

public class FileManager {

    private final Path fileLocation;
    private final ArrayList<String> fileContents = new ArrayList<>();

    public FileManager(Path fileLocation) throws IOException {
        this.fileLocation = fileLocation;
        if (!Files.exists(fileLocation)) {
            Files.createFile(fileLocation);
        }

        Scanner scan = new Scanner(fileLocation);
        while (scan.hasNextLine()) {
            this.fileContents.add(scan.nextLine());
        }
        scan.close();
    }

    public ArrayList<String> contents() {
        return this.fileContents;
    }

    public ArrayList<Passport> readPassports() {
        ArrayList<Passport> passports = new ArrayList<>();
        boolean firstLine = true;

        for (String line : fileContents) {
            if (firstLine) {
                firstLine = false;
                continue; // skip header
            }

            String[] parts = line.split(",", 3);
            if (parts.length == 3) {
                try {
                    int id = Integer.parseInt(parts[0].trim());
                    String username = parts[1].trim();
                    String type = parts[2].trim();
                    passports.add(new Passport(id, username, type));
                } catch (NumberFormatException e) {
                    System.err.println("Skipping invalid line: " + line);
                }
            }
        }

        return passports;
    }
}
