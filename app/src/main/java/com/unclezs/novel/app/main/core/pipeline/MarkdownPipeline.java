package com.unclezs.novel.app.main.core.pipeline;

import com.unclezs.novel.analyzer.model.Chapter;
import com.unclezs.novel.analyzer.model.Novel;
import com.unclezs.novel.analyzer.spider.pipline.AbstractTextPipeline;
import com.unclezs.novel.analyzer.util.FileUtils;
import com.unclezs.novel.analyzer.util.StringUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * markdown下载管道
 *
 * @author blog.unclezs.com
 * @since 2021/5/1 15:59
 */
@Setter
@Slf4j
public class MarkdownPipeline extends AbstractTextPipeline {
  private static final String DOWNLOAD_FILE_FORMAT = "%s/%d.%s.md";
  /**
   * 是否合并文件
   */
  private boolean merge;
  /**
   * 删除章节文件
   */
  private boolean deleteVolume;

  @Override
  public void processChapter(Chapter chapter) {
    String filePath = String.format(DOWNLOAD_FILE_FORMAT, getFilePath(), chapter.getOrder(), StringUtils.removeInvalidSymbol(chapter.getName()));
    try {
      // 写入文件
      String title = String.format("### %s\n", chapter.getName());
      FileUtils.writeString(filePath, title, getCharset());
      FileUtils.appendUtf8String(filePath, chapter.getContent());
    } catch (IOException e) {
      log.error("保存章节内容到：{} 失败.", filePath, e);
    }
  }

  @Override
  public void onComplete() {
    if (merge) {
      try {
        mergeNovel(new File(getFilePath()), getNovel(), deleteVolume);
      } catch (Exception e) {
        log.error("文件合并失败：{}", getFilePath(), e);
      }
    }
  }

  /**
   * 合并文件
   *
   * 2022/9/6 Curious 合并时添加书名、作者、简介在文章开头
   *
   * @param dir      小说目录
   * @param novel 小说
   * @param delete   合并后删除
   */
  private static void mergeNovel(File dir, Novel novel, boolean delete) throws IOException {
    // 书籍名
    String filename = novel.getTitle().concat(".md");
    // 保存到父目录下
    String saveFile = new File(dir.getParent(), filename).getAbsolutePath();

    // 书籍若存在，则删除
    FileUtils.deleteFile(saveFile);

    // 添加书名、作者、简介在开头
    String backgroundInfo = String.format("## %s\n#### 作者：%s\n#### 简介：\n%s\n", novel.getTitle() != null ? novel.getTitle() : "", novel.getAuthor() != null ? novel.getAuthor() : "", novel.getIntroduce() != null ? novel.getIntroduce() : "");
    FileUtils.appendUtf8String(saveFile,backgroundInfo);

    File[] mdFiles = dir.listFiles((dir1, name) -> name.endsWith(".md"));
    if (mdFiles != null) {
      Arrays.stream(mdFiles).sorted((o1, o2) -> {
        Integer a = Integer.valueOf(o1.getName().split("\\.")[0]);
        Integer b = Integer.valueOf(o2.getName().split("\\.")[0]);
        return a - b;
      }).forEach(file -> {
        try {
          String s = FileUtils.readUtf8String(file.getAbsoluteFile());
          FileUtils.appendUtf8String(saveFile, s);
          if (delete) {
            FileUtils.deleteFile(file);
          }
        } catch (IOException e) {
          log.error("小说合并失败：文件夹：{}，文件名：{}", dir, filename, e);
          e.printStackTrace();
        }
      });
    }
    if (delete) {
      FileUtils.deleteFile(dir);
    }
  }
}
