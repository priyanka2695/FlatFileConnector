package org.identityconnectors.flatfileconnector;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;

import com.jscape.inet.ssh.SshException;
import com.jscape.inet.ssh.SshSession;
@ConnectorClass(configurationClass=FlatFileConnectorConfiguration.class, displayNameKey="connector.display")
public class FlatFileConnector implements PoolableConnector {

	private SshSession session;
	private FlatFileConnectorConfiguration config;
	private boolean isAlive;
	
	@Override
	public void init(Configuration cfg) {
		//get the config object
		this.config=(FlatFileConnectorConfiguration)cfg;
		//connection eshtablishment to target system
		createSshConnection();
		System.out.println("Successfully connected to the target system"+this.config.getHostName()+"with username"+this.config.getUserName());
        //connection is now alive
		this.isAlive=true;
	}
	
	private void createSshConnection() {
		SshSession returnSession = null;
        FlatFileConnector ffc = this;
        this.config.getPassword().access(new GuardedString.Accessor() {        
                @Override
                public void access(char[] arg0) {
                    try {
                        session = new SshSession(config.getHostName(),config.getUserName(),new String(arg0));
                    } catch (SshException e) {
                        throw new RuntimeException(e);
                    }
                }
        }); 
        System.out.println("Successfully connected to the target system "+this.config.getHostName()+" with user name "+this.config.getUserName());
    }
	
	@Override
	public void dispose() {
		//disconnect the connection
		this.session.disconnect();
		this.isAlive=false;
	}

	@Override
	public Configuration getConfiguration() {
		 return this.config;
	}

		
	@Override
	public void checkAlive() {
		if(!isAlive){
            throw new IllegalStateException("This connector is dead!!");

	}
	}
	
}
