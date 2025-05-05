import java.io.*;
import java.util.*;

public class WordGraphAnalyzer {
    // 有向图使用邻接表表示：每个单词对应其出边及权重
    private Map<String, Map<String, Integer>> graph = new HashMap<>();
    private Random rand = new Random();

    public static void main(String[] args) {
        WordGraphAnalyzer analyzer = new WordGraphAnalyzer();
        Scanner scanner = new Scanner(System.in);
        String filePath;
        if (args.length > 0) {
            filePath = args[0];
        } else {
            System.out.println("请输入文本文件路径：");
            filePath = scanner.nextLine().trim();
        }
        // 读取文件并构建图
        try {
            analyzer.readFile(filePath);
        } catch (IOException e) {
            System.err.println("读取文件出错：" + e.getMessage());
            return;
        }
        // 命令行菜单
        while (true) {
            System.out.println("\n请选择功能：");
            System.out.println("1. 显示图结构");
            System.out.println("2. 查询桥接词");
            System.out.println("3. 生成新文本");
            System.out.println("4. 计算最短路径");
            System.out.println("5. 查询PageRank值");
            System.out.println("6. 随机游走");
            System.out.println("0. 退出");
            System.out.print("输入选项：");
            int choice = -1;
            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("无效输入，请输入数字。");
                continue;
            }
            if (choice == 0) {
                System.out.println("退出程序。");
                break;
            }
            switch (choice) {
                case 1:
                    analyzer.showDirectedGraph();
                    break;
                case 2:
                    System.out.print("输入第一个单词：");
                    String w1 = scanner.nextLine().trim().toLowerCase();
                    System.out.print("输入第二个单词：");
                    String w2 = scanner.nextLine().trim().toLowerCase();
                    System.out.println(analyzer.queryBridgeWords(w1, w2));
                    break;
                case 3:
                    System.out.println("输入新的英文文本：");
                    String text = scanner.nextLine().trim();
                    String newText = analyzer.generateNewText(text);
                    System.out.println("生成的新文本：");
                    System.out.println(newText);
                    break;
                case 4:
                    System.out.print("输入起点单词：");
                    String a = scanner.nextLine().trim().toLowerCase();
                    System.out.print("输入终点单词：");
                    String b = scanner.nextLine().trim().toLowerCase();
                    System.out.println(analyzer.calcShortestPath(a, b));
                    break;
                case 5:
                    System.out.print("输入要查询PageRank值的单词（留空则显示所有）：");
                    String query = scanner.nextLine().trim().toLowerCase();
                    if (query.isEmpty()) {
                        Map<String, Double> ranks = analyzer.calPageRankAll();
                        System.out.println("所有节点的PageRank值：");
                        for (Map.Entry<String, Double> entry : ranks.entrySet()) {
                            System.out.println(entry.getKey() + " : " + entry.getValue());
                        }
                    } else {
                        Double rank = analyzer.calPageRank(query);
                        if (rank == null) {
                            System.out.println("节点 " + query + " 不存在于图中。");
                        } else {
                            System.out.println("节点 " + query + " 的PageRank值为: " + rank);
                        }
                    }
                    break;
                case 6:
                    String walkResult = analyzer.randomWalk();
                    System.out.println("随机游走路径：");
                    System.out.println(walkResult);
                    try (PrintWriter pw = new PrintWriter("random_walk.txt")) {
                        pw.println(walkResult);
                    } catch (IOException e) {
                        System.err.println("保存随机游走结果出错：" + e.getMessage());
                    }
                    System.out.println("已将随机游走结果保存到 random_walk.txt");
                    break;
                default:
                    System.out.println("无效选项，请重新选择。");
            }
        }
        scanner.close();
    }

    // 读取文件并构建有向图：只保留字母，按照空格分词，词对依次连接
    public void readFile(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        String prevWord = null;
        while ((line = reader.readLine()) != null) {
            line = line.replaceAll("[^a-zA-Z ]", " ");
            String[] words = line.toLowerCase().split("\\s+");
            for (String word : words) {
                if (word.isEmpty()) continue;
                if (prevWord != null) {
                    addEdge(prevWord, word);
                }
                prevWord = word;
            }
        }
        reader.close();
    }

    // 添加有向边：若边已存在则权重+1，同时确保目标节点也加入图
    private void addEdge(String from, String to) {
        graph.putIfAbsent(from, new HashMap<>());
        Map<String, Integer> neighbors = graph.get(from);
        neighbors.put(to, neighbors.getOrDefault(to, 0) + 1);
        graph.putIfAbsent(to, new HashMap<>());
    }

    // 显示图结构：打印每个节点的出边和权重
    public void showDirectedGraph() {
        System.out.println("有向图结构：");
        for (Map.Entry<String, Map<String, Integer>> entry : graph.entrySet()) {
            String word = entry.getKey();
            Map<String, Integer> neighbors = entry.getValue();
            System.out.print(word + " -> ");
            if (neighbors.isEmpty()) {
                System.out.println("无出边");
                continue;
            }
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, Integer> e : neighbors.entrySet()) {
                parts.add(e.getKey() + "(" + e.getValue() + ")");
            }
            System.out.println(String.join(", ", parts));
        }
    }

    // 桥接词查询：找出所有使 word1->mid->word2 成立的 mid
    public String queryBridgeWords(String w1, String w2) {
        if (!graph.containsKey(w1) || !graph.containsKey(w2)) {
            return "单词不存在于图中，请检查输入。";
        }
        List<String> bridges = new ArrayList<>();
        for (String mid : graph.get(w1).keySet()) {
            if (graph.get(mid).containsKey(w2)) {
                bridges.add(mid);
            }
        }
        if (bridges.isEmpty()) {
            return String.format("没有找到从 %s 到 %s 的桥接词。", w1, w2);
        } else {
            return "桥接词有：" + String.join("，", bridges);
        }
    }

    // 新文本生成：在相邻单词间随机插入桥接词
    public String generateNewText(String inputText) {
        String[] words = inputText.toLowerCase()
                .replaceAll("[^a-zA-Z ]", " ")
                .split("\\s+");
        List<String> result = new ArrayList<>();
        if (words.length == 0) return "";
        result.add(words[0]);
        for (int i = 0; i < words.length - 1; i++) {
            String w1 = words[i];
            String w2 = words[i+1];
            List<String> bridges = new ArrayList<>();
            if (graph.containsKey(w1)) {
                for (String mid : graph.get(w1).keySet()) {
                    if (graph.get(mid).containsKey(w2)) {
                        bridges.add(mid);
                    }
                }
            }
            if (!bridges.isEmpty()) {
                // 随机选一个桥接词插入
                String chosen = bridges.get(rand.nextInt(bridges.size()));
                result.add(chosen);
            }
            result.add(w2);
        }
        return String.join(" ", result);
    }


        // 最短路径计算（BFS）
    public String calcShortestPath(String start, String end) {
        if (!graph.containsKey(start) || !graph.containsKey(end)) {
            return "输入单词未出现在图中。";
        }
        Queue<String> queue = new LinkedList<>();
        Map<String, String> prev = new HashMap<>();
        queue.offer(start);
        prev.put(start, null);
        boolean found = false;
        while (!queue.isEmpty()) {
            String curr = queue.poll();
            if (curr.equals(end)) {
                found = true;
                break;
            }
            for (String neighbor : graph.get(curr).keySet()) {
                if (!prev.containsKey(neighbor)) {
                    prev.put(neighbor, curr);
                    queue.offer(neighbor);
                }
            }
        }
        if (!found) {
            return "未找到从 " + start + " 到 " + end + " 的路径。";
        }
        List<String> path = new ArrayList<>();
        for (String at = end; at != null; at = prev.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);
        return "最短路径: " + String.join(" -> ", path);
    }

    // 计算单个节点的 PageRank 值
    public Double calPageRank(String word) {
        if (!graph.containsKey(word)) return null;
        Map<String, Double> ranks = calPageRankAll();
        return ranks.get(word);
    }
    // 计算所有节点的 PageRank 值
    public Map<String, Double> calPageRankAll() {
        int N = graph.size();
        double d = 0.85;  // 阻尼因子
        // 初始化每个节点的 PageRank
        Map<String, Double> pr = new HashMap<>();
        for (String node : graph.keySet()) {
            pr.put(node, 1.0 / N);
        }
        // 迭代计算 PageRank
        for (int iter = 0; iter < 100; iter++) {
            Map<String, Double> newPr = new HashMap<>();
            for (String node : graph.keySet()) {
                newPr.put(node, (1 - d) / N);
            }
            for (String u : graph.keySet()) {
                Map<String, Integer> neighbors = graph.get(u);
                int sumW = 0;
                for (int w : neighbors.values()) sumW += w;
                if (sumW == 0) continue;
                for (Map.Entry<String, Integer> e : neighbors.entrySet()) {
                    String v = e.getKey();
                    int weight = e.getValue();
                    double contrib = pr.get(u) * ((double) weight / sumW);
                    newPr.put(v, newPr.get(v) + d * contrib);
                }
            }
            pr = newPr;
        }
        return pr;
    }

    // 随机游走
    public String randomWalk() {
        if (graph.isEmpty()) return "";
        List<String> nodes = new ArrayList<>(graph.keySet());
        String current = nodes.get(rand.nextInt(nodes.size()));
        Set<String> visitedEdges = new HashSet<>();
        List<String> path = new ArrayList<>();
        path.add(current);
        Scanner sc = new Scanner(System.in);
        while (true) {
            Map<String, Integer> neighbors = graph.get(current);
            if (neighbors.isEmpty()) {
                path.add("(无出边，终止)");
                break;
            }
            // 随机选择一条出边
            List<String> neighList = new ArrayList<>(neighbors.keySet());
            String next = neighList.get(rand.nextInt(neighList.size()));
            String edge = current + "->" + next;
            if (visitedEdges.contains(edge)) {
                path.add("(发现重复边 " + edge + "，终止)");
                break;
            }
            visitedEdges.add(edge);
            current = next;
            path.add(edge);
            // 用户可通过输入中断游走
            System.out.print("继续随机游走，或输入 'q' 终止？ (回车继续) ");
            String line = sc.nextLine();
            if (line.trim().equalsIgnoreCase("q")) {
                path.add("(用户终止)");
                break;
            }
        }
        return String.join(" -> ", path);
    }
}
