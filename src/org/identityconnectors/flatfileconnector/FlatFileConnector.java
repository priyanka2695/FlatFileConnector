package org.identityconnectors.flatfileconnector;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.flatfileconnector.util.FlatFileUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.Uid;
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
	
    public Uid create(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
        Uid returnUid = null;                          
        File targetFile = this.config.getTargetFile();        
        String uidAttribute = this.config.getUniqueAttribute();       
        File customScriptFile = this.config.getCustomScriptForProvisioning();
        // if customScriptFile is not null, means the user provided a custom script when the IT resource was created in OIM advanced console,
        // use this for provisioning.
        if(customScriptFile != null){               
            String id=""; String lastName=""; String firstName=""; String email="";
            for(Attribute attr:attrs){
                if(attr.getName().equalsIgnoreCase("AccountId"))
                    id = attr.getValue().get(0).toString();
                if(attr.getName().equalsIgnoreCase("lastName"))
                    lastName = attr.getValue().get(0).toString();
                if(attr.getName().equalsIgnoreCase("firstName"))
                    firstName = attr.getValue().get(0).toString();
                if(attr.getName().equalsIgnoreCase("email"))
                    email = attr.getValue().get(0).toString();            
            }              
            ProcessBuilder pb = new ProcessBuilder(customScriptFile.getAbsolutePath(),id,firstName,lastName,email);
            BufferedReader bufferedReader = null;
            try {        
                pb = pb.redirectErrorStream(true);
                Process process = pb.start();                
                InputStream inputStream = process.getInputStream();
               InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
              bufferedReader  = new BufferedReader(inputStreamReader);
               String line = null;
                StringBuilder sb = new StringBuilder();
               while ((line = bufferedReader.readLine()) != null) {
                 sb.append(line);
                 sb.append("\n");
               }
                if(sb.length() > 0){
                    String response = sb.toString();                          
                    if (response.contains(FlatFileUtil.ERROR_MESSAGE)){
                        throw new RuntimeException("Error while provisioning "+sb);
                    }
                }                
            } catch (IOException e) {
                throw new RuntimeException("Error while executing script "+customScriptFile+". Error is "+e.getMessage());
            }            
            returnUid = FlatFileUtil.createUid(attrs, uidAttribute);
        }else{               
            if((!FlatFileUtil.accountExists(attrs, targetFile))){                    
                    returnUid = FlatFileUtil.createAccount(attrs,targetFile,uidAttribute);
            }     
        }return returnUid;      
    }
    
   
    public Schema schema() {
        Set<AttributeInfo> attrInfoSet = new HashSet<AttributeInfo>();
        attrInfoSet.add(AttributeInfoBuilder.build(FlatFileUtil.AccountId, String.class));
        attrInfoSet.add(AttributeInfoBuilder.build(FlatFileUtil.FirstName, String.class));
        attrInfoSet.add(AttributeInfoBuilder.build(FlatFileUtil.lastName, String.class));
        attrInfoSet.add(AttributeInfoBuilder.build(FlatFileUtil.email, String.class));
        SchemaBuilder schemaBld = new SchemaBuilder(FlatFileConnector.class);
        schemaBld.defineObjectClass(ObjectClass.ACCOUNT_NAME, attrInfoSet);
        Schema schema = schemaBld.build();
        return schema;
    }

    public void test() {
        createSshConnection();
    }
}
	

