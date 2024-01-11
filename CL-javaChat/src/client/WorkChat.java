package src.client;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.*;
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

//聊天功能
public class WorkChat implements Runnable{

    DataOutputStream outputToClient = ClientMain.outputToServer;       
    String friendName;
    JFrame frame = new JFrame();
    
    JTextPane outframe = new JTextPane();
    JTextField inframe = new JTextField();
    JScrollPane scrollPane;//滚动输出框
    JPanel inputPanel = new JPanel();//输入框

    boolean isWindowOpen = false;//记录窗口是否打开

    WorkChat(String friendName) {
        this.friendName = friendName;
    }

    //首次开启与对方的聊天窗口
    public void run() {          
        // 设置窗口               
        frame.setTitle(friendName);
        frame.setLocation(350, 200);
        frame.setLayout(new BorderLayout());

        // 输出框
        outframe.setEditable(false);
        scrollPane = new JScrollPane(outframe);
        frame.add(scrollPane, BorderLayout.CENTER);

        // 输入框      
        inputPanel.setLayout(new BorderLayout());
        inputPanel.add(inframe, BorderLayout.CENTER);
        inframe.addActionListener(new inframeListener());
        JButton fileButton = new JButton("选择文件");
        fileButton.addActionListener(new FileButtonListener());
        inputPanel.add(fileButton, BorderLayout.EAST);
        frame.add(inputPanel, BorderLayout.SOUTH);

        frame.addWindowListener(new CloseChat());
        frame.setSize(600, 400);
        frame.setVisible(true);

        isWindowOpen = true;     
    }

    //再次开启与对方的聊天窗口，组件不需要再设置
    public void createWindow(){
        // 设置窗口               
        frame.setTitle(friendName);
        frame.setLocation(350, 200);
        frame.setLayout(new BorderLayout());

        // 输出框
        frame.add(scrollPane, BorderLayout.CENTER);

        // 输入框
        frame.add(inputPanel, BorderLayout.SOUTH);

        frame.setSize(600, 400);
        frame.setVisible(true);

        isWindowOpen = true;
    }

    // 监听输入框，负责发送信息
    private class inframeListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String tempStr = inframe.getText().trim(); // 得到输入框输入的字符串
            inframe.setText("");
            try {
                //告诉服务端这是一个消息
                outputToClient.writeUTF("send-message");
                outputToClient.writeUTF(friendName);
                //消息内容
                outputToClient.writeUTF(tempStr);
                outputToClient.flush();    
                //显示在自己的窗口上              
                outframe.getStyledDocument().insertString(outframe.getStyledDocument().getLength(), "我:\n", new SimpleAttributeSet(){{ StyleConstants.setForeground(this, Color.BLUE); }});  
                outframe.getStyledDocument().insertString(outframe.getStyledDocument().getLength(), tempStr+"\n\n", null);                 
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (BadLocationException e1) {
                e1.printStackTrace();
            }
        }
    }

    //响应按钮，负责发送文件
    private class FileButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e){

            JFileChooser fileChooser = new JFileChooser();//选择文件
            fileChooser.setCurrentDirectory(new File("src/client/image"));
            int returnValue = fileChooser.showOpenDialog(null);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();//选择好的文件
                try {      
                    outputToClient.writeUTF("send-file"); //告诉服务器这是一个文件
                    outputToClient.writeUTF(friendName); 
                    
                    byte[] fileBytes = Files.readAllBytes(file.toPath());
                    outputToClient.writeUTF(file.getName()); // 发送文件名                       
                    outputToClient.writeInt(fileBytes.length); // 发送文件长度
                    outputToClient.write(fileBytes); // 发送文件内容
                    outputToClient.flush();

                    //图片类型的文件需要显示
                    if(isImageFile(file.getName())){
                        outframe.getStyledDocument().insertString(outframe.getStyledDocument().getLength(), "我: \n", new SimpleAttributeSet(){{ StyleConstants.setForeground(this, Color.BLUE); }});
                        String imagePath = file.getPath();
                        showImage(imagePath);
                    }else{
                        outframe.getStyledDocument().insertString(outframe.getStyledDocument().getLength(), "我发送了文件: " + file.getName() + "\n\n", new SimpleAttributeSet(){{ StyleConstants.setForeground(this, Color.BLUE); }});
                    }
                    
            } catch (IOException e2) {
                e2.printStackTrace();
            } catch (BadLocationException e1) {
                e1.printStackTrace();
            }
            }
        }
    }

    //接收信息并展示
    public void showMessage(String str) throws BadLocationException{
        if(str.equals("quit")){//接收到quit说明对方关闭了聊天
            outframe.getStyledDocument().insertString(outframe.getStyledDocument().getLength(), "系统消息：对方已退出聊天\n\n", new SimpleAttributeSet(){{ StyleConstants.setForeground(this, Color.BLUE); }});                                  
        }else{
            outframe.getStyledDocument().insertString(outframe.getStyledDocument().getLength(), "对方:\n", new SimpleAttributeSet(){{ StyleConstants.setForeground(this, Color.BLUE); }});
            outframe.getStyledDocument().insertString(outframe.getStyledDocument().getLength(), str+"\n\n", null);
        }        
    }

    //接收文件
    public void handleFile(String fileName, int fileLength, byte[] fileBytes) throws IOException, BadLocationException{
        //保存文件到本地
        String savePath = "C:\\Users\\蔡乐\\Desktop\\" + fileName; 
        Files.write(Paths.get(savePath), fileBytes);

        //若文件是图片，显示图片
        if (isImageFile(fileName)) {
            outframe.getStyledDocument().insertString(outframe.getStyledDocument().getLength(), "对方: \n", new SimpleAttributeSet(){{ StyleConstants.setForeground(this, Color.BLUE); }});
            showImage(savePath);
        }
        else           
        outframe.getStyledDocument().insertString(outframe.getStyledDocument().getLength(), "已接收到文件:" + fileName + "\n\n", new SimpleAttributeSet(){{ StyleConstants.setForeground(this, Color.BLUE); }});
    }

    //判断文件是否为图片
    private boolean isImageFile(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
        return extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("png") || extension.equalsIgnoreCase("bmp");
    }

    //显示图片
    private void showImage(String savePath) throws BadLocationException{
        ImageIcon icon = new ImageIcon(savePath); // 创建图标，使用指定路径的图像文件
        Image image = icon.getImage(); // 获取图标的图像
        Image scaledImage = image.getScaledInstance(100, 100, Image.SCALE_SMOOTH); // 将图像缩放
        ImageIcon scaledIcon = new ImageIcon(scaledImage); // 创建一个新的图标，使用缩放后的图像
        JLabel imageLabel = new JLabel(scaledIcon); // 创建一个标签，用于显示
        imageLabel.addMouseListener(new ImageClickListener(savePath)); // 点击放大图片

        outframe.setCaretPosition(outframe.getDocument().getLength()); // 设置光标位置到末尾
        outframe.insertComponent(imageLabel); // 图像标签添加到文本框中
        outframe.getStyledDocument().insertString(outframe.getStyledDocument().getLength(), "\n\n", null); 

    }
  
    //点击放大图片
    private class ImageClickListener extends MouseAdapter {
        private String imagePath;
    
        public ImageClickListener(String path) {
            this.imagePath = path;
        }
    
        public void mouseClicked(MouseEvent e) {
            ImageIcon icon = new ImageIcon(imagePath);
            Image image = icon.getImage(); // 获取原始图片
            int width = image.getWidth(null); // 获取原始图片宽度
            int height = image.getHeight(null); // 获取原始图片高度
    
            // 设置图片的最大宽度和高度
            int maxWidth = 300;
            int maxHeight = 300;
    
            // 如果图片宽度或高度超过最大值，则按比例缩放
            if (width > maxWidth || height > maxHeight) {
                if (width > height) {
                    height = (int) (height * ((double) maxWidth / width));
                    width = maxWidth;
                } else {
                    width = (int) (width * ((double) maxHeight / height));
                    height = maxHeight;
                }
                image = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            }
    
            //弹出窗口展示放大图片
            ImageIcon scaledIcon = new ImageIcon(image); 
            JLabel label = new JLabel();
            label.setIcon(scaledIcon);
            JOptionPane.showMessageDialog(null, label, "Scaled Image", JOptionPane.PLAIN_MESSAGE);
        }
    }   
   
    //关闭聊天
    class CloseChat extends WindowAdapter {
        public void windowClosing(WindowEvent e) {
            try {							                              
                outputToClient.writeUTF("send-message");
                outputToClient.writeUTF(friendName);

                outputToClient.writeUTF("quit"); 
                outputToClient.flush();
                isWindowOpen = false;
                outframe.getStyledDocument().insertString(outframe.getStyledDocument().getLength(), "你退出了上次聊天\n\n", new SimpleAttributeSet(){{ StyleConstants.setForeground(this, Color.BLUE); }});
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (BadLocationException e1) {
                e1.printStackTrace();
            } 	
        }
    }

}