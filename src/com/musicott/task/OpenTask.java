package com.musicott.task;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.concurrent.Task;

import com.musicott.SceneManager;
import com.musicott.error.ErrorHandler;
import com.musicott.error.ParseException;
import com.musicott.model.Track;
import com.musicott.task.parser.Mp3Parser;

public class OpenTask extends Task<List<Track>> {

	private List<Track> list;
	private List<File> files;
	
	public OpenTask(List<File> files) {
		this.files = files;
		list = new ArrayList<Track>();
	}
	
	@Override
	protected List<Track> call() {
		int i = 0;
		for(File file:files) 
			try {
				if(file.getName().substring(file.getName().length()-3).equals("mp3")) {
					updateProgress(++i, files.size());
					list.add(Mp3Parser.parseMp3File(file));
				}
				else
					if(file.getName().substring(file.getName().length()-3).equals("m4a")) {
						updateProgress(++i, files.size());
						//TODO M4aParser
					}
					else
						if(file.getName().substring(file.getName().length()-3).equals("wav")) {
							updateProgress(++i, files.size());
							//TODO WavParser
						}
						else
							if(file.getName().substring(file.getName().length()-4).equals("flac")) {
								updateProgress(++i, files.size());
								//TODO FlacParser
							}
			} catch (Exception e) {
				ParseException pe = new ParseException("Parsing Error", e, file);
				ErrorHandler.getInstance().addParseException(pe);
			}
		return list;
	}
	
	@Override
	protected void succeeded() {
		super.succeeded();
		Platform.runLater(() -> {SceneManager.getInstance().getRootController().addTracks(list);
								 SceneManager.getInstance().closeImportScene();});
	}
}