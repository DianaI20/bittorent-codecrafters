import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TorrentFileDecoder {

    private final Bencode bencode = new Bencode(false);
    private final Bencode bencode1 = new Bencode(true);

    public TorrentFile decodeTorrentFile(String fileName) throws IOException {
        byte[] input = Files.readAllBytes(Paths.get(fileName));
        Map<String, Object> decoded = bencode.decode(input, Type.DICTIONARY);
        Map<?, ?> info = this.getInfoDictionary(input);
        TorrentFile torrentFile = new TorrentFile();

        torrentFile.setTrackerUrl((String) decoded.get("announce"))
                .setLength((Long) info.get("length"))
                .setInfoHash(getInfoHash(input))
                .setPieceLength((Long) info.get("piece length"))
                .setPieceHashes(getPieceHashesList(input));

        return torrentFile;
    }

    public Object decodeBencode(String bencodedString) {

        if (Character.isDigit(bencodedString.charAt(0))) {
            return this.bencode.decode(bencodedString.getBytes(), Type.STRING);
        }

        if (bencodedString.charAt(0) == 'i' && bencodedString.charAt(bencodedString.length() - 1) == 'e') {
            return this.bencode.decode(bencodedString.getBytes(), Type.NUMBER);

        }

        if (bencodedString.charAt(0) == 'l' && bencodedString.charAt(bencodedString.length() - 1) == 'e') {
            return this.bencode.decode(bencodedString.getBytes(), Type.LIST);
        }

        if (bencodedString.charAt(0) == 'd') {
            return this.bencode.decode(bencodedString.getBytes(), Type.DICTIONARY);
        }

        return "Invalid decode string";
    }

    private byte[] getInfoHash(byte[] input) {
        Map<?, ?> infoEncoded = (Map<?, ?>) this.bencode1.decode(input, Type.DICTIONARY).get("info");
        byte[] info = this.bencode1.encode(infoEncoded);

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        return md.digest(info);
    }

    private List<String> getPieceHashesList(byte[] input) {
        Map<?, ?> decoded = bencode1.decode(input, Type.DICTIONARY);
        Map<?, ?> info = (Map<String, ?>) decoded.get("info");
        List<String> pieceHashes = new ArrayList<>();
        ByteBuffer piecesString = (ByteBuffer) info.get("pieces");
        StringBuilder stringBuilder;

        while (piecesString.hasRemaining()) {
            int chunkSize = Math.min(20, piecesString.remaining());
            stringBuilder = new StringBuilder();
            for (int i = 0; i < chunkSize; i++) {
                byte b = piecesString.get();
                stringBuilder.append(String.format("%02x", b));
            }
            pieceHashes.add(stringBuilder.toString());
        }

        return pieceHashes;
    }

    private Map<?, ?> getInfoDictionary(byte[] input) {
        Map<String, Object> decoded = bencode.decode(input, Type.DICTIONARY);

        return (Map<?, ?>) decoded.get("info");

    }
}
