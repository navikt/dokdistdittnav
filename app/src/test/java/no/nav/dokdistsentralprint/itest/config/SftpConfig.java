package no.nav.dokdistsentralprint.itest.config;


import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.AcceptAllPublickeyAuthenticator;
import org.apache.sshd.server.auth.pubkey.UserAuthPublicKeyFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
public final class SftpConfig {

	public static SshServer startSshServer(Path tempDir) {
		SshServer sshServer = SshServer.setUpDefaultServer();
		sshServer.setPort(0); //random port
		sshServer.setHost("localhost");
		sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("hostkey.ser")));
		sshServer.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
		sshServer.setPublickeyAuthenticator(AcceptAllPublickeyAuthenticator.INSTANCE);
		sshServer.setUserAuthFactories(Collections.singletonList(new UserAuthPublicKeyFactory()));
		sshServer.setFileSystemFactory(new VirtualFileSystemFactory(tempDir));
		try {
			sshServer.start();
			return sshServer;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

}
