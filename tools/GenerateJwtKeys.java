import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyPairGenerator;

/** Run once with Java 25: java tools/GenerateJwtKeys.java <private-directory>. Existing keys are never overwritten. */
public class GenerateJwtKeys {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Укажите каталог для постоянных серверных ключей.");
        }
        var directory = Path.of(args[0]).toAbsolutePath().normalize();
        Files.createDirectories(directory);
        var privateFile = directory.resolve("private-key.der");
        var publicFile = directory.resolve("public-key.der");
        if (Files.exists(privateFile) || Files.exists(publicFile)) {
            throw new IllegalStateException("Ключи уже существуют; автоматическая перезапись запрещена.");
        }
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(3072);
        var pair = generator.generateKeyPair();
        Files.write(privateFile, pair.getPrivate().getEncoded(), StandardOpenOption.CREATE_NEW);
        Files.write(publicFile, pair.getPublic().getEncoded(), StandardOpenOption.CREATE_NEW);
        System.out.println("Серверные ключи созданы. Настройте доступ к приватному файлу только для учётной записи backend.");
    }
}
