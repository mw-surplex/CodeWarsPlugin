package com.example.codewarsplugin.state.views;

import com.example.codewarsplugin.models.user.User;
import com.example.codewarsplugin.services.UserService;
import com.example.codewarsplugin.state.Panels;
import com.intellij.openapi.ui.ComboBox;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogedInView extends JPanel {

    private static User user;
    private static JPanel userPanel;

    private static final String[] options = {"Java", "Kotlin", "Scala", "Groovy", "Python", "C"};
    public static ComboBox<String> languageBox;

    public LogedInView() {
        super();
        userPanel = new JPanel();
        languageBox = new ComboBox<>(options);
    }


    public static boolean setup() {

        setupUserFields(user);

        Panels.getSidePanel().add(Panels.getKataPrompt(), BorderLayout.CENTER);
        Panels.getSidePanel().revalidate();
        Panels.getSidePanel().repaint();
        return true;
    }

    public static boolean cleanUp() {
        return false;
    }

    public static void init() {
        user = UserService.getUser();
    }

    private static String extractImageUrl(String html) {
        String regex = "<img[^>]+src\\s*=\\s*\"([^\"]+)\"[^>]*>";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(html);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }


    private static void setupUserFields(User user) {

        if (user == null) {
            userPanel.add(languageBox);
            return;
        }

        userPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 20, 20));
        JLabel nameLabel = new JLabel(user.getUsername());
        Font newFont = nameLabel.getFont().deriveFont(15f);
        nameLabel.setFont(newFont);
        userPanel.add(nameLabel);

        try{
            String url = extractImageUrl(user.getAvatar_tag());
            URL imageUrl = new URL(url);
            BufferedImage image = ImageIO.read(imageUrl);
            ImageIcon imageIcon = new ImageIcon(image);
            JLabel imageLabel = new JLabel(imageIcon);
            imageLabel.setBorder(new EmptyBorder(10, 0, 0, 0));
            userPanel.add(imageLabel, BorderLayout.NORTH);
        } catch (Exception ignored) {}

        JLabel rankLabel = new JLabel(String.valueOf(Math.abs(user.getRank())) + " kyu");
        rankLabel.setFont(newFont);
        userPanel.add(rankLabel);

        JLabel honorLabel = new JLabel(String.valueOf(user.getHonor()));
        honorLabel.setFont(newFont);
        userPanel.add(honorLabel);

        userPanel.add(languageBox);

        Panels.getSidePanel().add(userPanel, BorderLayout.NORTH);
    }

}
