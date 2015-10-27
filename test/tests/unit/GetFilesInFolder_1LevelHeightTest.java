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
 */

package tests.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import com.musicott.util.Utils;

/**
 * Tests for {@link com.musicott.util.Utils#getAllFilesInFolder} in whith the folder tree
 * to scan has 1 level height like:
 * 
 *   -rootFolder/
 *     -1/
 * 	   -2/
 *     -1.txt
 *     -2.txt
 *     -3/
 *
 * @author Octavio Calleya
 *
 */
public class GetFilesInFolder_1LevelHeightTest extends GetFilesInFolder_BaseTest {
	
	@Test(expected = IllegalArgumentException.class)
	public void nonExistentFolderArgumentTest() throws Exception {
		File badFolder = new File("/users/octaviospain/445");
		Utils.getAllFilesInFolder(badFolder, allFilter, 0);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void notExistendFolderArgumentTest() throws Exception {
		File badFolder = new File("/users/octavio/test/testeable.mp3");
		Utils.getAllFilesInFolder(badFolder, allFilter, 0);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void badMaxFilesNumberArgumentTest() throws Exception {
		File folder = new File("/users/octavio/test/");
		Utils.getAllFilesInFolder(folder, allFilter, -1);
	}
	
	@Test
	public void OnlyFolders_AcceptAll_NoMax() throws Exception {
		NUM_FOLDERS= 8;
		File[] testFolders = new File[NUM_FOLDERS];
		for(int i=0; i<NUM_FOLDERS; i++) {
			testFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			testFolders[i].mkdir();
		}
		files = Utils.getAllFilesInFolder(rootFolder, allFilter, 0);
		
		assertEquals(NUM_FOLDERS, files.size());
	}
	
	@Test
	public void OnlyFolders_AcceptAll_1Max() throws Exception {
		NUM_FOLDERS= 8;
		MAX_FILES = 1;
		File[] testFolders = new File[NUM_FOLDERS];
		for(int i=0; i<NUM_FOLDERS; i++) {
			testFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			testFolders[i].mkdir();
		}
		files = Utils.getAllFilesInFolder(rootFolder, allFilter, MAX_FILES);

		assertEquals(MAX_FILES, files.size());
	}
	
	@Test
	public void OnlyFolders_AcceptAll_5Max() throws Exception {
		NUM_FOLDERS= 8;
		MAX_FILES = 5;
		File[] testFolders = new File[NUM_FOLDERS];
		for(int i=0; i<NUM_FOLDERS; i++) {
			testFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			testFolders[i].mkdir();
		}
		files = Utils.getAllFilesInFolder(rootFolder, allFilter, MAX_FILES);

		assertEquals(MAX_FILES, files.size());
	}
	
	@Test
	public void OnlyFolders_AcceptAll_MoreThanMax() throws Exception {
		NUM_FOLDERS= 8;
		MAX_FILES = 10;
		File[] testFolders = new File[NUM_FOLDERS];
		for(int i=0; i<NUM_FOLDERS; i++) {
			testFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			testFolders[i].mkdir();
		}
		files = Utils.getAllFilesInFolder(rootFolder, allFilter, MAX_FILES);

		assertEquals(NUM_FOLDERS, files.size());
	}
	
	@Test
	public void OnlyFolders_AcceptFolder_NoMax() throws Exception {
		NUM_FOLDERS= 8;
		File[] testFolders = new File[NUM_FOLDERS];
		for(int i=0; i<NUM_FOLDERS; i++) {
			testFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			testFolders[i].mkdir();
		}
		files = Utils.getAllFilesInFolder(rootFolder, directoryFilter, 0);

		assertEquals(NUM_FOLDERS, files.size());
		for(File f: files)
			assertTrue(f.isDirectory());
	}
	
	@Test
	public void OnlyFolders_AcceptFolder_1Max() throws Exception {
		NUM_FOLDERS= 8;
		MAX_FILES = 1;
		File[] testFolders = new File[NUM_FOLDERS];
		for(int i=0; i<NUM_FOLDERS; i++) {
			testFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			testFolders[i].mkdir();
		}
		files = Utils.getAllFilesInFolder(rootFolder, directoryFilter, MAX_FILES);

		assertEquals(MAX_FILES, files.size());
		for(File f: files)
			assertTrue(f.isDirectory());
	}
	
	@Test
	public void OnlyFolders_AcceptFolder_5Max() throws Exception {
		NUM_FOLDERS= 8;
		MAX_FILES = 5;
		File[] testFolders = new File[NUM_FOLDERS];
		for(int i=0; i<NUM_FOLDERS; i++) {
			testFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			testFolders[i].mkdir();
		}
		files = Utils.getAllFilesInFolder(rootFolder, directoryFilter, MAX_FILES);
		
		assertEquals(MAX_FILES, files.size());
		for(File f: files)
			assertTrue(f.isDirectory());
	}
	
	@Test
	public void OnlyFolders_AcceptFolder_MoreThanMax() throws Exception {
		NUM_FOLDERS= 8;
		MAX_FILES = 10;
		File[] testFolders = new File[NUM_FOLDERS];
		for(int i=0; i<NUM_FOLDERS; i++) {
			testFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			testFolders[i].mkdir();
		}
		files = Utils.getAllFilesInFolder(rootFolder, directoryFilter, MAX_FILES);
		
		assertTrue(files.size() == NUM_FOLDERS);
		for(File f: files)
			assertTrue(f.isDirectory());
	}
	
	@Test
	public void OnlyFolders_AcceptNoFolder_NoMax() throws Exception {
		NUM_FOLDERS= 8;
		File[] testFolders = new File[NUM_FOLDERS];
		for(int i=0; i<NUM_FOLDERS; i++) {
			testFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			testFolders[i].mkdir();
		}
		files = Utils.getAllFilesInFolder(rootFolder, file -> {return !file.isDirectory();}, 0);

		assertEquals(0, files.size());
	}
	
	@Test
	public void OnlyFolders_AcceptNoFolder_1Max() throws Exception {
		NUM_FOLDERS= 8;
		MAX_FILES = 1;
		File[] testFolders = new File[NUM_FOLDERS];
		for(int i=0; i<NUM_FOLDERS; i++) {
			testFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			testFolders[i].mkdir();
		}
		files = Utils.getAllFilesInFolder(rootFolder, file -> {return !file.isDirectory();}, MAX_FILES);

		assertEquals(0, files.size());
	}
	
	@Test
	public void OnlyFolders_AcceptNoFolder_5Max() throws Exception {
		NUM_FOLDERS= 8;
		MAX_FILES = 5;
		File[] testFolders = new File[NUM_FOLDERS];
		for(int i=0; i<NUM_FOLDERS; i++) {
			testFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			testFolders[i].mkdir();
		}
		files = Utils.getAllFilesInFolder(rootFolder, file -> {return !file.isDirectory();}, MAX_FILES);

		assertEquals(0, files.size());
	}
	
	@Test
	public void OnlyFolders_AcceptNoFolder_MoreThanMax() throws Exception {
		NUM_FOLDERS= 8;
		MAX_FILES = 10;
		File[] testFolders = new File[NUM_FOLDERS];
		for(int i=0; i<NUM_FOLDERS; i++) {
			testFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			testFolders[i].mkdir();
		}
		files = Utils.getAllFilesInFolder(rootFolder, file -> {return !file.isDirectory();}, MAX_FILES);

		assertEquals(0, files.size());
	}
	
	@Test
	public void OneFileRestFolders_AcceptNoFolder_NoMax() throws Exception {
		NUM_FOLDERS= 8;
		File someValidFile = new File(rootFolder.getAbsolutePath()+"/somefile.txt");
		assertTrue(someValidFile.createNewFile());
		File[] testFolders = new File[NUM_FOLDERS];
		for(int i=0; i<NUM_FOLDERS; i++) {
			testFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			testFolders[i].mkdir();
		}
		files = Utils.getAllFilesInFolder(rootFolder, file -> {return !file.isDirectory();}, 0);

		assertEquals(1, files.size());
		assertTrue(!files.get(0).isDirectory());
	}
	
	@Test
	public void OneFileRestFolders_AcceptNoFolder_1Max() throws Exception {
		NUM_FOLDERS= 8;
		MAX_FILES = 1;
		File someValidFile = new File(rootFolder.getAbsolutePath()+"/somefile.txt");
		assertTrue(someValidFile.createNewFile());
		File[] testFolders = new File[NUM_FOLDERS];
		for(int i=0; i<NUM_FOLDERS; i++) {
			testFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			testFolders[i].mkdir();
		}
		files = Utils.getAllFilesInFolder(rootFolder, file -> {return !file.isDirectory();}, MAX_FILES);

		assertEquals(MAX_FILES, files.size());
		assertTrue(!files.get(0).isDirectory());
	}
	
	@Test
	public void OneFileRestFolders_AcceptNoFolder_5Max() throws Exception {
		NUM_FOLDERS= 8;
		MAX_FILES = 5;
		File someValidFile = new File(rootFolder.getAbsolutePath()+"/somefile.txt");
		assertTrue(someValidFile.createNewFile());
		File[] testFolders = new File[NUM_FOLDERS];
		for(int i=0; i<NUM_FOLDERS; i++) {
			testFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			testFolders[i].mkdir();
		}
		files = Utils.getAllFilesInFolder(rootFolder, file -> {return !file.isDirectory();}, MAX_FILES);

		assertEquals(1, files.size());
		assertTrue(!files.get(0).isDirectory());
	}
	
	@Test
	public void OneFileRestFolders_AcceptNoFolder_MoreThanMax() throws Exception {
		NUM_FOLDERS= 8;
		MAX_FILES = 10;
		File someValidFile = new File(rootFolder.getAbsolutePath()+"/somefile.txt");
		assertTrue(someValidFile.createNewFile());
		File[] testFolders = new File[NUM_FOLDERS];
		for(int i=0; i<NUM_FOLDERS; i++) {
			testFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			testFolders[i].mkdir();
		}
		files = Utils.getAllFilesInFolder(rootFolder, file -> {return !file.isDirectory();}, MAX_FILES);

		assertEquals(1, files.size());
		assertTrue(!files.get(0).isDirectory());
	}
	
	@Test
	public void OneFolderRestFiles_AcceptNoFolder_NoMax() throws Exception {
		NUM_FILES = 8;
		File someFolder = new File(rootFolder.getAbsolutePath()+"/1");
		assertTrue(someFolder.mkdir());
		File[] validFiles = new File[NUM_FILES];
		for(int i=0; i<NUM_FILES; i++) {
			validFiles[i] = new File(rootFolder.getAbsolutePath()+"/"+i+".txt");
			assertTrue(validFiles[i].createNewFile());
		}
		files = Utils.getAllFilesInFolder(rootFolder, file -> {return !file.isDirectory();}, 0);

		assertEquals(NUM_FILES, files.size());
		for(File f: files)
			assertTrue(!f.isDirectory());
	}
	
	@Test
	public void OneFolderRestFiles_AcceptNoFolder_1Max() throws Exception {
		NUM_FILES = 8;
		MAX_FILES = 1;
		File someFolder = new File(rootFolder.getAbsolutePath()+"/1");
		assertTrue(someFolder.mkdir());
		File[] validFiles = new File[NUM_FILES];
		for(int i=0; i<NUM_FILES; i++) {
			validFiles[i] = new File(rootFolder.getAbsolutePath()+"/"+i+".txt");
			assertTrue(validFiles[i].createNewFile());
		}
		files = Utils.getAllFilesInFolder(rootFolder, file -> {return !file.isDirectory();}, MAX_FILES);

		assertEquals(MAX_FILES, files.size());
		for(File f: files)
			assertTrue(!f.isDirectory());
	}
	
	@Test
	public void OneFolderRestFiles_AcceptNoFolder_5Max() throws Exception {
		NUM_FILES = 8;
		MAX_FILES = 5;
		File someFolder = new File(rootFolder.getAbsolutePath()+"/1");
		assertTrue(someFolder.mkdir());
		File[] validFiles = new File[NUM_FILES];
		for(int i=0; i<NUM_FILES; i++) {
			validFiles[i] = new File(rootFolder.getAbsolutePath()+"/"+i+".txt");
			assertTrue(validFiles[i].createNewFile());
		}
		files = Utils.getAllFilesInFolder(rootFolder, file -> {return !file.isDirectory();}, MAX_FILES);

		assertEquals(MAX_FILES, files.size());
		for(File f: files)
			assertTrue(!f.isDirectory());
	}
	
	@Test
	public void OneFolderRestFiles_AcceptNoFolder_MoreThanMax() throws Exception {
		NUM_FILES = 8;
		MAX_FILES = 15;
		File someFolder = new File(rootFolder.getAbsolutePath()+"/1");
		assertTrue(someFolder.mkdir());
		File[] validFiles = new File[NUM_FILES];
		for(int i=0; i<NUM_FILES; i++) {
			validFiles[i] = new File(rootFolder.getAbsolutePath()+"/"+i+".txt");
			assertTrue(validFiles[i].createNewFile());
		}
		files = Utils.getAllFilesInFolder(rootFolder, file -> {return !file.isDirectory();}, MAX_FILES);

		assertEquals(NUM_FILES, files.size());
		for(File f: files)
			assertTrue(!f.isDirectory());
	}
	
	@Test
	public void FoldersAndAllValidFiles_AcceptExtension_NoMax() throws Exception {
		final int NUM_FILES_FOLDERS = 8;
		
		File[] validFilesFolders = new File[NUM_FILES_FOLDERS];
		for(int i=0; i<NUM_FILES_FOLDERS; i++) {
			if(i%2 == 0) {
				validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+"."+extensionTest);
				assertTrue(validFilesFolders[i].createNewFile());
			}
			else {
				validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
				assertTrue(validFilesFolders[i].mkdir());
			}
		}
		files = Utils.getAllFilesInFolder(rootFolder, extensionFilter, 0);


		assertEquals(NUM_FILES_FOLDERS / 2, files.size());
		for(File f: files) {
			int pos = f.getName().lastIndexOf(".");	
			assertTrue(f.getName().substring(pos+1).equals(extensionTest));
		}
	}
	
	@Test
	public void FoldersAndAllValidFiles_AcceptExtension_1Max() throws Exception {
		final int NUM_FILES_FOLDERS = 8;
		MAX_FILES = 1;
		
		File[] validFilesFolders = new File[NUM_FILES_FOLDERS];
		for(int i=0; i<NUM_FILES_FOLDERS; i++) {
			if(i%2 == 0) {
				validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+"."+extensionTest);
				assertTrue(validFilesFolders[i].createNewFile());
			}
			else {
				validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
				assertTrue(validFilesFolders[i].mkdir());
			}
		}
		files = Utils.getAllFilesInFolder(rootFolder, extensionFilter, MAX_FILES);

		assertTrue(files.size() == MAX_FILES);
		for(File f: files) {
			int pos = f.getName().lastIndexOf(".");	
			assertTrue(f.getName().substring(pos+1).equals(extensionTest));
		}
	}
	
	@Test
	public void FoldersAndAllValidFiles_AcceptExtension_5Max() throws Exception {
		final int NUM_FILES_FOLDERS = 15;
		MAX_FILES = 5;
		
		File[] validFilesFolders = new File[NUM_FILES_FOLDERS];
		for(int i=0; i<NUM_FILES_FOLDERS; i++) {
			if(i%2 == 0) {
				validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+"."+extensionTest);
				assertTrue(validFilesFolders[i].createNewFile());
			}
			else {
				validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
				assertTrue(validFilesFolders[i].mkdir());
			}
		}
		files = Utils.getAllFilesInFolder(rootFolder, extensionFilter, MAX_FILES);

		assertEquals(MAX_FILES, files.size());
		for(File f: files) {
			int pos = f.getName().lastIndexOf(".");	
			assertTrue(f.getName().substring(pos+1).equals(extensionTest));
		}
	}
	
	@Test
	public void FoldersAndAllValidFiles_AcceptExtension_MoreThanMax() throws Exception {
		final int NUM_FILES_FOLDERS = 16;
		MAX_FILES = 16;
		
		File[] validFilesFolders = new File[NUM_FILES_FOLDERS];
		for(int i=0; i<NUM_FILES_FOLDERS; i++) {
			if(i%2 == 0) {
				validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+"."+extensionTest);
				assertTrue(validFilesFolders[i].createNewFile());
			}
			else {
				validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
				assertTrue(validFilesFolders[i].mkdir());
			}
		}
		files = Utils.getAllFilesInFolder(rootFolder, extensionFilter, MAX_FILES);

		assertEquals(NUM_FILES_FOLDERS / 2, files.size());
		for(File f: files) {
			int pos = f.getName().lastIndexOf(".");	
			assertTrue(f.getName().substring(pos+1).equals(extensionTest));
		}
	}
	
	@Test
	public void FoldersAndSomeValidFiles_AcceptExtension_NoMax() throws Exception {
		NUM_FILES = 8;
		
		File[] validFilesFolders = new File[NUM_FILES];
		File someFolder1 = new File(rootFolder.getAbsolutePath()+"/1");
		assertTrue(someFolder1.mkdir());
		File someFolder2 = new File(rootFolder.getAbsolutePath()+"/2");
		assertTrue(someFolder2.mkdir());
		for(int i=0; i<NUM_FILES; i++) {
			if(i%2 == 0) 
				validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+"."+extensionTest);
			else 
				validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+".arl");
			assertTrue(validFilesFolders[i].createNewFile());
		}
		files = Utils.getAllFilesInFolder(rootFolder, extensionFilter, 0);

		assertEquals(NUM_FILES / 2, files.size());
		for(File f: files) {
			int pos = f.getName().lastIndexOf(".");	
			assertTrue(f.getName().substring(pos+1).equals(extensionTest));
		}
	}
	
	@Test
	public void FoldersAndSomeValidFiles_AcceptExtension_1Max() throws Exception {
		NUM_FILES = 8;
		MAX_FILES = 1;
		
		File[] validFilesFolders = new File[NUM_FILES];
		File someFolder1 = new File(rootFolder.getAbsolutePath()+"/1");
		assertTrue(someFolder1.mkdir());
		File someFolder2 = new File(rootFolder.getAbsolutePath()+"/2");
		assertTrue(someFolder2.mkdir());
		for(int i=0; i<NUM_FILES; i++) {
			if(i%2 == 0) 
				validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+"."+extensionTest);
			else 
				validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+".arl");
			assertTrue(validFilesFolders[i].createNewFile());
		}
		files = Utils.getAllFilesInFolder(rootFolder, extensionFilter, MAX_FILES);

		assertEquals(MAX_FILES, files.size());
		for(File f: files) {
			int pos = f.getName().lastIndexOf(".");	
			assertTrue(f.getName().substring(pos+1).equals(extensionTest));
		}
	}
	
	@Test
	public void FoldersAndSomeValidFiles_AcceptExtension_5Max() throws Exception {
		NUM_FILES = 12;
		MAX_FILES = 5;
		
		File[] validFilesFolders = new File[NUM_FILES];
		File someFolder1 = new File(rootFolder.getAbsolutePath()+"/1");
		assertTrue(someFolder1.mkdir());
		File someFolder2 = new File(rootFolder.getAbsolutePath()+"/2");
		assertTrue(someFolder2.mkdir());
		for(int i=0; i<NUM_FILES; i++) {
			if(i%2 == 0) 
				validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+"."+extensionTest);
			else 
				validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+".arl");
			assertTrue(validFilesFolders[i].createNewFile());
		}
		files = Utils.getAllFilesInFolder(rootFolder, extensionFilter, MAX_FILES);

		assertEquals(MAX_FILES, files.size());
		for(File f: files) {
			int pos = f.getName().lastIndexOf(".");	
			assertTrue(f.getName().substring(pos+1).equals(extensionTest));
		}
	}
	
	@Test
	public void FoldersAndSomeValidFiles_AcceptExtension_MoreThanMax() throws Exception {
		NUM_FILES = 12;
		MAX_FILES = 8;
		
		File[] validFilesFolders = new File[NUM_FILES];
		File someFolder1 = new File(rootFolder.getAbsolutePath()+"/1");
		assertTrue(someFolder1.mkdir());
		File someFolder2 = new File(rootFolder.getAbsolutePath()+"/2");
		assertTrue(someFolder2.mkdir());
		for(int i=0; i<NUM_FILES; i++) {
			if(i%2 == 0) 
				validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+"."+extensionTest);
			else 
				validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+".arl");
			assertTrue(validFilesFolders[i].createNewFile());
		}
		files = Utils.getAllFilesInFolder(rootFolder, extensionFilter, MAX_FILES);

		assertEquals(NUM_FILES / 2, files.size());
		for(File f: files) {
			int pos = f.getName().lastIndexOf(".");	
			assertTrue(f.getName().substring(pos+1).equals(extensionTest));
		}
	}
	
	@Test
	public void NoFoldersAndSomeValidFiles_AcceptExtension_NoMax() throws Exception {
		NUM_FILES = 8;
		
		File[] validFilesFolders = new File[NUM_FILES];
		for(int i=0; i<NUM_FILES; i++) {
			if(i%2 == 0) 
				validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+"."+extensionTest);
			else 
				validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+".arl");
			assertTrue(validFilesFolders[i].createNewFile());
		}
		files = Utils.getAllFilesInFolder(rootFolder, extensionFilter, 0);

		assertEquals(NUM_FILES / 2, files.size());
		for(File f: files) {
			int pos = f.getName().lastIndexOf(".");	
			assertTrue(f.getName().substring(pos+1).equals(extensionTest));
		}
	}
	
	@Test
	public void NoFoldersAndSomeValidFiles_AcceptExtension_1Max() throws Exception {
		NUM_FILES = 8;
		MAX_FILES = 1;
		
		File[] validFilesFolders = new File[NUM_FILES];
		for(int i=0; i<NUM_FILES; i++) {
			if(i%2 == 0) 
				validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+"."+extensionTest);
			else 
				validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+".arl");
			assertTrue(validFilesFolders[i].createNewFile());
		}
		files = Utils.getAllFilesInFolder(rootFolder, extensionFilter, MAX_FILES);

		assertEquals(MAX_FILES, files.size());
		for(File f: files) {
			int pos = f.getName().lastIndexOf(".");	
			assertTrue(f.getName().substring(pos+1).equals(extensionTest));
		}
	}
	
	@Test
	public void NoFoldersAndSomeValidFiles_AcceptExtension_5Max() throws Exception {
		NUM_FILES = 10;
		MAX_FILES = 5;
		
		File[] validFilesFolders = new File[NUM_FILES];
		for(int i=0; i<NUM_FILES; i++) {
			if(i%2 == 0) 
				validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+"."+extensionTest);
			else 
				validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+".arl");
			assertTrue(validFilesFolders[i].createNewFile());
		}
		files = Utils.getAllFilesInFolder(rootFolder, extensionFilter, MAX_FILES);

		assertEquals(MAX_FILES, files.size());
		for(File f: files) {
			int pos = f.getName().lastIndexOf(".");	
			assertTrue(f.getName().substring(pos+1).equals(extensionTest));
		}
	}
	
	@Test
	public void NoFoldersAndSomeValidFiles_AcceptExtension_EqualMax() throws Exception {
		NUM_FILES = 8;
		MAX_FILES = 4;
		
		File[] validFilesFolders = new File[NUM_FILES];
		for(int i=0; i<NUM_FILES; i++) {
			if(i%2 == 0) 
				validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+"."+extensionTest);
			else 
				validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+".arl");
			assertTrue(validFilesFolders[i].createNewFile());
		}
		files = Utils.getAllFilesInFolder(rootFolder, extensionFilter, MAX_FILES);

		assertEquals(NUM_FILES / 2, files.size());
		for(File f: files) {
			int pos = f.getName().lastIndexOf(".");	
			assertTrue(f.getName().substring(pos+1).equals(extensionTest));
		}
	}
	
	@Test
	public void NoFoldersAndSomeValidFiles_AcceptExtension_MoreThanMax() throws Exception {
		NUM_FILES = 8;
		MAX_FILES = 8;
		
		File[] validFilesFolders = new File[NUM_FILES];
		for(int i=0; i<NUM_FILES; i++) {
			if(i%2 == 0) 
				validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+"."+extensionTest);
			else 
				validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+".arl");
			assertTrue(validFilesFolders[i].createNewFile());
		}
		files = Utils.getAllFilesInFolder(rootFolder, extensionFilter, MAX_FILES);

		assertEquals(NUM_FILES / 2, files.size());
		for(File f: files) {
			int pos = f.getName().lastIndexOf(".");	
			assertTrue(f.getName().substring(pos+1).equals(extensionTest));
		}
	}
	
	@Test
	public void NoFoldersAndNoValidFiles_AcceptExtension_NoMax() throws Exception {
		NUM_FILES = 8;
		
		File[] validFilesFolders = new File[NUM_FILES];
		File someFolder1 = new File(rootFolder.getAbsolutePath()+"/1");
		assertTrue(someFolder1.mkdir());
		File someFolder2 = new File(rootFolder.getAbsolutePath()+"/2");
		assertTrue(someFolder2.mkdir());
		for(int i=0; i<NUM_FILES; i++) {
			validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+".arl");
			assertTrue(validFilesFolders[i].createNewFile());
		}
		files = Utils.getAllFilesInFolder(rootFolder, extensionFilter, 0);

		assertEquals(0, files.size());
	}
	
	@Test
	public void NoFoldersAndNoValidFiles_AcceptExtension_1Max() throws Exception {
		NUM_FILES = 8;
		MAX_FILES = 1;
		
		File[] validFilesFolders = new File[NUM_FILES];
		File someFolder1 = new File(rootFolder.getAbsolutePath()+"/1");
		assertTrue(someFolder1.mkdir());
		File someFolder2 = new File(rootFolder.getAbsolutePath()+"/2");
		assertTrue(someFolder2.mkdir());
		for(int i=0; i<NUM_FILES; i++) {
			validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+".arl");
			assertTrue(validFilesFolders[i].createNewFile());
		}
		files = Utils.getAllFilesInFolder(rootFolder, extensionFilter, MAX_FILES);

		assertEquals(0, files.size());
	}
	
	@Test
	public void NoFoldersAndNoValidFiles_AcceptExtension_5Max() throws Exception {
		NUM_FILES = 8;
		MAX_FILES = 5;
		
		File[] validFilesFolders = new File[NUM_FILES];
		File someFolder1 = new File(rootFolder.getAbsolutePath()+"/1");
		assertTrue(someFolder1.mkdir());
		File someFolder2 = new File(rootFolder.getAbsolutePath()+"/2");
		assertTrue(someFolder2.mkdir());
		for(int i=0; i<NUM_FILES; i++) {
			validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+".arl");
			assertTrue(validFilesFolders[i].createNewFile());
		}
		files = Utils.getAllFilesInFolder(rootFolder, extensionFilter, MAX_FILES);

		assertEquals(0, files.size());
	}
	
	@Test
	public void NoFoldersAndNoValidFiles_AcceptExtension_EqualMax() throws Exception {
		NUM_FILES = 8;
		MAX_FILES = 5;
		
		File[] validFilesFolders = new File[NUM_FILES];
		File someFolder1 = new File(rootFolder.getAbsolutePath()+"/1");
		assertTrue(someFolder1.mkdir());
		File someFolder2 = new File(rootFolder.getAbsolutePath()+"/2");
		assertTrue(someFolder2.mkdir());
		for(int i=0; i<NUM_FILES; i++) {
			validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+".arl");
			assertTrue(validFilesFolders[i].createNewFile());
		}
		files = Utils.getAllFilesInFolder(rootFolder, extensionFilter, MAX_FILES);

		assertEquals(0, files.size());
	}
	
	@Test
	public void NoFoldersAndNoValidFiles_AcceptExtension_MoreThanMax() throws Exception {
		NUM_FILES = 8;
		MAX_FILES = 10;
		
		File[] validFilesFolders = new File[NUM_FILES];
		File someFolder1 = new File(rootFolder.getAbsolutePath()+"/1");
		assertTrue(someFolder1.mkdir());
		File someFolder2 = new File(rootFolder.getAbsolutePath()+"/2");
		assertTrue(someFolder2.mkdir());
		for(int i=0; i<NUM_FILES; i++) {
			validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+".arl");
			assertTrue(validFilesFolders[i].createNewFile());
		}
		files = Utils.getAllFilesInFolder(rootFolder, extensionFilter, MAX_FILES);

		assertEquals(0, files.size());
	}
	
	@Test
	public void NoFoldersAndAllValidFiles_AcceptExtension_NoMax() throws Exception {
		NUM_FILES = 8;
		
		File[] validFilesFolders = new File[NUM_FILES];
		File someFolder1 = new File(rootFolder.getAbsolutePath()+"/1");
		assertTrue(someFolder1.mkdir());
		File someFolder2 = new File(rootFolder.getAbsolutePath()+"/2");
		assertTrue(someFolder2.mkdir());
		for(int i=0; i<NUM_FILES; i++) {
			validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+"."+extensionTest);
			assertTrue(validFilesFolders[i].createNewFile());
		}
		files = Utils.getAllFilesInFolder(rootFolder, extensionFilter, 0);

		assertTrue(files.size() == NUM_FILES);
		for(File f: files) {
			int pos = f.getName().lastIndexOf(".");	
			assertTrue(f.getName().substring(pos+1).equals(extensionTest));
		}
	}
	
	@Test
	public void NoFoldersAndAllValidFiles_AcceptExtension_1Max() throws Exception {
		NUM_FILES = 8;
		MAX_FILES = 1;
		
		File[] validFilesFolders = new File[NUM_FILES];
		for(int i=0; i<NUM_FILES; i++) {
			validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+"."+extensionTest);
			assertTrue(validFilesFolders[i].createNewFile());
		}
		files = Utils.getAllFilesInFolder(rootFolder, extensionFilter, MAX_FILES);

		assertEquals(MAX_FILES, files.size());
		assertFiles(files);
	}
	
	@Test
	public void NoFoldersAndAllValidFiles_AcceptExtension_5Max() throws Exception {
		NUM_FILES = 8;
		MAX_FILES = 5;
		
		File[] validFilesFolders = new File[NUM_FILES];
		for(int i=0; i<NUM_FILES; i++) {
			validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+"."+extensionTest);
			assertTrue(validFilesFolders[i].createNewFile());
		}
		files = Utils.getAllFilesInFolder(rootFolder, extensionFilter, MAX_FILES);

		assertEquals(MAX_FILES, files.size());
		assertFiles(files);
	}
	
	@Test
	public void NoFoldersAndAllValidFiles_AcceptExtension_EqualMax() throws Exception {
		NUM_FILES = 8;
		MAX_FILES = 8;
		File[] validFilesFolders = new File[NUM_FILES];
		for(int i=0; i<NUM_FILES; i++) {
			validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+"."+extensionTest);
			assertTrue(validFilesFolders[i].createNewFile());
		}
		files = Utils.getAllFilesInFolder(rootFolder, extensionFilter, MAX_FILES);

		assertEquals(NUM_FILES, files.size());
		assertFiles(files);
	}
	
	@Test
	public void NoFoldersAndAllValidFiles_AcceptExtension_MoreThanMax() throws Exception {
		NUM_FILES = 8;
		MAX_FILES = 12;
		File[] validFilesFolders = new File[NUM_FILES];
		for(int i=0; i<NUM_FILES; i++) {
			validFilesFolders[i] = new File(rootFolder.getAbsolutePath()+"/"+i+"."+extensionTest);
			assertTrue(validFilesFolders[i].createNewFile());
		}
		files = Utils.getAllFilesInFolder(rootFolder, extensionFilter, MAX_FILES);

		assertEquals(NUM_FILES, files.size());
		assertFiles(files);
	}
}