package com.example.codewarsplugin.services.files.parse;

import com.example.codewarsplugin.models.kata.KataDirectory;
import com.example.codewarsplugin.models.kata.KataInput;
import com.example.codewarsplugin.models.kata.KataRecord;
import com.example.codewarsplugin.services.files.create.FileService;
import com.example.codewarsplugin.services.files.create.FileServiceFactory;
import com.example.codewarsplugin.services.project.MyProjectManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;


import java.util.ArrayList;
import java.util.List;

import static com.example.codewarsplugin.services.files.create.AbstractFileService.isSourcesRoot;

public class KataDirectoryParser {

    private Project project;
    private List<VirtualFile> sourcesRoots;
    private ArrayList<KataDirectory> directoryList;

    public KataDirectoryParser(Project project) {
        this.project = project;
    }

    public void parseSourceDirectories() {
        sourcesRoots = new ArrayList<>();
        directoryList = new ArrayList<>();
        getSourcesRoots(project.getBaseDir());
        sourcesRoots.forEach(this::processSourcesRoot);
    }

    private void processSourcesRoot(VirtualFile directory) {

        if (directory != null && directory.isDirectory()) {
            for (VirtualFile child : directory.getChildren()) {
                if (child.isDirectory()) {
                    if (child.getName().startsWith("codewars.")){
                        KataDirectory kataDirectory = new KataDirectory();
                        kataDirectory.setDirectory(child);
                        for (VirtualFile grandchild : child.getChildren()) {
                            if(grandchild.getName().equals("metadata")){
                                kataDirectory.setMetaDataDirectory(grandchild);
                            }
                        }
                        fillDirectoryWithFiles(kataDirectory);
                        if(kataDirectory.isComplete()){
                            directoryList.add(kataDirectory);
                        }
                    }
                }
            }
        }
    }

    private void fillDirectoryWithFiles(KataDirectory kataDirectory) {

        KataRecord record = null;
        KataInput input = null;
        ObjectMapper mapper = new ObjectMapper();

        for(VirtualFile metafile : kataDirectory.getMetaDataDirectory().getChildren()){
            if (!metafile.isDirectory() && metafile.getName().contains("Record.json")){
                try {
                    record = mapper.readValue(metafile.contentsToByteArray(), KataRecord.class);
                    kataDirectory.setRecord(record);
                } catch (Exception e){ System.out.println("exception record parsing failed"); e.printStackTrace();}
            } else if (!metafile.isDirectory() && metafile.getName().contains("Input.json")){
                try {
                    input = mapper.readValue(metafile.contentsToByteArray(), KataInput.class);
                    kataDirectory.setInput(input);
                } catch (Exception e){System.out.println("exception input parsing failed"); e.printStackTrace();}
            }
        }
        if(record == null || input == null) {
            return;
        }

        FileService fileService = FileServiceFactory.createFileService(input, record, project);
        String testFileName = fileService.getTestFileName();
        String workFileName = fileService.getFileName();

        for(VirtualFile file : kataDirectory.getDirectory().getChildren()){
            if (!file.isDirectory() && file.getName().equals(testFileName)){
                kataDirectory.setTestFile(file);
            } else if (!file.isDirectory() && file.getName().equals(workFileName)) {
                kataDirectory.setWorkFile(file);
            }
        }
    }

    public void getSourcesRoots(VirtualFile baseDir) {
        if (baseDir != null && baseDir.isDirectory()) {
            VirtualFile[] children = baseDir.getChildren();
            for (VirtualFile child : children) {
                if (child.isDirectory()) {
                    if (isSourcesRoot(child, project)) {
                        sourcesRoots.add(child);
                    } else if (sourcesRoots.size() < 1) {
                        getSourcesRoots(child);
                    }
                }
            }
        }
    }

    public ArrayList<KataDirectory> getDirectoryList() {
        parseSourceDirectories();
        return directoryList;
    }

    public void add(KataDirectory directory) {
        directoryList.add(directory);
    }
}
