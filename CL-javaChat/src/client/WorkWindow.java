package src.client;

import java.io.*;
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

//界面
public class WorkWindow extends JFrame{
    String username;
    DataOutputStream outputToServer = ClientMain.outputToServer;

    static JTable table = new JTable(ClientMain.defaultModel);
    static JScrollPane JSPanel = new JScrollPane(table);
    static JPanel SPanel = new JPanel();

    public WorkWindow(String name){
        username = name;
        showFriends();
    }

    public void showFriends(){       
    //按钮面板
    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

    JButton chatButton = new JButton("好友聊天");
    chatButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            try {
                startChat();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    });
    buttonPanel.add(chatButton);

    JButton deleteButton = new JButton("删除好友"); 
    deleteButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            try {
                deleteFriend();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    });
    buttonPanel.add(deleteButton);

    JButton addButton = new JButton("添加好友"); 
    addButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {                                
            try {
                addFriend();
            } catch (IOException e1) {
                e1.printStackTrace();
            }                                
        }
    });
    buttonPanel.add(addButton);
    
    //好友列表   
    this.setTitle(username+"的好友列表");        
    this.setBounds(50, 150, 300, 700);
    this.setResizable(false); 
    JSPanel.setPreferredSize(new Dimension(300, 700));
    SPanel.add(JSPanel);       
    this.add(SPanel, BorderLayout.CENTER);
    this.add(buttonPanel, BorderLayout.SOUTH);
    this.pack();
    this.addWindowListener(new CloseClient());
    this.setVisible(true);
    
}

    //发起聊天请求
    private void startChat() throws IOException{
       
        String friendName = JOptionPane.showInputDialog("请输入要通讯的好友名称:");
        if(friendName == null){
            return;
        }
        if(!isFriend(friendName)){
            JOptionPane.showMessageDialog(null, "只能和好友聊天", "提示", JOptionPane.INFORMATION_MESSAGE);
        }
        else{
            outputToServer.writeUTF("chat-request");
            outputToServer.flush();
            outputToServer.writeUTF(friendName);
            outputToServer.flush();
        }
    }

    //发起删除请求
    private void deleteFriend() throws IOException{
        
        String friendName = JOptionPane.showInputDialog("请输入要删除的好友名称:");
        if(friendName == null){
            return;
        }
        if(!isFriend(friendName)){
            JOptionPane.showMessageDialog(null, "好友名不存在", "提示", JOptionPane.INFORMATION_MESSAGE);
        }
        else{
            outputToServer.writeUTF("delete-request");
            outputToServer.flush();
            outputToServer.writeUTF(friendName);
            outputToServer.flush(); 
        }              	              
    }

    public boolean isFriend(String name){
        DefaultTableModel dfm = ClientMain.defaultModel;
        for(int i=0; i<dfm.getRowCount(); i++){
            if(dfm.getValueAt(i, 0).equals(name)){
                return true;
            }             
        }
        return false;
    }

    //发起添加请求
    public void addFriend() throws IOException{
        
        String friendName = JOptionPane.showInputDialog("请输入要添加的好友名称:");
        if(friendName == null){
            return;
        }
        if(friendName.equals(username)){
            JOptionPane.showMessageDialog(null, "不能添加你自己", "提示", JOptionPane.INFORMATION_MESSAGE);
        }
        else if(isFriend(friendName)){
            JOptionPane.showMessageDialog(null, friendName+"已经是你的好友了", "提示", JOptionPane.INFORMATION_MESSAGE);
        }
        else {
            outputToServer.writeUTF("add-request");
            outputToServer.flush();
            outputToServer.writeUTF(friendName);
            outputToServer.flush();                    
        }           
    }

    //发起退出请求
    class CloseClient extends WindowAdapter {
        public void windowClosing(WindowEvent e) {
            try {									
                outputToServer.writeUTF("exit-request"); 
                outputToServer.flush();

                ClientMain.receiveRequestThread.interrupt();
                ClientMain.inputFromServer.close();
                outputToServer.close();
                ClientMain.socket.close();
                System.exit(0);

            } catch (IOException e1) {
                e1.printStackTrace();
            }			
        }
    }
        
}
