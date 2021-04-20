package ltr.fileutils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.comparator.NameFileComparator;

public class FileExtraction {
    public static List<File> getAllFiles(File mainFile, String prefix, String extension) {

        File[] fileList = mainFile.listFiles();

        assert fileList != null;

        Arrays.sort(fileList, NameFileComparator.NAME_COMPARATOR);

        List<File> docs = new ArrayList<File>();

        for(File file: fileList){

            if(file.isDirectory()){
                docs.addAll(getAllFiles(file, prefix, extension));
            }
            else{
                if(prefix.isEmpty() || ( file.getName().startsWith(prefix) && file.getName().endsWith(extension)) )
                    docs.add(file);
            }
        }

        return docs;
    }
}
