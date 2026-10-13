package com.goaltracker.ai;
import java.util.List;
public final class AiDtos {private AiDtos(){} public record Milestone(String title,String description,Integer estimatedMinutes,Integer dayNumber){} public record Result(String status,String reason,List<Milestone> milestones){} public record ChatRequest(List<Message> messages,String model,double temperature,Object response_format){} public record Message(String role,String content){} public record ChatResponse(List<Choice> choices){} public record Choice(Message message){} }
