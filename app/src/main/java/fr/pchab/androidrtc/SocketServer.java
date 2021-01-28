package fr.pchab.androidrtc;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.icu.util.Output;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SocketServer {
    ServerSocket serverSocket = null;
    public int port = 9998;
    private Context mcontext;
    private Intent mintent;
    public Socket socket;

    public SocketServer(int _port, Context _context, Intent _intent){
        try {
            // InetAddress addr = InetAddress.getLocalHost();
            // System.out.println("local host:"+addr);
            System.out.println("New SocketServer");
            mcontext = _context;
            mintent = _intent;
            port = _port;
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void startService(){
        try {
            InetAddress addr = InetAddress.getLocalHost();
            System.out.println("local host:"+addr);
            socket = null;
            System.out.println("waiting...");
            while(true){
                socket = serverSocket.accept();
                System.out.println("connect to"+socket.getInetAddress()+":"+socket.getLocalPort());
                new ConnectThread(socket).start();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            System.out.println("IOException");
            e.printStackTrace();
        }
    }

    private static String REGEX_CHINESE = "[\u4e00-\u9fa5]";// 中文正则
    class ConnectThread extends Thread{
        Socket socket = null;
        private String lastPackageName;
        private List<String> textsToClickForOpenInGivenApp;
        private static final String MY_APP_NAME = "AndroidRTC";

        public ConnectThread(Socket socket){
            super();
            this.socket = socket;
            textsToClickForOpenInGivenApp = new ArrayList<>();
            textsToClickForOpenInGivenApp.add("其他应用打开");
            textsToClickForOpenInGivenApp.add(MY_APP_NAME);
            textsToClickForOpenInGivenApp.add("仅一次");
        }

        public void SendFile(Socket socket, File fs) throws IOException, InterruptedException {
            FileChannel channel = null;
            FileInputStream fis = new FileInputStream(fs);

            ByteBuffer buffer = null;
            try {
                channel = fis.getChannel();
                buffer = ByteBuffer.allocate((int) channel.size());
                while ((channel.read(buffer)) > 0) {

                }
            } catch (IOException e) {

            }
            byte[] data = buffer.array();
            int length = (int)fs.length();

            OutputStream os = socket.getOutputStream();
            System.out.println(fs.getName());
            os.write((String.valueOf(length) + "," + fs.getName() + '\n').getBytes());
            os.write(data);
            os.flush();

//            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
//
//            System.out.println((int)fs.length());
//            // OutputStream os = socket.getOutputStream();
//
//            int length = (int)fs.length();
//            byte[] lengthBytes = new byte[4];
//            lengthBytes[0] = (byte) (length & 0xff);
//            lengthBytes[1] = (byte) ((length >> 8) & 0xff);
//            lengthBytes[2] = (byte) ((length >> 16) & 0xff);
//            lengthBytes[3] = (byte) ((length >> 24) & 0xff);
//
//            // String len = String.format("%7d", data.length);
//            // System.out.println(len + "\n");
//            // os.write((len + "\n").getBytes(StandardCharsets.US_ASCII));
//            // dos.flush();
//            // dos.write((fs.getName() + "\n").getBytes());
//            // dos.flush();
//
//            os.write((String.valueOf(length) + " " + fs.getName() + "\n").getBytes("utf-8"));
//            os.flush();
//            // Thread.sleep(100);
//            // os.write((String.valueOf(length) + " " + fs.getName()).getBytes("utf-8"));
//
//            // os.write(data);
//            // os.flush();
///*
//            byte[] buffer = new byte[8192];
//            int count;
//            int tcnt = 0;
//            InputStream in = new FileInputStream(fs);
//            while ((count = in.read(buffer)) > 0) {
//                os.write(buffer, 0, count);
//                Thread.sleep(200);
//                os.write(buffer, 0, count);
//                tcnt += count;
//                System.out.println(tcnt);
//            }
//*/

        }

        @Override
        public void run(){
            try {
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(dis));
                while(true){
                    String msgRecv = bufferedReader.readLine();
                    System.out.println("msg from client:"+msgRecv);
                    if (msgRecv.equals("image")) {
                        String result = AccService.instance.autoSaveImg();
                        Pattern pat = Pattern.compile(REGEX_CHINESE);
                        Matcher mat = pat.matcher(result);
                        String temp = mat.replaceAll("");
                        temp = temp.replaceAll(" ", "");
                        Log.i("res", "Toast result " + temp);
                        Utility.runInMain(AccService.instance, ()->Toast.makeText(AccService.instance, result == null? "保存失败": "【保存结果】 " + result, Toast.LENGTH_LONG).show());

                        File file = new File("`/storage/emulated/0`"+ temp.substring(7) + "/");
                        List<File> fs = new ArrayList<File>();
                        for (File f : file.listFiles()) {
                            fs.add(f);
                        }
                        if (fs != null && fs.size() > 0) {
                            Collections.sort(fs, new Comparator<File>() {
                                public int compare(File file, File newFile) {
                                    if (file.lastModified() < newFile.lastModified()) {
                                        return 1;
                                    } else if (file.lastModified() == newFile.lastModified()) {
                                        return 0;
                                    } else {
                                        return -1;
                                    }
                                }
                            });
                            System.out.println(fs.get(0));
                            SendFile(socket, fs.get(0));
                        }
                    }
                    else if (msgRecv.equals("file")) {
                        // Runnable run = () -> {
                            lastPackageName = Utility.getCurrentPackageName(AccService.instance);
                            boolean result = Utility.clickByTextList(AccService.instance, textsToClickForOpenInGivenApp);
                            Utility.runInMain(AccService.instance, ()-> Toast.makeText(AccService.instance, result? "成功": "失败", Toast.LENGTH_SHORT).show());
                            System.out.println(result);
                            if(result){
                                try {
                                    Thread.sleep(3000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if (RtcActivity.instance.getIntent().getData() != null) {
                                    String filePath = RtcActivity.instance.getIntent().getData().toString();
                                    int index = filePath.indexOf("/Android");
                                    String s = "/storage/emulated/0" + filePath.substring(index);
                                    Pattern p = Pattern.compile("%..%..%..");
                                    Matcher m = p.matcher(s);
                                    String res = "";
                                    int last = 0;
                                    while (m.find()) {
                                        System.out.println(m.start());
                                        System.out.println(m.end());
                                        System.out.println(m.group());
                                        if (m.end() - m.start() == 9) {
                                            res = res + s.substring(last, m.start());
                                            last = m.end();
                                            String temp = m.group().replace("%", "");
                                            temp = Integer.toBinaryString(Integer.parseInt(temp, 16));
                                            String t = temp.substring(4, 8) + temp.substring(10, 16) + temp.substring(18, 24);
                                            String f = "";
                                            f += Integer.toHexString(Integer.parseInt(t.substring(0, 4), 2));
                                            f += Integer.toHexString(Integer.parseInt(t.substring(4, 8), 2));
                                            f += Integer.toHexString(Integer.parseInt(t.substring(8, 12), 2));
                                            f += Integer.toHexString(Integer.parseInt(t.substring(12, 16), 2));
                                            System.out.println((char) Integer.valueOf(f, 16).intValue());
                                            res = res + (char) Integer.valueOf(f, 16).intValue();
                                            System.out.println(res);
                                            // String ans = s.replace(s.substring(m.start(), m.end()), (char)Integer.valueOf(f, 16).intValue());
                                            // System.out.println(ans);
                                        }
                                    }
                                    res = res + s.substring(last);
                                    System.out.println(res);
                                    File file = new File(res);
                                    System.out.println("bringAppToFront");
                                    try {
                                        SendFile(socket, file);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                Utility.bringAppToFrontByPackageName(AccService.instance, lastPackageName);
                            }
                        // };
                        // new Thread(run).start();
                    }
                }
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}