import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

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
                printPieceHashes(args[1]);
                break;
            case "peers":
                Map<String, String> params = createParamsMap(args[1]);
                String tracker = params.get("tracker");
                params.remove("tracker");
                makeRequestToTracker(tracker, params);
                break;
            default:
                System.out.println("Unknown command: " + command);
                break;
        }
    }

    //TODO : implemement Bencode object algorithms

    static Object decodeBencode(String bencodedString) {

        if (Character.isDigit(bencodedString.charAt(0))) {
            return bencode.decode(bencodedString.getBytes(), Type.STRING);
        }

        if (bencodedString.charAt(0) == 'i' && bencodedString.charAt(bencodedString.length() - 1) == 'e') {
            return bencode.decode(bencodedString.getBytes(), Type.NUMBER);

        }

        if (bencodedString.charAt(0) == 'l' && bencodedString.charAt(bencodedString.length() - 1) == 'e') {
            return bencode.decode(bencodedString.getBytes(), Type.LIST);
        }

        if (bencodedString.charAt(0) == 'd') {
            return bencode.decode(bencodedString.getBytes(), Type.DICTIONARY);
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
        output.add("Info Hash: " + getHexString(calculateInfoHash(bencode1.encode(
                (Map<String, Object>) bencode1.decode(input, Type.DICTIONARY)
                        .get("info")))));


        return output;
    }

    static byte[] calculateInfoHash(byte[] info) throws IOException, NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(info);

        return digest;
    }

    static void printPieceHashes(String fileName) throws IOException {

        byte[] input = Files.readAllBytes(Paths.get(fileName));

        Map<?, ?> decoded = bencode1.decode(input, Type.DICTIONARY);
        Map<String, ?> info = (Map<String, ?>) decoded.get("info");

        ByteBuffer piecesString = (ByteBuffer) info.get("pieces");
        System.out.println("Piece Length: " + info.get("piece length"));
        System.out.println("Piece hashes: ");
        System.out.println("ByteBuffer content (20 bytes at a time):");
        while (piecesString.hasRemaining()) {
            // Determine how many bytes to print (up to 20)
            int chunkSize = Math.min(20, piecesString.remaining());

            // Print 20 bytes in one iteration
            for (int i = 0; i < chunkSize; i++) {
                byte b = piecesString.get();
                System.out.printf("%02x", b);  // Print byte in hexadecimal format
            }
            System.out.println();  // New line after printing 20 bytes
        }
    }

    static String getHexString(byte[] bytes) {

        StringBuilder hexString = new StringBuilder();

        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }

        return hexString.toString();
    }

    static void makeRequestToTracker(String tracker, Map<String, String> params) throws IOException {

        StringBuilder paramStringBuilder = new StringBuilder(tracker);
        paramStringBuilder.append("/?");
        params.remove("tracker");
        for (Map.Entry<String, String> entrySet : params.entrySet()) {
            paramStringBuilder.append(entrySet.getKey()).append("=").append(entrySet.getValue()).append("&");
        }
        System.out.println(paramStringBuilder.toString());

        URL url = new URL(paramStringBuilder.toString());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        byte[] byteArray = null;

        try {
            byteArray = con.getInputStream().readAllBytes();
        }

        // Catch block to handle the exceptions
        catch (IOException e) {

            // Print and display the exceptions
            System.out.println(e);
        }
        Map<?, ?> contentDecoded = bencode1.decode(byteArray, Type.DICTIONARY);
        ByteBuffer peers = (ByteBuffer) contentDecoded.get("peers");
     //   InetAddress ipAddress = InetAddress.getByAddress();
//        byte[] ip = new byte[]{peers.get(0),
//                peers.get(1), peers.get(2), peers.get(3) };
      //  byte[] port = new byte[]{peers.get(4), peers.get(5)};
    //    InetAddress ipAddress = InetAddress.getByAddress(ip);


        while (peers.hasRemaining()) {
            byte[] p = new byte[6];

            for (int i = 0; i < 6; i++) {
                byte b = peers.get();
                p[i] = b;
            }

            InetAddress ipAddress = InetAddress.getByAddress(Arrays.copyOfRange(p,0,4));
            byte[] port = new byte[]{p[4], p[5]};
            int value = (((port[0] & 0xFF) << 8) | (port[1] & 0xFF));
            System.out.println(ipAddress.getHostAddress() + ":" + value);
        }

    }

    static Map<String, String> createParamsMap(String fileName) throws IOException, NoSuchAlgorithmException {
        Map<String, String> paramsMap = new HashMap<>();

        byte[] input = Files.readAllBytes(Paths.get(fileName));

        Map<String, Object> decoded = bencode.decode(input, Type.DICTIONARY);
        Map<String, Object> info = (Map<String, Object>) decoded.get("info");

        byte[] infoHash = calculateInfoHash(bencode1.encode((Map<String, Object>) bencode1.decode(input, Type.DICTIONARY).get("info")));
        String encodedHash = URLEncoder.encode(new String(infoHash, StandardCharsets.ISO_8859_1), StandardCharsets.ISO_8859_1);
        paramsMap.put("tracker", (String) decoded.get("announce"));
        paramsMap.put("info_hash", encodedHash);
        paramsMap.put("peer_id", "my_unique_id_very_fa");
        paramsMap.put("port", "6681");
        paramsMap.put("uploaded", "0");
        paramsMap.put("downloaded", "0");
        paramsMap.put("left", "" + input.length);
        paramsMap.put("compact", "1");

        return paramsMap;
    }
}
