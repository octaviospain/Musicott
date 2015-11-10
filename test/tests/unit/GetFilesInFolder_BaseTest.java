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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileFilter;
import java.util.List;

import org.junit.After;
import org.junit.Before;

import com.musicott.view.ImportController;

/**
 * @author Octavio Calleya
 *
 */
public abstract class GetFilesInFolder_BaseTest {
	
	File rootFolder = new File("./temp/foldertest/");
	ImportController icc = new ImportController();
	List<File> files;
	int NUM_FOLDERS, NUM_FILES, MAX_FILES;
	String extensionTest = "txt";
	FileFilter allFilter = file -> {return true;};
	FileFilter directoryFilter = file -> {return file.isDirectory();};
	FileFilter extensionFilter = file -> {
		int pos = file.getName().lastIndexOf(".");
		return file.getName().substring(pos+1).equals(extensionTest);
	};
	
	@Before
	public void tearUp() {
		rootFolder.mkdirs();
	}
	
	@After
	public void tearDown() {
		deleteTestFiles(rootFolder);
		assertTrue(rootFolder.delete());
	}
	
	public void deleteTestFiles(File folder) {
		File[] files = folder.listFiles();
		for(File f: files)
			if(f.isDirectory()) {
				deleteTestFiles(f);
				assertTrue(f.delete());
			}
			else
				assertTrue(f.delete());
	}
	
	public void assertFiles(List<File> files) {
		for(File f: files) {
			int pos = f.getName().lastIndexOf(".");	
			assertTrue(f.getName().substring(pos+1).equals(extensionTest));
		}
	}
}