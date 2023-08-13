package com.example.codewarsplugin.services.files.create;

import com.example.codewarsplugin.SidePanel;
import com.example.codewarsplugin.exceptions.ModuleNotFoundException;
import com.example.codewarsplugin.exceptions.SourcesRootNotFoundException;
import com.example.codewarsplugin.models.kata.JsonSource;
import com.example.codewarsplugin.models.kata.KataDirectory;
import com.example.codewarsplugin.models.kata.KataInput;
import com.example.codewarsplugin.models.kata.KataRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.codewarsplugin.config.StringConstants.*;

public abstract class AbstractFileService {

    final KataInput input;
    final KataRecord record;
    final Project project;
    VirtualFile directory;
    VirtualFile testDirectory;
    VirtualFile workDirectory;
    VirtualFile sourcesRoot;
    VirtualFile metaData;
    VirtualFile testFile;
    VirtualFile workFile;
    VirtualFile inputFile;
    VirtualFile recordFile;
    ArrayList<VirtualFile> sourcesRoots = new ArrayList<>();
    ArrayList<Module> modules = new ArrayList<>();

    public AbstractFileService(KataInput input, KataRecord record, Project project){
        this.input = input;
        this.record = record;
        this.project = project;
    }

    public void createDirectory() throws IOException {

        VirtualFile newDirectory = sourcesRoot.createChildDirectory(this, getDirectoryName());
        newDirectory.refresh(false, true);
        VirtualFile metaData = newDirectory.createChildDirectory(this, "metadata");
        metaData.refresh(false, true);
        directory = newDirectory;
        this.metaData = metaData;
    }

    public KataDirectory createKataDirectory() {

        KataDirectory kataDirectory = new KataDirectory();
        kataDirectory.setWorkFile(workFile);
        kataDirectory.setTestFile(testFile);
        kataDirectory.setInput(input);
        kataDirectory.setRecord(record);
        kataDirectory.setDirectory(directory);
        kataDirectory.setMetaDataDirectory(metaData);
        kataDirectory.setWorkDirectory(workDirectory);
        kataDirectory.setTestDirectory(testDirectory);

        return kataDirectory;
    }

    public void createRecordFile() throws IOException {
        createJsonFile(record);
    }

    public void createInputFile() throws IOException {
        createJsonFile(input);
    }

    public void updateRecord(KataDirectory dir) throws IOException {
        dir.getRecordFile().setBinaryContent(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(dir.getRecord()).getBytes(StandardCharsets.UTF_8));
    }

    public void createJsonFile(JsonSource source) throws IOException {

        String filename = source instanceof KataRecord? getRecordFileName() : getInputFileName();

        if (metaData != null) {
            VirtualFile file = null;
            file = metaData.createChildData(this, filename);
            file.refresh(false, true);
            VirtualFile finalFile = file;
            finalFile.setBinaryContent(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(source).getBytes(StandardCharsets.UTF_8));
            if (source instanceof KataRecord) {
                recordFile = finalFile;
            } else {
                inputFile = recordFile;
            }
        }
    }

    public void createWorkFile() throws IOException {
        createFile(true);
    }

    public void createTestFile() throws IOException {
        createFile(false);
    }

    public void createFile(boolean isWorkFile) throws IOException {

        VirtualFile directory = isWorkFile? workDirectory : testDirectory;
        
        if (directory != null) {
            VirtualFile file = null;
            file = directory.createChildData(this, isWorkFile? getFileName() : getTestFileName());
            file.refresh(false, true);
            file.setBinaryContent(getFileContent(isWorkFile));

            if(isWorkFile) {
                workFile = file;
            } else {
                testFile = file;
            }
        }
    }

    public void getModules(String language) {
        ModuleManager moduleManager = ModuleManager.getInstance(project);
        for (Module module : moduleManager.getModules()) {
            ModuleType<?> moduleType = ModuleType.get(module);
            System.out.println("module name: " + moduleType.getName());
            if (shouldAddModule(moduleType, language)) {
                modules.add(module);
            }
        }
        if (modules.size() < 1) {
            if (language.equals("javascript")){
                throw new ModuleNotFoundException(JS_MODULE_NOT_FOUND);
            } else {
                throw new ModuleNotFoundException(MessageFormat.format(MODULE_NOT_FOUND, language));
            }
        }
    }

    private boolean shouldAddModule(@NotNull ModuleType<?> moduleType, String language) {
        return (moduleType.getName().toLowerCase().contains(language) && !moduleType.getName().toLowerCase().contains("unknown")) ||
                (moduleType.getName().toLowerCase().contains("web") && language.equals("javascript"));
    }

    public Module pickModule() {
        if (modules.size() == 0){
            throw new RuntimeException("Modules array empty!");
        } else if (modules.size() == 1) {
            return modules.get(0);
        } else {
            AtomicInteger index = new AtomicInteger(0);
            ApplicationManager.getApplication().invokeAndWait(() -> {
                index.set(Messages.showIdeaMessageDialog(project, String.format(SEVERAL_MODULES, input.getLanguageName()), String.format(PICK_MODULE, input.getLanguageName()), modules.stream().map(Module::getName).toArray(String[]::new), 0, IconLoader.getIcon(MESSAGE_ICON, SidePanel.class), null));
            });
            return modules.get(index.get());
        }
    }

    public void getSourcesRoot() {

        Module module = pickModule();

        ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
        VirtualFile[] roots = this instanceof JavaFileService? moduleRootManager.getSourceRoots(false) : moduleRootManager.getContentRoots();

        System.out.println("roots size: " + roots.length);
        Arrays.stream(roots).filter(root -> !root.getName().equals("resources")).forEach(sourcesRoots::add);

        System.out.println("list size: " + sourcesRoots.size());
        if (sourcesRoots.size() == 1) {
            this.sourcesRoot = sourcesRoots.get(0);
        } else if (sourcesRoots.size() > 1) {
            AtomicInteger index = new AtomicInteger(0);
            ApplicationManager.getApplication().invokeAndWait(() -> {
                index.set(Messages.showIdeaMessageDialog(project, SEVERAL_SOURCES, PICK_SOURCE, sourcesRoots.stream().map(VirtualFile::getName).toArray(String[]::new), 0, IconLoader.getIcon(MESSAGE_ICON, SidePanel.class), null));
            });
            this.sourcesRoot = sourcesRoots.get(index.get());
        } else {
            throw new SourcesRootNotFoundException("Sources root directory not found in the current java module. Create sources root and try again!");
        }
    }

    public abstract String getTestFileName();
    public abstract String getFileName();
    protected abstract String getRecordFileName();
    protected abstract String getInputFileName();
    protected abstract String getDirectoryName();
    protected abstract byte[] getFileContent(boolean isWorkFile);
    public abstract void initDirectory();
}
