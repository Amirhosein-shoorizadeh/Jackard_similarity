import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class LCSComparator {
    // caculate jakard
    public static double jaccardSimilarity(String sent1, String sent2) {
        Set<String> set1 = new HashSet<>(Arrays.asList(sent1.split(" ")));
        Set<String> set2 = new HashSet<>(Arrays.asList(sent2.split(" ")));
        if (set1.isEmpty() || set2.isEmpty()) return 0.0;
        int intersection = 0;
        for (String w : set1) if (set2.contains(w)) intersection++;
        int union = set1.size() + set2.size() - intersection;
        return (double) intersection / union;
    }

    //for matching
    static class MatchPair {
        int idx1, idx2;
        String sent1, sent2;
        double similarity;

        MatchPair(int i1, int i2, String s1, String s2, double sim) {
            this.idx1 = i1;
            this.idx2 = i2;
            this.sent1 = s1;
            this.sent2 = s2;
            this.similarity = sim;
        }
    }

    public static List<MatchPair> lcsWithThreshold(List<String> sents1, List<String> sents2, double threshold) {
        int m = sents1.size(), n = sents2.size();
        if (m == 0 || n == 0) return new ArrayList<>();
        double[][] simCache = new double[m][n];
        // main for for ist sent fron 1st doc and jst sent from sec doc
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                simCache[i][j] = jaccardSimilarity(sents1.get(i), sents2.get(j));
            }
        }
        byte[][] dir = new byte[m + 1][n + 1];
        int[] dpPrev = new int[n + 1];
        int[] dpCurr = new int[n + 1];
        // main for , lcs ,dp
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (simCache[i - 1][j - 1] >= threshold) {
                    dpCurr[j] = dpPrev[j - 1] + 1;
                    dir[i][j] = 1;
                } else if (dpPrev[j] >= dpCurr[j - 1]) {
                    dpCurr[j] = dpPrev[j];
                    dir[i][j] = 2;
                } else {
                    dpCurr[j] = dpCurr[j - 1];
                    dir[i][j] = 3;
                }
            }
            int[] t = dpPrev;
            dpPrev = dpCurr;
            dpCurr = t;
            Arrays.fill(dpCurr, 0);
        }
        // back tracking for path
        List<MatchPair> result = new ArrayList<>();
        int i = m, j = n;
        while (i > 0 && j > 0) {
            if (dir[i][j] == 1) {
                result.add(new MatchPair(i - 1, j - 1, sents1.get(i - 1), sents2.get(j - 1), simCache[i - 1][j - 1]));
                i--;
                j--;
            } else if (dir[i][j] == 2) i--;
            else j--;
        }
        Collections.reverse(result);
        return result;
    }
    // finding top 10 similar sent
    public static List<MatchPair> topKSimilarPairs(List<String> sents1, List<String> sents2, int k) {
        List<MatchPair> allPairs = new ArrayList<>();
        for (int i = 0; i < sents1.size(); i++) {
            for (int j = 0; j < sents2.size(); j++) {
                double sim = jaccardSimilarity(sents1.get(i), sents2.get(j));
                allPairs.add(new MatchPair(i, j, sents1.get(i), sents2.get(j), sim));
            }
        }
        allPairs.sort((a, b) -> Double.compare(b.similarity, a.similarity));
        return allPairs.stream().limit(k).collect(Collectors.toList());
    }

    // tokenized folder
    public static Path findTokenizedFolder() {
        Path current = Paths.get(".");
        Path tokenizedCurrent = current.resolve("Tokenized");
        if (Files.exists(tokenizedCurrent) && Files.isDirectory(tokenizedCurrent))
            return tokenizedCurrent;
        Path parent = current.getParent();
        if (parent != null) {
            Path tokenizedParent = parent.resolve("Tokenized");
            if (Files.exists(tokenizedParent) && Files.isDirectory(tokenizedParent))
                return tokenizedParent;
        }
        return null;
    }

    // Saving
    public static void saveResultsToFile(String fileName, String content) throws IOException {
        Path resultDir = Paths.get("SimilarityResults");
        if (!Files.exists(resultDir)) Files.createDirectories(resultDir);
        Path filePath = resultDir.resolve(fileName);
        Files.writeString(filePath, content, StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws IOException {
        //finding folder
        Path tokenizedFolder = findTokenizedFolder();
        if (tokenizedFolder == null) {
            System.err.println("Tokenized folder not found.");
            return;
        }
        System.out.println("Tokenized folder found : " + tokenizedFolder.toAbsolutePath());
        List<Path> tokenizedFiles;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tokenizedFolder, "*_tokenized.txt")) {
            tokenizedFiles = new ArrayList<>();
            for (Path p : stream) tokenizedFiles.add(p);
        }
        if (tokenizedFiles.size() < 2) {
            System.out.println("Need at least two tokenized files.");
            return;
        }

        double threshold = 0.6;
        int topK = 10;

        for (int a = 0; a < tokenizedFiles.size(); a++) {
            for (int b = a + 1; b < tokenizedFiles.size(); b++) {
                Path fileA = tokenizedFiles.get(a);
                Path fileB = tokenizedFiles.get(b);
                System.out.println("\n========================================");
                System.out.println("Cmp: " + fileA.getFileName() + "  vs  " + fileB.getFileName());

                List<String> sentsA = Files.readAllLines(fileA, StandardCharsets.UTF_8);
                List<String> sentsB = Files.readAllLines(fileB, StandardCharsets.UTF_8);
                System.out.println("Sent num 1: " + sentsA.size());
                System.out.println("sent num 2: " + sentsB.size());

                // LCS
                long startLCS = System.currentTimeMillis();
                List<MatchPair> lcsMatches = lcsWithThreshold(sentsA, sentsB, threshold);
                long timeLCS = System.currentTimeMillis() - startLCS;
                System.out.println("\n[LCS] Similar sent : " + lcsMatches.size() + " (Time : " + timeLCS + " ms)");
                System.out.println("Samples ( 5 ):");
                for (int i = 0; i < Math.min(5, lcsMatches.size()); i++) {
                    MatchPair p = lcsMatches.get(i);
                    System.out.printf("  example  %d (Similarity = %.3f)\n    %s\n    %s\n", i + 1, p.similarity, p.sent1, p.sent2);
                }

                //Top k
                long startTop = System.currentTimeMillis();
                List<MatchPair> topPairs = topKSimilarPairs(sentsA, sentsB, topK);
                long timeTop = System.currentTimeMillis() - startTop;
                System.out.println("\n[Top " + topK + "] whith high similarity  ( Time : " + timeTop + " ms):");
                for (int i = 0; i < topPairs.size(); i++) {
                    MatchPair p = topPairs.get(i);
                    System.out.printf("  Rank %d (Similarity = %.4f)\n    %s\n    %s\n", i + 1, p.similarity, p.sent1, p.sent2);
                }

                // Overall similarity
                double lcsPercent = (double) lcsMatches.size() / Math.min(sentsA.size(), sentsB.size()) * 100;
                double avgTopSim = topPairs.stream().mapToDouble(p -> p.similarity).average().orElse(0);

                //
                boolean overallSimilar = (lcsPercent >= 15.0) ;

                System.out.println("\n===  Overall similarity  ===");
                System.out.printf(" Overall similarity : %.2f%%\n", lcsPercent);
                if (overallSimilar) {
                    System.out.println("➡ (similar).");
                } else {
                    System.out.println("➡ (nonsimilar) .");
                }

                // Save result
                StringBuilder sb = new StringBuilder();
                sb.append("Cmp: ").append(fileA.getFileName()).append(" vs ").append(fileB.getFileName()).append("\n");
                sb.append("num sent of 1doc: ").append(sentsA.size()).append("\n");
                sb.append("num sent of 2doc: ").append(sentsB.size()).append("\n\n");
                sb.append("[LCS] : ").append(lcsMatches.size()).append("\n");
                sb.append("Example:\n");
                for (int i = 0; i < Math.min(5, lcsMatches.size()); i++) {
                    MatchPair p = lcsMatches.get(i);
                    sb.append(String.format("  Example %d (شباهت=%.3f)\n    %s\n    %s\n", i + 1, p.similarity, p.sent1, p.sent2));
                }
                sb.append("\n[Top ").append(topK).append("] With high similarity:\n");
                for (int i = 0; i < topPairs.size(); i++) {
                    MatchPair p = topPairs.get(i);
                    sb.append(String.format("Rank %d (Similar=%.4f)\n  %s\n  %s\n", i + 1, p.similarity, p.sent1, p.sent2));
                }
                sb.append("\n=== overall similarity ===\n");
                sb.append(String.format(" percent of similar sent  : %.2f%%\n", lcsPercent));
                sb.append(overallSimilar ? "similar" : "nonsimilar\n");
                String resultFileName = fileA.getFileName().toString().replace("_tokenized.txt", "")
                        + "_vs_" + fileB.getFileName().toString().replace("_tokenized.txt", "") + "_report.txt";
                saveResultsToFile(resultFileName, sb.toString());
            }
        }
        System.out.println("\n SimilarityResults seved .");
    }
}

