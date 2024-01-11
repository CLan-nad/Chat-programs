package src.server;

import java.net.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.io.*;
import javax.swing.table.DefaultTableModel;

//本线程负责处理单个客户端
public class HandleClient implements Runnable {
    private Socket socket = null;
    private DataInputStream inputFromClient;
    private DataOutputStream outputToClient;
    private boolean goon = true;		
    private String name = "", password = "";
    DefaultTableModel defaultModel = ServerMain.defaultModel; 
    Map<String, Socket> userSocketMap = ServerMain.userSocketMap;

    HandleClient(Socket socket) throws IOException {
        this.socket = socket;
        inputFromClient = new DataInputStream(socket.getInputStream());
        outputToClient = new DataOutputStream(socket.getOutputStream());
    }

    public void run() {						          
        try {                           
            if(!checkUser()) return; //检验用户名和密码

            sendFriendInfo(name);//查询出其好友列表并发送

            saveInfo(name);//保存客户端，通知其他在线用户这人已上线

            //循环处理请求					
            while(goon){				
                String select = inputFromClient.readUTF();
                System.out.println("接收到请求"+select);
                switch (select){
                    case "chat-request":
                        handleChat(name);
                        break;

                    case "add-request":
                        handleAdd(name);
                        break;
                    
                    case "delete-request":
                        handledelete(name);
                        break;

                    case "exit-request":
                        handleOffline(name);
                        break;

                    case "send-message":
                        String addrName = inputFromClient.readUTF();
                        handleSendMessage(name, addrName);
                        break;
                    
                    case "send-file":
                        addrName = inputFromClient.readUTF();
                        handleSendFile(name, addrName);

                    default:
                        System.out.println("请求类型错误");
                        break;                   
                }
            }

        } catch (IOException e) {
            goon = false;
            e.printStackTrace();
        }													
    }

    //检验用户名和密码
    public boolean checkUser() throws IOException {       
        name = inputFromClient.readUTF();
        password = inputFromClient.readUTF();

        //在文本中检索对应用户名和密码
        File file = new File("src/server/user.txt");
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] user_info = line.split(",");

                if (user_info[0].equals(name)) {
                    if (user_info[1].equals(password)) {
                        System.out.println(name+"已登录");
                        outputToClient.writeUTF("登录成功");
                        outputToClient.flush();
                        return true;                           
                    } else {
                        System.out.println(name+"密码错误");
                        outputToClient.writeUTF("登录失败");
                        outputToClient.flush();
                        return false;                  
                    }
                } 
            }
            reader.close();
        } 

        //若文本没有对应的用户名，则为新用户
        FileWriter writer = new FileWriter(file, true);
        writer.write(name + "," + password + "\n");//保存进文件
        UpdateDB.Adduser(name);//保存进数据库
        System.out.println(name+"已注册");
        outputToClient.writeUTF("注册成功");
        outputToClient.flush();
        writer.close();
        return true;
        
    }

    //查询出其好友列表并发送
    public void sendFriendInfo(String name) throws IOException{
        //查询好友关系表
        String[] friendInfo = QueryDB.Queryfriend(name);
        int length = 0;
        for(String i:friendInfo){
            if(i!=null) length++;
        }
        outputToClient.writeInt(length);
        for(int i=0; i<5; i++){
            if(friendInfo[i]!=null){				
                //判断是否在线
                String state = "离线";
                for (int j = 0; j < defaultModel.getRowCount(); j++) {
                    if (friendInfo[i].equals((String) defaultModel.getValueAt(j, 0))&&defaultModel.getValueAt(j, 3).equals("在线")) {
                        state = "在线";
                        break;
                    }					
                }
                outputToClient.writeUTF(friendInfo[i]);
                outputToClient.writeUTF(state);
                outputToClient.flush();
                System.out.println("已发送:"+friendInfo[i]);
            }
        }

    }

    //处理删除好友请求
    public void handledelete(String userName){
        try {
            String deleteName = inputFromClient.readUTF();
            String response = "delete-fail";

            if(QueryDB.Finduser(deleteName)){//判断是否存在该用户
                if(UpdateDB.Deletefriend(userName, deleteName)&&UpdateDB.Deletefriend(deleteName, userName))//为双方删除好友
                {
                    response = "delete-success";					
                }
                else{
                    response = "delete-fail";
                }
            }else {
                response = "delete-fail";
            }
            outputToClient.writeUTF(response);
            if(response.equals("delete-success"))
                outputToClient.writeUTF(deleteName);
            outputToClient.flush();
            System.out.println("已响应请求客户端");

            //如果被拉黑的用户在线，发送通知
            for(int i=0;i<defaultModel.getRowCount();i++){
                if(defaultModel.getValueAt(i, 0).equals(deleteName)&&defaultModel.getValueAt(i, 3).equals("在线")){
                    Socket addrSocket = userSocketMap.get(deleteName);
                    DataOutputStream outputToaddr = new DataOutputStream(addrSocket.getOutputStream());				
                    outputToaddr.writeUTF("delete");
                    outputToaddr.writeUTF(userName);
                    outputToaddr.flush();
                    System.out.println("已向目标客户端发送请求");	
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    //处理添加好友请求
    public void handleAdd(String userName){
    try {          
        String addName = inputFromClient.readUTF();
        String response = "";
        String state = "离线";
        if(QueryDB.Finduser(addName)){
            if(UpdateDB.Addfriend(userName, addName)&&UpdateDB.Addfriend(addName, userName))//为双方添加好友
            {
                response = "add-success";					
            }
            else{
                response = "add-fail";
            }
        }else {
            response = "add-fail";
        }
        outputToClient.writeUTF(response);
        if(response.equals("add-success")){

            //判断是否在线
            for (int i = 0; i < defaultModel.getRowCount(); i++) {
                if (addName.equals((String) defaultModel.getValueAt(i, 0))&&defaultModel.getValueAt(i, 3).equals("在线")) {
                    state = "在线";
                    break;
                }					
            }
            outputToClient.writeUTF(addName);
            outputToClient.writeUTF(state);
        }
        outputToClient.flush();
        System.out.println("已响应请求客户端");

        //如果对方在线，发送请求
        for(int i=0;i<defaultModel.getRowCount();i++){
            if(defaultModel.getValueAt(i, 0).equals(addName)&&defaultModel.getValueAt(i, 3).equals("在线")){
                Socket addrSocket = userSocketMap.get(addName);
                DataOutputStream outputToaddr = new DataOutputStream(addrSocket.getOutputStream());				
                outputToaddr.writeUTF("add");
                outputToaddr.writeUTF(userName);
                outputToaddr.flush();
                System.out.println("已向目标客户端发送请求");	
                break;
            }
        }
            

    }catch (IOException e) {
        e.printStackTrace();
        }		
    }

    //处理聊天请求
    public void handleChat(String username) {
        try {
            String addrName = inputFromClient.readUTF();
            String response = "chat-fail";

            //判断是否在线
            for (int i = 0; i < defaultModel.getRowCount(); i++) {
                if (addrName.equals((String) defaultModel.getValueAt(i, 0))&&defaultModel.getValueAt(i, 3).equals("在线")) {
                    response = "chat-success";
                    break;
                }					
            }

            //响应
            outputToClient.writeUTF(response);
            if(response.equals("chat-success"))
                outputToClient.writeUTF(addrName);
                
            outputToClient.flush();
            System.out.println("已响应客户端："+response);
            
            if(response.equals("chat-success")){
                //向目标客户端发送请求						
                Socket addrSocket = userSocketMap.get(addrName);
                DataOutputStream outputToaddr = new DataOutputStream(addrSocket.getOutputStream());				
                outputToaddr.writeUTF("chat");
                outputToaddr.writeUTF(username);
                outputToaddr.flush();
                //System.out.println("已发送");						
            }
            
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    //转发信息
    public void handleSendMessage(String senderName, String addrName) throws IOException{
        Socket addrsocket = userSocketMap.get(addrName);
        DataOutputStream outputToaddr = new DataOutputStream(addrsocket.getOutputStream());	

        String str = inputFromClient.readUTF();
        System.out.println("要发送的信息:"+str);

        outputToaddr.writeUTF("message");//告诉客户端发送的是消息
        outputToaddr.writeUTF(senderName);
        outputToaddr.writeUTF(str);
        outputToaddr.flush();
        System.out.println("消息已发送");
                                                                    
    }
    
    //转发文件
    public void handleSendFile(String senderName, String addrName) throws IOException{
        Socket addrsocket = userSocketMap.get(addrName);
        DataOutputStream outputToAddr = new DataOutputStream(addrsocket.getOutputStream());	

        String fileName = inputFromClient.readUTF();
        int fileLength = inputFromClient.readInt();
        byte[] fileBytes = new byte[fileLength];
        inputFromClient.readFully(fileBytes, 0, fileLength); // 接收文件内容

        outputToAddr.writeUTF("file");//告诉客户端发送的是文件 
        outputToAddr.writeUTF(senderName);
        outputToAddr.writeUTF(fileName); // 发送文件名
        outputToAddr.writeInt(fileLength); // 发送文件长度
        outputToAddr.write(fileBytes); // 发送文件内容
        outputToAddr.flush();
        System.out.println("文件已发送");
    }

    //处理下线请求
    public void handleOffline(String username){	
        try {	
            //设置离线状态，同时通知各客户端
            for (int i = 0; i < defaultModel.getRowCount(); i++) {
                String addrname = (String) defaultModel.getValueAt(i, 0);
                if (username.equals(addrname)) {																				
                    defaultModel.setValueAt("离线", i, 3);						 				 													
                }else{						
                    if(defaultModel.getValueAt(i, 3).equals("在线")){
                        Socket addrsocket = userSocketMap.get(addrname);
                        DataOutputStream dos = new DataOutputStream(addrsocket.getOutputStream());
                        dos.writeUTF("friend-online");
                        dos.writeUTF(username);
                        dos.writeUTF("离线");
                        dos.flush();
                        System.out.println("已通知"+addrname+","+name+"下线");
                    }
                }					        
            }

            //关闭连接										
            outputToClient.close();
            inputFromClient.close();
            socket.close();
            goon = false;
        } catch (IOException e) {				
            e.printStackTrace();
        }
    }

    //保存客户端，通知其他在线用户这人已上线
    public void saveInfo(String name) throws IOException{
        //保存客户端，并显示			
        userSocketMap.put(name, socket);		
        Object[] row = {name,
                        LocalDateTime.now(), 
                        "" + socket.getInetAddress(),        
                        "在线" };
        defaultModel.addRow(row); 

        //通知各客户端
        for (int i = 0; i < defaultModel.getRowCount(); i++) {
            String addrname = (String) defaultModel.getValueAt(i, 0);
            if (!name.equals(addrname)&&defaultModel.getValueAt(i, 3).equals("在线")) {														
                Socket addrsocket = userSocketMap.get(addrname);
                DataOutputStream dos = new DataOutputStream(addrsocket.getOutputStream());
                dos.writeUTF("friend-online");
                dos.writeUTF(name);
                dos.writeUTF("在线");
                dos.flush();	
                System.out.println("已通知"+addrname+","+name+"上线"); 				 																			
            }
                                        
        }

    }

}


