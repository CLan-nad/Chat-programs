package src.server;

import java.sql.*;

//含有功能：查询好友列表，查询用户是否存在
public class QueryDB {
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";  
    static final String DB_URL = "jdbc:mysql://localhost:3306/lab";
   
    static final String USER = "root";
    static final String PASS = "root";

    //查询好友列表
    public static String[] Queryfriend(String name){ 
       
        String username = name; // 要查询的用户名
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String[] friends = new String[5];//返回结果

        try {
            // 注册 JDBC 驱动
            Class.forName(JDBC_DRIVER);
        
            // 打开链接
            System.out.println("连接数据库...");
            conn = DriverManager.getConnection(DB_URL,USER,PASS);

            // 构造 SQL 查询语句
            String sql = "SELECT friend1, friend2, friend3, friend4, friend5 FROM friendship WHERE username = ?";

            // 预编译 SQL 语句
            pstmt = conn.prepareStatement(sql);

            // 设置查询参数
            pstmt.setString(1, username);

            // 执行查询
            rs = pstmt.executeQuery();

            // 处理查询结果
            if (rs.next()) {               
                String friend1 = rs.getString("friend1");
                String friend2 = rs.getString("friend2");
                String friend3 = rs.getString("friend3");
                String friend4 = rs.getString("friend4");
                String friend5 = rs.getString("friend5");

                // 收集查询结果
                System.out.println(username+"的好友如下：");
                if (friend1 != null && !friend1.isEmpty()) {
                    System.out.println("好友1：" + friend1);
                    friends[0] = friend1;
                }              
                if (friend2 != null && !friend2.isEmpty()) {
                    System.out.println("好友2：" + friend2);
                    friends[1] = friend2;
                }
                if (friend3 != null && !friend3.isEmpty()) {
                    System.out.println("好友3：" + friend3);
                    friends[2] = friend3;
                }
                if (friend4 != null && !friend4.isEmpty()) {
                    System.out.println("好友4：" + friend4);
                    friends[3] = friend4;
                }
                if (friend5 != null && !friend5.isEmpty()) {
                    System.out.println("好友5：" + friend5);
                    friends[4] = friend5;
                }

                return friends;
            } else {
                System.out.println("未找到用户或用户没有好友关系");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch(Exception e){
            // 处理 Class.forName 错误
            e.printStackTrace();
        }finally {
            // 关闭数据库连接
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    //查询用户是否存在
    public static boolean Finduser(String name) { 
        String username = name; // 要查询的用户名
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            // 注册 JDBC 驱动
            Class.forName(JDBC_DRIVER);
        
            // 打开链接
            System.out.println("连接数据库...");
            conn = DriverManager.getConnection(DB_URL,USER,PASS);

            // 构造 SQL 查询语句
            String sql = "SELECT username FROM friendship WHERE username = ?";

            // 预编译 SQL 语句
            pstmt = conn.prepareStatement(sql);

            // 设置查询参数
            pstmt.setString(1, username);

            // 执行查询
            rs = pstmt.executeQuery();

            // 处理查询结果
            if (rs.next()) {               
                System.out.println("查找成功");
                return true;

            } else {
                System.out.println("未找到用户"+name);
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch(Exception e){
            // 处理 Class.forName 错误
            e.printStackTrace();
        }finally {
            // 关闭数据库连接
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
   
}
