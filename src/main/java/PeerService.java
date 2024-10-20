import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PeerService {

    private final Bencode bencode1 = new Bencode(true);

    public void discoverPeers(TorrentFile torrentFile) throws IOException {
        Map<String, String> params = this.createParamsMap(torrentFile);
        URL url = new URL(createURLString(torrentFile.getTrackerUrl(), params));
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("GET");
        byte[] byteArray = null;

        try {
            byteArray = con.getInputStream().readAllBytes();
            Map<?, ?> contentDecoded = bencode1.decode(byteArray, Type.DICTIONARY);
            ByteBuffer peerInfo = (ByteBuffer) contentDecoded.get("peers");

            this.printPeersConnectionInfo(this.getPeerIpAdressAndPort(peerInfo));

        } catch (IOException e) {
            System.out.println(e);
        }

    }

    public Map<String, String> createParamsMap(TorrentFile file) {
        Map<String, String> paramsMap = new HashMap<>();
        String encodedHash = URLEncoder.encode(
                new String(file.getInfoHash(), StandardCharsets.ISO_8859_1),
                StandardCharsets.ISO_8859_1);

        paramsMap.put("info_hash", encodedHash);
        paramsMap.put("peer_id", "my_unique_id_very_fa");
        paramsMap.put("port", "6681");
        paramsMap.put("uploaded", "0");
        paramsMap.put("downloaded", "0");
        paramsMap.put("left", String.valueOf(file.getLength()));
        paramsMap.put("compact", "1");

        return paramsMap;
    }

    public byte[] connectToPeer(String ipAddress, String port, TorrentFile torrentFile) throws IOException {
        InetAddress ip = InetAddress.getByName(ipAddress);
        int portValue = Integer.parseInt(port);
        Socket socket = new Socket(ip, portValue);

        socket.getOutputStream().write(generateMessageForPeer(torrentFile.getInfoHash()));
        byte[] response = socket.getInputStream().readAllBytes();
        byte[] peerId = Arrays.copyOfRange(response, 48, 68);

        socket.close();

        return peerId;
    }

    public byte[] generateMessageForPeer(byte[] infoHash) {

        byte[] lengthArray = {19};
        byte[] bittorentBytes = "BitTorrent protocol".getBytes(StandardCharsets.ISO_8859_1);
        byte[] zeroBytes = {0, 0, 0, 0, 0, 0, 0, 0};
        byte[] randomPeerId = generateRandomBytes(20);
        int length = lengthArray.length + bittorentBytes.length + zeroBytes.length + randomPeerId.length + infoHash.length;

        byte[] message = new byte[length];

        ByteBuffer byteBuffer = ByteBuffer.wrap(message);
        byteBuffer.put(lengthArray);
        byteBuffer.put(bittorentBytes);
        byteBuffer.put(zeroBytes);
        byteBuffer.put(infoHash);
        byteBuffer.put(randomPeerId);

        return byteBuffer.array();
    }

    public byte[] generateRandomBytes(int length) {
        byte[] b = new byte[length];
        new Random().nextBytes(b);

        return b;
    }

    private String createURLString(String trackerURL, Map<String, String> params) {
        StringBuilder paramStringBuilder = new StringBuilder(trackerURL);
        paramStringBuilder.append("/?");

        for (Map.Entry<String, String> entrySet : params.entrySet()) {
            paramStringBuilder.append(entrySet.getKey()).append("=").append(entrySet.getValue()).append("&");
        }

        return paramStringBuilder.toString();
    }

    private List<Peer> getPeerIpAdressAndPort(ByteBuffer peersInfo) throws UnknownHostException {

        List<Peer> peers = new ArrayList<>();

        while (peersInfo.hasRemaining()) {
            byte[] p = new byte[6];

            for (int i = 0; i < 6; i++) {
                byte b = peersInfo.get();
                p[i] = b;
            }

            InetAddress ipAddress = InetAddress.getByAddress(Arrays.copyOfRange(p, 0, 4));
            byte[] port = new byte[]{p[4], p[5]};
            int portValue = (((port[0] & 0xFF) << 8) | (port[1] & 0xFF));

            peers.add(new Peer(ipAddress.toString(), portValue));
        }

        return peers;
    }

    private void printPeersConnectionInfo(List<Peer> peers) {
        peers.forEach(
                peer -> {
                    System.out.println(peer.getIp() + ":" + peer.getPort());
                }
        );
    }
}
