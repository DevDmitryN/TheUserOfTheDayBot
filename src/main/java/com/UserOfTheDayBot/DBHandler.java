package com.UserOfTheDayBot;

import com.UserOfTheDayBot.enums.DBColumns;
import com.UserOfTheDayBot.enums.Games;
import org.telegram.telegrambots.meta.api.objects.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.UserOfTheDayBot.exceptions.existedUserException;

public class DBHandler {
    private Connection connection;
    public DBHandler(){

    }
    public DBHandler(String url,String login,String password) {
            connectToDB(url,login,password);
    }
    public void connectToDB(String url,String login,String password){
        Properties properties = new Properties();
        properties.put("User", login);
        properties.put("password", password);
        properties.put("autoReconnect", "true");
        properties.put("characterUnicode", "true");
        properties.put("useUnicode", "true");
        properties.put("useSSL", "false");
        properties.put("useLegacyDatetimeCode", "false");
        properties.put("serverTimezone", "UTC");
        try {
            DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());
            connection =  DriverManager.getConnection(url,properties);
        } catch (SQLException  e) {
            e.printStackTrace();
        }
    }
    public void closeConnection(){
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void registration(String chatId, User user)throws existedUserException{
        String query = "SELECT * FROM chat_user WHERE chat_id=" + chatId + " AND user_id ="+user.getId();
        try(Statement statement = connection.createStatement()){
            if(statement.executeQuery(query).next()){
                throw new existedUserException();
            }else {
                query = "INSERT INTO chat_user (chat_id,user_id) VALUE (" + chatId + "," + user.getId() + ")";
                statement.executeUpdate(query);

                try{
                    query = "INSERT INTO users (user_id, username) " +
                            "VALUE ("+user.getId()+",\""+user.getUserName()+"\")";
                    statement.executeUpdate(query);
                    query="UPDATE users SET firstname=\""+user.getFirstName()+"\" WHERE user_id="+user.getId();
                    statement.executeUpdate(query);
                }catch (SQLIntegrityConstraintViolationException e){
                    System.out.println("user already exists in 'users'");
                }
                try{
                    query = "INSERT INTO chats (chat_id) VALUE ("+chatId+")";
                    statement.executeUpdate(query);
                }catch (SQLIntegrityConstraintViolationException e){
                    System.out.println("chat already exists in 'chats'");
                }
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
    }
    public boolean isTheSameDayRunning(String chatId, int day, DBColumns column){
        String query = "SELECT " + column + " FROM chats WHERE chat_id =" + chatId;
        try(Statement statement = connection.createStatement()){
            ResultSet result = statement.executeQuery(query);
            if(result.next()){
                return day == result.getInt(1);
            }

        }catch (SQLException e){
            e.printStackTrace();
        }
        return false;
    }
    public List<UserForBD> getListOfPlayers(String chatId){
        List<UserForBD> players = new ArrayList<UserForBD>();
        UserForBD user;
        String query = "select users.user_id,username,firstname,user_day_counter,loser_counter" + " from users join chat_user on chat_id="+chatId+" where chat_user.user_id=users.user_id";
        try(Statement statement = connection.createStatement()){
            ResultSet usersFromBD = statement.executeQuery(query);
            while (usersFromBD.next()){
                user = createUserForBD(usersFromBD);
                user.setUserDayCounter(usersFromBD.getInt(4));
                user.setLoserDayCounter(usersFromBD.getInt(5));
                players.add(user);
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return players;
    }
    private UserForBD createUserForBD(ResultSet resultSet)throws SQLException{
        return new UserForBD(resultSet.getInt(1),resultSet.getString(2),resultSet.getString(3));
    }
    public void setWinnerAndDayRunning(String chatId, UserForBD user, int dayRunning, Games column){
        String dayColumn = "";
        String counterColumn = "";
        switch (column){
            case user_of_the_day:
                dayColumn = "user_of_the_day_run_day";
                counterColumn = "user_day_counter";
                break;
            case loser_of_the_day:
                dayColumn = "loser_of_the_day_run_day";
                counterColumn = "loser_counter";
                break;
        }
        String query = "UPDATE chats SET " + column + " = \""+user.getName()+"\", " + dayColumn + " ="+dayRunning+" WHERE chat_id="+chatId;
        try(Statement statement = connection.createStatement()){
            statement.executeUpdate(query);
            query = "UPDATE chat_user SET " + counterColumn + "="+counterColumn+"+1 WHERE chat_id="+chatId+" AND user_id="+user.getID();
            statement.executeUpdate(query);
        }catch (SQLException e){
            e.printStackTrace();
        }
    }
    public String getWinnerOfTheGame(String chatId,Games game){
        try(Statement statement = connection.createStatement()){
            ResultSet result = statement.executeQuery("SELECT " + game + " FROM chats WHERE chat_id="+chatId);
            if(result.next()){
                return result.getString(1);
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }
}
