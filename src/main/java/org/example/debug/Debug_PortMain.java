package org.example.debug;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.*;

public class Debug_PortMain {

    public String getPortStatistics() throws Exception {
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        StringBuilder output = new StringBuilder();

        // key: 协议:端口（例如 TCP:80） -> PID 集合
        Map<String, Set<String>> portToPids = new HashMap<>();
        // pid -> 进程名 映射
        Map<String, String> pidToProcess = new HashMap<>();

        // 执行 netstat -ano 获取所有连接/监听信息
        Process netstat = Runtime.getRuntime().exec("netstat -ano");
        // 使用平台默认编码（某些 Windows 环境可能是 CP936/GBK）
        BufferedReader reader = new BufferedReader(new InputStreamReader(netstat.getInputStream(), Charset.defaultCharset()));
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            // 仅处理以 TCP 或 UDP 开头的行
            if (!(line.startsWith("TCP") || line.startsWith("UDP"))) continue;

            String[] tokens = line.split("\\s+");
            if (tokens.length < 2) continue;

            String protocol = tokens[0];
            String localAddress = tokens[1];

            // PID 通常位于行尾
            String pid = tokens[tokens.length - 1];

            int port = extractPort(localAddress);
            if (port < 1000 || port > 9999) continue; // 仅保留 1000~9999

            String key = protocol + ":" + port;
            portToPids.computeIfAbsent(key, k -> new HashSet<>()).add(pid);
        }
        reader.close();

        // 使用 tasklist 获取所有 PID 对应的进程名（CSV 输出，去掉表头）
        Set<String> allPids = new HashSet<>();
        for (Set<String> s : portToPids.values()) allPids.addAll(s);

        for (String pid : allPids) {
            try {
                // 使用 /FO CSV /NH 方便解析
                Process tasklist = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", "tasklist /FI \"PID eq " + pid + "\" /FO CSV /NH"});
                BufferedReader tr = new BufferedReader(new InputStreamReader(tasklist.getInputStream(), Charset.defaultCharset()));
                String taskLine = tr.readLine();
                tr.close();

                if (taskLine != null && !taskLine.trim().isEmpty()) {
                    // 解析 CSV 的第一列（进程镜像名）
                    String parsedName = parseImageNameFromTasklistCsv(taskLine);
                    pidToProcess.put(pid, parsedName);
                } else {
                    pidToProcess.put(pid, "(unknown)");
                }
            } catch (Exception e) {
                pidToProcess.put(pid, "(error)");
            }
        }

        // 输出按 协议 / 端口 排序的摘要：协议、端口、PIDs、进程名
        List<String> keys = new ArrayList<>(portToPids.keySet());
        keys.sort((a, b) -> {
            // 先按协议排序，再按端口号顺序排序
            String[] aa = a.split(":");
            String[] bb = b.split(":");
            int cmp = aa[0].compareTo(bb[0]);
            if (cmp != 0) return cmp;
            int pa = Integer.parseInt(aa[1]);
            int pb = Integer.parseInt(bb[1]);
            return Integer.compare(pa, pb);
        });

        // 定义格式字符串：左对齐，固定宽度
        String headerFormat = "%-5s %-4s %-9s %-20s%n";
        String rowFormat = "%-5s %-4s %-9s %-20s%n";

        output.append(String.format(headerFormat, "Prtcl", "Port", "PIDs", "Names"));
        for (String k : keys) {
            String[] parts = k.split(":");
            String proto = parts[0];
            String port = parts[1];
            Set<String> pids = portToPids.get(k);
            List<String> pidList = new ArrayList<>(pids);
            // 使用 List.sort 替代 Collections.sort
            pidList.sort(Comparator.comparingInt(Integer::parseInt));
            String pidJoined = String.join(",", pidList);

            // 将 PID 映射为进程名（去重且保持顺序）
            Set<String> procNames = new LinkedHashSet<>();
            for (String p : pidList) {
                procNames.add(pidToProcess.getOrDefault(p, "(unknown)"));
            }
            String procJoined = String.join(",", procNames);

            output.append(String.format(rowFormat, proto, port, pidJoined, procJoined));
        }

        // 统计总共被占用的端口数（按端口号去重，跨协议合并）
        Set<Integer> uniquePorts = new HashSet<>();
        for (String k : keys) {
            String[] parts = k.split(":");
            try {
                uniquePorts.add(Integer.parseInt(parts[1]));
            } catch (NumberFormatException ignored) {
            }
        }
        output.append("1000~9999总占用的端口数: ").append(uniquePorts.size()).append("\n");

        // 计算并输出总查询时长
        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;
        double seconds = totalDuration / 1000.0;
        output.append("总查询时长: ").append(String.format("%.2f", seconds)).append(" 秒\n");

        return output.toString();
    }

    // 从本地地址字符串中提取端口号，失败返回 -1
    private static int extractPort(String localAddress) {
        if (localAddress == null) return -1;
        localAddress = localAddress.trim();
        if (localAddress.isEmpty()) return -1;

        // localAddress 示例：
        // 127.0.0.1:80
        // 0.0.0.0:443
        // [::]:1234
        // fe80::1%lo0:5353  （IPv6 - 取最后一个冒号后的部分）
        int idx = localAddress.lastIndexOf(':');
        if (idx < 0) return -1;
        String portStr = localAddress.substring(idx + 1);
        // 某些条目可能显示 '*'，跳过它们
        if (portStr.equals("*")) return -1;
        try {
            return Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // 从 tasklist 的 CSV 行中提取第一个被引号包裹的字段（进程镜像名）
    private static String parseImageNameFromTasklistCsv(String csvLine) {
        if (csvLine == null) return "(unknown)";
        String line = csvLine.trim();
        // tasklist CSV 行以 "镜像名","PID",... 格式开始，使用正则提取第一个引号内字段
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("^\"([^\"]+)\"");
            java.util.regex.Matcher m = p.matcher(line);
            if (m.find()) {
                return m.group(1).trim();
            }
        } catch (Exception ignored) {
        }
        // 兜底解析
        if (line.startsWith("\"")) line = line.substring(1);
        int idx = line.indexOf('"');
        if (idx > 0) {
            return line.substring(0, idx).replaceAll("\"", "").trim();
        }
        return line.replaceAll("\"", "").trim();
    }
}
