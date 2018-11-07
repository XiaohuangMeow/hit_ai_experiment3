import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Bayes {
    private static BufferedReader reader;
    private static int n = 0;
    private static List<String> incidents;
    private static int[][] relations;
    private static double probilities[][][];
    private static Scanner scanner = new Scanner(System.in);
    private static int question;
    private static int[] conditions;

    private static double[] JointDistribution;

    /**
     * 读取并分析network文件
     * 
     * @param fileName network文件名
     */
    public static void ReadFile(String fileName) {
        File file = new File(fileName);
        reader = null;
        try {
            // System.out.println("以行为单位读取文件内容，一次读一整行：");
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            int cnt = 0;
            while ((line = reader.readLine()) != null) {
                if (line.equals("")) {
                    continue;
                }
                if (cnt == 0) {
                    n = Integer.valueOf(line);
                    probilities = new double[n][][];
                } else if (cnt == 1) {
                    incidents = new ArrayList<>();
                    String[] name = new String[n];
                    name = line.split(" ");
                    for (int i = 0; i < n; i++) {
                        incidents.add(name[i]);
                    }
                } else if (cnt == 2) {
                    relations = new int[n][n];
                    String[] r = line.split(" ");
                    for (int j = 0; j < n; j++) {
                        relations[0][j] = Integer.valueOf(r[j]);
                    }
                    for (int i = 1; i < n; i++) {
                        line = reader.readLine();
                        r = line.split(" ");
                        for (int j = 0; j < n; j++) {
                            relations[i][j] = Integer.valueOf(r[j]);
                        }
                    }
                } else {
                    int FirstDimention = cnt - 3;
                    int num = 1;
                    for (int i = 0; i < n; i++) {
                        if (relations[i][FirstDimention] == 1) {
                            num++;
                        }
                    }
                    int LineNumPerBlock = (int) Math.pow(2, num - 1);
                    probilities[FirstDimention] = new double[LineNumPerBlock][2];
                    String[] r = line.split(" ");
                    double a = Double.valueOf(r[0]);
                    double b = Double.valueOf(r[1]);
                    probilities[FirstDimention][0][0] = a;
                    probilities[FirstDimention][0][1] = b;
                    for (int i = 1; i < LineNumPerBlock; i++) {
                        line = reader.readLine();
                        r = line.split(" ");
                        a = Double.valueOf(r[0]);
                        b = Double.valueOf(r[1]);
                        probilities[FirstDimention][i][0] = a;
                        probilities[FirstDimention][i][1] = b;

                    }
                }
                cnt++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 解析问题，存储问题特征
     * 
     * @param input 一条查询语句
     * @return 查询结果格式语句
     */
    public static String[] readSearch(String input) {
        String[] result = new String[2];
        String[] inputs = input.split("\\|");
        Matcher askMatcher = Pattern.compile("P\\s*\\(\\s*(\\w+)").matcher(inputs[0]);
        if (askMatcher.find()) {
            question = incidents.indexOf(askMatcher.group(1));
        }
        result[0] = "P(" + askMatcher.group(1) + "=true  |" + inputs[1];
        result[1] = "P(" + askMatcher.group(1) + "=false |" + inputs[1];

        conditions = new int[n];
        for (int i = 0; i < n; i++) {
            conditions[i] = -1;
        }
        Pattern conPattern = Pattern.compile("\\s*(\\w+)\\s*\\=\\s*(\\w+)\\s*");
        Matcher conMatcher = conPattern.matcher(inputs[1]);
        while (conMatcher.find()) {
            int i = incidents.indexOf(conMatcher.group(1));
            conditions[i] = conMatcher.group(2).equals("true") ? 1 : 0;
        }

        return result;
    }

    /**
     * 计算联合分布主函数
     */
    private static void ComputeJointDistribution() {
        int lines = (int) Math.pow(2, n);
        // System.out.println("lines=" + lines);
        JointDistribution = new double[lines];
        for (int i = 0; i < lines; i++) {
            double p = ComputeProbility(i, lines);
            JointDistribution[i] = p;
        }
    }

    // 0 fffff
    // 1 fffft
    // 2 ffftf
    // 3 ffftt
    /**
     * 计算联合分布表中某行概率
     * 
     * @param x 待求行行数
     * @param lines 总行数
     * @return 该行对应概率
     */
    private static double ComputeProbility(int x, int lines) {
        boolean[] b = new boolean[n];
        int begin = 0, end = lines;
        int mid = (begin + end) / 2;
        int cnt = 0;
        while (begin + 1 != end) {
            if (x < mid) {
                b[cnt] = false;
                end = mid;
                mid = (begin + end) / 2;
            } else {
                b[cnt] = true;
                begin = mid;
                mid = (begin + end) / 2;
            }
            cnt++;
        }
        // for (int i = 0; i < n; i++) {
        // System.out.println(i + "=" + b[i]);
        // }

        // System.out.println();
        double sum = 1;
        for (int i = 0; i < n; i++) {
            List<Integer> parents = FindParents(i);
            int LinesNum = (int) Math.pow(2, parents.size());
            begin = 0;
            end = LinesNum;
            int temp = 0;
            if (!b[i]) {
                temp = 1;
            } else if (b[i]) {
                temp = 0;
            }
            for (int j = 0; j < parents.size(); j++) {
                int parent = parents.get(j);
                if (!b[parent]) {
                    end = (begin + end) / 2;
                } else if (b[parent]) {
                    begin = (begin + end) / 2;
                }
            }
            // System.out.println(begin + " " + end);
            double p = probilities[i][begin][temp];
            // System.out.println(p);
            sum *= p;
        }
        // System.out.println(sum);
        // System.out.println();
        return sum;
    }

    /**
     * 求解查询问题主函数
     * 
     * @param bool 待求问题bool值
     * @return 待求问题概率
     */
    private static double solve(boolean bool) {
        int[] p = conditions.clone();
        if (bool)
            p[question] = 1;
        else
            p[question] = 0;
        int[] q = conditions.clone();
        return TotalProbility(p) / TotalProbility(q);
    }

    private static double TotalProbility(int[] p) {
        boolean flag = false;
        int temp = -1;
        for (int i = 0; i < n; i++) {
            if (p[i] == -1) {
                flag = true;
                temp = i;
                break;
            }
        }
        double sum = 0;
        if (flag) {
            int[] x = p.clone();
            x[temp] = 0;
            sum += TotalProbility(x);
            int[] y = p.clone();
            y[temp] = 1;
            sum += TotalProbility(y);
            return sum;
        } else {
            int cnt = 1;
            int num = 0;
            for (int i = n - 1; i >= 0; i--) {
                if (p[i] == 0) {
                    num += 0;
                } else {
                    num += cnt;
                }
                cnt *= 2;
            }
            sum = JointDistribution[num];
        }
        return sum;
    }

    private static List<Integer> FindParents(int x) {
        List<Integer> parents = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (relations[i][x] == 1) {
                parents.add(i);
            }
        }
        return parents;
    }

    public static void main(String[] args) {
        ReadFile("carnetwork.txt");

        ComputeJointDistribution();

        // 读取查询信息
        List<String> queries = null;
        try {
            queries = Files.readAllLines(Paths.get("carqueries.txt"));
        } catch (IOException e) {
            System.err.println("Failed to read burglarqueries.txt!");
            e.printStackTrace();
        }
        // 查询
        // System.out.println(incidents);
        for (String string : queries) {
            if (string.length() == 0) {
                continue;
            }
            String[] result = readSearch(string);
            System.out.println(result[0] + " = " + solve(true));
            System.out.println(result[1] + " = " + solve(false)+"\n");
        }

    }
}
