package ltr.index;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ltr.fileutils.FileExtraction;

/**
 * Particionador do processamento em mini-batches
 */
public class Partitioner implements Iterable<List<File>> {
    private final String corpus;
    private final String prefixFilter;
    private final Integer chunkSize;
    private Integer numberBlocks;
    private String extension;
    private final Map<Integer, List<File>> blocks;
    
    public Partitioner(String corpus, String prefixFilter, int chunkSize, String extension) throws FileNotFoundException {
        this.corpus = corpus;
        this.chunkSize = chunkSize;
        this.blocks = new HashMap<>();
        this.numberBlocks = 0;
        this.prefixFilter = prefixFilter;
        this.extension = extension;
        this.run();
    }

    public Partitioner(String corpus, String prefixFilter, int chunkSize) throws FileNotFoundException {
        this(corpus, prefixFilter, chunkSize, ".xml");
    }

    public Map<Integer, List<File>> concurrentMap() {
        return Collections.synchronizedMap(this.blocks);
    }

    public Integer getNumberBlocks() {
        return numberBlocks;
    }

    private void run() throws FileNotFoundException {
        List<File> docs = FileExtraction.getAllFiles(new File(this.corpus), this.prefixFilter, this.extension);

        if(docs.size() == 0) throw new FileNotFoundException("The corpus need at least one file!");

        this.numberBlocks = (int) Math.ceil((double) docs.size() / (double) this.chunkSize);

        for(int i = 0; i < this.numberBlocks; i++) {

            int arrayPosition = i * this.chunkSize;
            int length = Math.min(docs.size() - arrayPosition, chunkSize);

            List<File> chunk = new ArrayList<File>(docs.subList(arrayPosition, arrayPosition+length));
            this.blocks.put(i, chunk);
        }
    }

    public List<File> getAllFiles() {
        return FileExtraction.getAllFiles(new File(this.corpus), this.prefixFilter, ".xml");
    }

    public Iterator<List<File>> iterator() {
        return new PartitionerIterator(this.blocks);
    }

	public String getExtension() {
		return extension;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	} 
}

class PartitionerIterator implements Iterator<List<File>> {
    private final Map<Integer, List<File>> blocks;
    private int currentNode = 0;

    PartitionerIterator(Map<Integer, List<File>> blocks) {
        this.blocks = new HashMap<>(blocks);
    }

    public boolean hasNext() {
        return this.blocks.size() != currentNode;
    }

    public List<File> next() {
        List<File> nextNode = this.blocks.get(this.currentNode);
        this.currentNode++;
        return nextNode;
    }

    public void remove() {}    

}

