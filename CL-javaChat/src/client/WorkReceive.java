package src.client;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;

//登录成功后，循环接收来自服务端的各种类型的请求
public class WorkReceive implements Runnable {

    DefaultTableModel defaultModel = ClientMain.defaultModel;
    DataInputStream inputFromServer = ClientMain.inputFromServer;

    Map<String, WorkChat> workChatMap = new HashMap<>();//用于查找WorkChat类

    public void run() {
        try {
            System.out.println("接收线程已开启");
            while (true) {                  

                String request = inputFromServer.readUTF();
                System.out.println("接收到"+request+"请求");
                switch (request) {
                    //某个好友向你发起了聊天
                    case "chat":                           
                        String friendName = inputFromServer.readUTF();
                        JOptionPane.showMessageDialog(null, friendName+"向您发起了聊天", "提示", JOptionPane.INFORMATION_MESSAGE);
                        
                        //第一次和对方开启聊天，启动线程类。若是再次开启聊天，只调用方法
                        WorkChat wc = workChatMap.get(friendName);
                        if (wc == null) {
                            wc = new WorkChat(friendName);
                            workChatMap.put(friendName, wc);
                            Thread thread = new Thread(wc);
                            thread.start();
                        } else {
                            if (!wc.isWindowOpen) { // 检查窗口是否已经打开
                                wc.createWindow(); // 如果窗口未打开，则创建窗口
                            }
                        }
                        break;
                    
                    //某个用户添加了你
                    case "add":
                        String addName = inputFromServer.readUTF();
                        Object[] row = {addName, "在线"};
                        defaultModel.addRow(row);
                        JOptionPane.showMessageDialog(null, addName+"添加了你为好友", "提示", JOptionPane.INFORMATION_MESSAGE);
                        break;

                    //某个好友删除了你
                    case "delete":
                        String deleteName = inputFromServer.readUTF();
                        JOptionPane.showMessageDialog(null, deleteName+"把你拉黑了", "提示", JOptionPane.INFORMATION_MESSAGE);
                        for(int i=0;i<defaultModel.getRowCount();i++){
                            if(defaultModel.getValueAt(i, 0).equals(deleteName)){
                                defaultModel.removeRow(i);
                                break;
                            }
                        }
                        break;

                    //你向某个好友发起的聊天成功了
                    case "chat-success":
                        friendName = inputFromServer.readUTF();
                        
                        wc = workChatMap.get(friendName);
                        if (wc == null) {
                            System.out.println("开启了与好友"+friendName+"的聊天线程");
                            wc = new WorkChat(friendName);
                            workChatMap.put(friendName, wc);
                            Thread thread = new Thread(wc);
                            thread.start();
                        } else {
                            if (!wc.isWindowOpen) {
                                System.out.println("开启了与好友"+friendName+"的聊天窗口"); 
                                wc.createWindow(); 
                            }
                        }
                        break;

                    //你向某个好友发起的聊天失败了
                    case "chat-fail":
                        JOptionPane.showMessageDialog(null, "对方不在线或好友不存在", "提示", JOptionPane.INFORMATION_MESSAGE);
                        break;

                    //删除好友成功了
                    case "delete-success":
                        deleteName = inputFromServer.readUTF();      
                        for (int i = 0; i < defaultModel.getRowCount(); i++) {
                            if (deleteName.equals((String) defaultModel.getValueAt(i, 0))) {
                                defaultModel.removeRow(i);
                                JOptionPane.showMessageDialog(null, "删除成功", "提示", JOptionPane.INFORMATION_MESSAGE);
                                break;     
                            }
                        }           
                        break;

                    //删除好友失败了
                    case "delete-fail":
                        JOptionPane.showMessageDialog(null, "用户名不存在！", "提示", JOptionPane.INFORMATION_MESSAGE);
                        break;

                    //添加好友成功了
                    case "add-success": 
                        addName = inputFromServer.readUTF();
                        String state = inputFromServer.readUTF();                         
                        JOptionPane.showMessageDialog(null, "添加成功", "提示", JOptionPane.INFORMATION_MESSAGE);
                        Object[] row1 = {addName, state};
                        defaultModel.addRow(row1); 
                        break;

                    //添加好友失败了
                    case "add-fail":
                        JOptionPane.showMessageDialog(null, "用户名不存在！", "提示", JOptionPane.INFORMATION_MESSAGE);
                        break;

                    //服务器突然关闭
                    case "exit":
                        JOptionPane.showMessageDialog(null, "服务器已关闭", "提示", JOptionPane.INFORMATION_MESSAGE);
                        System.exit(0);
                        break;
                    
                    //某个好友发来的消息
                    case "message":
                        String senderName = inputFromServer.readUTF();                                                                             
                        String str = inputFromServer.readUTF();

                        WorkChat workChat = workChatMap.get(senderName);
                        workChat.showMessage(str);                   
                        break;

                    //某个好友发来的文件
                    case "file":
                        senderName = inputFromServer.readUTF();
                        String fileName = inputFromServer.readUTF(); // 获取文件名
                        int fileLength = inputFromServer.readInt(); // 获取文件长度
                        byte[] fileBytes = new byte[fileLength];
                        inputFromServer.readFully(fileBytes, 0, fileLength); // 获取文件内容

                        workChat = workChatMap.get(senderName);
                        workChat.handleFile(fileName, fileLength, fileBytes);
                        break;

                    
                    //好友上线下线通知
                    case "friend-online":
                        friendName = inputFromServer.readUTF();
                        state = inputFromServer.readUTF();

                        String online = "下线";
                        if(state.equals("在线")) online = "上线";
                        
                        //friendName有可能不是好友，需要判断
                        for (int j = 0; j < defaultModel.getRowCount(); j++) {
                            if (friendName.equals((String) defaultModel.getValueAt(j, 0))) {
                                JOptionPane.showMessageDialog(null, "你的好友"+friendName+"已"+online, "提示", JOptionPane.INFORMATION_MESSAGE);
                                defaultModel.setValueAt(state, j, 1);
                                break;
                            }					
                        }
                        break;                            

                    default:
                        System.out.println("请求错误");
                        break;
                }                     
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}