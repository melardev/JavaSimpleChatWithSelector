package com.melardev.chat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;


public class Server {

    private ServerSocketChannel server;
    private Selector selector;

    public void start() throws Exception {
        selector = Selector.open();
        server = ServerSocketChannel.open();
        InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 3002);
        server.socket().bind(inetSocketAddress);
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started");
        while (selector.select() > 0) {
            /*
             Set selectedKeys = selector.selectedKeys();
            Iterator iter = selectedKeys.iterator();

            while (iter.hasNext()) {

                SelectionKey ky = iter.next();
             */

            for (SelectionKey key : selector.selectedKeys()) {
                selector.selectedKeys().remove(key);
                if (key.isAcceptable()) {
                    onAccept(key);
                } else if (key.isReadable()) {
                    onDataReceived(key);
                }
            }
        }
    }

    private void onDataReceived(SelectionKey key) {
        SocketChannel sourceChannel = (SocketChannel) key.channel();
        // If the client sends more than 1024 then a buffer overflow will occur
        // If you want a more solid implementation to handle that scenario
        // Look at XeytanJAsync application in my Github
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        try {
            while (sourceChannel.read(byteBuffer) > 0) {
            }
            byteBuffer.flip();
            String message = new String(byteBuffer.array(), 0, byteBuffer.limit(), StandardCharsets.UTF_8);
            byteBuffer.clear();

            System.out.println("[Server]: " + message);

            if (message.length() > 0) {
                for (SelectionKey sk : selector.keys()) {
                    Channel targetChannel = sk.channel();
                    if (targetChannel != sourceChannel && targetChannel instanceof SocketChannel) {
                        SocketChannel dest = (SocketChannel) targetChannel;
                        ByteBuffer toWrite = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
                        // .write will ideally send all at once: from position to limit
                        // but if for some reason it can't send it all then keep sending, but why
                        // we are not adjusting the offsets? well because, .write will keep
                        // setting the position as the bytes are sent to we don't need to set it manually
                        // the limit is unchanged obviously
                        while (toWrite.position() != toWrite.limit())
                            dest.write(toWrite);
                    }
                }
            } else { // If length <=0 then socket closed
                sourceChannel.close();
                key.cancel();
            }

        } catch (IOException e) {
            key.cancel();
            if (key.channel() != null) {
                try {
                    key.channel().close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void onAccept(SelectionKey key) throws IOException {
        SocketChannel sc = server.accept();
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_READ);
    }

    public static void main(String[] args) throws Exception {
        try {
            new Server().start();
        } catch (IOException ex) {
            System.exit(1);
        }
    }
}
