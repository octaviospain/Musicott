/*
 * This file is part of Musicott software.
 *
 * Musicott software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Musicott library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Musicott. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2015, 2016 Octavio Calleya
 */

package com.transgressoft.musicott.util;

import org.junit.jupiter.api.*;
import org.junit.platform.runner.*;
import org.junit.runner.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Octavio Calleya
 */
@RunWith (JUnitPlatform.class)
public class Utils_GetAllFilesInFolderTest {

    private Path testPath = Paths.get("./test-resources", "/treefoldertest/");
    private List<File> files;

    private int NUM_FOLDERS = 8;
    private int NUM_FILES = 0;

    private int maxFiles;
    private String extension = "txt";

    private FileFilter allFilter = file -> true;
    private FileFilter directoryFilter = File::isDirectory;
    private FileFilter nonDirectoryFilter = file -> ! file.isDirectory();
    private FileFilter extensionFilter = file -> {
        int pos = file.getName().lastIndexOf(".");
        return file.getName().substring(pos + 1).equals(extension);
    };

    private void deleteTestFiles(File folder) {
        for (File file : folder.listFiles())
            if (file.isDirectory()) {
                deleteTestFiles(file);
                assertTrue(file.delete());
            }
            else {
                assertTrue(file.delete());
            }
    }

    private void assertFiles(List<File> files) {
        for (File file : files) {
            int pos = file.getName().lastIndexOf(".");
            assertTrue(extension.equals(file.getName().substring(pos + 1)));
        }
    }

    private void createTestFolders(int numFolders, Path folder) {
        for (int i = 0; i < numFolders; i++)
            assertTrue(new File(folder.toAbsolutePath().toString() + "/" + i + 1).mkdir());
    }

    private void createTestFilesInFolder(String extension, int numFiles, Path folder) throws Exception {
        for (int i = 0; i < numFiles; i++)
            assertTrue(
                    new File(folder.toAbsolutePath().toString() + "/" + i + 1 + "." + extension).createNewFile());
    }

    @Test
    @DisplayName ("with non existent folder")
    void nonExistentFolderArgumentTest() {
        File badFolder = new File("/nonexistenfolder");
        IllegalArgumentException exception = expectThrows(IllegalArgumentException.class,
                                                          () -> Utils.getAllFilesInFolder(badFolder, allFilter, 0));
        assertEquals("rootFolder argument is not a directory", exception.getMessage());
    }

    @Test
    @DisplayName ("with a file as argument")
    void givenFileAsArgumentTest() {
        File badFolder = new File("/test-resources/testfiles/testeable.mp3");
        IllegalArgumentException exception = expectThrows(IllegalArgumentException.class,
                                                          () -> Utils.getAllFilesInFolder(badFolder, allFilter, 0));
        assertEquals("rootFolder argument is not a directory", exception.getMessage());
    }

    @Test
    @DisplayName ("with null folder")
    void givenNullFolderTest() {
        IllegalArgumentException exception = expectThrows(IllegalArgumentException.class,
                                                          () -> Utils.getAllFilesInFolder(null, allFilter, 0));
        assertEquals("folder or filter null", exception.getMessage());
    }

    @Test
    @DisplayName ("with null filter")
    void givenNullFilterTest() {
        IllegalArgumentException exception = expectThrows(IllegalArgumentException.class,
                                                          () -> Utils.getAllFilesInFolder(testPath.toFile(), null, 0));
        assertEquals("folder or filter null", exception.getMessage());
    }

    @Test
    @DisplayName ("with negative max files")
    void givenNegativeMaxFilesTest() {
        IllegalArgumentException exception = expectThrows(IllegalArgumentException.class, () -> Utils
                .getAllFilesInFolder(testPath.toFile(), allFilter, - 1));
        assertEquals("maxFilesRequired argument less than zero", exception.getMessage());
    }

    @Nested
    @DisplayName ("One folder depth")
            /**
             *  test-folder/
             *      ...
             *      ...
             *      ...
             */
    class DepthOneTest {

        @Nested
        @DisplayName ("with only folders")
                /**
                 *  test-folder/
                 *      1/
                 *      2/
                 *      3/
                 *      ...
                 */
        class OnlyFolders {

            @BeforeEach
            void beforeEachTest() throws Exception {
                NUM_FOLDERS = 8;
                NUM_FILES = 0;
                assertTrue(testPath.toFile().mkdirs());
                createTestFolders(NUM_FOLDERS, testPath);
                createTestFilesInFolder(extension, NUM_FILES, testPath);
            }

            @AfterEach
            void afterEachTest() {
                deleteTestFiles(testPath.toFile());
                assertTrue(testPath.toFile().delete());
                assertFalse(testPath.toFile().exists());
            }

            @Test
            @DisplayName ("accept all with no max")
            void acceptAllNoMax() {
                maxFiles = 0;
                files = Utils.getAllFilesInFolder(testPath.toFile(), allFilter, maxFiles);
                assertEquals(NUM_FOLDERS, files.size());
            }

            @Test
            @DisplayName ("accept all with 1 max")
            void acceptAll1Max() {
                maxFiles = 1;
                files = Utils.getAllFilesInFolder(testPath.toFile(), allFilter, maxFiles);
                assertEquals(maxFiles, files.size());
            }

            @Test
            @DisplayName ("accept all with 5 max")
            void acceptAll5Max() {
                maxFiles = 5;
                files = Utils.getAllFilesInFolder(testPath.toFile(), allFilter, maxFiles);
                assertEquals(maxFiles, files.size());
            }

            @Test
            @DisplayName ("accept all with max greater than num files")
            void acceptAllMoreThanMax() {
                maxFiles = NUM_FOLDERS + 2;
                files = Utils.getAllFilesInFolder(testPath.toFile(), allFilter, maxFiles);
                assertEquals(NUM_FOLDERS, files.size());
            }

            @Test
            @DisplayName ("accept folder with no max")
            void acceptFoldersNoMax() {
                maxFiles = 0;
                files = Utils.getAllFilesInFolder(testPath.toFile(), directoryFilter, maxFiles);
                assertEquals(NUM_FOLDERS, files.size());
                for (File file : files)
                    assertTrue(file.isDirectory());
            }

            @Test
            @DisplayName ("accept folder with 1 max")
            void acceptFolders1Max() {
                maxFiles = 1;
                files = Utils.getAllFilesInFolder(testPath.toFile(), directoryFilter, maxFiles);
                assertEquals(maxFiles, files.size());
                for (File file : files)
                    assertTrue(file.isDirectory());
            }

            @Test
            @DisplayName ("accept folder with 5 max")
            void acceptFolders5Max() {
                maxFiles = 5;
                files = Utils.getAllFilesInFolder(testPath.toFile(), directoryFilter, maxFiles);
                assertEquals(maxFiles, files.size());
                for (File file : files)
                    assertTrue(file.isDirectory());
            }

            @Test
            @DisplayName ("accept folder with max greater than num files")
            void acceptFoldersMoreThanMax() {
                maxFiles = NUM_FOLDERS + 2;
                files = Utils.getAllFilesInFolder(testPath.toFile(), directoryFilter, maxFiles);
                assertEquals(NUM_FOLDERS, files.size());
                for (File file : files)
                    assertTrue(file.isDirectory());
            }

            @Test
            @DisplayName ("accept no folders with no max")
            void acceptNoFoldersNoMax() {
                maxFiles = 0;
                files = Utils.getAllFilesInFolder(testPath.toFile(), nonDirectoryFilter, maxFiles);
                assertEquals(0, files.size());
            }

            @Test
            @DisplayName ("accept no folders with 1 max")
            void acceptNoFolders1Max() {
                maxFiles = 1;
                files = Utils.getAllFilesInFolder(testPath.toFile(), nonDirectoryFilter, maxFiles);
                assertEquals(0, files.size());
            }

            @Test
            @DisplayName ("accept no folders with 5 max")
            void acceptNoFolders5Max() {
                maxFiles = 5;
                files = Utils.getAllFilesInFolder(testPath.toFile(), nonDirectoryFilter, maxFiles);
                assertEquals(0, files.size());
            }

            @Test
            @DisplayName ("accept no folders with max files greater than num files")
            void acceptNoFoldersMoreThanMax() {
                maxFiles = NUM_FOLDERS + 2;
                files = Utils.getAllFilesInFolder(testPath.toFile(), nonDirectoryFilter, maxFiles);
                assertEquals(0, files.size());
            }
        }

        @Nested
        @DisplayName ("with one file and the rest are folders")
                /**
                 *  test-folder/
                 *      1.txt
                 *      1/
                 *      2/
                 *      3/
                 *      ...
                 */
        class OneFileRestFolders {

            @BeforeEach
            void beforeEachTest() throws Exception {
                NUM_FOLDERS = 7;
                NUM_FILES = 1;
                assertTrue(testPath.toFile().mkdirs());
                createTestFolders(NUM_FOLDERS, testPath);
                createTestFilesInFolder(extension, NUM_FILES, testPath);
            }

            @AfterEach
            void afterEachTest() {
                deleteTestFiles(testPath.toFile());
                assertTrue(testPath.toFile().delete());
                assertFalse(testPath.toFile().exists());
            }

            @Test
            @DisplayName ("accept no folders and no max")
            void acceptNoFoldersMoreThanMax() {
                maxFiles = 0;
                files = Utils.getAllFilesInFolder(testPath.toFile(), nonDirectoryFilter, maxFiles);
                assertEquals(1, files.size());
                assertTrue(! files.get(0).isDirectory());
            }

            @Test
            @DisplayName ("accept no folders and 1 max")
            void acceptNoFolders1Max() {
                maxFiles = 1;
                files = Utils.getAllFilesInFolder(testPath.toFile(), nonDirectoryFilter, maxFiles);
                assertEquals(maxFiles, files.size());
                assertTrue(! files.get(0).isDirectory());
            }

            @Test
            @DisplayName ("accept no folders and 5 max")
            void acceptNoFolders5Max() {
                maxFiles = 5;
                files = Utils.getAllFilesInFolder(testPath.toFile(), nonDirectoryFilter, maxFiles);
                assertEquals(1, files.size());
                assertTrue(! files.get(0).isDirectory());
            }

            @Test
            @DisplayName ("accept no folders and max files greater than num files")
            void acceptNoFoldersMoretThanMax() {
                maxFiles = NUM_FOLDERS + 2;
                files = Utils.getAllFilesInFolder(testPath.toFile(), nonDirectoryFilter, maxFiles);
                assertEquals(1, files.size());
                assertTrue(! files.get(0).isDirectory());
            }
        }

        @Nested
        @DisplayName ("with one folder and the rest are files")
                /**
                 *  test-folder/
                 *      1/
                 *      1.txt
                 *      2.txt
                 *      3.txt
                 *      ...
                 */
        class OneFolderRestFiles {

            @BeforeEach
            void beforeEachTest() throws Exception {
                NUM_FOLDERS = 1;
                NUM_FILES = 7;
                assertTrue(testPath.toFile().mkdirs());
                createTestFolders(NUM_FOLDERS, testPath);
                createTestFilesInFolder(extension, NUM_FILES, testPath);
            }

            @AfterEach
            void afterEachTest() {
                deleteTestFiles(testPath.toFile());
                assertTrue(testPath.toFile().delete());
                assertFalse(testPath.toFile().exists());
            }

            @Test
            @DisplayName ("accept no folder and with no max")
            void acceptNoFolderNoMax() {
                maxFiles = 0;
                files = Utils.getAllFilesInFolder(testPath.toFile(), nonDirectoryFilter, maxFiles);
                assertEquals(NUM_FILES, files.size());
                for (File file : files)
                    assertTrue(! file.isDirectory());
            }

            @Test
            @DisplayName ("accept no folder and with 1 max")
            void acceptNoFolder1Max() {
                maxFiles = 1;
                files = Utils.getAllFilesInFolder(testPath.toFile(), nonDirectoryFilter, maxFiles);
                assertEquals(1, files.size());
                for (File file : files)
                    assertTrue(! file.isDirectory());
            }

            @Test
            @DisplayName ("accept no folder and with 5 max")
            void acceptNoFolder5Max() {
                maxFiles = 5;
                files = Utils.getAllFilesInFolder(testPath.toFile(), nonDirectoryFilter, maxFiles);
                assertEquals(maxFiles, files.size());
                for (File file : files)
                    assertTrue(! file.isDirectory());
            }

            @Test
            @DisplayName ("accept no folder and with max files greater than num files")
            void acceptNoFolderMoreThanMax() {
                maxFiles = NUM_FILES + 2;
                files = Utils.getAllFilesInFolder(testPath.toFile(), nonDirectoryFilter, maxFiles);
                assertEquals(NUM_FILES, files.size());
                for (File file : files)
                    assertTrue(! file.isDirectory());
            }
        }

        @Nested
        @DisplayName ("with files in one extension and folders")
                /**
                 *  test-folder/
                 *      1/
                 *      1.txt
                 *      2/
                 *      2.txt
                 *      3/
                 *      3.txt
                 *      ...
                 */
        class FilesOneExtensionAndFolders {

            @BeforeEach
            void beforeEachTest() throws Exception {
                extension = "txt";
                NUM_FOLDERS = 8;
                NUM_FILES = 8;
                assertTrue(testPath.toFile().mkdirs());
                createTestFolders(NUM_FOLDERS, testPath);
                createTestFilesInFolder(extension, NUM_FILES, testPath);
            }

            @AfterEach
            void afterEachTest() {
                deleteTestFiles(testPath.toFile());
                assertTrue(testPath.toFile().delete());
                assertFalse(testPath.toFile().exists());
            }

            @Test
            @DisplayName ("accept extension with no max")
            void acceptExtensionNoMax() {
                maxFiles = 0;
                files = Utils.getAllFilesInFolder(testPath.toFile(), extensionFilter, maxFiles);
                assertEquals(NUM_FILES, files.size());
                for (File file : files) {
                    int pos = file.getName().lastIndexOf(".");
                    assertTrue(file.getName().substring(pos + 1).equals(extension));
                }
            }

            @Test
            @DisplayName ("accept extension with 1 max")
            void acceptExtension1Max() {
                maxFiles = 1;
                files = Utils.getAllFilesInFolder(testPath.toFile(), extensionFilter, maxFiles);
                assertEquals(1, files.size());
                for (File file : files) {
                    int pos = file.getName().lastIndexOf(".");
                    assertTrue(file.getName().substring(pos + 1).equals(extension));
                }
            }

            @Test
            @DisplayName ("accept extension with 5 max")
            void acceptExtension5Max() {
                maxFiles = 5;
                files = Utils.getAllFilesInFolder(testPath.toFile(), extensionFilter, maxFiles);
                assertEquals(5, files.size());
                for (File file : files) {
                    int pos = file.getName().lastIndexOf(".");
                    assertTrue(file.getName().substring(pos + 1).equals(extension));
                }
            }

            @Test
            @DisplayName ("accept extension with max files greater than num files")
            void acceptExtensionMoreThanMax() {
                maxFiles = NUM_FILES + 5;
                files = Utils.getAllFilesInFolder(testPath.toFile(), extensionFilter, maxFiles);
                assertEquals(NUM_FILES, files.size());
                for (File file : files) {
                    int pos = file.getName().lastIndexOf(".");
                    assertTrue(file.getName().substring(pos + 1).equals(extension));
                }
            }
        }

        @Nested
        @DisplayName ("with files of various extensions and folders")
                /**
                 *  test-folder/
                 *      1/
                 *      1.txt
                 *      1.arl
                 *      2/
                 *      2.txt
                 *      2.arl
                 *      3/
                 *      3.txt
                 *      3.arl
                 *      ...
                 */
        class FilesVariousExtensionsAndFolders {

            @BeforeEach
            void beforeEachTest() throws Exception {
                NUM_FOLDERS = 8;
                NUM_FILES = 8;
                assertTrue(testPath.toFile().mkdirs());
                createTestFolders(NUM_FOLDERS, testPath);
                createTestFilesInFolder(extension, NUM_FILES, testPath);
                createTestFilesInFolder("arl", NUM_FILES, testPath);
            }

            @AfterEach
            void afterEachTest() {
                deleteTestFiles(testPath.toFile());
                assertTrue(testPath.toFile().delete());
                assertFalse(testPath.toFile().exists());
            }

            @Test
            @DisplayName ("accept extension with no max")
            void acceptExtensionNoMax() {
                maxFiles = 0;
                files = Utils.getAllFilesInFolder(testPath.toFile(), extensionFilter, maxFiles);
                assertEquals(NUM_FILES, files.size());
                for (File file : files) {
                    int pos = file.getName().lastIndexOf(".");
                    assertTrue(file.getName().substring(pos + 1).equals(extension));
                }
            }

            @Test
            @DisplayName ("accept extension with 1 max")
            void acceptExtension1Max() {
                maxFiles = 1;
                files = Utils.getAllFilesInFolder(testPath.toFile(), extensionFilter, maxFiles);
                assertEquals(1, files.size());
                for (File file : files) {
                    int pos = file.getName().lastIndexOf(".");
                    assertTrue(file.getName().substring(pos + 1).equals(extension));
                }
            }

            @Test
            @DisplayName ("accept extension with 5 max")
            void acceptExtension5Max() {
                maxFiles = 5;
                files = Utils.getAllFilesInFolder(testPath.toFile(), extensionFilter, maxFiles);
                assertEquals(5, files.size());
                for (File file : files) {
                    int pos = file.getName().lastIndexOf(".");
                    assertTrue(file.getName().substring(pos + 1).equals(extension));
                }
            }

            @Test
            @DisplayName ("accept extension with max files greather than num files")
            void acceptExtensionMoreThanMax() {
                maxFiles = NUM_FILES + 5;
                files = Utils.getAllFilesInFolder(testPath.toFile(), extensionFilter, maxFiles);
                assertEquals(NUM_FILES, files.size());
                for (File file : files) {
                    int pos = file.getName().lastIndexOf(".");
                    assertTrue(file.getName().substring(pos + 1).equals(extension));
                }
            }
        }

        @Nested
        @DisplayName ("with only files of one unmatched extension")
                /**
                 *  test-folder/
                 *      1.arl
                 *      2.arl
                 *      3.arl
                 *      ...
                 */
        class OnlyFilesNonMatchExtension {

            @BeforeEach
            void beforeEachTest() throws Exception {
                NUM_FOLDERS = 0;
                NUM_FILES = 8;
                assertTrue(testPath.toFile().mkdirs());
                createTestFolders(NUM_FOLDERS, testPath);
                createTestFilesInFolder("arl", NUM_FILES, testPath);
            }

            @AfterEach
            void afterEachTest() {
                deleteTestFiles(testPath.toFile());
                assertTrue(testPath.toFile().delete());
                assertFalse(testPath.toFile().exists());
            }

            @Test
            @DisplayName ("accept other extension with no max")
            void acceptExtensionNoMax() {
                maxFiles = 0;
                files = Utils.getAllFilesInFolder(testPath.toFile(), extensionFilter, maxFiles);
                assertEquals(0, files.size());
            }

            @Test
            @DisplayName ("accept other extension with 1 max")
            void acceptExtension1Max() {
                maxFiles = 1;
                files = Utils.getAllFilesInFolder(testPath.toFile(), extensionFilter, maxFiles);
                assertEquals(0, files.size());
            }

            @Test
            @DisplayName ("accept other extension with 5 max")
            void acceptExtension5Max() {
                maxFiles = 5;
                files = Utils.getAllFilesInFolder(testPath.toFile(), extensionFilter, maxFiles);
                assertEquals(0, files.size());
            }

            @Test
            @DisplayName ("accept other extension with max files greather than num files")
            void acceptExtensionMoreThanMax() {
                maxFiles = NUM_FILES + 5;
                files = Utils.getAllFilesInFolder(testPath.toFile(), extensionFilter, maxFiles);
                assertEquals(0, files.size());
            }
        }

        @Nested
        @DisplayName ("with only files of one matched extension")
                /**
                 *  test-folder/
                 *      1.txt
                 *      2.txt
                 *      3.txt
                 *      ...
                 */
        class OnlyFilesOneMatchExtension {

            @BeforeEach
            void beforeEachTest() throws Exception {
                NUM_FOLDERS = 0;
                NUM_FILES = 8;
                assertTrue(testPath.toFile().mkdirs());
                createTestFolders(NUM_FOLDERS, testPath);
                createTestFilesInFolder(extension, NUM_FILES, testPath);
            }

            @AfterEach
            void afterEachTest() {
                deleteTestFiles(testPath.toFile());
                assertTrue(testPath.toFile().delete());
                assertFalse(testPath.toFile().exists());
            }

            @Test
            @DisplayName ("accept other extension with no max")
            void acceptExtensionNoMax() {
                maxFiles = 0;
                files = Utils.getAllFilesInFolder(testPath.toFile(), extensionFilter, maxFiles);
                assertEquals(NUM_FILES, files.size());
            }

            @Test
            @DisplayName ("accept other extension with 1 max")
            void acceptExtension1Max() {
                maxFiles = 1;
                files = Utils.getAllFilesInFolder(testPath.toFile(), extensionFilter, maxFiles);
                assertEquals(1, files.size());
            }

            @Test
            @DisplayName ("accept other extension with 5 max")
            void acceptExtension5Max() {
                maxFiles = 5;
                files = Utils.getAllFilesInFolder(testPath.toFile(), extensionFilter, maxFiles);
                assertEquals(5, files.size());
            }

            @Test
            @DisplayName ("accept other extension with max files greather than num files")
            void acceptExtensionMoreThanMax() {
                maxFiles = NUM_FILES + 5;
                files = Utils.getAllFilesInFolder(testPath.toFile(), extensionFilter, maxFiles);
                assertEquals(NUM_FILES, files.size());
            }
        }
    }

    @Nested
    @DisplayName ("Two folders depth")
            /**
             *  test-folder/
             *      1/
             *          ...
             *      2/
             *          ...
             *      3/
             *          ...
             */
    class DepthTwoTest {

        @Nested
        @DisplayName ("with only folders")
                /**
                 *  test-folder/
                 *      1/
                 *          1/
                 *          2/
                 *          3/
                 *          ...
                 *      2/
                 *          1/
                 *          2/
                 *          3/
                 *          ...
                 *      3/
                 *          1/
                 *          2/
                 *          3/
                 *          ...
                 */
        class OnlyFoldersInFolders {

            @BeforeEach
            void beforeEachTest() throws Exception {
                NUM_FOLDERS = 8;
                NUM_FILES = 0;
                assertTrue(testPath.toFile().mkdirs());
                createTestFolders(NUM_FOLDERS, testPath);
                for (File folder: testPath.toFile().listFiles())
                    if (folder.isDirectory())
                        createTestFolders(NUM_FOLDERS, folder.toPath());
                createTestFilesInFolder(extension, NUM_FILES, testPath);
            }

            @AfterEach
            void afterEachTest() {
                deleteTestFiles(testPath.toFile());
                assertTrue(testPath.toFile().delete());
                assertFalse(testPath.toFile().exists());
            }

            @Test
            @DisplayName ("accept all with no max")
            void acceptAllNoMax() {
                maxFiles = 0;
                files = Utils.getAllFilesInFolder(testPath.toFile(), allFilter, maxFiles);
                assertEquals(NUM_FOLDERS * NUM_FOLDERS + NUM_FOLDERS, files.size());
            }

            @Test
            @DisplayName ("accept all with 1 max")
            void acceptAll1Max() {
                maxFiles = 1;
                files = Utils.getAllFilesInFolder(testPath.toFile(), allFilter, maxFiles);
                assertEquals(maxFiles, files.size());
            }

            @Test
            @DisplayName ("accept all with 5 max")
            void acceptAll5Max() {
                maxFiles = 5;
                files = Utils.getAllFilesInFolder(testPath.toFile(), allFilter, maxFiles);
                assertEquals(maxFiles, files.size());
            }

            @Test
            @DisplayName ("accept all with max greater than num files")
            void acceptAllMoreThanMax() {
                maxFiles = NUM_FOLDERS * NUM_FOLDERS + NUM_FOLDERS + 2;
                files = Utils.getAllFilesInFolder(testPath.toFile(), allFilter, maxFiles);
                assertEquals(NUM_FOLDERS * NUM_FOLDERS + NUM_FOLDERS, files.size());
            }

            @Test
            @DisplayName ("accept folder with no max")
            void acceptFoldersNoMax() {
                maxFiles = 0;
                files = Utils.getAllFilesInFolder(testPath.toFile(), directoryFilter, maxFiles);
                assertEquals(NUM_FOLDERS * NUM_FOLDERS + NUM_FOLDERS, files.size());
                for (File file : files)
                    assertTrue(file.isDirectory());
            }

            @Test
            @DisplayName ("accept folder with 1 max")
            void acceptFolders1Max() {
                maxFiles = 1;
                files = Utils.getAllFilesInFolder(testPath.toFile(), directoryFilter, maxFiles);
                assertEquals(maxFiles, files.size());
                for (File file : files)
                    assertTrue(file.isDirectory());
            }

            @Test
            @DisplayName ("accept folder with 5 max")
            void acceptFolders5Max() {
                maxFiles = 5;
                files = Utils.getAllFilesInFolder(testPath.toFile(), directoryFilter, maxFiles);
                assertEquals(maxFiles, files.size());
                for (File file : files)
                    assertTrue(file.isDirectory());
            }

            @Test
            @DisplayName ("accept folder with max greater than num files")
            void acceptFoldersMoreThanMax() {
                maxFiles = NUM_FOLDERS * NUM_FOLDERS + NUM_FOLDERS + 2;
                files = Utils.getAllFilesInFolder(testPath.toFile(), directoryFilter, maxFiles);
                assertEquals(NUM_FOLDERS * NUM_FOLDERS + NUM_FOLDERS, files.size());
                for (File file : files)
                    assertTrue(file.isDirectory());
            }

            @Test
            @DisplayName ("accept no folders with no max")
            void acceptNoFoldersNoMax() {
                maxFiles = 0;
                files = Utils.getAllFilesInFolder(testPath.toFile(), nonDirectoryFilter, maxFiles);
                assertEquals(0, files.size());
            }

            @Test
            @DisplayName ("accept no folders with 1 max")
            void acceptNoFolders1Max() {
                maxFiles = 1;
                files = Utils.getAllFilesInFolder(testPath.toFile(), nonDirectoryFilter, maxFiles);
                assertEquals(0, files.size());
            }

            @Test
            @DisplayName ("accept no folders with 5 max")
            void acceptNoFolders5Max() {
                maxFiles = 5;
                files = Utils.getAllFilesInFolder(testPath.toFile(), nonDirectoryFilter, maxFiles);
                assertEquals(0, files.size());
            }

            @Test
            @DisplayName ("accept no folders with max files greater than num files")
            void acceptNoFoldersMoreThanMax() {
                maxFiles = NUM_FOLDERS + 2;
                files = Utils.getAllFilesInFolder(testPath.toFile(), nonDirectoryFilter, maxFiles);
                assertEquals(0, files.size());
            }
        }

        @Nested
        @DisplayName ("with files in each folder")
                /**
                 *  test-folder/
                 *      1/
                 *          1.txt
                 *          2.txt
                 *          3.txt
                 *          ...
                 *      2/
                 *          1.txt
                 *          2.txt
                 *          3.txt
                 *          ...
                 *      3/
                 *          1.txt
                 *          2.txt
                 *          3.txt
                 *          ...
                 *      ...
                 */
        class OnlyFilesInFolders {

            @BeforeEach
            void beforeEachTest() throws Exception {
                NUM_FOLDERS = 8;
                NUM_FILES = 8;
                assertTrue(testPath.toFile().mkdirs());
                createTestFolders(NUM_FOLDERS, testPath);
                for (File folder: testPath.toFile().listFiles())
                    if (folder.isDirectory())
                        createTestFilesInFolder(extension, NUM_FILES, folder.toPath());
            }

            @AfterEach
            void afterEachTest() {
                deleteTestFiles(testPath.toFile());
                assertTrue(testPath.toFile().delete());
                assertFalse(testPath.toFile().exists());
            }

            @Nested
            @DisplayName ("accept all")
            class AcceptAll {

                @Test
                @DisplayName ("and no max")
                void NoMax() {
                    maxFiles = 0;
                    files = Utils.getAllFilesInFolder(testPath.toFile(), allFilter, maxFiles);
                    assertEquals(NUM_FOLDERS * NUM_FILES + NUM_FOLDERS, files.size());
                }

                @Test
                @DisplayName ("and 1 max")
                void OneMax() {
                    maxFiles = 1;
                    files = Utils.getAllFilesInFolder(testPath.toFile(), allFilter, maxFiles);
                    assertEquals(1, files.size());
                }

                @Test
                @DisplayName ("and 5 max")
                void FiveMax() {
                    maxFiles = 5;
                    files = Utils.getAllFilesInFolder(testPath.toFile(), allFilter, maxFiles);
                    assertEquals(5, files.size());
                }

                @Test
                @DisplayName ("max files greater than num files")
                void MoreThanMax() {
                    maxFiles = NUM_FOLDERS * NUM_FILES + NUM_FOLDERS + 2;
                    files = Utils.getAllFilesInFolder(testPath.toFile(), allFilter, maxFiles);
                    assertEquals(NUM_FOLDERS * NUM_FILES + NUM_FOLDERS, files.size());
                }
            }

            @Nested
            @DisplayName ("accept files")
            class AcceptFiles {

                @Test
                @DisplayName ("and no max")
                void NoMax() {
                    maxFiles = 0;
                    files = Utils.getAllFilesInFolder(testPath.toFile(), nonDirectoryFilter, maxFiles);
                    assertEquals(NUM_FOLDERS * NUM_FILES, files.size());
                }

                @Test
                @DisplayName ("and 1 max")
                void OneMax() {
                    maxFiles = 1;
                    files = Utils.getAllFilesInFolder(testPath.toFile(), nonDirectoryFilter, maxFiles);
                    assertEquals(1, files.size());
                }

                @Test
                @DisplayName ("and 5 max")
                void FiveMax() {
                    maxFiles = 5;
                    files = Utils.getAllFilesInFolder(testPath.toFile(), nonDirectoryFilter, maxFiles);
                    assertEquals(5, files.size());
                }

                @Test
                @DisplayName ("max files greater than num files")
                void MoreThanMax() {
                    maxFiles = NUM_FOLDERS * NUM_FILES + 2;
                    files = Utils.getAllFilesInFolder(testPath.toFile(), nonDirectoryFilter, maxFiles);
                    assertEquals(NUM_FOLDERS * NUM_FILES, files.size());
                }
            }
        }
    }
}