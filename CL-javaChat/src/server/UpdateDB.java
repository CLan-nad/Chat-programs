package src.server;

import java.sql.*;

//含有功能：注册新用户，为用户添加好友,为用户删除好友
public class UpdateDB{
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";  
    static final String DB_URL = "jdbc:mysql://localhost:3306/lab";
   
    static final String USER = "root";
    static final String PASS = "root";
    //注册新用户
    public static void Adduser(String name){ 
       
        String username = name; 
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            // 注册 JDBC 驱动
            Class.forName(JDBC_DRIVER);
        
            // 打开链接
            System.out.println("连接数据库...");
            conn = DriverManager.getConnection(DB_URL,USER,PASS);

            // 构造 SQL 语句
            String sql = "INSERT INTO friendship VALUES (?, null, null, null, null, null)";

            // 预编译 SQL 语句
            pstmt = conn.prepareStatement(sql);

            // 设置参数
            pstmt.setString(1, username);

            // 执行
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("添加成功");
            } else {
                System.out.println("添加失败");
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
    }

    //为用户添加好友
    public static boolean Addfriend(String username, String addname){ 
       
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            // 注册 JDBC 驱动
            Class.forName(JDBC_DRIVER);
        
            // 打开链接
            System.out.println("连接数据库...");
            conn = DriverManager.getConnection(DB_URL,USER,PASS);

            //先找到第一个非空的位置(数据库限定了用户只有五个好友,所以穷举搜索)
            String sql1 = " SELECT \r\n" + //
                    "    CASE \r\n" + //
                    "        WHEN friend1 IS NULL OR friend1 = '' THEN 'friend1'\r\n" + //
                    "        WHEN friend2 IS NULL OR friend2 = '' THEN 'friend2'\r\n" + //
                    "        WHEN friend3 IS NULL OR friend3 = '' THEN 'friend3'\r\n" + //
                    "        WHEN friend4 IS NULL OR friend4 = '' THEN 'friend4'\r\n" + //
                    "        WHEN friend5 IS NULL OR friend5 = '' THEN 'friend5'\r\n" + //
                    "    END AS first_empty_field\r\n" + //
                    "FROM friendship\r\n" + //
                    "WHERE username = ?\r\n" + //
                    "";
            pstmt = conn.prepareStatement(sql1);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();
            String first_null = null;
            if (rs.next()){
                first_null = rs.getString("first_empty_field");
            }
            pstmt.close();
            rs.close();

           //插入好友名
           String sql2 = "UPDATE friendship " + //
                    "SET " + first_null + " = ? " + //
                    "WHERE username = ?";
            pstmt = conn.prepareStatement(sql2);
            pstmt.setString(1, addname);
            pstmt.setString(2, username);
            int rowsAffected = pstmt.executeUpdate();
            if(rowsAffected > 0){
                System.out.println("插入成功");
                return true;
            }else {
                System.out.println("插入失败");
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

    //为用户删除好友
    public static boolean Deletefriend(String username, String deletename){ 
       
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            // 注册 JDBC 驱动
            Class.forName(JDBC_DRIVER);
        
            // 打开链接
            System.out.println("连接数据库...");
            conn = DriverManager.getConnection(DB_URL,USER,PASS);

           //依此搜索该好友在哪个字段，然后删除
           String sql = "UPDATE friendship SET " +
           "friend1 = CASE WHEN friend1 = ? THEN NULL ELSE friend1 END, " +
           "friend2 = CASE WHEN friend2 = ? THEN NULL ELSE friend2 END, " +
           "friend3 = CASE WHEN friend3 = ? THEN NULL ELSE friend3 END, " +
           "friend4 = CASE WHEN friend4 = ? THEN NULL ELSE friend4 END, " +
           "friend5 = CASE WHEN friend5 = ? THEN NULL ELSE friend5 END " +
           "WHERE username = ?";

            pstmt = conn.prepareStatement(sql);

            // 设置参数
            pstmt.setString(1, deletename);
            pstmt.setString(2, deletename);
            pstmt.setString(3, deletename);
            pstmt.setString(4, deletename);
            pstmt.setString(5, deletename);
            pstmt.setString(6, username);

            // 执行更新
            int rowsAffected = pstmt.executeUpdate();
            if(rowsAffected > 0){
                System.out.println("删除成功");
                return true;
            }else {
                System.out.println("删除失败");
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
