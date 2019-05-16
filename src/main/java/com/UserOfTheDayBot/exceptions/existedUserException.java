package com.UserOfTheDayBot.exceptions;

public class existedUserException extends Exception{
    public existedUserException() {
        super("Игрок уже в игре");
    }
}
