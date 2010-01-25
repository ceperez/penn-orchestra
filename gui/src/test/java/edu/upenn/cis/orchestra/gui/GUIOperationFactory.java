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

import static org.fest.swing.core.matcher.DialogMatcher.withTitle;
import static org.fest.swing.core.matcher.JButtonMatcher.withText;
import static org.fest.swing.timing.Pause.pause;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.dbunit.JdbcDatabaseTester;
import org.fest.swing.finder.JFileChooserFinder;
import org.fest.swing.fixture.DialogFixture;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JFileChooserFixture;
import org.fest.swing.fixture.JOptionPaneFixture;
import org.fest.swing.fixture.JTabbedPaneFixture;
import org.fest.swing.fixture.JTextComponentFixture;
import org.fest.swing.timing.Timeout;
import org.testng.Assert;

import edu.upenn.cis.orchestra.AbstractDsFileOperationFactory;
import edu.upenn.cis.orchestra.DbUnitUtil;
import edu.upenn.cis.orchestra.IOrchestraOperation;
import edu.upenn.cis.orchestra.MetaDataChecker;
import edu.upenn.cis.orchestra.OrchestraSchema;
import edu.upenn.cis.orchestra.gui.peers.PeerCommands;
import edu.upenn.cis.orchestra.gui.peers.PeersMgtPanel;
import edu.upenn.cis.orchestra.reconciliation.BdbDataSetFactory;

/**
 * Creates {@code IOrchestraOperation}s which carry out operations via the GUI.
 * 
 * @see edu.upenn.cis.orchestra.AbstractDsFileOperationFactory
 * 
 * @author John Frommeyer
 * 
 */
public final class GUIOperationFactory extends AbstractDsFileOperationFactory<Void> {
	/**
	 * An Orchestra Create operation.
	 * 
	 */
	private class CreateOperation implements IOrchestraOperation {
		/** The DbUnit dataset file we will be writing or testing against. */
		private final File datasetFile;

		/**
		 * Creates a create operation.
		 * 
		 * @param datasetFile
		 */
		private CreateOperation(
				@SuppressWarnings("hiding") final File datasetFile) {
			this.datasetFile = datasetFile;
		}

		/**
		 * @see edu.upenn.cis.orchestra.IOrchestraOperation#execute()
		 */
		@Override
		public void execute() throws Exception {
			window.menuItemWithPath("Services", "Create DB from Schema")
					.click();
			MetaDataChecker checker = new MetaDataChecker.Builder()
					.checkTypes().build();
			DbUnitUtil.dumpOrCheckAndMetaCheck(datasetFile, checker,
					orchestraSchema, dumpDatasets, dbTester, bdbDataSetFactory);
		}
	}

	/**
	 * An Orchestra Migrate operation.
	 * 
	 */
	private class MigrateOperation implements IOrchestraOperation {
		/** The DbUnit dataset file we will be writing or testing against. */
		private final File datasetFile;

		/**
		 * Creates a migrate operation.
		 * 
		 * @param datasetFile
		 */
		public MigrateOperation(
				@SuppressWarnings("hiding") final File datasetFile) {
			this.datasetFile = datasetFile;
		}

		/**
		 * @see edu.upenn.cis.orchestra.IOrchestraOperation#execute()
		 */
		@Override
		public void execute() throws Exception {
			window.menuItemWithPath("Services", "Migrate Existing DB").click();
			// We found that when testing with remote database, we need to pause
			// here so
			// that migration finishes before DbUnit goes to work.
			pause(30, TimeUnit.SECONDS);

			MetaDataChecker checker = new MetaDataChecker.Builder()
					.checkForLabeledNulls().checkTypes().build();
			DbUnitUtil.dumpOrCheckAndMetaCheck(datasetFile, checker,
					orchestraSchema, dumpDatasets, dbTester, bdbDataSetFactory);
		}
	}

	/**
	 * An Orchestra Import operation.
	 * 
	 */
	private class ImportOperation implements IOrchestraOperation {

		/** The DbUnit dataset file we will be writing or testing against. */
		private final File datasetFile;

		/**
		 * Creates a new import operation.
		 * 
		 * @param datasetFile
		 */
		private ImportOperation(
				@SuppressWarnings("hiding") final File datasetFile) {
			this.datasetFile = datasetFile;
		}

		/**
		 * @see edu.upenn.cis.orchestra.IOrchestraOperation#execute()
		 */
		@Override
		public void execute() throws Exception {
			window.menuItemWithPath("File", "Import CDSS Data...").click();
			JFileChooserFixture fileChooser = JFileChooserFinder
					.findFileChooser().using(window.robot);
			JTextComponentFixture fileNameBox = fileChooser.fileNameTextBox();
			String filePath = testDataDirectory.getAbsolutePath();
			String filledIn = fileNameBox.text();

			if (!filePath.equals(filledIn) && filePath.startsWith(filledIn)) {
				// Try to speed things up a little.
				int prefixLength = filledIn.length();
				String remainingText = filePath.substring(prefixLength);
				fileNameBox.selectText(prefixLength, prefixLength).enterText(
						remainingText);
			} else {
				fileNameBox.enterText(filePath);
			}
			fileChooser.approve();

			// Click OK
			DialogFixture dialog = window
					.dialog(withTitle("CDSS Data Import Complete"));
			Assert.assertFalse(dialog.textBox().text().contains(
					"Failed to import:"),
					"Some datafiles failed to be imported.");
			dialog.button(withText("OK")).click();

			DbUnitUtil.dumpOrCheck(datasetFile, orchestraSchema, dumpDatasets,
					dbTester, bdbDataSetFactory);

		}
	}

	/**
	 * An Orchestra Publication operation.
	 * 
	 */
	private class PublishOperation implements IOrchestraOperation {

		/** The DbUnit dataset file we will be writing or testing against. */
		private final File datasetFile;

		/** The publishing peer. */
		private final String peerName;

		/**
		 * Creates a new publish operation.
		 * 
		 * @param datasetFile
		 * @param peerName
		 */
		public PublishOperation(
				@SuppressWarnings("hiding") final File datasetFile,
				@SuppressWarnings("hiding") final String peerName) {
			this.datasetFile = datasetFile;
			this.peerName = peerName;
		}

		/**
		 * @see edu.upenn.cis.orchestra.IOrchestraOperation#execute()
		 */
		@Override
		public void execute() throws Exception {
			JTabbedPaneFixture tabPane = window
					.tabbedPane(PeersMgtPanel.PEERS_MGT_TABBED_PANE);
			tabPane.selectTab("Peer " + peerName);

			window.button(withText("Publish").andShowing()).click();
			// Click OK

			JOptionPaneFixture result = window.optionPane().requireMessage(
					PeerCommands.PUBLISH_SUCCESS_MESSAGE);
			result.buttonWithText("OK").click();
			DbUnitUtil.dumpOrCheck(datasetFile, orchestraSchema, dumpDatasets,
					dbTester, bdbDataSetFactory);
		}
	}

	/**
	 * An Orchestra Reconciliation operation.
	 * 
	 */
	private class ReconcileOperation implements IOrchestraOperation {

		/** The DbUnit dataset file we will be writing or testing against. */
		private final File datasetFile;

		/** The peer which is reconciling. */
		private final String peerName;

		/**
		 * Creates a new reconcile operation.
		 * 
		 * @param datasetFile
		 * @param peerName
		 */
		public ReconcileOperation(
				@SuppressWarnings("hiding") final File datasetFile,
				@SuppressWarnings("hiding") final String peerName) {
			this.datasetFile = datasetFile;
			this.peerName = peerName;
		}

		/**
		 * @see edu.upenn.cis.orchestra.IOrchestraOperation#execute()
		 */
		@Override
		public void execute() throws Exception {
			JTabbedPaneFixture tabPane = window
					.tabbedPane(PeersMgtPanel.PEERS_MGT_TABBED_PANE);
			tabPane.selectTab("Peer " + peerName);

			window.button(withText("Reconcile").andShowing()).click();

			// Click OK
			JOptionPaneFixture result = window.optionPane(
					Timeout.timeout(15, TimeUnit.SECONDS)).requireMessage(
					PeerCommands.RECONCILE_SUCCESS_MESSAGE);
			result.buttonWithText("OK").click();

			DbUnitUtil.dumpOrCheck(datasetFile, orchestraSchema, dumpDatasets,
					dbTester, bdbDataSetFactory);
		}
	}

	/**
	 * The Orchestra {@code FrameFixture} which will used to carry out the
	 * operations.
	 */
	private FrameFixture window;

	/**
	 * Creates an executor which will use the Orchestra GUI.
	 * 
	 * @param window
	 * @param orchestraSchema
	 * @param testDataDirectory
	 * @param dumpDatasets
	 * @param dbTester
	 * @param bdbDataSetFactory
	 */
	public GUIOperationFactory(
			@SuppressWarnings("hiding") final FrameFixture window,
			@SuppressWarnings("hiding") final OrchestraSchema orchestraSchema,
			@SuppressWarnings("hiding") final File testDataDirectory,
			@SuppressWarnings("hiding") final boolean dumpDatasets,
			@SuppressWarnings("hiding") final JdbcDatabaseTester dbTester,
			@SuppressWarnings("hiding") final BdbDataSetFactory bdbDataSetFactory) {
		super(orchestraSchema, testDataDirectory, dumpDatasets, dbTester,
				bdbDataSetFactory);
		this.window = window;
	}

	/**
	 * The allowed values of {@code operationName} are:
	 * <dl>
	 * <dt>create</dt>
	 * <dd>Creates initial database tables.</dd>
	 * <dt>migrate</dt>
	 * <dd>Migrates existing database schema to Orchestra system.</dd>
	 * <dt>import</dt>
	 * <dd>Imports data from delimited files.</dd>
	 * <dt>publish</dt>
	 * <dd>Publishes data to update store. {@code peerName} is required.</dd>
	 * <dt>reconcile</dt>
	 * <dd>Update exchange and reconciliation. {@code peerName} is required.</dd>
	 * </dl>
	 * 
	 */
	@Override
	public IOrchestraOperation operation(String operationName, String peerName,
			File datasetFile) {
		if (operationName.equalsIgnoreCase("create")) {
			return new CreateOperation(datasetFile);
		} else if (operationName.equalsIgnoreCase("migrate")) {
			return new MigrateOperation(datasetFile);
		} else if (operationName.equalsIgnoreCase("import")) {
			return new ImportOperation(datasetFile);
		} else if (operationName.equalsIgnoreCase("publish")) {
			return new PublishOperation(datasetFile, peerName);
		} else if (operationName.equalsIgnoreCase("reconcile")) {
			return new ReconcileOperation(datasetFile, peerName);
		} else {
			throw new IllegalStateException("Unrecognized operation: ["
					+ operationName
					+ ((peerName == null) ? "" : "-" + peerName)
					+ "] from file: " + datasetFile);
		}
	}
}
