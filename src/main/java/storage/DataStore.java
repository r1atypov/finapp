package storage;

import model.User;
import java.io.*;
import java.util.*;

public class DataStore {
    private static final String FILE = "finapp_users.dat";
    private final Map<String, User> users = new HashMap<>();

    @SuppressWarnings("unchecked")
    public void load() {
        File f = new File(FILE);
        if (!f.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            users.putAll((Map<String, User>) ois.readObject());
        } catch (Exception ignored) {}
    }

    public void save() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE))) {
            oos.writeObject(users);
        } catch (IOException ignored) {}
    }

    public User getUser(String login) { return users.get(login); }
    public void addUser(User u) { users.put(u.getLogin(), u); }
    public boolean hasUser(String login) { return users.containsKey(login); }
    public Map<String, User> getAll() { return users; }
}