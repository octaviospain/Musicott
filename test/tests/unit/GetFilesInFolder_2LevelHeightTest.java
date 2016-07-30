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

import com.transgressoft.musicott.util.*;
import org.junit.*;

import java.io.*;

import static org.junit.Assert.*;

/**
 * @author Octavio Calleya
 *
 */
public class GetFilesInFolder_2LevelHeightTest extends GetFilesInFolder_BaseTest {
	
	@Test
	public void OnlyFoldersLevel1_OnlyFoldersLevel2_AcceptAll_NoMax() throws Exception {
		NUM_FOLDERS = 4;
		File[] folders1 = new File[NUM_FOLDERS];
		File[] folders2 = new File[NUM_FOLDERS];
		for(int i=0; i<NUM_FOLDERS; i++) {
			folders1[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			folders1[i].mkdir();
			for(int j=0; j<NUM_FOLDERS; j++) {
				folders2[j] = new File(folders1[i].getAbsolutePath()+"/"+i+"."+j);
				folders2[j].mkdir();
			}
		}
		files = Utils.getAllFilesInFolder(rootFolder, allFilter, 0);
		assertEquals(NUM_FOLDERS * NUM_FOLDERS + NUM_FOLDERS, files.size());
	}
	
	@Test
	public void OnlyFoldersLevel1_OnlyFoldersLevel2_AcceptAll_1Max() throws Exception {
		NUM_FOLDERS = 4;
		MAX_FILES = 1;
		File[] folders1 = new File[NUM_FOLDERS];
		File[] folders2 = new File[NUM_FOLDERS];
		for(int i=0; i<NUM_FOLDERS; i++) {
			folders1[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			folders1[i].mkdir();
			for(int j=0; j<NUM_FOLDERS; j++) {
				folders2[j] = new File(folders1[i].getAbsolutePath()+"/"+i+"."+j);
				folders2[j].mkdir();
			}
		}
		files = Utils.getAllFilesInFolder(rootFolder, allFilter, MAX_FILES);
		assertEquals(MAX_FILES, files.size());
	}
	
	@Test
	public void OnlyFoldersLevel1_OnlyFoldersLevel2_AcceptAll_12Max() throws Exception {
		NUM_FOLDERS = 4;
		MAX_FILES = 12;
		File[] folders1 = new File[NUM_FOLDERS];
		File[] folders2 = new File[NUM_FOLDERS];
		for(int i=0; i<NUM_FOLDERS; i++) {
			folders1[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			folders1[i].mkdir();
			for(int j=0; j<NUM_FOLDERS; j++) {
				folders2[j] = new File(folders1[i].getAbsolutePath()+"/"+i+"."+j);
				folders2[j].mkdir();
			}
		}
		files = Utils.getAllFilesInFolder(rootFolder, allFilter, MAX_FILES);
		assertEquals(MAX_FILES, files.size());
	}
	
	@Test
	public void OnlyFoldersLevel1_OnlyFoldersLevel2_AcceptAll_EqualMax() throws Exception {
		NUM_FOLDERS = 4;
		MAX_FILES = NUM_FOLDERS * NUM_FOLDERS + NUM_FOLDERS;
		File[] folders1 = new File[NUM_FOLDERS];
		File[] folders2 = new File[NUM_FOLDERS];
		for(int i=0; i<NUM_FOLDERS; i++) {
			folders1[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			folders1[i].mkdir();
			for(int j=0; j<NUM_FOLDERS; j++) {
				folders2[j] = new File(folders1[i].getAbsolutePath()+"/"+i+"."+j);
				folders2[j].mkdir();
			}
		}
		files = Utils.getAllFilesInFolder(rootFolder, allFilter, MAX_FILES);
		assertEquals(MAX_FILES, files.size());
	}
	
	@Test
	public void OnlyFoldersLevel1_OnlyFoldersLevel2_AcceptAll_MoreThanMax() throws Exception {
		NUM_FOLDERS = 4;
		MAX_FILES = NUM_FOLDERS * NUM_FOLDERS + NUM_FOLDERS + 5;
		File[] folders1 = new File[NUM_FOLDERS];
		File[] folders2 = new File[NUM_FOLDERS];
		for(int i=0; i<NUM_FOLDERS; i++) {
			folders1[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			folders1[i].mkdir();
			for(int j=0; j<NUM_FOLDERS; j++) {
				folders2[j] = new File(folders1[i].getAbsolutePath()+"/"+i+"."+j);
				folders2[j].mkdir();
			}
		}
		files = Utils.getAllFilesInFolder(rootFolder, allFilter, MAX_FILES);
		assertEquals(NUM_FOLDERS * NUM_FOLDERS + NUM_FOLDERS, files.size());
	}
	
	@Test
	public void OnlyFoldersLevel1_OnlyFilesLevel2_AcceptAll_NoMax() throws Exception {
		NUM_FOLDERS = 4;
		NUM_FILES = 4;
		File[] folders1 = new File[NUM_FOLDERS];
		File[] files2 = new File[NUM_FILES];
		for(int i=0; i<NUM_FOLDERS; i++) {
			folders1[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			folders1[i].mkdir();
			for(int j=0; j<NUM_FILES; j++) {
				files2[j] = new File(folders1[i].getAbsolutePath()+"/"+i+"-"+j+"."+extensionTest);
				files2[j].createNewFile();
			}
		}
		files = Utils.getAllFilesInFolder(rootFolder, allFilter, 0);
		assertEquals(NUM_FOLDERS * NUM_FILES + NUM_FOLDERS, files.size());
	}
	
	@Test
	public void OnlyFoldersLevel1_OnlyFilesLevel2_AcceptAll_1Max() throws Exception {
		NUM_FOLDERS = 4;
		NUM_FILES = 4;
		MAX_FILES = 1;
		File[] folders1 = new File[NUM_FOLDERS];
		File[] files2 = new File[NUM_FILES];
		for(int i=0; i<NUM_FOLDERS; i++) {
			folders1[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			folders1[i].mkdir();
			for(int j=0; j<NUM_FILES; j++) {
				files2[j] = new File(folders1[i].getAbsolutePath()+"/"+i+"-"+j+"."+extensionTest);
				files2[j].createNewFile();
			}
		}
		files = Utils.getAllFilesInFolder(rootFolder, allFilter, MAX_FILES);
		assertEquals(MAX_FILES, files.size());
	}
	
	@Test
	public void OnlyFoldersLevel1_OnlyFilesLevel2_AcceptAll_12Max() throws Exception {
		NUM_FOLDERS = 4;
		NUM_FILES = 4;
		MAX_FILES = 12;
		File[] folders1 = new File[NUM_FOLDERS];
		File[] files2 = new File[NUM_FILES];
		for(int i=0; i<NUM_FOLDERS; i++) {
			folders1[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			folders1[i].mkdir();
			for(int j=0; j<NUM_FILES; j++) {
				files2[j] = new File(folders1[i].getAbsolutePath()+"/"+i+"-"+j+"."+extensionTest);
				files2[j].createNewFile();
			}
		}
		files = Utils.getAllFilesInFolder(rootFolder, allFilter, MAX_FILES);
		assertEquals(MAX_FILES, files.size());
	}
	
	@Test
	public void OnlyFoldersLevel1_OnlyFilesLevel2_AcceptAll_EqualMax() throws Exception {
		NUM_FOLDERS = 4;
		NUM_FILES = 4;
		MAX_FILES = NUM_FOLDERS * NUM_FILES + NUM_FOLDERS;
		File[] folders1 = new File[NUM_FOLDERS];
		File[] files2 = new File[NUM_FILES];
		for(int i=0; i<NUM_FOLDERS; i++) {
			folders1[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			folders1[i].mkdir();
			for(int j=0; j<NUM_FILES; j++) {
				files2[j] = new File(folders1[i].getAbsolutePath()+"/"+i+"-"+j+"."+extensionTest);
				files2[j].createNewFile();
			}
		}
		files = Utils.getAllFilesInFolder(rootFolder, allFilter, MAX_FILES);
		assertEquals(MAX_FILES, files.size());
	}
	
	@Test
	public void OnlyFoldersLevel1_OnlyFilesLevel2_AcceptAll_MoreThanMax() throws Exception {
		NUM_FOLDERS = 4;
		NUM_FILES = 4;
		MAX_FILES = 30;
		File[] folders1 = new File[NUM_FOLDERS];
		File[] files2 = new File[NUM_FILES];
		for(int i=0; i<NUM_FOLDERS; i++) {
			folders1[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			folders1[i].mkdir();
			for(int j=0; j<NUM_FILES; j++) {
				files2[j] = new File(folders1[i].getAbsolutePath()+"/"+i+"-"+j+"."+extensionTest);
				files2[j].createNewFile();
			}
		}
		files = Utils.getAllFilesInFolder(rootFolder, allFilter, MAX_FILES);
		assertEquals(NUM_FOLDERS * NUM_FILES + NUM_FOLDERS, files.size());
	}
	
	@Test
	public void OnlyFoldersLevel1_OnlyFilesLevel2_AcceptFiles_NoMax() throws Exception {
		NUM_FOLDERS = 4;
		NUM_FILES = 4;
		File[] folders1 = new File[NUM_FOLDERS];
		File[] files2 = new File[NUM_FILES];
		for(int i=0; i<NUM_FOLDERS; i++) {
			folders1[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			folders1[i].mkdir();
			for(int j=0; j<NUM_FILES; j++) {
				files2[j] = new File(folders1[i].getAbsolutePath()+"/"+i+"-"+j+"."+extensionTest);
				files2[j].createNewFile();
			}
		}
		files = Utils.getAllFilesInFolder(rootFolder, file -> {return !file.isDirectory();}, 0);
		assertEquals(NUM_FOLDERS * NUM_FILES, files.size());
	}
	
	@Test
	public void OnlyFoldersLevel1_OnlyFilesLevel2_AcceptFiles_1Max() throws Exception {
		NUM_FOLDERS = 4;
		NUM_FILES = 4;
		MAX_FILES = 1;
		File[] folders1 = new File[NUM_FOLDERS];
		File[] files2 = new File[NUM_FILES];
		for(int i=0; i<NUM_FOLDERS; i++) {
			folders1[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			folders1[i].mkdir();
			for(int j=0; j<NUM_FILES; j++) {
				files2[j] = new File(folders1[i].getAbsolutePath()+"/"+i+"-"+j+"."+extensionTest);
				files2[j].createNewFile();
			}
		}
		files = Utils.getAllFilesInFolder(rootFolder, file -> {return !file.isDirectory();}, MAX_FILES);
		assertEquals(MAX_FILES, files.size());
	}
	
	@Test
	public void OnlyFoldersLevel1_OnlyFilesLevel2_AcceptFiles_9Max() throws Exception {
		NUM_FOLDERS = 4;
		NUM_FILES = 4;
		MAX_FILES = 9;
		File[] folders1 = new File[NUM_FOLDERS];
		File[] files2 = new File[NUM_FILES];
		for(int i=0; i<NUM_FOLDERS; i++) {
			folders1[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			folders1[i].mkdir();
			for(int j=0; j<NUM_FILES; j++) {
				files2[j] = new File(folders1[i].getAbsolutePath()+"/"+i+"-"+j+"."+extensionTest);
				files2[j].createNewFile();
			}
		}
		files = Utils.getAllFilesInFolder(rootFolder, file -> {return !file.isDirectory();}, MAX_FILES);
		assertEquals(MAX_FILES, files.size());
	}
	
	@Test
	public void OnlyFoldersLevel1_OnlyFilesLevel2_AcceptFiles_EqualMax() throws Exception {
		NUM_FOLDERS = 4;
		NUM_FILES = 4;
		MAX_FILES = NUM_FOLDERS * NUM_FILES;
		File[] folders1 = new File[NUM_FOLDERS];
		File[] files2 = new File[NUM_FILES];
		for(int i=0; i<NUM_FOLDERS; i++) {
			folders1[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			folders1[i].mkdir();
			for(int j=0; j<NUM_FILES; j++) {
				files2[j] = new File(folders1[i].getAbsolutePath()+"/"+i+"-"+j+"."+extensionTest);
				files2[j].createNewFile();
			}
		}
		files = Utils.getAllFilesInFolder(rootFolder, file -> {return !file.isDirectory();}, MAX_FILES);
		assertEquals(MAX_FILES, files.size());
	}
	
	@Test
	public void OnlyFoldersLevel1_OnlyFilesLevel2_AcceptFiles_MoreThanMax() throws Exception {
		NUM_FOLDERS = 4;
		NUM_FILES = 4;
		MAX_FILES = 20;
		File[] folders1 = new File[NUM_FOLDERS];
		File[] files2 = new File[NUM_FILES];
		for(int i=0; i<NUM_FOLDERS; i++) {
			folders1[i] = new File(rootFolder.getAbsolutePath()+"/"+i);
			folders1[i].mkdir();
			for(int j=0; j<NUM_FILES; j++) {
				files2[j] = new File(folders1[i].getAbsolutePath()+"/"+i+"-"+j+"."+extensionTest);
				files2[j].createNewFile();
			}
		}
		files = Utils.getAllFilesInFolder(rootFolder, file -> {return !file.isDirectory();}, MAX_FILES);
		assertEquals(NUM_FOLDERS * NUM_FILES, files.size());
	}
}
