package com.hafs.cn;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.util.Progressable;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.sql.Timestamp;

/**
 * HDFS测试代码
 * <pre>
 * Modify Information:
 * Author       Date        Description
 * ========= =========== ============================
 *  hlz        2018/8/3     Create this file
 * </pre>
 */
public class HdfsTest {

    FileSystem fs = null;

    @Before
    public void init() throws Exception{
        fs = FileSystem.get(new URI("hdfs://hadoop01:9000"), new Configuration(), "root");
    }

    /**
     * 下载
     * @throws Exception
     */
    @Test
    public void testDownload() throws Exception{
        FSDataInputStream fis = fs.open(new Path("/hlz/data.log"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copyBytes(fis, baos, 4096, true);
        System.out.println(new String(baos.toByteArray()));
    }

    /**
     * 上传
     * @throws Exception
     */
    @Test
    public void testUpload() throws Exception{
        FileInputStream in = new FileInputStream(new File("E:/20180724162404.pdf"));
        FSDataOutputStream out = fs.create(new Path("/hadoop/upload1.txt"));
        IOUtils.copyBytes(in, out, 4096, true);
    }

    /**
     *创建文件夹
     */
    @Test
    public void testMkdir() throws Exception{
        boolean flag = fs.mkdirs(new Path("/hadoop"));
        System.out.println(flag);
    }

    /**
     * 删除文件
     * @throws Exception
     */
    @Test
    public void testRemove() throws Exception {
        boolean flag = fs.delete(new Path("/hadoop/upload.txt"), true);
        System.out.println(flag);
    }

    @Test
    public void testFile() throws Exception {
        FileStatus file = fs.getFileStatus(new Path("/hadoop/upload.txt"));
        long len = file.getLen();                   //文件长度
        String pathSource = file.getPath().toString();//文件路径
        String fileName = file.getPath().getName();   // 文件名称
        String parentPath = file.getPath().getParent().toString();//文件父路径
        Timestamp timestamp = new Timestamp(file.getModificationTime());//文件最后修改时间
        long blockSize = file.getBlockSize();   //文件块大小
        String group = file.getGroup();         //文件所属组
        String owner = file.getOwner();          // 文件拥有者
        long accessTime = file.getAccessTime();  //该文件上次访问时间
        short replication = file.getReplication(); //文件副本数
        System.out.println("文件长度: "+len+"\n"+
                "文件路径: "+pathSource+"\n"+
                "文件名称: "+fileName+"\n"+
                "文件父路径: "+parentPath+"\n"+
                "文件最后修改时间: "+timestamp+"\n"+
                "文件块大小: "+blockSize+"\n"+
                "文件所属组: "+group+"\n"+
                "文件拥有者: "+owner+"\n"+
                "该文件上次访问时间: "+accessTime+"\n"+
                "文件副本数: "+replication+"\n"+
                "==============================");
        System.out.println(file.isDirectory()?"是目录" : file.isFile() ? "是文件" : "都不是");
        System.out.println("权限是："+file.getPermission());
    }

    @Test
    public void testFileList() throws Exception {
        FileStatus []fss = fs.listStatus(new Path("/hadoop"));
        Path[] paths = FileUtil.stat2Paths(fss);

        for (Path path : paths) {
            System.out.println("路径:" + path);

        }
    }

}
