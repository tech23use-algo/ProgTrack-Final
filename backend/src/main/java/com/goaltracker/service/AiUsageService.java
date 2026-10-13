package com.goaltracker.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import org.springframework.stereotype.Service;

@Service
public class AiUsageService {
    private record Counters(LongAdder calls, LongAdder rejected) {}
    private final Map<String,Counters> usage = new ConcurrentHashMap<>();
    public void call(String email){usage.computeIfAbsent(LocalDate.now()+":"+email,k->new Counters(new LongAdder(),new LongAdder())).calls.increment();}
    public void rejected(String email){usage.computeIfAbsent(LocalDate.now()+":"+email,k->new Counters(new LongAdder(),new LongAdder())).rejected.increment();}
    public List<Map<String,Object>> summary(){return usage.entrySet().stream().map(e->{var p=e.getKey().split(":",2);var c=e.getValue();return Map.<String,Object>of("day",p[0],"user",p[1],"calls",c.calls.sum(),"rejected",c.rejected.sum());}).toList();}
}
