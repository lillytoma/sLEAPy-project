package com.sleapy.project;

import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class Main {


    public static void greeter(String name) {
        System.out.printf("Hello %s, welcome to the sLEAPy!\n", name);
    }

    public static void calcBumbleBee(int c1, int c2) {
        System.out.printf("%d + %d = %d bumblebees", c1, c2, c1 + c2);
        //setting up branch 
    }

    
    public static void main(String[] args) {
        // TODO: replace <team-name> with your team's actual name
        System.out.println("Hello world from the sLEAPy");
        calcBumbleBee(5, 8);
    }
}

