package com.melardev.chat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Client {

    private Selector selector;

    public void start() throws IOException {
        selector = Selector.open();
        InetSocketAddress isa = new InetSocketAddress(InetAddress.getLoopbackAddress(), 3002);
        SocketChannel socketChannel = SocketChannel.open(isa);
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);

        new Thread(new NetRunnable()).start();

        Scanner scan = new Scanner(System.in);
        while (scan.hasNextLine()) {
            System.out.println("SimpleSelectChat> ");
            String message = scan.nextLine();
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            socketChannel.write(ByteBuffer.wrap(messageBytes));
        }
    }

    private class NetRunnable implements Runnable {
        public void run() {
            SocketChannel socketChannel = null;
            try {
                while (selector.select() > 0) {
                    for (SelectionKey selectionKey : selector.selectedKeys()) {
                        selector.selectedKeys().remove(selectionKey);
                        if (selectionKey.isReadable()) {
                            socketChannel = (SocketChannel) selectionKey.channel();
                            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

                            while (socketChannel.read(byteBuffer) > 0) {
                            }

                            byteBuffer.flip();
                            String message = new String(byteBuffer.array(), 0, byteBuffer.limit());
                            byteBuffer.clear();
                            System.out.println("[Client]: " + message);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (socketChannel != null) {
                    try {
                        socketChannel.close();
                        System.exit(1);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new Client().start();
    }
}
