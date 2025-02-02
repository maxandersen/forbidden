///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.6.3
//DEPS de.thetaphi:forbiddenapis:3.8

import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "forbidden", mixinStandardHelpOptions = true, version = "forbidden 0.1", description = "forbidden made with jbang")
class forbidden implements Callable<Integer> {

    @Option(names = { "-d", "--dir" }, description = "directory with class files to check")
    private Path dir;

    @Parameters(description = "everything else")
    private List<String> params = new ArrayList<>();

    public static void main(String... args) {
        int exitCode = new CommandLine(new forbidden()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception { // your business logic goes here...
       
        if(exists(dir) && !isDirectory(dir) && dir.getFileName().toString().endsWith(".jar")) {
             try {
        // Create a temporary directory
        Path tempDir = Files.createTempDirectory("jar_extract_");
        tempDir.toFile().deleteOnExit();
        System.out.println("Directory is a jar - temporary extracting to " + tempDir);

        // Extract JAR contents
        try (JarFile jarFile = new JarFile(dir.toFile())) {
          Enumeration<JarEntry> entries = jarFile.entries();
          while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            Path entryDestination = tempDir.resolve(entry.getName());

            if (entry.isDirectory()) {
              Files.createDirectories(entryDestination);
            } else {
              Files.createDirectories(entryDestination.getParent());
              try (InputStream in = jarFile.getInputStream(entry);
                   OutputStream out = Files.newOutputStream(entryDestination)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) > 0) {
                  out.write(buffer, 0, len);
                }
              }
            }
            entryDestination.toFile().deleteOnExit();
          }
        }
        dir = tempDir.toAbsolutePath();
      } catch (IOException e) {
      throw new IllegalStateException("Could not unpack jar file: " + e);
    }
        }
        
        List<String> args = new ArrayList<>();
        args.add("-d");
        args.add(dir.toString());
        args.addAll(params);

        de.thetaphi.forbiddenapis.cli.CliMain.main(args.toArray(new String[0]));

        return 0;
    }
}
