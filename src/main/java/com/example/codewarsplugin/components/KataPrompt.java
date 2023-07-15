package com.example.codewarsplugin.components;

import com.example.codewarsplugin.models.kata.KataRecord;
import com.example.codewarsplugin.services.katas.KataIdService;
import com.example.codewarsplugin.state.Panels;
import com.intellij.ui.AnimatedIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class KataPrompt extends JPanel {

    private static JTextField textField;
    private static JButton submitButton;
    private static JLabel promptLabel;
    private static JLabel invalidKataLabel;
    private static JPanel cardButtonPanel = new JPanel();
    private static CardLayout cardButtonLayout = new CardLayout();

    private static JPanel cardKataPanel = new JPanel();
    private static CardLayout cardKataLayout = new CardLayout();
    private static JPanel emptyKataPanel = new JPanel();
    private static JLabel spinner = new JLabel(new AnimatedIcon.Big());
    private static final GridBagConstraints constraints = new GridBagConstraints();


    public KataPrompt(){
        super();
        setLayout(new GridBagLayout());
        textField = new JTextField(30);
        submitButton = new JButton("Get Kata");
        promptLabel = new JLabel("Paste kata title!");
        invalidKataLabel = new JLabel("Kata not found!");
        invalidKataLabel.setForeground(Color.red);
        invalidKataLabel.setFont(promptLabel.getFont().deriveFont(15f));
        cardButtonPanel.setLayout(cardButtonLayout);
        cardKataPanel.setLayout(cardKataLayout);
        //addEnterKeyListener();
        addElementsToPanel();
        addButtonPushedListener();
    }

    private void addButtonPushedListener() {
        submitButton.addActionListener(this::searchKata);
    }

    private void searchKata(ActionEvent event) {
        startSpinner();
        KataRecord record = KataIdService.getKataRecord(textField.getText());
        System.out.println(record);
    }

    public static void complete(boolean success){
        System.out.println("complete: " + KataIdService.record);
        Panels.getKataPrompt().stopSpinner();
        if (!success) {
            cardKataLayout.show(cardKataPanel, "invalid");
            Panels.getSidePanel().revalidate();
            Panels.getSidePanel().repaint();
            Timer timer = new Timer(2000, e -> SwingUtilities.invokeLater(() -> {
                cardKataLayout.show(cardKataPanel, "empty");
                Panels.getSidePanel().revalidate();
                Panels.getSidePanel().repaint();
            }));
            timer.setRepeats(false);
            timer.start();
        }
    }

    private void addElementsToPanel() {

        promptLabel.setFont(promptLabel.getFont().deriveFont(14f));

        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = new Insets(5, 5, 5, 5);

        constraints.gridx = 0;
        constraints.gridy = 0;
        add(textField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;

        constraints.insets = new Insets(0, 5, 5, 5);
        add(promptLabel, constraints);

        cardButtonPanel.add(submitButton, "submitButton");
        cardButtonPanel.add(spinner, "spinner");

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.insets = new Insets(5, 5, 50, 5);
        add(cardButtonPanel, constraints);

        cardKataPanel.add(emptyKataPanel, "empty");
        cardKataPanel.add(invalidKataLabel, "invalid");

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.insets = new Insets(5, 5, 5, 5);
        add(cardKataPanel, constraints);
    }

    public void startSpinner() {
        cardButtonLayout.show(cardButtonPanel, "spinner");
        revalidate();
        repaint();
    }

    public void stopSpinner() {
        cardButtonLayout.show(cardButtonPanel, "submitButton");
        revalidate();
        repaint();
    }

}
