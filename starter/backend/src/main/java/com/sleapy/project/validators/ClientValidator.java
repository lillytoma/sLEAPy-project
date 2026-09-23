package com.sleapy.project.validators;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.sleapy.project.exceptions.InvalidEmailFormatException;

@Service 
public class ClientValidator {

    private final Pattern emailRegex;
    ClientValidator(){
        //This is the official standard regex for emails according to RFC 5322
        // https://emailregex.com/
        var regexStr = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{"+
        "|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\["+
        "\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)"+
        "+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.)"+
        "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\"+
        "x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";
        this.emailRegex = Pattern.compile(regexStr);
    }


    public void validateEmail(String email) throws InvalidEmailFormatException{
        if(email == null || email.isBlank()){
            throw new InvalidEmailFormatException("Email must not be empty");
        }
        email = email.trim();
        boolean hasWhitespace = email.chars().anyMatch(Character::isWhitespace);
        if (hasWhitespace){
            throw new InvalidEmailFormatException("Email must not contain whitespace");
        }
        Matcher matcher = this.emailRegex.matcher(email);
        
        if(!matcher.find()){
            throw new InvalidEmailFormatException("Invalid email format");
        }

    }
}
