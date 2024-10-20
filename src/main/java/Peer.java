public class Peer {

    private String ip;
    private int port;

    public Peer(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public Peer setIp(String ip) {
        this.ip = ip;

        return this;
    }

    public int getPort() {
        return port;
    }

    public Peer setPort(int port) {
        this.port = port;

        return this;
    }
}
