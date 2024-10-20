import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TorrentFile {

    private String trackerUrl;
    private Long length;
    private byte[] infoHash;
    private List<String> pieceHashes;
    private Map<?, ?> info;
    private Long pieceLength;

    public TorrentFile() {
        pieceHashes = new ArrayList<>();
    }

    public Long getPieceLength() {
        return pieceLength;
    }

    public TorrentFile setPieceLength(Long pieceLength) {
        this.pieceLength = pieceLength;

        return this;
    }

    public String getTrackerUrl() {
        return trackerUrl;
    }

    public TorrentFile setTrackerUrl(String trackerUrl) {
        this.trackerUrl = trackerUrl;

        return this;
    }

    public Long getLength() {
        return length;
    }

    public TorrentFile setLength(Long length) {
        this.length = Long.valueOf(length);

        return this;
    }

    public byte[] getInfoHash() {
        return infoHash;
    }

    public TorrentFile setInfoHash(byte[] infoHash) {
        this.infoHash = infoHash;

        return this;
    }

    public List<String> getPieceHashes() {
        return pieceHashes;
    }

    public TorrentFile setPieceHashes(List<String> pieceHashes) {
        this.pieceHashes = pieceHashes;

        return this;
    }

    public Map<?, ?> getInfo() {
        return info;
    }

    public TorrentFile setInfo(Map<?, ?> info) {
        this.info = info;

        return this;
    }
    public TorrentFile addPieceHash(String pieceHash) {
        this.pieceHashes.add(pieceHash);

        return this;
    }

    public TorrentFile removePieceHash(String pieceHash) {
        this.pieceHashes.removeIf(
                el -> el.equals(pieceHash)
        );

        return this;
    }
}
