package src.client;

import java.net.*;
import java.io.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class ClientMain {
    static Socket socket = null;//连接服务器
    static DataInputStream inputFromServer = null;
    static DataOutputStream outputToServer = null;
    static Thread receiveRequestThread;

    //好友列表
    static String[] columnNames = { "好友昵称", "在线状态" };
    static Object[][] rowData = {};
    static DefaultTableModel defaultModel = new DefaultTableModel(rowData, columnNames);

    public static void main(String[] args){
        new ClientMain();
    }

    public ClientMain(){
        String text[] = inputText();//登录界面，获取输入信息

        linkServer(text[0], text[1], Integer.parseInt(text[2]), text[3]);// 连接服务器

        new WorkWindow(text[0]);//显示好友列表

        receiveRequestThread = new Thread(new WorkReceive());//创建接收请求的线程
        receiveRequestThread.start();       
    }
     
    //登录界面
    public String[] inputText(){
        //初始化文本输入
        JTextField textName = new JTextField(10);
        textName.setText("张三");
        JPasswordField textPassword = new JPasswordField(10);
        textPassword.setText("aS1234567");
        JTextField textAddress = new JTextField(16);
        textAddress.setText("127.0.0.1");
        JTextField textPort = new JTextField(5);
        textPort.setText("9001");

        //接收输入
        String text[] = new String[4];
        boolean goon = true;
        while(goon){

            //登录窗口
            int a = JOptionPane.showConfirmDialog(null,
                new Object[] { "请输入你的昵称(新用户将自动注册)", textName, "请输入你的登录密码", textPassword, 
                "请输入你要连接的地址: ", textAddress, "请输入端口号: ", textPort},
                "登录",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if(a == JOptionPane.OK_OPTION){ //点击确认
                text[0] = textName.getText();
                text[1] = textAddress.getText().trim();
                text[2] = textPort.getText().trim();
                text[3] = new String(textPassword.getPassword());

                //检验输入
                if (text[0].length() < 1 || text[0].length() > 10) {
                    JOptionPane.showMessageDialog(null, "昵称长度应为1-10个字符", "输入错误", JOptionPane.ERROR_MESSAGE);
                    continue; 
                }
                if(!text[3].matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{8,}$")) {
                    JOptionPane.showMessageDialog(null, "密码必须包含至少一个小写字母、一个大写字母、一个数字，并且长度至少为8位", "输入错误", JOptionPane.ERROR_MESSAGE);
                    continue; 
                }
                if(Integer.parseInt(text[2]) > 65535 || Integer.parseInt(text[2]) < 1024){
                    JOptionPane.showMessageDialog(null, "端口号不可用", "输入错误", JOptionPane.ERROR_MESSAGE);
                    continue;
                }
                if(!text[1].matches("(\\d{1,3}\\.){3}\\d{1,3}")) {
                    JOptionPane.showMessageDialog(null, "请输入正确的IP地址", "输入错误", JOptionPane.ERROR_MESSAGE);
                    continue; 
                }    
                goon = false;

            }else{
                System.exit(0);
            }        
        }
        return text;
    }

    // 连接服务器
	public void linkServer(String name, String ip, int port, String password) {
		try {
            socket = new Socket(ip, port);
			inputFromServer = new DataInputStream(socket.getInputStream());
            outputToServer = new DataOutputStream(socket.getOutputStream());
            outputToServer.writeUTF(name);
            outputToServer.writeUTF(password);
            outputToServer.flush();
            
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "服务器不在线或端口错误", "连接失败", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}

        try {
            String str = inputFromServer.readUTF();
            if(str.equals("登录失败")){//服务端检测到密码错误
            //退出程序              
            JOptionPane.showMessageDialog(null, "密码错误", "警告", JOptionPane.ERROR_MESSAGE);
            inputFromServer.close();
            outputToServer.close();
            socket.close();          
            System.exit(0);                   
            } 
            
            //接收好友列表
            int num = inputFromServer.readInt();
            String friendInfo[] = new String[num];
            for(int i=0; i<num; i++){
                friendInfo[i] = inputFromServer.readUTF();
                String state = inputFromServer.readUTF();
                Object[] row = {friendInfo[i], state};
                defaultModel.addRow(row);
            }

        } catch (Exception e) {
                e.printStackTrace();
            }
	}

}
