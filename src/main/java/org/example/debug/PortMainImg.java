package org.example.debug;

import java.io.BufferedReader;
import java.io.InputStreamReader;
// imports adjusted: removed unused ManagementFactory/OperatingSystemMXBean
import java.nio.charset.Charset;
import java.util.*;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Color;
import java.awt.image.BufferedImage;

public class PortMainImg {
    /**
     * 生成端口统计信息图片
     * @return 图片的绝对路径
     * @throws Exception 如果处理失败
     */
    public static String generatePortStatisticsImage() throws Exception {
        try {
            String result = getPortStatistics();
            // 输出到项目目录（当前工作目录）
            String outFile = System.getProperty("user.dir") + File.separator + "ports_table.png";
            try {
                renderJsonToPng(result, outFile, 1200);
                return outFile;
            } catch (Exception e) {
                throw new RuntimeException("生成图片失败: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new RuntimeException("错误: " + e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        try {
            String imagePath = generatePortStatisticsImage();
            System.out.println(imagePath);
        } catch (Exception e) {
            System.err.println("错误: " + e);
        }
    }
    
    public static String getPortStatistics() throws Exception {
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

        // 使用 tasklist 获取所有 PID 对应的进程名与内存（CSV 输出，去掉表头）
        Set<String> allPids = new HashSet<>();
        for (Set<String> s : portToPids.values()) allPids.addAll(s);

        // pid -> memory MB
        Map<String, Long> pidToMemoryMB = new HashMap<>();

        for (String pid : allPids) {
            try {
                // 使用 /FO CSV /NH 方便解析（返回："Image Name","PID","Session Name","Session#","Mem Usage"）
                Process tasklist = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", "tasklist /FI \"PID eq " + pid + "\" /FO CSV /NH"});
                BufferedReader tr = new BufferedReader(new InputStreamReader(tasklist.getInputStream(), Charset.defaultCharset()));
                String taskLine = tr.readLine();
                tr.close();

                if (taskLine != null && !taskLine.trim().isEmpty()) {
                    // 解析 CSV 的字段
                    String parsedName = parseImageNameFromTasklistCsv(taskLine);
                    long memMb = parseMemoryFromTasklistCsv(taskLine);
                    pidToProcess.put(pid, parsedName);
                    pidToMemoryMB.put(pid, memMb);
                } else {
                    pidToProcess.put(pid, "(unknown)");
                    pidToMemoryMB.put(pid, 0L);
                }
            } catch (Exception e) {
                pidToProcess.put(pid, "(error)");
                pidToMemoryMB.put(pid, 0L);
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
    // 构建 JSON 输出：ports 数组与汇总
        List<String> portJsonObjects = new ArrayList<>();
        // 记录实际会被输出的端口集合、已统计过的进程名（按进程名去重内存）
        Set<Integer> uniqueDisplayedPorts = new HashSet<>();
        Set<String> countedProcessNames = new HashSet<>();
        long totalDisplayedMemMB = 0L;

        for (String k : keys) {
            String[] parts = k.split(":");
            String proto = parts[0];
            String port = parts[1];
            Set<String> pids = portToPids.get(k);
            List<String> pidList = new ArrayList<>(pids);
            pidList.sort(Comparator.comparingInt(Integer::parseInt));

            // 将 PID 映射为进程名（去重且保持顺序），并去掉 .exe 后缀
            Set<String> procNames = new LinkedHashSet<>();
            for (String p : pidList) {
                String raw = pidToProcess.getOrDefault(p, "(unknown)");
                String stripped = raw.replaceAll("(?i)\\.exe$", "");
                procNames.add(stripped);
            }

            // 过滤：如果该端口对应的所有进程都为系统进程则跳过
            boolean allSystem = true;
            for (String p : pidList) {
                String name = pidToProcess.getOrDefault(p, "(unknown)");
                if (!"0".equals(p) && !isSystemProcessName(name)) {
                    allSystem = false;
                    break;
                }
            }
            if (allSystem) continue;

            long memSum = 0L;
            for (String p : pidList) memSum += pidToMemoryMB.getOrDefault(p, 0L);

            try { uniqueDisplayedPorts.add(Integer.parseInt(port)); } catch (Exception ignored) {}
            
            // 只对未统计过的进程名计入总内存（按进程名去重，避免同名进程被重复计数）
            for (String procName : procNames) {
                if (!countedProcessNames.contains(procName)) {
                    // 对该进程名的所有 PID 求和
                    long procMemSum = 0L;
                    for (String p : pidList) {
                        String pName = pidToProcess.getOrDefault(p, "(unknown)").replaceAll("(?i)\\.exe$", "");
                        if (pName.equals(procName)) {
                            procMemSum += pidToMemoryMB.getOrDefault(p, 0L);
                        }
                    }
                    totalDisplayedMemMB += procMemSum;
                    countedProcessNames.add(procName);
                }
            }

            // 构建单个端口的 JSON 对象
            StringBuilder po = new StringBuilder();
            po.append("{");
            po.append("\"protocol\":").append("\"").append(jsonEscape(proto)).append("\"").append(",");
            po.append("\"port\":").append(port).append(",");
            // pids 数组
            po.append("\"pids\":[");
            for (int i = 0; i < pidList.size(); i++) {
                String p = pidList.get(i);
                // 尝试作为数字输出
                try {
                    po.append(Integer.parseInt(p));
                } catch (Exception e) {
                    po.append("\"").append(jsonEscape(p)).append("\"");
                }
                if (i < pidList.size() - 1) po.append(",");
            }
            po.append("],");
            // names 数组
            po.append("\"names\":[");
            List<String> namesList = new ArrayList<>(procNames);
            for (int i = 0; i < namesList.size(); i++) {
                po.append("\"").append(jsonEscape(namesList.get(i))).append("\"");
                if (i < namesList.size() - 1) po.append(",");
            }
            po.append("],");
            po.append("\"memMB\":").append(memSum);
            po.append("}");

            portJsonObjects.add(po.toString());
        }

        // 拼装最终 JSON
        output.append("{");
        output.append("\"ports\":[");
        for (int i = 0; i < portJsonObjects.size(); i++) {
            output.append(portJsonObjects.get(i));
            if (i < portJsonObjects.size() - 1) output.append(",");
        }
        output.append("],");
        output.append("\"totalDisplayedPorts\":").append(uniqueDisplayedPorts.size()).append(",");
    output.append("\"totalDisplayedMemMB\":").append(totalDisplayedMemMB).append(",");
    long endTime = System.currentTimeMillis();
    double seconds = (endTime - startTime) / 1000.0;
    output.append("\"durationSeconds\":").append(String.format("%.2f", seconds));
    output.append("}");
        
    return output.toString();
    }

    // 简单 JSON 字符串转义（处理引号与反斜杠）
    private static String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int)c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    // ----------------------- JSON->表格渲染为图片的辅助方法 -----------------------
    private static void renderJsonToPng(String json, String outFile, int maxWidth) throws Exception {
        List<String[]> rows = parseJsonToRows(json);
        
        String[] cols = new String[] {"序号","Protocol","Port","PIDs","Names","MemMB"};

        final DefaultTableModel model = new DefaultTableModel(cols, 0);
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            // 扩展为6列（序号 + 原有5列）
            String[] extendedRow = new String[6];
            extendedRow[0] = String.valueOf(i + 1); // 序号从1开始
            System.arraycopy(r, 0, extendedRow, 1, 5);
            model.addRow(extendedRow);
        }

        final JTable table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Font tableFont = new Font("Microsoft YaHei UI", Font.PLAIN, 14);
        table.setFont(tableFont);
        table.setRowHeight(26);

        final BufferedImage[] holder = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> {
            // 使用 FontMetrics 精确计算每列所需宽度
            Graphics2D tempG = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
            tempG.setFont(tableFont);
            java.awt.FontMetrics fm = tempG.getFontMetrics();
            
            int[] colWidths = new int[table.getColumnCount()];
            // 先根据列头计算最小宽度
            for (int c = 0; c < table.getColumnCount(); c++) {
                String header = table.getColumnName(c);
                colWidths[c] = fm.stringWidth(header) + 30; // 30px padding
            }
            
            // 再遍历所有行数据，更新每列最大宽度
            for (int r = 0; r < table.getRowCount(); r++) {
                for (int c = 0; c < table.getColumnCount(); c++) {
                    Object val = table.getValueAt(r, c);
                    if (val != null) {
                        String str = val.toString();
                        int width = fm.stringWidth(str) + 30;
                        colWidths[c] = Math.max(colWidths[c], width);
                    }
                }
            }
            tempG.dispose();

            // 应用列宽
            int totalWidth = 0;
            for (int c = 0; c < table.getColumnCount(); c++) {
                table.getColumnModel().getColumn(c).setPreferredWidth(colWidths[c]);
                table.getColumnModel().getColumn(c).setWidth(colWidths[c]);
                totalWidth += colWidths[c];
            }

            // 计算图像尺寸（增加边距）
            int padding = 10;
            int imgWidth = Math.min(totalWidth + padding * 2, maxWidth);
            
            JTableHeader header = table.getTableHeader();
            header.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
            int headerHeight = 30; // 固定表头高度
            int rowCount = table.getRowCount();
            int rowHeight = table.getRowHeight();
            int imgHeight = headerHeight + rowCount * rowHeight + padding * 2;

            // 创建图像
            BufferedImage img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            // 白色背景
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, imgWidth, imgHeight);

            // 设置表格和表头大小
            table.setSize(totalWidth, rowCount * rowHeight);
            header.setSize(totalWidth, headerHeight);

            // 绘制表头
            g2.translate(padding, padding);
            header.paint(g2);

            // 绘制表体
            g2.translate(0, headerHeight);
            table.paint(g2);
            
            g2.dispose();
            holder[0] = img;
        });

        BufferedImage image = holder[0];
        if (image == null) throw new RuntimeException("生成图片失败");
        ImageIO.write(image, "png", new File(outFile));
    }

    private static List<String[]> parseJsonToRows(String json) {
        List<String[]> rows = new ArrayList<>();
        if (json == null || !json.contains("\"ports\":")) return rows;
        
        // 找到ports数组的起始和结束位置
        int pstart = json.indexOf("\"ports\":");
        int arrStart = json.indexOf('[', pstart);
        if (arrStart < 0) return rows;
        
        // 找到匹配的结束括号（处理嵌套）
        int depth = 0;
        int arrEnd = -1;
        for (int i = arrStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) {
                    arrEnd = i;
                    break;
                }
            }
        }
        
        if (arrEnd < 0) return rows;
        String arr = json.substring(arrStart + 1, arrEnd).trim();
        if (arr.isEmpty()) return rows;
        
        // 手动解析每个对象（处理嵌套的数组）
        int objStart = -1;
        depth = 0;
        for (int i = 0; i < arr.length(); i++) {
            char c = arr.charAt(i);
            if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart >= 0) {
                    String obj = arr.substring(objStart, i + 1);
                    String proto = extractJsonString(obj, "protocol");
                    String port = extractJsonString(obj, "port");
                    String pids = extractJsonArrayAsString(obj, "pids");
                    String names = extractJsonArrayAsString(obj, "names");
                    String mem = extractJsonString(obj, "memMB");
                    if (mem == null || mem.isEmpty()) mem = "0";
                    rows.add(new String[] { proto, port, pids, names, mem });
                    objStart = -1;
                }
            }
        }
        return rows;
    }

    private static String extractJsonString(String obj, String key) {
        String q = "\"" + key + "\":";
        int idx = obj.indexOf(q);
        if (idx < 0) return "";
        int start = idx + q.length();
        if (start < obj.length() && obj.charAt(start) == '"') {
            int end = obj.indexOf('"', start + 1);
            if (end < 0) return obj.substring(start + 1).replaceAll("\"", "").trim();
            return obj.substring(start + 1, end);
        } else {
            int end = obj.indexOf(',', start);
            if (end < 0) end = obj.indexOf('}', start);
            if (end < 0) end = obj.length();
            return obj.substring(start, end).replaceAll("\"", "").trim();
        }
    }

    private static String extractJsonArrayAsString(String obj, String key) {
        String q = "\"" + key + "\":";
        int idx = obj.indexOf(q);
        if (idx < 0) return "";
        int start = obj.indexOf('[', idx);
        int end = obj.indexOf(']', start);
        if (start < 0 || end < 0) return "";
        String inside = obj.substring(start + 1, end).trim();
        if (inside.isEmpty()) return "";
        inside = inside.replaceAll("\"", "");
        return inside.replaceAll("\\s*,\\s*", ", ");
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

    // 判定进程名是否为系统进程（用于过滤输出）
    private static boolean isSystemProcessName(String name) {
        if (name == null) return true;
        String n = name.trim().toLowerCase(Locale.ROOT);
        if (n.isEmpty()) return true;
        // 常见 Windows 系统进程名字
        if (n.equals("system") || n.equals("system idle process") || n.equals("idle") || n.equals("svchost.exe") == false && n.equals("svchost") ) {
            // 注意：svchost 有时是普通系统服务的宿主，但用户可能希望排除它；这里我们只把显式 System / Idle 视为系统核心进程
        }
        Set<String> sysNames = new HashSet<>(Arrays.asList(
                "system","system idle process","idle",
                "ntoskrnl.exe","wininit.exe","services.exe",
                "lsass.exe","csrss.exe","svchost.exe","svchost","systemsettings.exe"
        ));
        // 比较时去掉扩展名差异
        String simple = n.replaceAll("\\.exe$", "");
        if (sysNames.contains(n) || sysNames.contains(simple)) return true;
        return false;
    }

    // 从 tasklist 的 CSV 行中解析内存列（Mem Usage），例如 "12,345 K" -> 返回 MB（向下取整）
    private static long parseMemoryFromTasklistCsv(String csvLine) {
        if (csvLine == null) return 0L;
        try {
            // 将 CSV 按逗号分割，但字段被引号包裹，简单解析：逐个提取引号内字段
            List<String> fields = new ArrayList<>();
            int idx = 0;
            while (idx < csvLine.length()) {
                if (csvLine.charAt(idx) == '"') {
                    int next = csvLine.indexOf('"', idx + 1);
                    if (next < 0) break;
                    fields.add(csvLine.substring(idx + 1, next));
                    idx = next + 1;
                    // 跳过逗号
                    if (idx < csvLine.length() && csvLine.charAt(idx) == ',') idx++;
                } else {
                    int next = csvLine.indexOf(',', idx);
                    if (next < 0) {
                        fields.add(csvLine.substring(idx).trim());
                        break;
                    }
                    fields.add(csvLine.substring(idx, next).trim());
                    idx = next + 1;
                }
            }
            // Mem Usage 通常是第5列（索引4），但为了兼容性，取最后一列作为内存表示
            if (fields.size() == 0) return 0L;
            String memField = fields.get(fields.size() - 1);
            // 示例: "12,345 K" 或者 "8,192 K"
            memField = memField.replaceAll("\\s*K$", "").replaceAll(",", "").trim();
            if (memField.isEmpty()) return 0L;
            long kb = Long.parseLong(memField);
            long mb = kb / 1024;
            return mb;
        } catch (Exception e) {
            return 0L;
        }
    }
}
