package edu.upenn.cis.orchestra.gui;

import java.io.File;
import java.net.URI;

import org.dbunit.JdbcDatabaseTester;
import org.fest.swing.core.Robot;
import org.fest.swing.fixture.FrameFixture;

import edu.upenn.cis.orchestra.Config;
import edu.upenn.cis.orchestra.OrchestraSchema;
import edu.upenn.cis.orchestra.OrchestraTestFrame;

/**
 * These are the test framework items which we need on a per GUI basis.
 * 
 * @author John Frommeyer
 * 
 */
public class OrchestraGUITestFrame {
	/** The test frame */
	private final OrchestraTestFrame testFrame;
	private final FrameFixture window;

	OrchestraGUITestFrame(OrchestraSchema orchestraSchema,
			OrchestraTestFrame orchestraTestFrame, Robot robot) throws Exception {
		testFrame = orchestraTestFrame;
		// We need to write out the schema file without clashing with the other
		// GUIs which will be created. So we temporarily change the name of the
		// schema.
		String oldSchemaName = Config.getTestSchemaName();
		String tempSchemaName = oldSchemaName + "." + testFrame.getPeerName();
		Config.setTestSchemaName(tempSchemaName);
		File file = new File(new URI(Config.getSchemaFile()));
		orchestraSchema.write(file, testFrame.getDbURL(),
				testFrame.getDbUser(), testFrame.getDbPassword(), testFrame
						.getPeerName());
		
		window = GUITestUtils.launchOrchestra(robot, tempSchemaName);
		Config.setTestSchemaName(oldSchemaName);
	}

	/**
	 * @return the window
	 */
	public FrameFixture getWindowFrameFixture() {
		return window;
	}

	/**
	 * Returns the appropriate {@code JdbcDatabaseTester} for the underlying
	 * {@code OrchestraSystem}.
	 * 
	 * @return the appropriate {@code JdbcDatabaseTester} for the underlying
	 *         {@code OrchestraSystem}
	 */
	JdbcDatabaseTester getDbTester() {
		return testFrame.getDbTester();
	}

	/**
	 * Returns the {@code OrchestraTestFrame}.
	 * 
	 * @return the {@code OrchestraTestFrame}
	 */
	OrchestraTestFrame getOrchestraTestFrame() {
		return testFrame;
	}
}
