import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.google.gson.Gson;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Main {
    private static final Gson gson = new Gson();
    private static final Bencode bencode = new Bencode(false);
    private static final Bencode bencode1 = new Bencode(true);
    private static final TorrentFileDecoder torrentFileDecoder = new TorrentFileDecoder();
    private static final PeerService peerService = new PeerService();

    public static void main(String[] args) throws Exception {
        String command = args[0];
        TorrentFile parsedTorrentFile;

        switch (command) {
            case "decode":
                String bencodedValue = args[1];
                Object decoded;
                try {
                    decoded = torrentFileDecoder.decodeBencode(bencodedValue);
                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());
                    return;
                }
                System.out.println(gson.toJson(decoded));
                break;
            case "info":
                parsedTorrentFile = torrentFileDecoder.decodeTorrentFile(args[1]);
                printTorrentFileInfo(parsedTorrentFile);
                break;
            case "peers":
                parsedTorrentFile = torrentFileDecoder.decodeTorrentFile(args[1]);
                peerService.discoverPeers(parsedTorrentFile);
                break;
            case "handshake":
                parsedTorrentFile = torrentFileDecoder.decodeTorrentFile(args[1]);
                String[] peerInfo = getIpAdressAndPort(args[2]);
                byte[] peerId = peerService.connectToPeer(peerInfo[0], peerInfo[1], parsedTorrentFile);
                System.out.println("Peer ID: " + getHexString(peerId));
                break;
            default:
                System.out.println("Unknown command: " + command);
                break;
        }
    }

    static String getHexString(byte[] bytes) {

        StringBuilder hexString = new StringBuilder();

        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }

        return hexString.toString();
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
    }

    public static String[] getIpAdressAndPort(String value) {
        return value.split(":");
    }
}
