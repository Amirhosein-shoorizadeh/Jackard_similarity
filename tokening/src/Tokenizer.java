import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

public class Tokenizer {
    // sent spliter
    private static final Pattern SENTENCE_SPLITTER = Pattern.compile("[.]\\s*");
    // pronun deleter
    private static final Pattern PUNCTUATION = Pattern.compile("[،؛؟!»«\":;.,!?()\\[\\]{}]");
    // Persian stop words
    private static final Set<String> FA_STOP_WORDS = new HashSet<>(Arrays.asList("و", "به", "از", "در", "یک", "را", "با", "برای", "تا", "بر", "این", "آن", "که", "نیز", "هم", "ای", "است", "شد", "شود", "می", "خود", "های"));
    // English stop words
    private static final Set<String> EN_STOP_WORDS = new HashSet<>(Arrays.asList("a", "an", "and", "the", "of", "to", "in", "for", "on", "with", "by", "is", "are", "was", "were", "be", "been", "being", "that", "this"));

    public static List<String> tokenizeText(String text, String language) {
        //spliting text
        String[] rawSentences = SENTENCE_SPLITTER.split(text);
        List<String> result = new ArrayList<>();
        // languege choosing
        Set<String> stopWords = language.equals("fa") ? FA_STOP_WORDS : EN_STOP_WORDS;
        // tokening words
        for (String sent : rawSentences) {
            sent = sent.trim();
            if (sent.isEmpty()) continue;
            sent = PUNCTUATION.matcher(sent).replaceAll(" ");
            if (language.equals("en")) sent = sent.toLowerCase();
            String[] words = sent.split("\\s+");
            StringBuilder cleanLine = new StringBuilder();
            //tokening
            for (String w : words) {
                w = w.trim();
                if (w.isEmpty()) continue;
                if (!stopWords.contains(w) && w.length() > 1) {
                    if (cleanLine.length() > 0) cleanLine.append(" ");
                    cleanLine.append(w);
                }
            }
            if (cleanLine.length() > 0) result.add(cleanLine.toString());
        }
        return result;
    }

    public static void tokenizeFolder(String inputFolderPath, String language) throws IOException {
        Path inputDir = Paths.get(inputFolderPath);
        // geting result folder pass
        Path outputDir = Paths.get("Tokenized");
        // deleting if didnt exist
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
            System.out.println("tokenized folder created");
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputDir, "*.txt")) {
            int fileCount = 0;
            for (Path entry : stream) {
                String text = new String(Files.readAllBytes(entry), StandardCharsets.UTF_8);
                List<String> tokenized = tokenizeText(text, language);
                String outFileName = entry.getFileName().toString().replace(".txt", "_tokenized.txt");
                Path outPath = outputDir.resolve(outFileName);
                Files.write(outPath, tokenized, StandardCharsets.UTF_8);
                System.out.println("tokenized " + entry.getFileName() + " -> " + outPath + " (" + tokenized.size() + " SENT)");
                fileCount++;
            }
            if (fileCount == 0) {
                System.out.println("nothing found.");
            } else {
                System.out.println("  process  " + fileCount + "  end  & seved in tkenized folder");
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.print(" file path :");
        String inputFolder = scanner.nextLine().trim();
        //if user didnt submit anything use all thing in directory
        if (inputFolder.isEmpty()) {
            inputFolder = ".";
            System.out.println("using code directory " + inputFolder);
        }
        System.out.print("languege (fa/en): ");
        String lang = scanner.nextLine().trim().toLowerCase();
        while (!lang.equals("fa") && !lang.equals("en")) {
            System.out.print("please enter correct languege (fa/en): ");
            lang = scanner.nextLine().trim().toLowerCase();
        }
        scanner.close();
        tokenizeFolder(inputFolder, lang);
    }
}