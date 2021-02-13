package ltr.parser;

import java.io.File;
import java.util.List;

public interface Parser<T> {
    /**
     * Transforma um conjunto de arquivos em objetos T.
     * @param files
     * @return
     * @throws Exception
     */
    List<T> parse(List<File> files) throws Exception;
}
