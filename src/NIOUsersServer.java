import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * Created by PC on 23.10.2016 г..
 */
public class NIOUsersServer implements Runnable {
    static String lastMsg=null;
    static List<Message> messages;
    static Map<String,User> users;
    static List<User> usersList;

    @Override
    public void run() {
        try {
            runServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void runServer() throws IOException {
        //users = new HashMap<>();
        usersList = new ArrayList<>();
        messages = new ArrayList<>();
        ServerSocketChannel server = ServerSocketChannel.open();
        server.socket().bind(new InetSocketAddress(8080));
        server.configureBlocking(false);
        Selector selector = Selector.open();
        server.register(selector, SelectionKey.OP_ACCEPT);

        while(true) {
            //gets ready channels
            int readyChannels = selector.selectNow();
            if(readyChannels==0){
                continue;
            }

            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectionKeys.iterator();
            //Iterator<SelectionKey> writeKeyIterator = selectionKeys.iterator();

            while(keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove();

                if(key.isAcceptable()){
                    ServerSocketChannel acceptableServer = (ServerSocketChannel)key.channel();
                    SocketChannel client = server.accept();
                    if(client!=null){
                        System.out.println("Client accepted!");
                        client.configureBlocking(false);
                        SelectionKey selectionKey = client.register(selector, SelectionKey.OP_READ|SelectionKey.OP_WRITE);

                        //read client name
                            ByteBuffer newClientBuffer = ByteBuffer.allocate(20);
                            newClientBuffer.clear();
                            int bytesRead = ((SocketChannel)selectionKey.channel()).read(newClientBuffer);
                            while (bytesRead>0) {
                                bytesRead=((SocketChannel)selectionKey.channel()).read(newClientBuffer);
                                if(bytesRead==-1){
                                    selectionKey.channel().close();
                                    selectionKey.cancel();
                                }
                                newClientBuffer.flip();
                                String username="";
                                while(newClientBuffer.hasRemaining()){
                                    username += ((char)newClientBuffer.get());
                                }
                                User newUser = new User(username, (SocketChannel)selectionKey.channel());
                                usersList.add(newUser);
                            }
                    }
                }
                //collecting client message
                if (key.isReadable()) {
                    read(key);
                }
                /*if(key.isWritable()){
                    write(key);
                }*/
            }

            //sending messages to all active users after collecting
            for (User user : usersList) {
                writeMessages(user.channel,user.username);
            }
            if(!messages.isEmpty()) {
                messages.clear();
            }
        }
    }

    public void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel)key.channel();
        channel.configureBlocking(false);
        ByteBuffer buffer = ByteBuffer.allocate(100);
        buffer.clear();
        int bytesRead = channel.read(buffer);

        while(bytesRead>0){
            String newMessage="";
            System.out.println("Read bytes: "+ bytesRead);
            bytesRead=channel.read(buffer);
            if(bytesRead==-1){
                channel.close();
                key.cancel();
            }
            buffer.flip();
            while(buffer.hasRemaining()){
                newMessage+=(char)buffer.get();
                //System.out.print((char)buffer.get());
            }
            Message message = new Message(newMessage);
            messages.add(message);
            System.out.println(newMessage);
            //lastMsg= newMessage+ "\n";
        }
    }


    public void writeMessages(SocketChannel channel, String username) throws IOException {
        //SocketChannel channel = (SocketChannel)key.channel();
        channel.setOption(StandardSocketOptions.TCP_NODELAY,new Boolean(true));
        channel.configureBlocking(false);
        ByteBuffer buffer = ByteBuffer.allocate(100);
        //buffer.clear();

        for (Message message : messages) {
            buffer.clear();
            String output = username + ": " + message.text;
            buffer.put(output.getBytes());
            buffer.flip();
            channel.write(buffer);
            buffer.clear();
        }
    }

    //
    /*public void write(SelectionKey key) throws IOException {
        if(lastMsg!=null) {
            SocketChannel channel = (SocketChannel)key.channel();
            channel.setOption(StandardSocketOptions.TCP_NODELAY,new Boolean(true));
            channel.configureBlocking(false);
            ByteBuffer buffer = ByteBuffer.allocate(100);
            buffer.clear();
            //String message = "Server: " + getServerMessage();
            String message = "You said: " + lastMsg;
            //String message = "";
            buffer.put(message.getBytes());
            buffer.flip();
            int bytesWritten = channel.write(buffer);
            buffer.clear();
            lastMsg=null;
        }
    }
    */

}
