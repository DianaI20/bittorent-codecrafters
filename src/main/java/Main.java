import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.google.gson.Gson;

import java.io.*;
import java.lang.reflect.Array;
import java.net.*;
import java.nio.ByteBuffer;
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
        TorrentFile parsedTorrentFile;
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
                parsedTorrentFile = parseTorrentFile(args[1]);
                printTorrentFileInfo(parsedTorrentFile);
                break;
            case "peers":
                Map<String, String> params = createParamsMap(args[1]);
                String tracker = params.get("tracker");
                params.remove("tracker");
                makeRequestToTracker(tracker, params);
                break;
            case "handshake":
                parsedTorrentFile = parseTorrentFile(args[1]);
                String[] peerInfo = getIpAdressAndPort(args[2]);
                connectToPeer(peerInfo[0], peerInfo[1], parsedTorrentFile);
                break;
            default:
                System.out.println("Unknown command: " + command);
                break;
        }
    }

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

    static TorrentFile parseTorrentFile(String fileName) throws IOException, NoSuchAlgorithmException {

        TorrentFile torrentFile = new TorrentFile();
        byte[] input = Files.readAllBytes(Paths.get(fileName));

        Map<String, Object> decoded = bencode.decode(input, Type.DICTIONARY);
        Map<String, Object> info = (Map<String, Object>) decoded.get("info");
        torrentFile.setTrackerUrl((String) decoded.get("announce"));
        torrentFile.setLength((Long) info.get("length"));
        torrentFile.setInfoHash(calculateInfoHash(
                        bencode1.encode(
                                (Map<String, Object>) bencode1.decode(input, Type.DICTIONARY).get("info")
                        )));
        torrentFile.setPieceLength((Long) info.get("piece length"));

        addPieceHashes(input, torrentFile);

        return torrentFile;
    }

    static byte[] calculateInfoHash(byte[] info) throws IOException, NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(info);

        return digest;
    }

    static void addPieceHashes(byte[] input, TorrentFile file) throws IOException {

        Map<?, ?> decoded = bencode1.decode(input, Type.DICTIONARY);
        Map<String, ?> info = (Map<String, ?>) decoded.get("info");

        ByteBuffer piecesString = (ByteBuffer) info.get("pieces");
        StringBuilder stringBuilder;
        while (piecesString.hasRemaining()) {
            // Determine how many bytes to print (up to 20)
            int chunkSize = Math.min(20, piecesString.remaining());
            stringBuilder = new StringBuilder();
            // Print 20 bytes in one iteration
            for (int i = 0; i < chunkSize; i++) {
                byte b = piecesString.get();
                stringBuilder.append(String.format("%02x", b));

            }
            file.addPieceHash(stringBuilder.toString());
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
        } catch (IOException e) {
            System.out.println(e);
        }
        Map<?, ?> contentDecoded = bencode1.decode(byteArray, Type.DICTIONARY);
        ByteBuffer peers = (ByteBuffer) contentDecoded.get("peers");

        while (peers.hasRemaining()) {
            byte[] p = new byte[6];

            for (int i = 0; i < 6; i++) {
                byte b = peers.get();
                p[i] = b;
            }

            InetAddress ipAddress = InetAddress.getByAddress(Arrays.copyOfRange(p, 0, 4));
            byte[] port = new byte[]{p[4], p[5]};
            int value = (((port[0] & 0xFF) << 8) | (port[1] & 0xFF));
            System.out.println(ipAddress.getHostAddress() + ":" + value);
        }

    }

    static Map<String, String> createParamsMap(String fileName) throws IOException, NoSuchAlgorithmException {
        Map<String, String> paramsMap = new HashMap<>();

        byte[] input = getBytesFromFile(fileName);

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

    static byte[] getBytesFromFile(String fileName) throws IOException {
        return Files.readAllBytes(Paths.get(fileName));
    }

    static void printTorrentFileInfo(TorrentFile torrentFile) {

        System.out.println("Tracker URL: " + torrentFile.getTrackerUrl());
        System.out.println("Length: " + torrentFile.getLength());
        System.out.println("Info Hash: " + getHexString(torrentFile.getInfoHash()));
        System.out.println("Piece Length: " + torrentFile.getPieceLength());
        System.out.println("Piece hashes: ");

        torrentFile.getPieceHashes().forEach(
                (pieceHash) -> {
                    System.out.println(pieceHash);
                }
        );
        System.out.println("ByteBuffer content (20 bytes at a time):");
    }

    static void connectToPeer(String ipAddress, String port, TorrentFile torrentFile) throws IOException {
        InetAddress ip = InetAddress.getByName(ipAddress);
        int portValue = Integer.parseInt(port);

        Socket socket = new Socket(ip,portValue);

        //PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        // Setup input stream to receive data from the server
        socket.getOutputStream().write(generateMessageForPeer(torrentFile.getInfoHash()));
        byte[] response = socket.getInputStream().readAllBytes();
        byte[] peerId = Arrays.copyOfRange(response, 48, 68  );
        System.out.println("Peer ID: " + getHexString(peerId));

        // Close the socket
        socket.close();
    }

    static byte[] generateMessageForPeer(byte[] infoHash) {

        byte[] lengthArray = {19};
        byte[] bittorentBytes =  "BitTorrent protocol".getBytes(StandardCharsets.ISO_8859_1);
        byte[] zeroBytes = {0, 0, 0, 0, 0,0 ,0,0};
        byte[] randomPeerId = generateRandomBytes(20);
        int length = lengthArray.length + bittorentBytes.length + zeroBytes.length + randomPeerId.length + infoHash.length;

        byte[] message = new byte[length];

        ByteBuffer byteBuffer =  ByteBuffer.wrap(message);
        byteBuffer.put(lengthArray);
        byteBuffer.put(bittorentBytes);
        byteBuffer.put(zeroBytes);
        byteBuffer.put(infoHash);
        byteBuffer.put(randomPeerId);

        return byteBuffer.array();
    }

    static byte[] generateRandomBytes(int length) {
        byte[] b = new byte[length];
        new Random().nextBytes(b);

        return b;
    }

    static String[] getIpAdressAndPort(String value) {
        return value.split(":");
    }
}
