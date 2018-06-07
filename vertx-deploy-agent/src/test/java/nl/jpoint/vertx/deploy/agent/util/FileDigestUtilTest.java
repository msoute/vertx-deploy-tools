package nl.jpoint.vertx.deploy.agent.util;

import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class FileDigestUtilTest {

    private FileDigestUtil fileDigestUtil;

    @Before
    public void init() {
        fileDigestUtil = new FileDigestUtil();
    }

    @Test
    public void testFileMd5SumEquals_SameFile() throws Exception {
        Path path1 = Paths.get(FileDigestUtilTest.class.getResource("file1.txt").toURI());
        Path path2 = Paths.get(FileDigestUtilTest.class.getResource("file1.txt").toURI());

        byte[] digest1 = fileDigestUtil.getFileMd5Sum(path1);
        byte[] digest2 = fileDigestUtil.getFileMd5Sum(path2);
        assertTrue(Arrays.equals(digest1, digest2));
    }

    @Test
    public void testFileMd5SumEquals_OtherFile_SameContent() throws Exception {
        Path path1 = Paths.get(FileDigestUtilTest.class.getResource("file1.txt").toURI());
        Path path2 = Paths.get(FileDigestUtilTest.class.getResource("file2.txt").toURI());

        byte[] digest1 = fileDigestUtil.getFileMd5Sum(path1);
        byte[] digest2 = fileDigestUtil.getFileMd5Sum(path2);
        assertTrue(Arrays.equals(digest1, digest2));
    }

    @Test
    public void testFileMd5SumNotEquals() throws Exception {
        Path path1 = Paths.get(FileDigestUtilTest.class.getResource("file1.txt").toURI());
        Path path2 = Paths.get(FileDigestUtilTest.class.getResource("file3.txt").toURI());
        byte[] digest1 = fileDigestUtil.getFileMd5Sum(path1);
        byte[] digest2 = fileDigestUtil.getFileMd5Sum(path2);
        assertFalse(Arrays.equals(digest1, digest2));
    }

    @Test
    public void testFileMd5SumNotEquals_NotExistingFile() throws Exception {
        Path path1 = Paths.get(FileDigestUtilTest.class.getResource("file1.txt").toURI());
        Path path2 = Paths.get("doesnotexist.txt");
        byte[] digest1 = fileDigestUtil.getFileMd5Sum(path1);
        byte[] digest2 = fileDigestUtil.getFileMd5Sum(path2);
        assertFalse(Arrays.equals(digest1, digest2));
    }

    @Test
    public void testFileMd5SumNotEquals_NotExistingFileBoth() throws Exception {
        Path path1 = Paths.get("doesnotexist.txt");
        Path path2 = Paths.get("doesnotexist.txt");
        byte[] digest1 = fileDigestUtil.getFileMd5Sum(path1);
        byte[] digest2 = fileDigestUtil.getFileMd5Sum(path2);
        assertTrue(Arrays.equals(digest1, digest2));
    }
}