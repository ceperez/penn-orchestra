/*
 * Copyright (C) 2010 Trustees of the University of Pennsylvania
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS of ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.upenn.cis.orchestra.gui;

import static edu.upenn.cis.orchestra.OrchestraUtil.newHashMap;

import java.io.File;
import java.util.Map;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.core.BasicRobot;
import org.fest.swing.core.Robot;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.fixture.FrameFixture;
import org.testng.Assert;

import edu.upenn.cis.orchestra.AbstractMultiSystemOrchestraTest;
import edu.upenn.cis.orchestra.BdbDataSetFactory;
import edu.upenn.cis.orchestra.Config;
import edu.upenn.cis.orchestra.IOrchestraOperationFactory;
import edu.upenn.cis.orchestra.MultiSystemOrchestraOperationExecutor;
import edu.upenn.cis.orchestra.OrchestraTestFrame;

/**
 * An Orchestra test via the GUI.
 * 
 * @see edu.upenn.cis.orchestra.AbstractMultiSystemOrchestraTest
 * @author John Frommeyer
 * 
 */
@GUITest
public final class OrchestraTestGUI extends AbstractMultiSystemOrchestraTest {

	/** The robot for our test. */
	private Robot robot;

	/** Translates Berkeley update store into DbUnit dataset. */
	private BdbDataSetFactory bdbDataSetFactory;

	private final Map<String, OrchestraGUITestFrame> peerNameToTestFrame = newHashMap();

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.upenn.cis.orchestra.AbstractOrchestraTest#beforePrepareImpl()
	 */
	@Override
	protected void beforePrepareImpl() {}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.upenn.cis.orchestra.AbstractOrchestraTest#betweenPrepareAndTestImpl()
	 */
	@Override
	protected void betweenPrepareAndTestImpl() throws Exception {
		File f = new File("updateStore_env");
		if (f.exists() && f.isDirectory()) {
			File[] files = f.listFiles();
			for (File file : files) {
				file.delete();
			}
			String[] contents = f.list();
			Assert.assertTrue(contents.length == 0,
					"Store server did not clear.");
		}
		FailOnThreadViolationRepaintManager.install();
		Robot robot = BasicRobot.robotWithNewAwtHierarchy();
		for (OrchestraTestFrame testFrame : testFrames) {
			OrchestraGUITestFrame guiTestFrame = new OrchestraGUITestFrame(
					orchestraSchema, testFrame, robot);
			peerNameToTestFrame.put(testFrame.getPeerName(), guiTestFrame);
		}
		bdbDataSetFactory = new BdbDataSetFactory(new File("updateStore_env"));
		IOrchestraOperationFactory factory = new MultiGUIOperationFactory(
				peerNameToTestFrame, orchestraSchema, testDataDirectory,
				onlyGenerateDataSets, bdbDataSetFactory);
		executor = new MultiSystemOrchestraOperationExecutor(factory);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.upenn.cis.orchestra.AbstractOrchestraTest#shutdownImpl()
	 */
	@Override
	protected void shutdownImpl() throws Exception {
		for (OrchestraGUITestFrame guiTestFrame : peerNameToTestFrame.values()) {
			FrameFixture window = guiTestFrame.getWindowFrameFixture();
			if (window != null) {
				window.component().toFront();
				// This setting allows us to exit the GUI without also exiting
				// the
				// JVM.
				String previousGuiMode = Config.getProperty("gui.mode");
				Config.setProperty("gui.mode", "Ajax");
				window.menuItemWithPath("File", "Exit").click();
				if (previousGuiMode == null) {
					Config.removeProperty("gui.mode");
				} else {
					Config.setProperty("gui.mode", previousGuiMode);
				}
				window.cleanUp();
			}
		}
		if (bdbDataSetFactory != null) {
			bdbDataSetFactory.close();
		}
	}
}