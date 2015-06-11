package com.musicott.task;

import com.musicott.view.TestViewController;

import javafx.application.Platform;
import javafx.concurrent.Task;

public class TestTask extends Task<Integer> {
	
	private static final int MAX = 100;
	private TestViewController viewController;
	
	public TestTask(TestViewController tvc) {
		viewController=tvc;
	}
	
	@Override
	protected Integer call() throws Exception {
		for(double i=1; i<=MAX;i+=0.5) {
			if(isCancelled()) {
				updateMessage("Cancelled");
				break;
			}
			try {
                Thread.sleep(10);
    			updateProgress(i,MAX);
            } catch (InterruptedException e) {
                if (isCancelled()) {
                    updateMessage("Interrupted!");
                    break;
                }
            }
		}
		return MAX;
	}
	
	@Override
	protected void running() {
		super.running();
		updateMessage("Running!");
	}
	
	@Override
	protected void succeeded() {
		super.succeeded();
		updateMessage("Succeded!");
		Platform.runLater(() -> {viewController.finished();});
	}
	
	@Override
	protected void cancelled() {
		super.cancelled();
		updateMessage("Cancelled!");
	}
}