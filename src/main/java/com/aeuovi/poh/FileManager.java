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
    }
    
    public void add(String name) {
        this.fileContents.add(name);
    }
    
    public void remove(String name) {
        if (this.fileContents.contains(name)) {
            this.fileContents.remove(name);
        }
    }
    
    public ArrayList<String> contents() {
        return this.fileContents;
    }
}
