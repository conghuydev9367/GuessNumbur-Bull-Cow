package com.guessnumber.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Message {
    private final String command;
    private final List<String> fields;

    public Message(String command, List<String> fields) {
        this.command = command == null ? "" : command.trim();
        this.fields = new ArrayList<>(fields == null ? Collections.emptyList() : fields);
    }

    public static Message of(String command, String... fields) {
        List<String> list = new ArrayList<>();
        if (fields != null) {
            Collections.addAll(list, fields);
        }
        return new Message(command, list);
    }

    public static Message parse(String line) {
        if (line == null) {
            return new Message("", Collections.emptyList());
        }
        String[] parts = line.split("\\|", -1);
        String cmd = parts.length > 0 ? parts[0] : "";
        List<String> args = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            args.add(parts[i]);
        }
        return new Message(cmd, args);
    }

    public String serialize() {
        if (fields.isEmpty()) {
            return command;
        }
        StringBuilder sb = new StringBuilder(command);
        for (String field : fields) {
            sb.append("|").append(field == null ? "" : field);
        }
        return sb.toString();
    }

    public String command() {
        return command;
    }

    public List<String> fields() {
        return Collections.unmodifiableList(fields);
    }

    public String field(int index) {
        return index >= 0 && index < fields.size() ? fields.get(index) : "";
    }

    public int fieldCount() {
        return fields.size();
    }
}
