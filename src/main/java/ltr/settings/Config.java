package ltr.settings;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Responsável por linkar o projeto com as variaveis de ambiente configuradas em
 * Config.properties.
 */
public class Config {
    public static Properties configFile = new Properties();

    static{
        try {
            InputStream stream = Config.class.getResourceAsStream("/Config.properties");
            configFile.load(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
