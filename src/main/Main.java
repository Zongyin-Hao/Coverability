package main;

import petrinet.*;
import reverseunfolding.ReverseUnfolder;
import unfolding.Unfolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class Main {
    // 检查输出的触发序列是否可行
    public static boolean check(List<Transition> ans, List<Place> M0, List<Place> Mf) {
        Set<Place> M = new HashSet<> ();
        for (Place p : M0) {
            M.add(p);
        }
        for (Transition t : ans) {
            for (Place p : t.getPreSet()) {
                if (!M.contains(p)) {
                	return false;
                }
                M.remove(p);
            }
            for (Place p : t.getPostSet()) {
                M.add(p);
            }
        }
        for (Place p : Mf) {
            if (!M.contains(p)) return false;
        }
        return true;
    }

    // 测试正向展开算法
    public static void testUnf(String benchmark, int flag, int maxIter, long maxTime) throws IOException {
        File dir = new File(benchmark);
        File[] files = dir.listFiles();

        int total = 0; // 总共验证了total个Mf
        int coverable = 0; // 其中有coverable个Mf可覆盖
        FileWriter fw = new FileWriter("output/Unf/"+dir.getName()+flag+".txt");

        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }

            String filePath = file.getAbsolutePath();
            System.out.println("****************************************");
            System.out.println("[Test case]" + filePath);

            PetriNetReader input = new PetriNetReader(filePath);
            input.start();

            List<List<Place>> Mfs = input.getMfs();

            for (int i = 0; i < Mfs.size(); i++) {
                total++;
                List<Place> Mf = Mfs.get(i);
                System.out.println("----------------------------------------");
                System.out.println("[Mf] " + (i+1));

                // Unfolding
                Unfolder RUnf = new Unfolder(input.getP(), input.getT(), input.getM0(), Mf, flag);
                List<Transition> ans = RUnf.start(maxIter, maxTime);

                System.out.println("[End] |Po| = " + RUnf.getPOsz());
                System.out.println("[End] |To| = " + RUnf.getTOsz());
//                System.out.println("[End] |P| = " + RUnf.getPsz());
//                System.out.println("[End] |T| = " + RUnf.getTsz());
                System.out.println("[End] |C| = " + RUnf.getCsSize());
                System.out.println("[End] |E| = " + RUnf.getEsSize());
                System.out.println("[End] |Ext| = " + RUnf.getRExtSize());
                System.out.println("[End] |cut-off| = " + RUnf.getCutoffNumber());
                System.out.println("[End] time = " + RUnf.getTime());
                if (ans == null) {
                    System.out.println("[End]!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    System.out.println("[End] UnCoverable");
                    System.out.println("[End]!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                } else {
                    System.out.println("[End] Coverable");
                    if (check(ans, input.getM0(), Mf)) {
                        // 触发序列可行
                        System.out.println("[End] |firing sequence| = " + ans.size());
                        System.out.println("[End] Accepted");
                        coverable++;
                    } else {
                        System.out.println("[End]!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        System.err.println("[End] Wrong Answer");
                        System.out.println("[End]!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        System.exit(0);
                    }
                }

                int t1 = 0, t2 = -1;
                if (ans != null) {
                    t1 = 1;
                    t2 = ans.size();
                }
                String s = String.format("%6d%6d%6d%6d%6d%6d%3d%6d %s", RUnf.getPOsz(), RUnf.getTOsz(), RUnf.getCsSize(), RUnf.getEsSize(),
                        RUnf.getRExtSize(), RUnf.getTime(), t1, t2, file.getName());
                fw.write(s + "\n");

            }
        }
        System.out.println("[All End] coverable/total = " + coverable + "/" + total);
        fw.write("[All End] coverable/total = " + coverable + "/" + total + "\n");

        fw.close();
    }

    // 测试反向展开算法
    public static void testRUnf(String benchmark, int flag, int maxIter, long maxTime) throws IOException {
        File dir = new File(benchmark);
        File[] files = dir.listFiles();

        int total = 0; // 当前用例中总共要验证的Mf数量
        int coverable = 0; // 其中可覆盖的Mf数量
        FileWriter fw = new FileWriter("output/RUnf/"+dir.getName()+flag+".txt");

        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }
            String filePath = file.getAbsolutePath();
            System.out.println("****************************************");
            System.out.println("[Test case]" + filePath);

            PetriNetReader input = new PetriNetReader(filePath);
            input.start();

            List<List<Place>> Mfs = input.getMfs();

            for (int i = 0; i < Mfs.size(); i++) {
                total++;
                List<Place> Mf = Mfs.get(i);
                System.out.println("----------------------------------------");
                System.out.println("[Mf] " + (i+1));

                // Reverse Unfolding
                ReverseUnfolder RUnf = new ReverseUnfolder(input.getP(), input.getT(), input.getM0(), Mf, flag);
                List<Transition> ans = RUnf.start(maxIter, maxTime);

                System.out.println("[End] |Po| = " + RUnf.getPOsz());
                System.out.println("[End] |To| = " + RUnf.getTOsz());
//                System.out.println("[End] |P| = " + RUnf.getPsz());
//                System.out.println("[End] |T| = " + RUnf.getTsz());
                System.out.println("[End] |C| = " + RUnf.getCsSize());
                System.out.println("[End] |E| = " + RUnf.getEsSize());
                System.out.println("[End] |RExt| = " + RUnf.getRExtSize());
                System.out.println("[End] |cut-off| = " + RUnf.getCutoffNumber());
//                System.out.println("[End] |redundant| = " + RUnf.getRedundantNumber());
                System.out.println("[End] time = " + RUnf.getTime());
                if (ans == null) {
                    System.out.println("[End]!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    System.out.println("[End] UnCoverable");
                    System.out.println("[End]!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                } else {
                    System.out.println("[End] Coverable");
                    if (check(ans, input.getM0(), Mf)) {
                        // 触发序列可行
                        System.out.println("[End] |firing sequence| = " + ans.size());
                        System.out.println("[End] Accepted");
                        coverable++;
                    } else {
                        System.out.println("[End]!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        System.err.println("[End] Wrong Answer");
                        System.out.println("[End]!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        System.exit(0);
                    }
                }

                int t1 = 0, t2 = -1;
                if (ans != null) {
                    t1 = 1;
                    t2 = ans.size();
                }
                String s = String.format("%6d%6d%6d%6d%6d%6d%3d%6d %s", RUnf.getPOsz(), RUnf.getTOsz(), RUnf.getCsSize(), RUnf.getEsSize(),
                        RUnf.getRExtSize(), RUnf.getTime(), t1, t2, file.getName());
                fw.write(s + "\n");

            }
        }
        System.out.println("[All End] coverable/total = " + coverable + "/" + total);
        fw.write("[All End] coverable/total = " + coverable + "/" + total + "\n");

        fw.close();
    }

    public static void main(String[] args) throws IOException {
        // 启发式策略参数(flag)：
        // RUnf: 1.adequate order 2.dfs 3.block+dfs 4.hmax 5.hsum 6.block+hsum
        // Unf: 1.adequate order 2.dfs 3.hmax 4.hsum

        testRUnf("benchmarks/randomtree", 1, 10000, 35000);
//        testUnf("benchmarks/randomtree", 2, 10000, 35000);
//        testRUnf("benchmarks/randomtree", 2, 10000, 35000);
//        testUnf("benchmarks/randomtree", 2, 10000, 35000);
//        testRUnf("benchmarks/randomtree", 3, 10000, 35000);
//        testRUnf("benchmarks/randomtree", 4, 10000, 35000);
//        testRUnf("benchmarks/randomtree", 5, 10000, 35000);
//        testRUnf("benchmarks/randomtree", 6, 10000, 35000);
//        testUnf("benchmarks/randomtree", 3, 10000, 35000);
//        testUnf("benchmarks/randomtree", 4, 10000, 35000);

//
//        testRUnf("benchmarks/dartes", 4, 10000, 35000);
//        testUnf("benchmarks/dartes", 4, 10000, 35000);
//
//        testRUnf("benchmarks/threadlock", 3, 10000, 70000);
//        testUnf("benchmarks/threadlock", 2, 10000, 70000);
//        testUnf("benchmarks/threadlock", 3, 10000, 70000);
//        testUnf("benchmarks/threadlock", 4, 10000, 70000);

//        testRUnf("benchmarks/random", 3, 10000, 35000);
//        testRUnf("benchmarks/random", 4, 10000, 35000);
//        testRUnf("benchmarks/random", 5, 10000, 35000);
//        testRUnf("benchmarks/random", 6, 10000, 35000);
//        testUnf("benchmarks/random", 3, 10000, 35000);
//        testUnf("benchmarks/random", 4, 10000, 35000);

    }
}
