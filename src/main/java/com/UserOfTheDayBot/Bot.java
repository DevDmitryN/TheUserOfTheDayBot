package com.UserOfTheDayBot;

import com.UserOfTheDayBot.enums.Commands;
import com.UserOfTheDayBot.enums.DBColumns;
import com.UserOfTheDayBot.enums.Games;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.SimpleDateFormat;
import java.util.*;
import com.UserOfTheDayBot.exceptions.existedUserException;

public class Bot extends TelegramLongPollingBot {

    //class for sending messages with delay
    class TimerSendingTask extends TimerTask {
        private String message;
        private String chatId;

        TimerSendingTask(String chatId, String message) {
            this.chatId = chatId;
            this.message = message;
        }

        @Override
        public void run() {
            sendMsg(chatId, message);
        }
    }

    private final String BOT_USERNAME = "username";
    private final String TOKEN = "token";

    private String[] messagesForUserOfTheDay = {
            "\uD83C\uDF89 Сегодня красавчик дня - ",
            "ВНИМАНИЕ \uD83D\uDD25",
            "Ищем красавчика в этом чате",
            "Гадаем на бинарных опционах \uD83D\uDCCA",
            "Анализируем лунный гороскоп \uD83C\uDF16",
            "Лунная призма дай мне силу \uD83D\uDCAB",
            "СЕКТОР ПРИЗ НА БАРАБАНЕ \uD83C\uDFAF"
    };
    private String[] messagesForLoserOfTheDay = {
            "\uD83C\uDF89 Сегодня неудачник \uD83C\uDF08 дня - ",
            "ВНИМАНИЕ \uD83D\uDD25",
            "ФЕДЕРАЛЬНЫЙ \uD83D\uDD0D РОЗЫСК НЕУДАНИКА \uD83D\uDEA8",
            "4 - спутник запущен \uD83D\uDE80",
            "3 - сводки Интерпола проверены \uD83D\uDE93",
            "2 - твои друзья опрошены \uD83D\uDE45",
            "1 - твой профиль в соцсетях проанализирован \uD83D\uDE40"

    };
    //for DB
    private String URL = "jdbc:mysql://localhost:3306/chats_users_db";
    private String LOGIN = "root";
    private String PASSWORD = "root";

    /*method that gets a message
    * then handles it and does action according to command
     */
    public void onUpdateReceived(Update update) {
        String message = update.getMessage().getText();
        if(!message.startsWith("/")){
            return;
        }
        int commandEnd = message.lastIndexOf("@"+BOT_USERNAME);
        if(commandEnd == -1){
            commandEnd = message.length();
        }
        Commands command = Commands.valueOf(message.substring(1, commandEnd));
        String chatId = update.getMessage().getChatId().toString();
        switch (command) {
            case run:
                runGame(chatId, Games.user_of_the_day);
                break;
            case reg:
                addUserInGame(chatId, update.getMessage().getFrom());
                break;
            case stat_user:
                sendStatisticOfTheGame(chatId,Games.user_of_the_day);
                break;
            case loser:
                runGame(chatId,Games.loser_of_the_day);
                break;
            case stat_loser:
                sendStatisticOfTheGame(chatId,Games.loser_of_the_day);
                break;
            default:
                break;
        }
    }


    private void runGame(String chatId,Games game){
        DBHandler dbHandler = new DBHandler(URL, LOGIN, PASSWORD);
        List<UserForBD> usersInGame = dbHandler.getListOfPlayers(chatId);
        String[] messages;
        if (usersInGame.size() == 0) {
            sendMsg(chatId,"Нет игроков");
            return;
        }
        switch (game){
            case user_of_the_day:
                if (dbHandler.isTheSameDayRunning(chatId,getToday(), DBColumns.user_of_the_day_run_day)) {
                    sendMsg(chatId,messagesForUserOfTheDay[0] + dbHandler.getWinnerOfTheGame(chatId,Games.user_of_the_day));
                    return;
                }
                messages = messagesForUserOfTheDay;
                break;
            case loser_of_the_day:
                if (dbHandler.isTheSameDayRunning(chatId,getToday(), DBColumns.loser_of_the_day_run_day)) {
                    sendMsg(chatId, messagesForLoserOfTheDay[0] + dbHandler.getWinnerOfTheGame(chatId,Games.loser_of_the_day));
                    return;
                }
                messages = messagesForLoserOfTheDay;
                break;

                default:
                    messages = null;
        }
        Timer timer = new Timer();
        int MESSAGE_DELAY = 1500;
        for(int  i = 1; i < messages.length; i++){
            timer.schedule(new TimerSendingTask(chatId,messages[i]),MESSAGE_DELAY*i);
        }
        int i = new Random().nextInt(usersInGame.size());
        UserForBD winner = usersInGame.get(i);
        timer.schedule(new TimerSendingTask(chatId, messages[0] + winner.getNotificationName()),
                MESSAGE_DELAY*messages.length);
        dbHandler.setWinnerAndDayRunning(chatId,winner,getToday(),game);
        dbHandler.closeConnection();
    }
    private void addUserInGame(String chatId, User user){
        try {
            DBHandler dbHandler = new DBHandler(URL, LOGIN, PASSWORD);
            dbHandler.registration(chatId, user);
            dbHandler.closeConnection();
        }catch (existedUserException e){
            sendMsg(chatId, "Ты уже в игре");
            return;
        }
        sendMsg(chatId, user.getFirstName() + ", Ты в игре");
    }

    private void sendStatisticOfTheGame(String chatId,Games game) {
        String message = null;
        StringBuilder statisticUserOfTheDay = null;
        DBHandler dbHandler = new DBHandler(URL, LOGIN, PASSWORD);
        int i=1;
        switch (game){
            case user_of_the_day:
                message = "\uD83C\uDF89 Результаты Красавчик Дня\n";
                statisticUserOfTheDay = new StringBuilder(message);
                for (UserForBD user : dbHandler.getListOfPlayers(chatId)) {
                    statisticUserOfTheDay.append(i++ + ")" + user.getNotificationName() +" - " +user.getUserDayCounter()  + " раз(а)\n");
                }
                break;
            case loser_of_the_day:
                message = "Результаты \uD83C\uDF08НЕУДАЧНИКА Дня\n";
                statisticUserOfTheDay = new StringBuilder(message);
                for (UserForBD user : dbHandler.getListOfPlayers(chatId)) {
                    statisticUserOfTheDay.append(i++ + ")" + user.getNotificationName() +" - " +user.getLoserDayCounter()  + " раз(а)\n");
                }
                break;
        }
        dbHandler.closeConnection();
        sendMsg(chatId, statisticUserOfTheDay.toString());
    }

    //method for sending messages in the chat
    private synchronized void sendMsg(String chatId, String s) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(s);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    //method returns signed up username of bot
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return TOKEN;
    }

    private int getToday(){
        return Integer.valueOf(new SimpleDateFormat("DD").format(new Date()));
    }
}