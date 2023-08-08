package com.example.codewarsplugin.services.files.create;

import com.example.codewarsplugin.SidePanel;
import com.example.codewarsplugin.exceptions.ModuleNotFoundException;
import com.example.codewarsplugin.exceptions.SourcesRootNotFoundException;
import com.example.codewarsplugin.models.kata.KataDirectory;
import com.example.codewarsplugin.models.kata.KataInput;
import com.example.codewarsplugin.models.kata.KataRecord;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.codewarsplugin.config.StringConstants.*;

public abstract class AbstractFileService implements FileService {

    final KataInput input;
    final KataRecord record;
    final Project project;
    VirtualFile baseDir;
    VirtualFile directory;
    VirtualFile testDirectory;
    VirtualFile workDirectory;
    VirtualFile sourcesRoot;
    VirtualFile metaData;
    VirtualFile testFile;
    VirtualFile workFile;
    ArrayList<VirtualFile> sourcesRoots = new ArrayList<>();
    ArrayList<Module> modules = new ArrayList<>();

    public AbstractFileService(KataInput input, KataRecord record, Project project){
        this.input = input;
        this.record = record;
        this.project = project;
    }

    @Override
    public void createDirectory() throws IOException {

        VirtualFile newDirectory = sourcesRoot.createChildDirectory(this, getDirectoryName());
        newDirectory.refresh(false, true);
        VirtualFile metaData = newDirectory.createChildDirectory(this, "metadata");
        metaData.refresh(false, true);
        directory = newDirectory;
        this.metaData = metaData;
    }


    public static boolean isSourcesRoot(VirtualFile directory, Project project) {
        Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
            ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
            for (VirtualFile sourceRoot : rootManager.getSourceRoots()) {
                if (VfsUtil.isAncestor(sourceRoot, directory, false)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
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

    @Override
    public void createWorkFile() throws IOException {
        createFile(true);
    }

    @Override
    public void createTestFile() throws IOException {
        createFile(false);
    }

    @Override
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

    @Override
    public void getModules(String language) {
        ModuleManager moduleManager = ModuleManager.getInstance(project);
        for (Module module : moduleManager.getModules()) {
            ModuleType<?> moduleType = ModuleType.get(module);
            System.out.println("module name: " + moduleType.getName());
            if (moduleType.getName().toLowerCase().contains(language) && !moduleType.getName().toLowerCase().contains("unknown")) {
                modules.add(module);
            }
        }
        if (modules.size() < 1) {
            throw new ModuleNotFoundException(MessageFormat.format(MODULE_NOT_FOUND, language));
        }
    }

    @Override
    public Module pickModule() {
        if (modules.size() == 0){
            throw new RuntimeException("Modules array empty!");
        } else if (modules.size() == 1) {
            return modules.get(0);
        } else {
            AtomicInteger index = new AtomicInteger(0);
            ApplicationManager.getApplication().invokeAndWait(() -> {
                index.set(Messages.showIdeaMessageDialog(project, String.format(SEVERAL_MODULES, input.getLanguageName()), String.format(PICK_MODULE, input.getLanguageName()), modules.stream().map(Module::getName).toArray(String[]::new), 0, IconLoader.getIcon("/icons/new_cw_logo.svg", SidePanel.class), null));
            });
            return modules.get(index.get());
        }
    }

    @Override
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
                index.set(Messages.showIdeaMessageDialog(project, SEVERAL_SOURCES, PICK_SOURCE, sourcesRoots.stream().map(VirtualFile::getName).toArray(String[]::new), 0, IconLoader.getIcon("/icons/new_cw_logo.svg", SidePanel.class), null));
            });
            this.sourcesRoot = sourcesRoots.get(index.get());
        } else {
            throw new SourcesRootNotFoundException("Sources root directory not found in the current java module. Create sources root and try again!");
        }
    }

    protected abstract String getRecordFileName();

    protected abstract String getInputFileName();
}
