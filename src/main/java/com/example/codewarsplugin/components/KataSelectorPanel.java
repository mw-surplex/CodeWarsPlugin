package com.example.codewarsplugin.components;

import com.example.codewarsplugin.SidePanel;
import com.example.codewarsplugin.models.kata.KataDirectory;
import com.example.codewarsplugin.services.files.create.FileManager;
import com.example.codewarsplugin.services.files.parse.KataDirectoryParser;
import com.example.codewarsplugin.state.Manager;
import com.example.codewarsplugin.state.Store;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

import static com.example.codewarsplugin.config.StringConstants.MESSAGE_ICON;

public class KataSelectorPanel extends JPanel {

    private final ComboBox<KataDirectory> directoryBox;
    private final DefaultComboBoxModel<KataDirectory> model;
    private final JButton selectorButton = new JButton("Setup Kata");
    private final JButton deleteButton = new JButton("Delete Kata");
    private final JPanel innerPanel = new JPanel(new GridBagLayout());
    private final JPanel buttonPanel = new JPanel(new FlowLayout());
    private final JLabel title = new JLabel("", null, JLabel.CENTER);
    private final Store store;
    private final Manager manager;
    private ArrayList<KataDirectory> kataDirectoryList;
    private final KataDirectoryParser parser;


    public KataSelectorPanel(Store store, Manager manager){
        this.store = store;
        parser = new KataDirectoryParser(store.getProject());
        this.manager = manager;
        kataDirectoryList = parser.getDirectoryList();
        removeCurrentDirectory();
        model = new DefaultComboBoxModel<>(kataDirectoryList.toArray(new KataDirectory[0]));
        directoryBox = new ComboBox<>(model);

        if(kataDirectoryList == null || kataDirectoryList.size() < 1) {
            return;
        }
        initComponents();
    }

    private void initComponents() {
        directoryBox.setRenderer(new KataDirectoryRenderer());
        setLayout(new BorderLayout());


        var constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.CENTER;

        constraints.gridx = 0;
        constraints.gridy = 0;
        title.setText(kataDirectoryList.size() > 0 ? "Select Kata From The Current Project" : "Current Project Contains No Previously Started Katas");
        innerPanel.add(title, constraints);


        constraints.gridx = 0;
        constraints.gridy = 2;

        innerPanel.add(directoryBox, constraints);


        buttonPanel.add(selectorButton);
        buttonPanel.add(deleteButton);

        constraints.gridx = 0;
        constraints.gridy = 3;

        innerPanel.add(buttonPanel, constraints);

        add(innerPanel, BorderLayout.CENTER);
        addSelectorListeners();
    }

    private void addSelectorListeners() {
        selectorButton.addActionListener((event) -> {
            var directory = directoryBox.getSelectedItem();
            if (directory == null) {
                return;
            }
            manager.getShouldFetchAndCreateFilesOnUrlLoad().set(false);
            manager.loadWorkspaceComponent((KataDirectory) directory, true);
        });

        deleteButton.addActionListener((event) -> {
            var directory = (KataDirectory) directoryBox.getSelectedItem();
            if (directory == null) {
                return;
            }
            int result = Messages.showOkCancelDialog("All \"" + directory.getRecord().getName() + "\" kata files will be deleted!", "Confirm Delete Kata Files", "Ok", "Cancel", IconLoader.getIcon(MESSAGE_ICON, SidePanel.class));
            if (result == 0) {
                new FileManager().deleteFiles(directory, store.getProject());
                kataDirectoryList = parser.getDirectoryList();
                removeCurrentDirectory();
                if (kataDirectoryList.size() < 1) {
                    Box.Filler filler = new Box.Filler(innerPanel.getPreferredSize(), new Dimension(2000, 2000), new Dimension(2000, 2000));
                    removeAll();
                    add(filler);
                    revalidate();
                    repaint();
                    return;
                }
                model.removeAllElements();
                model.addAll(kataDirectoryList);
                if (model.getSize() > 0) {
                    directoryBox.setSelectedIndex(0);
                }
                title.setText("Select Kata From The Current Project");
                revalidate();
                repaint();
            }
        });
    }

    private void removeCurrentDirectory() {
        kataDirectoryList.removeIf(directory -> directory.isTheSame(store.getDirectory()));
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(5000, 5000);
    }
}
