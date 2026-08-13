package com.shiyq.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Desktop.ini文件工具类
 * 用于创建和管理Windows文件夹自定义图标
 *
 * @author shiyq
 * @since 2024-12-19
 */
@Slf4j
public class DesktopIniUtil {

    /**
     * 为文件夹创建desktop.ini文件并设置自定义图标
     *
     * @param folderPath 文件夹路径
     * @param iconPath 图标文件路径（相对于文件夹的路径）
     * @param localizedResourceName 本地化资源名称（中文名称）
     * @return 是否成功
     */
    public static boolean createDesktopIni(String folderPath, String iconPath, String localizedResourceName) {
        try {
            File folder = new File(folderPath);
            if (!folder.exists() || !folder.isDirectory()) {
                log.error("文件夹不存在或不是目录: {}", folderPath);
                return false;
            }

            // 创建desktop.ini文件
            File desktopIniFile = new File(folder, "desktop.ini");
            
            // 构建desktop.ini内容
            StringBuilder content = new StringBuilder();
            content.append("[.ShellClassInfo]\n");
            content.append("IconResource=").append(iconPath).append(",0\n");
            if (localizedResourceName != null && !localizedResourceName.trim().isEmpty()) {
                content.append("LocalizedResourceName=").append(localizedResourceName).append("\n");
            }
            content.append("InfoTip=游戏文件夹\n");

            // 写入desktop.ini文件
            try (FileWriter writer = new FileWriter(desktopIniFile, false)) {
                writer.write(content.toString());
            }

            // 设置文件属性
            setDesktopIniAttributes(folderPath);

            log.info("成功创建desktop.ini文件: {}", desktopIniFile.getAbsolutePath());
            return true;

        } catch (Exception e) {
            log.error("创建desktop.ini文件失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 为文件夹创建desktop.ini文件
     *
     * @param folderPath 文件夹路径
     * @param localizedResourceName 本地化资源名称（中文名称）
     * @return 是否成功
     */
    public static boolean createDesktopIni(String folderPath, String localizedResourceName) {
        try {
            File folder = new File(folderPath);
            if (!folder.exists() || !folder.isDirectory()) {
                log.error("文件夹不存在或不是目录: {}", folderPath);
                return false;
            }

            // 创建desktop.ini文件
            File desktopIniFile = new File(folder, "desktop.ini");
            
            // 构建desktop.ini内容
            StringBuilder content = new StringBuilder();
            content.append("[.ShellClassInfo]\n");
            if (localizedResourceName != null && !localizedResourceName.trim().isEmpty()) {
                content.append("LocalizedResourceName=").append(localizedResourceName).append("\n");
            }
            content.append("InfoTip=游戏文件夹\n");

            // 写入desktop.ini文件
            try (FileWriter writer = new FileWriter(desktopIniFile, false)) {
                writer.write(content.toString());
            }

            // 设置文件属性
            setDesktopIniAttributes(folderPath);

            log.info("成功创建desktop.ini文件: {}", desktopIniFile.getAbsolutePath());
            return true;

        } catch (Exception e) {
            log.error("创建desktop.ini文件失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 设置desktop.ini文件和文件夹的必要属性
     *
     * @param folderPath 文件夹路径
     * @throws IOException IO异常
     */
    private static void setDesktopIniAttributes(String folderPath) throws IOException {
        try {
            Path folderPathObj = Paths.get(folderPath);
            Path desktopIniPath = Paths.get(folderPath, "desktop.ini");

            // 设置文件夹为系统文件夹（必须）
            Files.setAttribute(folderPathObj, "dos:system", true, LinkOption.NOFOLLOW_LINKS);

            // 设置desktop.ini为隐藏和系统文件
            Files.setAttribute(desktopIniPath, "dos:hidden", true, LinkOption.NOFOLLOW_LINKS);
            Files.setAttribute(desktopIniPath, "dos:system", true, LinkOption.NOFOLLOW_LINKS);

            log.debug("成功设置文件属性: {}", folderPath);

        } catch (Exception e) {
            log.warn("设置文件属性失败，尝试使用命令行方式: {}", e.getMessage());
            // 如果NIO方式失败，尝试使用命令行
            setAttributesUsingCommand(folderPath);
        }
    }

    /**
     * 使用命令行设置文件属性（备用方案）
     *
     * @param folderPath 文件夹路径
     */
    private static void setAttributesUsingCommand(String folderPath) {
        try {
            // 设置文件夹为系统文件夹
            ProcessBuilder pb1 = new ProcessBuilder("attrib", "+s", folderPath);
            Process process1 = pb1.start();
            process1.waitFor();

            // 设置desktop.ini为隐藏和系统文件
            String desktopIniPath = folderPath + File.separator + "desktop.ini";
            ProcessBuilder pb2 = new ProcessBuilder("attrib", "+h", "+s", desktopIniPath);
            Process process2 = pb2.start();
            process2.waitFor();

            log.debug("使用命令行成功设置文件属性: {}", folderPath);

        } catch (Exception e) {
            log.error("使用命令行设置文件属性也失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 刷新文件夹显示（强制Windows重新读取desktop.ini）
     *
     * @param folderPath 文件夹路径
     */
    public static void refreshFolder(String folderPath) {
        try {
            // 通过修改文件夹属性来触发刷新
            ProcessBuilder pb = new ProcessBuilder("attrib", "+s", folderPath);
            Process process = pb.start();
            process.waitFor();
            
            log.debug("刷新文件夹显示: {}", folderPath);
        } catch (Exception e) {
            log.warn("刷新文件夹显示失败: {}", e.getMessage());
        }
    }

    /**
     * 检查desktop.ini文件是否存在
     *
     * @param folderPath 文件夹路径
     * @return 是否存在
     */
    public static boolean desktopIniExists(String folderPath) {
        File desktopIniFile = new File(folderPath, "desktop.ini");
        return desktopIniFile.exists();
    }

    /**
     * 删除desktop.ini文件
     *
     * @param folderPath 文件夹路径
     * @return 是否成功删除
     */
    public static boolean removeDesktopIni(String folderPath) {
        try {
            File desktopIniFile = new File(folderPath, "desktop.ini");
            if (desktopIniFile.exists()) {
                // 先移除文件属性
                try {
                    Path desktopIniPath = desktopIniFile.toPath();
                    Files.setAttribute(desktopIniPath, "dos:hidden", false, LinkOption.NOFOLLOW_LINKS);
                    Files.setAttribute(desktopIniPath, "dos:system", false, LinkOption.NOFOLLOW_LINKS);
                } catch (Exception e) {
                    log.warn("移除文件属性失败，直接删除: {}", e.getMessage());
                }
                
                boolean deleted = desktopIniFile.delete();
                if (deleted) {
                    log.info("成功删除desktop.ini文件: {}", desktopIniFile.getAbsolutePath());
                }
                return deleted;
            }
            return true;
        } catch (Exception e) {
            log.error("删除desktop.ini文件失败: {}", e.getMessage(), e);
            return false;
        }
    }
}