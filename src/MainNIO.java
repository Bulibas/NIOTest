/**
 * Created by PC on 18.10.2016 г..
 */
public class MainNIO {
    public static void main(String[] args) {
        NIOUsersServer server = new NIOUsersServer();
        Thread thread = new Thread(server);
        thread.start();
    }
}

