package it.fvaleri.integ;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GreetingService {
    public Greeting getGreeting(String name) {
        return new Greeting("hello " + name);
    }
}

