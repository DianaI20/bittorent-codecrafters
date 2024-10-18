import java.util.ArrayList;
import java.util.List;

public class TorrentFile {

    private String trackerUrl;
    private Long length;
    private byte[] infoHash;
    private List<String> pieceHashes;

    public Long getPieceLength() {
        return pieceLength;
    }

    public void setPieceLength(Long pieceLength) {
        this.pieceLength = pieceLength;
    }

    private Long pieceLength;

    public TorrentFile() {
        pieceHashes = new ArrayList<>();
    }

    public String getTrackerUrl() {
        return trackerUrl;
    }

    public void setTrackerUrl(String trackerUrl) {
        this.trackerUrl = trackerUrl;
    }

    public Long getLength() {
        return length;
    }

    public void setLength(Long length) {
        this.length = Long.valueOf(length);
    }

    public byte[] getInfoHash() {
        return infoHash;
    }

    public void setInfoHash(byte[] infoHash) {
        this.infoHash = infoHash;
    }

    public List<String> getPieceHashes() {
        return pieceHashes;
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
