import com.dampcake.bencode.Type;
import com.google.gson.Gson;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import com.dampcake.bencode.Bencode;// - available if you need it!

import javax.sound.midi.Soundbank;

public class Main {
    private static final Gson gson = new Gson();
    private static final Bencode bencode = new Bencode(false);
    private static final Bencode bencode1 = new Bencode(true);

    public static void main(String[] args) throws Exception {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        String command = args[0];
        switch (command) {
            case "decode":
                String bencodedValue = args[1];
                Object decoded;
                try {
                    decoded = decodeBencode(bencodedValue);
                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());
                    return;
                }
                System.out.println(gson.toJson(decoded));
                break;
            case "info":
                Object parsedTorrentFile = parseTorrentFile(args[1]);
                System.out.println(gson.toJson(parsedTorrentFile));
                break;
            default:
                System.out.println("Unknown command: " + command);
                break;
        }
    }

    //TODO : implemement Bencode object algorithms

    static Object decodeBencode(String bencodedString) {

        if (Character.isDigit(bencodedString.charAt(0))) {
            return bencode.decode(bencodedString.getBytes(StandardCharsets.UTF_8), Type.STRING);
        }

        if (bencodedString.charAt(0) == 'i' && bencodedString.charAt(bencodedString.length() - 1) == 'e') {
            return bencode.decode(bencodedString.getBytes(StandardCharsets.UTF_8), Type.NUMBER);

        }

        if (bencodedString.charAt(0) == 'l' && bencodedString.charAt(bencodedString.length() - 1) == 'e') {
            return bencode.decode(bencodedString.getBytes(StandardCharsets.UTF_8), Type.LIST);
        }

        if (bencodedString.charAt(0) == 'd') {
            return bencode.decode(bencodedString.getBytes(StandardCharsets.UTF_8), Type.DICTIONARY);
        }

        return "Invalid decode string";
    }

    static List<Object> parseTorrentFile(String fileName) throws IOException, NoSuchAlgorithmException {

        byte[] input = Files.readAllBytes(Paths.get(fileName));

        Map<String, Object> decoded = bencode.decode(input, Type.DICTIONARY);
        List<Object> output = new ArrayList<>();
        Map<String, Object> info = (Map<String, Object>) decoded.get("info");

        output.add("Tracker URL: " + decoded.get("announce"));
        output.add("Length: " + info.get("length"));
        output.add("Info Hash: " + calculateInfoHash(bencode1.encode(
                (Map<String, Object>)bencode1.decode(input, Type.DICTIONARY)
                        .get("info"))));

        return output;
    }

    static String calculateInfoHash(byte[] info) throws IOException, NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(info);

        StringBuilder hexString = new StringBuilder();

        for (byte b : digest) {
            hexString.append(String.format("%02x", b));
        }

        return hexString.toString();
    }
}
