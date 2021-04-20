package ltr.collections.jrc.extractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ltr.fileutils.FileExtraction;
import ltr.index.Partitioner;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static ltr.settings.Config.configFile;

// divide os dados para treino/teste e dados de avaliação
public class EuroVocSplitData {
    public void split(String corpus, String mlDir, String evalDir) throws IOException {
        String prefixFilter = "jrc";
        double splitRatio = 0.7;

        int chunkSize = (int) (FileExtraction.getAllFiles(new File(corpus), prefixFilter, ".xml").size() * splitRatio);

        Partitioner partitioner = new Partitioner(corpus, prefixFilter, chunkSize);

        Iterator<List<File>> blocksIter = partitioner.iterator();

        new File(mlDir).mkdirs();
        new File(evalDir).mkdirs();

        for( File mlFile : blocksIter.next()) {
            File f = new File(mlDir + "/" + mlFile.getName());
            f.createNewFile();
            Files.copy(mlFile.toPath(), f.toPath(), REPLACE_EXISTING);
        }

        for( File evalFile: blocksIter.next()) {
            File f = new File(evalDir + "/" + evalFile.getName());
            f.createNewFile();
            Files.copy(evalFile.toPath(), f.toPath(), REPLACE_EXISTING);
        }

    }
    public static void main(String[] args) throws IOException {
        EuroVocSplitData splitter = new EuroVocSplitData();
        splitter.split(
                configFile.getProperty("CORPUS_JRC"),
                configFile.getProperty("TRAIN_JRC"),
                configFile.getProperty("TEST_JRC")
        );
    }
}


