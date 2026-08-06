package com.monsterhouse.notification.line.dto;

import java.util.List;

public record LinePushRequest(
        String to,
        List<TextMessage> messages
) {
    public static LinePushRequest text(String to, String text){
        return new LinePushRequest(to, List.of(new TextMessage("text", truncate(text))));
    }
    private static String truncate(String text){
        if(text == null){
            return "";
        }
        return text.length() <= 5000 ? text : text.substring(0, 4997) + "...";
    }
    public record TextMessage(String type, String text){
    }
}
