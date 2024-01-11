package src.server;

import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.io.*;
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class ServerMain extends JFrame {

    public static int port = 9001;
    ServerSocket serverSocket = null;
    Socket socket = null;
    Thread thread;
	static Map<String, Socket> userSocketMap = new HashMap<>();

    // 界面显示客户端信息
	static String[] columnNames = { "用户名", "上线时间", "ip", "状态" };
	static Object[][] rowData = {};
	static DefaultTableModel defaultModel = new DefaultTableModel(rowData, columnNames);
	JTable table = new JTable(defaultModel);
	JScrollPane JSPanel = new JScrollPane(table);
	JPanel SPanel = new JPanel();
	

    public static void main(String[] args) {
        new ServerMain();
    }
    
    public ServerMain(){
		//设置表格宽度
		table.getColumnModel().getColumn(3).setPreferredWidth(50);
		table.getColumnModel().getColumn(1).setPreferredWidth(150); 

        //设置窗口
        this.setTitle("CL服务器");	
		this.setBounds(50, 150, 300, 600);
		this.setResizable(false); 
		SPanel.add(JSPanel);
		this.add(SPanel, BorderLayout.SOUTH);
		this.pack();
		this.addWindowListener(new CloseSrever());
		this.setVisible(true);

		//启动服务器，开始接收客户端连接
        acceptClient();
    }

	// 监听类 处理屏幕关闭
	class CloseSrever extends WindowAdapter {
		public void windowClosing(WindowEvent e) {
		try {
			// 通知在线的各客户端
			for (int i = 0; i < defaultModel.getRowCount(); i++) {
				if (defaultModel.getValueAt(i, 3).equals("在线")) {
					Socket socket = userSocketMap.get(defaultModel.getValueAt(i, 0));
					DataOutputStream outputToClient = new DataOutputStream(socket.getOutputStream());
					outputToClient.writeUTF("exit");
					outputToClient.flush();
				}					
			}
		
			serverSocket.close();
			System.exit(0);          
		} catch (IOException e2) {
			e2.printStackTrace();
		}
			
		}
	}
    
	//启动服务器，开始接收客户端连接
	public void acceptClient() {
        boolean goon = true;
		//打开端口
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e1) {
			JOptionPane.showMessageDialog(null, "端口已被使用,请重新运行", "启动失败", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
		
        //开启监听
		try {
			while (goon) {
				socket = serverSocket.accept();
                			
				if(socket != null){			
				HandleClient hc = new HandleClient(socket); 		
				thread = new Thread(hc);
				thread.start();
				} 			
			}
		} catch (IOException e) {
			e.printStackTrace();
			goon = false;
		} 
	}
}

	
